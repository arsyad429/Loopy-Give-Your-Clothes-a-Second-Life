package id.ac.loopy.feature.clothingcheck

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClothingDetectorScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sampleDoesNotEnableAnalysisAndPhotoSourcesCallTheirCallbacks() {
        var camera = 0
        var gallery = 0
        var back = 0
        compose.setContent {
            MaterialTheme {
                ClothingDetectorContent(ClothingDetectorUiState(), { back++ }, { camera++ }, { gallery++ }, {})
            }
        }
        compose.onNodeWithText("Periksa pakaian").assertIsNotEnabled()
        compose.onNodeWithText("Ambil foto").performClick()
        compose.onNodeWithText("Pilih galeri").performClick()
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.runOnIdle { assertEquals(1, camera); assertEquals(1, gallery); assertEquals(1, back) }
    }

    @Test fun validPhotoEnablesAnalysisAndShowsItsName() {
        var analyses = 0
        compose.setContent {
            MaterialTheme {
                ClothingDetectorContent(
                    ClothingDetectorUiState(photo = ImageBitmap(16, 16), photoName = "pakaian.jpg"),
                    {}, {}, {}, { analyses++ },
                )
            }
        }
        compose.onNodeWithText("pakaian.jpg").assertExists()
        compose.onNodeWithText("Periksa pakaian").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, analyses) }
    }

    @Test fun loadingBlocksPhotoSourcesAndAnalysis() {
        compose.setContent {
            MaterialTheme { ClothingDetectorContent(ClothingDetectorUiState(isLoading = true), {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Ambil foto").assertIsNotEnabled()
        compose.onNodeWithText("Pilih galeri").assertIsNotEnabled()
        compose.onNodeWithText("Periksa pakaian").assertIsNotEnabled()
    }
}
