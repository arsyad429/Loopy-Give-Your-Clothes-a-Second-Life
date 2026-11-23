package id.ac.loopy.feature.clothingcheck

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.repository.ClothingCheckRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class CheckResultScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun eligibleResultShowsPhotoAndRequiresConfirmationBeforePassingCheckId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = File(context.cacheDir, "clothing_photos").apply { mkdirs() }
        val file = File.createTempFile("result_test_", ".png", folder)
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val check = inspection(AiResult.POTENTIALLY_ELIGIBLE).copy(clothingPhoto = uri.toString())
            var pickupId: String? = null
            val repository = object : ClothingCheckRepository {
                override suspend fun analyzeClothing(photoUri: String, clothingType: ClothingType): ClothingCheck =
                    error("Pemeriksaan baru tidak dipanggil dari S06")

                override suspend fun getCheckById(checkId: String): ClothingCheck {
                    assertEquals(check.checkId, checkId)
                    return check
                }

                override fun observeCheck(checkId: String) = flowOf(check)
            }
            compose.setContent {
                MaterialTheme {
                    val model = remember { CheckResultViewModel(check.checkId, repository) }
                    CheckResultScreen(model, onBack = {}, onRecheck = {}, onPickup = { pickupId = it })
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Foto pakaian yang diperiksa")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Foto pakaian yang diperiksa").assertExists()
            compose.onNodeWithText("Jenis pakaian: Kemeja").assertExists()
            val pickup = compose.onNodeWithText("Ajukan penjemputan")
            val confirmation = compose.onNodeWithText("Saya memastikan pakaian bersih dan aman digunakan.")
            pickup.assertIsNotEnabled()
            confirmation.performScrollTo().assertIsOff().performClick().assertIsOn()
            pickup.assertIsEnabled()
            confirmation.performClick().assertIsOff()
            pickup.assertIsNotEnabled()
            confirmation.performClick()
            pickup.performClick()
            compose.runOnIdle { assertEquals(check.checkId, pickupId) }
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    @Test
    fun needsRecheckOffersRetakeWithoutPickup() {
        verifyRetake(AiResult.NEEDS_RECHECK, "Perlu foto ulang", "Ambil foto di tempat terang.")
    }

    @Test
    fun notRecommendedShowsAdviceWithoutPickup() {
        verifyRetake(AiResult.NOT_RECOMMENDED, "Tidak direkomendasikan", "Pertimbangkan perbaikan atau daur ulang.")
    }

    @Test
    fun retryReplacesErrorWithLoadingWithoutShowingPickup() {
        compose.setContent {
            var state by remember { mutableStateOf(CheckResultUiState(error = "Hasil pemeriksaan gagal dimuat.")) }
            MaterialTheme {
                CheckResultContent(
                    state = state,
                    onBack = {},
                    onRecheck = {},
                    onPickup = {},
                    onRetry = { state = CheckResultUiState(isLoading = true) },
                    onConfirmationChange = {},
                )
            }
        }
        compose.onNodeWithText("Hasil pemeriksaan gagal dimuat.").assertExists()
        compose.onNodeWithText("Ajukan penjemputan").assertDoesNotExist()
        compose.onNodeWithText("Coba lagi").performClick()
        compose.onNodeWithText("Memuat hasil pemeriksaan…").assertExists()
        compose.onNodeWithText("Coba lagi").assertDoesNotExist()
    }

    private fun verifyRetake(result: AiResult, title: String, advice: String) {
        var retakes = 0
        val check = inspection(result).copy(
            actionRecommendation = if (result == AiResult.NEEDS_RECHECK) advice else null,
        )
        compose.setContent {
            val density = Density(LocalDensity.current.density, fontScale = 1.3f)
            CompositionLocalProvider(LocalDensity provides density) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        CheckResultContent(
                            state = CheckResultUiState(check = check),
                            onBack = {},
                            onRecheck = { retakes++ },
                            onPickup = { error("Status ini tidak boleh membuka penjemputan") },
                            onRetry = null,
                            onConfirmationChange = {},
                        )
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Judul harus dapat dibaca dalam paling banyak dua baris", layouts.single().lineCount <= 2)
        compose.onNodeWithText(advice).performScrollTo().assertExists()
        compose.onNodeWithText("Ajukan penjemputan").assertDoesNotExist()
        compose.onNodeWithText("Saya memastikan pakaian bersih dan aman digunakan.").assertDoesNotExist()
        compose.onNodeWithText("Ambil foto lagi").performClick()
        compose.runOnIdle { assertEquals(1, retakes) }
    }

    private fun inspection(result: AiResult) = ClothingCheck(
        checkId = "clothing-42",
        userId = "user-1",
        clothingPhoto = "",
        clothingType = ClothingType.SHIRT,
        aiResult = result,
        aiDescription = "Ini deskripsi hasil pemeriksaan.",
        createdAt = "2026-11-23T00:00:00Z",
    )
}
