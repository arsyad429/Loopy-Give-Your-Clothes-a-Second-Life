package id.ac.loopy.feature.clothingcheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClothingPhotoQualityTest {
    @Test
    fun rejectsDarkImageEvenWithAFewBrightPixels() {
        val pixels = image { x, y -> if (x < 4 && y < 4) 255 else 18 }

        assertEquals(ClothingPhotoIssue.TOO_DARK, findClothingPhotoIssue(pixels, SIZE, SIZE))
    }

    @Test
    fun rejectsTransparentImage() {
        val pixels = IntArray(SIZE * SIZE) { 0x00FFFFFF }

        assertEquals(ClothingPhotoIssue.NO_VISIBLE_CONTENT, findClothingPhotoIssue(pixels, SIZE, SIZE))
    }

    @Test
    fun rejectsBroadSoftTransitionsWithoutSharpEdges() {
        val pixels = image { x, _ -> 70 + ((x - 42).coerceIn(0, 44) * 5) / 2 }

        assertEquals(ClothingPhotoIssue.LOW_DETAIL, findClothingPhotoIssue(pixels, SIZE, SIZE))
    }

    @Test
    fun acceptsDarkFabricAgainstABrightBackground() {
        val pixels = image { x, _ -> if (x < 95) 20 else 180 }

        assertNull(findClothingPhotoIssue(pixels, SIZE, SIZE))
    }

    @Test
    fun permitsWellLitLowTexturePhotoWhenFocusIsInconclusive() {
        val pixels = image { _, _ -> 170 }

        assertNull(findClothingPhotoIssue(pixels, SIZE, SIZE))
    }

    private fun image(shade: (x: Int, y: Int) -> Int) = IntArray(SIZE * SIZE) { index ->
        val level = shade(index % SIZE, index / SIZE)
        (255 shl 24) or (level shl 16) or (level shl 8) or level
    }

    private companion object {
        const val SIZE = 128
    }
}
