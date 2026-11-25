package id.ac.loopy.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import id.ac.loopy.domain.model.*
import id.ac.loopy.domain.repository.RewardRepository
import id.ac.loopy.feature.rewards.RewardsViewModel
import id.ac.loopy.feature.rewards.serverRewardDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { stores.forEach { it.clear() }; Dispatchers.resetMain() }

    private fun model(repo: FakeRewards, saved: SavedStateHandle = SavedStateHandle()) =
        RewardsViewModel(repo, saved, "u1").also { stores += ViewModelStore().apply { put("rewards", it) } }

    @Test fun serverCatalogAndReplayUseFreshBalanceAndOnlyOneConcurrentRequest() = runTest(dispatcher) {
        val repo = FakeRewards()
        val model = model(repo)
        advanceUntilIdle()
        assertEquals("Server reward", model.state.value.rewards.single().name)
        model.select(repo.reward)
        model.redeem()
        model.redeem()
        advanceUntilIdle()
        assertEquals(1, repo.keys.size)
        assertEquals(23, model.state.value.balance) // response's historical remainingBalance is 0
        assertEquals(1, model.state.value.redemptions.size)
        assertNotNull(model.state.value.message)
        assertNull(model.state.value.selected)
    }

    @Test fun uncertainRetryKeepsKeyAfterDismissAndViewModelRecreationEvenIfCatalogRemovesReward() = runTest(dispatcher) {
        val repo = FakeRewards().apply { redeemFailure = IOException("response lost") }
        val saved = SavedStateHandle()
        var model = model(repo, saved)
        advanceUntilIdle()
        model.select(repo.reward); model.redeem()
        advanceUntilIdle()
        assertTrue(model.state.value.pending)
        model.dismissSelection()
        val firstKey = repo.keys.single()
        stores.last().clear()
        repo.catalogItems = emptyList()
        repo.redeemFailure = null
        model = model(repo, saved)
        advanceUntilIdle()
        model.resumePending(); model.redeem()
        advanceUntilIdle()
        assertEquals(listOf(firstKey, firstKey), repo.keys)
        assertFalse(model.state.value.pending)
        assertNotNull(model.state.value.message)
    }

    @Test fun confirmedRedemptionRemainsSuccessfulWhenBalanceRefreshFails() = runTest(dispatcher) {
        val repo = FakeRewards().apply { failBalanceAfterRedeem = true }
        val model = model(repo)
        advanceUntilIdle()
        model.select(repo.reward); model.redeem()
        advanceUntilIdle()
        val state = model.state.value
        assertNotNull(state.message)
        assertNull(state.redeemError)
        assertNotNull(state.error)
        assertNull(state.balance)
        assertFalse(state.loaded)
        assertFalse(state.pending)
        assertEquals(1, state.redemptions.size)
        model.redeem()
        assertEquals(1, repo.keys.size)
    }

    @Test fun serverInsufficientBalanceDoesNotDebitLocallyAndRequiresReload() = runTest(dispatcher) {
        val repo = FakeRewards().apply {
            redeemFailure = HttpException(Response.error<Any>(409,
                """{"error":{"code":"INSUFFICIENT_POINTS"}}""".toResponseBody()))
        }
        val model = model(repo)
        advanceUntilIdle()
        model.select(repo.reward); model.redeem()
        advanceUntilIdle()
        assertEquals(30, model.state.value.balance)
        assertFalse(model.state.value.pending)
        assertTrue(model.state.value.redeemError!!.contains("tidak cukup"))
        assertFalse(model.state.value.loaded)
    }

    @Test fun failedLoadDoesNotPresentInventedZeroBalanceOrAllowRedeem() = runTest(dispatcher) {
        val repo = FakeRewards().apply { balanceFailure = true }
        val model = model(repo)
        advanceUntilIdle()
        assertNull(model.state.value.balance)
        assertFalse(model.state.value.loaded)
        model.select(repo.reward); model.redeem()
        assertTrue(repo.keys.isEmpty())
        repo.balanceFailure = false
        model.refresh(); model.refresh()
        advanceUntilIdle()
        assertEquals(30, model.state.value.balance)
        assertTrue(model.state.value.loaded)
    }

    @Test fun accountChangeCancelsInFlightDataAndClearsPreviousAccount() = runTest(dispatcher) {
        val repo = FakeRewards()
        val model = model(repo)
        advanceUntilIdle()
        repo.delayRedemption = true
        model.select(repo.reward); model.redeem()
        runCurrent()
        repo.userId.value = "other-user"
        advanceUntilIdle()
        assertNull(model.state.value.balance)
        assertTrue(model.state.value.redemptions.isEmpty())
        assertNull(model.state.value.message)
        assertFalse(model.state.value.busy)
    }

    @Test fun fractionalAndOffsetServerDatesDisplayInJakarta() {
        assertEquals("25 Nov 2026 · 09.30 WIB", serverRewardDate("2026-11-25T02:30:00.123456789Z"))
        assertEquals("25 Nov 2026 · 09.30 WIB", serverRewardDate("2026-11-25T09:30:00+07:00"))
    }
}

private class FakeRewards : RewardRepository {
    override val userId = MutableStateFlow<String?>("u1")
    val reward = Reward("server-id", "Server reward", "From API", "gift", 10, "BADGE", true)
    var catalogItems = listOf(reward)
    val keys = mutableListOf<String>()
    var redeemFailure: Exception? = null
    var balanceFailure = false
    var failBalanceAfterRedeem = false
    var delayRedemption = false
    private var redeemed = false
    private val redemption = Redemption("r1", reward.rewardId, reward.name, "From API", "gift", "BADGE", 10, "2026-11-25T02:30:00Z")
    override suspend fun balance(): Int {
        if (balanceFailure || redeemed && failBalanceAfterRedeem) throw IOException("offline")
        return if (redeemed) 23 else 30
    }
    override suspend fun catalog() = catalogItems
    override suspend fun history() = if (redeemed) listOf(redemption) else emptyList()
    override suspend fun redeem(rewardId: String, requestKey: String): RedemptionResult {
        keys += requestKey
        if (delayRedemption) awaitCancellation()
        redeemFailure?.let { throw it }
        redeemed = true
        return RedemptionResult(redemption, 0, true)
    }
}
