package id.ac.loopy.server.checks

import id.ac.loopy.server.auth.LoopyPrincipal
import id.ac.loopy.server.auth.UserRole
import id.ac.loopy.server.storage.PhotoStorage
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.sql.SQLException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class CheckProblem(
    val status: HttpStatusCode,
    val code: String,
    message: String,
    val retryAfterSeconds: Long? = null,
) : RuntimeException(message)

internal data class CheckUpload(
    val clothingType: String,
    val mime: ContentType,
    val bytes: ByteArray,
)

internal data class CheckCreation(
    val check: StoredClothingCheck,
    val isNew: Boolean,
)

private val supportedClothingTypes = setOf(
    "TSHIRT", "SHIRT", "POLO", "BLOUSE", "TANK_TOP", "SWEATER", "HOODIE",
    "JACKET", "CARDIGAN", "LONG_PANTS", "SHORTS", "JEANS", "JOGGER",
    "SKIRT", "DRESS", "GAMIS", "TUNIC", "SPORTSWEAR", "SLEEPWEAR", "OTHER",
)

private val requestKeyFormat = Regex("[A-Za-z0-9._-]{1,128}")

internal class ClothingCheckService(
    private val gateway: AiVisionGateway,
    private val store: ClothingCheckStore,
    private val photos: PhotoStorage,
) {
    suspend fun create(actor: LoopyPrincipal, key: String, upload: CheckUpload): CheckCreation {
        if (actor.role != UserRole.USER) {
            throw CheckProblem(HttpStatusCode.Forbidden, "USER_REQUIRED", "Gunakan akun donatur.")
        }
        if (!requestKeyFormat.matches(key)) {
            throw CheckProblem(
                HttpStatusCode.BadRequest, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key tidak valid.",
            )
        }
        if (upload.clothingType !in supportedClothingTypes) {
            throw CheckProblem(
                HttpStatusCode.BadRequest, "INVALID_CLOTHING_TYPE", "Jenis pakaian tidak dikenali.",
            )
        }

        val hash = withContext(Dispatchers.Default) {
            MessageDigest.getInstance("SHA-256").apply {
                update(upload.clothingType.encodeToByteArray())
                update(0.toByte())
                update(upload.mime.toString().encodeToByteArray())
                update(0.toByte())
            }.digest(upload.bytes)
        }

        return when (val claim = store.claim(actor, key, hash)) {
            is CheckClaim.Completed -> CheckCreation(find(actor, claim.checkId), isNew = false)
            CheckClaim.PayloadMismatch -> throw CheckProblem(
                HttpStatusCode.Conflict, "IDEMPOTENCY_CONFLICT",
                "Key ini sudah digunakan untuk foto atau jenis pakaian berbeda.",
            )
            CheckClaim.InProgress -> throw CheckProblem(
                HttpStatusCode.Conflict, "CHECK_IN_PROGRESS", "Pemeriksaan masih berjalan.", 2,
            )
            is CheckClaim.Acquired -> complete(actor, claim, upload)
        }
    }

    suspend fun find(actor: LoopyPrincipal, id: UUID): StoredClothingCheck =
        store.findFor(actor, id) ?: notFound()

    suspend fun photo(actor: LoopyPrincipal, id: UUID): ByteArray =
        photos.findFor(actor, id) ?: notFound()

    private suspend fun complete(
        actor: LoopyPrincipal,
        claim: CheckClaim.Acquired,
        upload: CheckUpload,
    ): CheckCreation {
        var saved = false

        try {
            val jpeg = withContext(Dispatchers.Default) {
                normalizeCheckPhoto(upload.bytes, upload.mime)
            }
            val analysis = analyze(jpeg, upload.clothingType)
            val id = store.complete(claim, upload.clothingType, analysis, jpeg)
            saved = true
            return CheckCreation(find(actor, id), isNew = true)
        } catch (_: CheckClaimLostException) {
            throw CheckProblem(
                HttpStatusCode.Conflict, "CHECK_RESERVATION_EXPIRED",
                "Reservasi pemeriksaan berakhir. Coba lagi dengan key yang sama.",
            )
        } finally {
            if (!saved) {
                withContext(NonCancellable) {
                    try {
                        store.release(claim)
                    } catch (_: SQLException) {
                        // The reservation expires automatically if the database is unavailable.
                    }
                }
            }
        }
    }

    private suspend fun analyze(jpeg: ByteArray, clothingType: String): AiVisionResult =
        withTimeoutOrNull(25_000) {
            try {
                gateway.analyze(jpeg, clothingType)
            } catch (failure: AiVisionException) {
                val retry = failure.retryAfterSeconds
                if (failure.failure != AiVisionFailure.RATE_LIMITED || retry == null || retry !in 0L..2L) {
                    throw failure
                }
                delay(retry * 1_000)
                gateway.analyze(jpeg, clothingType)
            }
        } ?: throw AiVisionException(AiVisionFailure.TIMEOUT)

    private fun notFound(): Nothing = throw CheckProblem(
        HttpStatusCode.NotFound, "CHECK_NOT_FOUND", "Hasil pemeriksaan tidak ditemukan.",
    )
}
