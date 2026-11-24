package id.ac.loopy.feature.clothingcheck

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import id.ac.loopy.domain.model.ClothingType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

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

    @Test fun unclearPhotoStaysVisibleAndCanBeReplaced() {
        val message = ClothingPhotoIssue.TOO_DARK.message
        val unclear = loadTestPhoto { 18 }
        val replacement = loadTestPhoto { x -> if (x < 120) 20 else 180 }
        assertEquals(message, unclear.error)
        assertEquals(null, replacement.error)
        compose.setContent {
            var state by remember { mutableStateOf(unclear) }
            MaterialTheme {
                ClothingDetectorContent(
                    state = state,
                    onBack = {},
                    onTakePhoto = {},
                    onPickPhoto = { state = replacement },
                    onAnalyze = {},
                )
            }
        }
        compose.onNodeWithText(message).assertExists()
        compose.onNodeWithContentDescription("Pratinjau pakaian yang dipilih").assertExists()
        compose.onNodeWithText("Periksa pakaian").assertIsNotEnabled()
        compose.onNodeWithText("Ambil foto").assertIsEnabled()
        compose.onNodeWithText("Pilih galeri").performScrollTo().performClick()
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.onNodeWithText("Periksa pakaian").assertIsEnabled()
    }

    private fun loadTestPhoto(shade: (Int) -> Int): ClothingDetectorUiState {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "clothing_photos").apply { mkdirs() }
        val photo = File.createTempFile("photo_quality_", ".png", directory)
        val size = 160
        val pixels = IntArray(size * size) { index ->
            val level = shade(index % size)
            (255 shl 24) or (level shl 16) or (level shl 8) or level
        }
        val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        try {
            photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", photo)
            return runBlocking { loadClothingPhotoForAnalysis(context, uri) }
        } finally {
            bitmap.recycle()
            photo.delete()
        }
    }

    @Test fun clothingTypeSelectionUpdatesLabelAndBackendValue() {
        var selected = ClothingType.TSHIRT
        compose.setContent {
            var state by remember { mutableStateOf(ClothingDetectorUiState()) }
            MaterialTheme {
                ClothingDetectorContent(state, {}, {}, {}, {}, onClothingTypeChange = {
                    selected = it
                    state = state.copy(clothingType = it)
                })
            }
        }
        compose.onNodeWithText("Kaos").performScrollTo().performClick()
        compose.onNodeWithText("Kemeja").performClick()
        compose.onNodeWithText("Kemeja").assertExists()
        compose.runOnIdle { assertEquals("SHIRT", selected.backendValue) }
    }
}
