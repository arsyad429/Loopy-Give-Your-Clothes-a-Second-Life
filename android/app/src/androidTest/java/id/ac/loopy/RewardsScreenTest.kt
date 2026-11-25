package id.ac.loopy

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.domain.model.*
import id.ac.loopy.domain.repository.RewardRepository
import id.ac.loopy.feature.rewards.RewardsScreen
import id.ac.loopy.feature.rewards.RewardsViewModel
import id.ac.loopy.ui.theme.LoopyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*

/** Test fixtures are confined to androidTest; runtime always uses RewardApi. */
class RewardsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @After fun close() { compose.runOnIdle { store.clear() } }

    @Test fun cancelDoesNotRedeemAndConfirmRendersServerBalanceAndHistory() {
        val repo = ScreenRewards()
        lateinit var model: RewardsViewModel
        compose.runOnIdle {
            model = RewardsViewModel(repo, SavedStateHandle(), "ui-user")
            store.put("rewards", model)
        }
        compose.setContent {
            val state by model.state.collectAsStateWithLifecycle()
            LoopyTheme {
                RewardsScreen(state, {}, {}, model::select, model::redeem, model::refresh,
                    model::dismissSelection, model::resumePending)
            }
        }
        compose.waitUntil(5_000) { model.state.value.loaded }
        compose.onNodeWithText("Butuh 10 poin lagi").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Tukar 10 poin").performScrollTo().performClick()
        compose.onNodeWithText("Batal").performClick()
        compose.runOnIdle { assertEquals(0, repo.calls) }
        compose.onNodeWithText("Tukar 10 poin").performScrollTo().performClick()
        compose.onNodeWithText("Konfirmasi penukaran").performClick()
        compose.waitUntil(5_000) { model.state.value.message != null && !model.state.value.busy }
        compose.onNodeWithText("Badge dari server").assertExists()
        compose.onNodeWithText("10 poin", substring = false).assertExists()
        compose.onNodeWithText("Badge dari server berhasil ditukar.").assertExists()
        compose.runOnIdle { assertEquals(1, repo.calls) }
    }
}

private class ScreenRewards : RewardRepository {
    override val userId = MutableStateFlow<String?>("ui-user")
    private val reward = Reward("badge-api", "Badge dari server", "Kontribusi donasi", "leaf", 10, "BADGE", true)
    var calls = 0
    private val redemption = Redemption("redemption-api", reward.rewardId, reward.name,
        reward.description, reward.icon, reward.type, 10, "2026-11-25T02:30:00.123Z")
    override suspend fun balance() = if (calls == 0) 20 else 10
    override suspend fun catalog() = listOf(reward, reward.copy(rewardId = "other", name = "Reward 30", requiredPoints = 30))
    override suspend fun history() = if (calls == 0) emptyList() else listOf(redemption)
    override suspend fun redeem(rewardId: String, requestKey: String): RedemptionResult {
        calls++
        return RedemptionResult(redemption, 10, false)
    }
}
