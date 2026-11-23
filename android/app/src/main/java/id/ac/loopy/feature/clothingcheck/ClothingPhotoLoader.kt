package id.ac.loopy.feature.clothingcheck

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val MAX_PHOTO_BYTES = 10L * 1024 * 1024
private const val PREVIEW_MAX_DIMENSION = 1600
private const val QUALITY_SAMPLE_DIMENSION = 128
private val resultPhotoClient by lazy {
    OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .build()
}

/** Local policy: JPEG/PNG <= 10 MiB. Agree this limit with the backend before integration. */
private suspend fun loadClothingPhoto(context: Context, uri: Uri): ClothingDetectorUiState = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    require(resolver.getType(uri) in setOf("image/jpeg", "image/png")) { "Gunakan foto berformat JPG atau PNG." }
    resolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_PHOTO_BYTES) { "Ukuran foto maksimal 10 MB. Silakan pilih foto yang lebih kecil." }
        }
        require(total > 0) { "File foto kosong. Silakan pilih foto lain." }
    } ?: error("Foto tidak dapat diakses. Silakan pilih ulang.")
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    require(options.outWidth > 0 && options.outHeight > 0) { "File ini tidak dapat dibaca sebagai foto." }
    options.preparePreviewDecode()
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Foto tidak dapat dibaca. Silakan pilih ulang.")
    val orientation = resolver.openInputStream(uri)?.use(::readOrientation)
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "Foto pakaian"
    ClothingDetectorUiState(photo = bitmap.oriented(orientation).asImageBitmap(), photoName = name)
}

/** S05 preflight. Keep the preview so the user can see which photo needs replacing. */
internal suspend fun loadClothingPhotoForAnalysis(context: Context, uri: Uri): ClothingDetectorUiState {
    val state = loadClothingPhoto(context, uri)
    val photo = checkNotNull(state.photo).asAndroidBitmap()
    val issue = withContext(Dispatchers.Default) {
        val scale = (QUALITY_SAMPLE_DIMENSION.toFloat() / maxOf(photo.width, photo.height)).coerceAtMost(1f)
        val width = (photo.width * scale).roundToInt().coerceAtLeast(1)
        val height = (photo.height * scale).roundToInt().coerceAtLeast(1)
        val sample = photo.scale(width, height)
        try {
            val pixels = IntArray(width * height)
            sample.getPixels(pixels, 0, width, 0, 0, width, height)
            findClothingPhotoIssue(pixels, width, height)
        } finally {
            if (sample !== photo) sample.recycle()
        }
    }
    return state.copy(error = issue?.message)
}

/** The repository supplies a local content URI or a short-lived HTTPS URL for a private photo. */
internal suspend fun loadCheckResultPhoto(context: Context, reference: String): ImageBitmap? {
    val uri = reference.toUri()
    if (uri.scheme == "content") return loadClothingPhoto(context, uri).photo
    val url = reference.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: return null

    return withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val bytes = resultPhotoClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null || body.contentLength() > MAX_PHOTO_BYTES) {
                return@withContext null
            }
            val source = body.source()
            if (source.request(MAX_PHOTO_BYTES + 1)) return@withContext null
            source.readByteArray()
        }
        currentCoroutineContext().ensureActive()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
        options.preparePreviewDecode()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: return@withContext null
        val orientation = ByteArrayInputStream(bytes).use(::readOrientation)
        bitmap.oriented(orientation).asImageBitmap()
    }
}

private fun BitmapFactory.Options.preparePreviewDecode() {
    inJustDecodeBounds = false
    inSampleSize = 1
    while (maxOf(outWidth, outHeight) / inSampleSize > PREVIEW_MAX_DIMENSION) inSampleSize *= 2
}

private fun readOrientation(input: InputStream): Int = runCatching {
    ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun Bitmap.oriented(orientation: Int?): Bitmap {
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }
    if (matrix.isIdentity) return this
    val upright = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (upright !== this) recycle()
    return upright
}
