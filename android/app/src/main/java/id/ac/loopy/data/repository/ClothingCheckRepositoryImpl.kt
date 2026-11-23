package id.ac.loopy.data.repository

import android.content.Context
import android.net.Uri
import id.ac.loopy.data.remote.api.ClothingCheckApi
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.repository.ClothingCheckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.source
import java.security.MessageDigest
import java.util.UUID

class ClothingCheckRepositoryImpl(
    context: Context,
    private val api: ClothingCheckApi,
    private val auth: AuthRepositoryImpl,
) : ClothingCheckRepository {
    private val resolver = context.applicationContext.contentResolver
    private val uploadMutex = Mutex()
    private var pending: PendingUpload? = null

    private data class PendingUpload(
        val userId: String,
        val type: ClothingType,
        val mime: String,
        val digest: List<Byte>,
        val key: String = UUID.randomUUID().toString(),
    )

    override suspend fun analyzeClothing(
        photoUri: String,
        clothingType: ClothingType,
    ): ClothingCheck = uploadMutex.withLock {
        val userId = auth.session.value?.user?.userId ?: error("Silakan masuk.")
        val uri = Uri.parse(photoUri)

        val (mime, bytes) = withContext(Dispatchers.IO) {
            val type = resolver.getType(uri)
            require(type == "image/jpeg" || type == "image/png") {
                "Gunakan JPG atau PNG."
            }

            val data = resolver.openInputStream(uri)?.source()?.buffer()?.use { input ->
                require(!input.request(10L * 1024 * 1024 + 1)) {
                    "Ukuran foto maksimal 10 MiB."
                }
                input.readByteArray()
            } ?: error("Foto tidak dapat dibaca.")

            require(data.isNotEmpty()) { "Foto kosong." }
            type to data
        }

        val digest = withContext(Dispatchers.Default) {
            MessageDigest.getInstance("SHA-256").digest(bytes).toList()
        }
        val previous = pending
        val upload = if (previous != null &&
            previous.userId == userId && previous.type == clothingType &&
            previous.mime == mime && previous.digest == digest
        ) {
            previous
        } else {
            PendingUpload(userId, clothingType, mime, digest)
        }
        pending = upload

        val extension = if (mime == "image/png") "png" else "jpg"
        val image = MultipartBody.Part.createFormData(
            "image",
            "pakaian.$extension",
            bytes.toRequestBody(mime.toMediaType()),
        )
        val type = clothingType.backendValue.toRequestBody("text/plain".toMediaType())

        auth.withBearer { bearer ->
            api.analyze(bearer, upload.key, type, image).toDomain()
        }.also { pending = null }
    }

    override suspend fun getCheckById(checkId: String): ClothingCheck =
        auth.withBearer { api.detail(it, checkId).toDomain() }

    suspend fun downloadPhoto(checkId: String): ByteArray =
        auth.withBearer { bearer ->
            api.photo(bearer, checkId).use { body ->
                withContext(Dispatchers.IO) {
                    val source = body.source()
                    require(!source.request(2L * 1024 * 1024 + 1)) {
                        "Foto hasil terlalu besar."
                    }
                    source.readByteArray()
                }
            }
        }

    override fun observeCheck(checkId: String) = flowOf<ClothingCheck?>(null)
}
