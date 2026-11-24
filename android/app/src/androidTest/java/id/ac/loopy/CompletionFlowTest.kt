package id.ac.loopy

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.loopy.data.local.CompletionDemoDatabase
import id.ac.loopy.data.repository.CompletionDemoRepository
import id.ac.loopy.feature.rewards.CompletionViewModel
import id.ac.loopy.di.AppContainer
import id.ac.loopy.ui.LoopyApp
import id.ac.loopy.ui.TemplateUiState
import id.ac.loopy.ui.theme.LoopyTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class CompletionFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, CompletionDemoDatabase::class.java).build()

    private var model: CompletionViewModel? = null
    @After fun close() { model?.viewModelScope?.cancel(); db.close() }

    @Test fun adminCompletionThenCancelAndRedeemUpdatesHomeAndHistory() {
        val repo = CompletionDemoRepository(db)
        val container = AppContainer(context)
        runBlocking { repo.prepare() }
        compose.runOnUiThread { model = CompletionViewModel(repo) }
        compose.setContent {
            val activity = LocalActivity.current as? ComponentActivity
            DisposableEffect(activity) { activity?.enableEdgeToEdge(); onDispose {} }
            LoopyTheme { LoopyApp(TemplateUiState(), {}, {}, {}, {}, container = container, completion = model) }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Demo E–F · Admin penyelesaian donasi & poin"))
        compose.onNodeWithText("Demo E–F · Admin penyelesaian donasi & poin").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("2 pakaian · #LP021").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("2 pakaian · #LP021").performScrollTo().performClick()
        compose.onNodeWithText("Konfirmasi sudah dijemput").performClick()
        compose.onNodeWithText("Ya, konfirmasi").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Selesaikan donasi (+10 poin)").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Selesaikan donasi (+10 poin)").performClick()
        compose.onNodeWithText("Ya, konfirmasi").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Donasi selesai").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("S10 · Rewards & My Rewards"))
        compose.onNodeWithText("S10 · Rewards & My Rewards").performClick()
        compose.onAllNodesWithText("30 poin").onFirst().assertExists()
        compose.onNodeWithText("Tukar 10 poin").performScrollTo().performClick()
        compose.onNodeWithText("Batal").performClick()
        assertEquals(30, runBlocking { db.completionDao().balance() })
        compose.onNodeWithText("Tukar 10 poin").performScrollTo().performClick()
        capture("completion-redeem-confirm")
        compose.onNodeWithText("Konfirmasi penukaran").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Kembali ke katalog").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Eco Supporter Badge").assertExists()
        capture("completion-my-rewards")
        compose.onNodeWithText("Kembali ke katalog").performScrollTo().performClick()
        compose.onNodeWithText("Butuh 10 poin lagi").performScrollTo().assertIsNotEnabled()
        compose.onAllNodesWithText("20 poin").onFirst().performScrollTo()
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -10_000f) }
        capture("completion-rewards")
        compose.onNodeWithText("Beranda").performClick()
        compose.onNodeWithText("poin terkumpul").performScrollTo().assertExists()
        capture("completion-home")
        compose.onNodeWithText("Donasiku").performClick()
        compose.onNodeWithText("2 pakaian · #LP021").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Detail donasi").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Kebaikanmu\nsudah tersampaikan.").assertExists()
        capture("completion-donation-detail")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Compose idle does not include Android's dialog-window fade animation.
        automation.waitForIdle(500, 5_000)
        val bitmap = automation.takeScreenshot()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        // Keep QA artifacts after the Gradle runner uninstalls the test application.
        val copy = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "cp /sdcard/Android/data/id.ac.loopy/files/$name.png /sdcard/Download/$name.png",
        )
        ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
    }
}
