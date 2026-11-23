package id.ac.loopy.feature.clothingcheck

import kotlin.math.abs

internal enum class ClothingPhotoIssue(val message: String) {
    NO_VISIBLE_CONTENT("Foto tidak menampilkan gambar. Silakan pilih foto lain."),
    TOO_DARK("Foto terlalu gelap. Ambil ulang di tempat yang lebih terang."),
    LOW_DETAIL("Detail foto kurang jelas. Ambil ulang dan pastikan pakaian terlihat fokus."),
}

private const val DARK_LUMINANCE = 32
private const val EDGE_CONTRAST = 12
private const val BROAD_EDGE_RADIUS = 3

/** A conservative quality check on an already resized ARGB image. */
internal fun findClothingPhotoIssue(pixels: IntArray, width: Int, height: Int): ClothingPhotoIssue? {
    var visiblePixels = 0
    var darkPixels = 0
    val luminance = IntArray(pixels.size) { index ->
        val pixel = pixels[index]
        if (pixel ushr 24 < 128) {
            -1
        } else {
            visiblePixels++
            val red = (pixel ushr 16) and 255
            val green = (pixel ushr 8) and 255
            val blue = pixel and 255
            val level = (54 * red + 183 * green + 19 * blue) / 256
            if (level < DARK_LUMINANCE) darkPixels++
            level
        }
    }
    if (visiblePixels == 0) return ClothingPhotoIssue.NO_VISIBLE_CONTENT
    if (darkPixels.toDouble() / visiblePixels >= 0.98) return ClothingPhotoIssue.TOO_DARK

    var fineEdges = 0
    var broadEdges = 0
    val radius = BROAD_EDGE_RADIUS
    for (y in radius until height - radius) {
        for (x in radius until width - radius) {
            val center = y * width + x
            if (luminance[center] < 0) continue
            if (luminance.hasEdge(center, width, 1)) fineEdges++
            if (luminance.hasEdge(center, width, radius)) broadEdges++
        }
    }
    // Smooth transitions with almost no sharp edges suggest heavy blur. Flat areas alone are inconclusive.
    return if (broadEdges.toDouble() / visiblePixels >= 0.05 && fineEdges.toDouble() / visiblePixels < 0.005) {
        ClothingPhotoIssue.LOW_DETAIL
    } else {
        null
    }
}

private fun IntArray.hasEdge(center: Int, width: Int, radius: Int): Boolean {
    return contrast(center - radius, center + radius) >= EDGE_CONTRAST ||
        contrast(center - radius * width, center + radius * width) >= EDGE_CONTRAST
}

private fun IntArray.contrast(first: Int, second: Int): Int {
    val a = this[first]
    val b = this[second]
    return if (a >= 0 && b >= 0) abs(a - b) else 0
}
