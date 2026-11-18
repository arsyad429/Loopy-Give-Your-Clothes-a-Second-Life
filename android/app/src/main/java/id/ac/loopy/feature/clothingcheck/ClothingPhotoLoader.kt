package id.ac.loopy.feature.clothingcheck

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Local policy: JPEG/PNG <= 10 MiB. Agree this limit with the backend before integration. */
internal suspend fun loadClothingPhoto(context: Context, uri: Uri): ClothingDetectorUiState = withContext(Dispatchers.IO) {
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
            require(total <= 10L * 1024 * 1024) { "Ukuran foto maksimal 10 MB. Silakan pilih foto yang lebih kecil." }
        }
        require(total > 0) { "File foto kosong. Silakan pilih foto lain." }
    } ?: error("Foto tidak dapat diakses. Silakan pilih ulang.")
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    require(options.outWidth > 0 && options.outHeight > 0) { "File ini tidak dapat dibaca sebagai foto." }
    val width = options.outWidth
    val height = options.outHeight
    options.inJustDecodeBounds = false
    options.inSampleSize = 1
    while (width / options.inSampleSize > 1600 || height / options.inSampleSize > 1600) options.inSampleSize *= 2
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Foto tidak dapat dibaca. Silakan pilih ulang.")
    val orientation = resolver.openInputStream(uri)?.use {
        runCatching { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }
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
    val upright = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (upright !== bitmap) bitmap.recycle()
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "Foto pakaian"
    ClothingDetectorUiState(photo = upright.asImageBitmap(), photoName = name)
}
