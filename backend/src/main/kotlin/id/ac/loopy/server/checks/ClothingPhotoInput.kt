package id.ac.loopy.server.checks

import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.imaging.jpeg.JpegProcessingException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifReader
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.IIOImage
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.imageio.stream.MemoryCacheImageOutputStream

internal const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024
private const val MAX_INPUT_SIDE = 12_000
private const val MAX_INPUT_PIXELS = 50_000_000L
private const val MAX_OUTPUT_SIDE = 1_600

internal fun normalizeCheckPhoto(bytes: ByteArray, mime: ContentType): ByteArray {
    if (bytes.isEmpty()) invalidPhoto()
    if (bytes.size > MAX_UPLOAD_BYTES) {
        throw CheckProblem(HttpStatusCode.PayloadTooLarge, "PHOTO_TOO_LARGE", "Ukuran foto maksimal 10 MiB.")
    }

    val expectedFormat = when (mime) {
        ContentType.Image.JPEG -> "jpeg"
        ContentType.Image.PNG -> "png"
        else -> unsupportedPhoto()
    }

    val decoded = try {
        MemoryCacheImageInputStream(ByteArrayInputStream(bytes)).use { source ->
            val readers = ImageIO.getImageReaders(source)
            if (!readers.hasNext()) invalidPhoto()

            val reader = readers.next()
            try {
                if (!reader.formatName.equals(expectedFormat, ignoreCase = true)) {
                    unsupportedPhoto()
                }

                reader.setInput(source, true, true)
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width < 1 || height < 1 ||
                    maxOf(width, height) > MAX_INPUT_SIDE ||
                    width.toLong() * height > MAX_INPUT_PIXELS
                ) {
                    throw CheckProblem(
                        HttpStatusCode.UnprocessableEntity,
                        "INVALID_PHOTO_DIMENSIONS",
                        "Dimensi foto terlalu besar atau tidak valid.",
                    )
                }

                val sample = (maxOf(width, height) + MAX_OUTPUT_SIDE - 1) / MAX_OUTPUT_SIDE
                val params = reader.defaultReadParam.apply {
                    setSourceSubsampling(sample, sample, 0, 0)
                }
                reader.read(0, params) ?: invalidPhoto()
            } finally {
                reader.dispose()
            }
        }
    } catch (_: IOException) {
        invalidPhoto()
    }

    val rgb = try {
        val orientation = if (expectedFormat == "jpeg") readJpegOrientation(bytes) else 1
        toOrientedRgb(decoded, orientation)
    } finally {
        decoded.flush()
    }

    try {
        for (quality in listOf(0.86f, 0.72f, 0.55f)) {
            val jpeg = writeJpeg(rgb, quality)
            if (jpeg.size <= MAX_GROQ_JPEG_BYTES) return jpeg
        }
    } finally {
        rgb.flush()
    }

    throw CheckProblem(
        HttpStatusCode.UnprocessableEntity,
        "PHOTO_CANNOT_BE_NORMALIZED",
        "Foto belum bisa diproses. Gunakan foto yang lebih kecil.",
    )
}

private fun readJpegOrientation(bytes: ByteArray): Int = try {
    JpegMetadataReader.readMetadata(ByteArrayInputStream(bytes), listOf(ExifReader()))
        .getFirstDirectoryOfType(ExifIFD0Directory::class.java)
        ?.getInteger(ExifIFD0Directory.TAG_ORIENTATION)
        ?.takeIf { it in 1..8 } ?: 1
} catch (_: IOException) {
    1
} catch (_: JpegProcessingException) {
    1
}

private fun toOrientedRgb(source: BufferedImage, orientation: Int): BufferedImage {
    val w = source.width.toDouble()
    val h = source.height.toDouble()
    val transform = when (orientation) {
        2 -> AffineTransform(-1.0, 0.0, 0.0, 1.0, w, 0.0)
        3 -> AffineTransform(-1.0, 0.0, 0.0, -1.0, w, h)
        4 -> AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, h)
        5 -> AffineTransform(0.0, 1.0, 1.0, 0.0, 0.0, 0.0)
        6 -> AffineTransform(0.0, 1.0, -1.0, 0.0, h, 0.0)
        7 -> AffineTransform(0.0, -1.0, -1.0, 0.0, h, w)
        8 -> AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, w)
        else -> AffineTransform()
    }
    val swap = orientation in 5..8
    val image = BufferedImage(
        if (swap) source.height else source.width,
        if (swap) source.width else source.height,
        BufferedImage.TYPE_INT_RGB,
    )

    val graphics = image.createGraphics()
    try {
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.drawImage(source, transform, null)
    } finally {
        graphics.dispose()
    }
    return image
}

private fun writeJpeg(image: BufferedImage, quality: Float): ByteArray {
    val bytes = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()

    try {
        MemoryCacheImageOutputStream(bytes).use { output ->
            writer.output = output
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality
            }
            writer.write(null, IIOImage(image, null, null), params)
        }
    } finally {
        writer.dispose()
    }

    return bytes.toByteArray()
}

private fun invalidPhoto(): Nothing = throw CheckProblem(
    HttpStatusCode.UnprocessableEntity, "INVALID_PHOTO", "Foto tidak bisa dibaca.",
)

private fun unsupportedPhoto(): Nothing = throw CheckProblem(
    HttpStatusCode.UnsupportedMediaType, "UNSUPPORTED_PHOTO", "Gunakan file JPEG atau PNG yang valid.",
)
