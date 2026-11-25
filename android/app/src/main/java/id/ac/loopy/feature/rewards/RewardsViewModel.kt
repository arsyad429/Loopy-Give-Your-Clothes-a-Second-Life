package id.ac.loopy.feature.rewards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import id.ac.loopy.domain.model.Reward
import id.ac.loopy.domain.repository.RewardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID

class RewardsViewModel(
    private val repository: RewardRepository,
    private val saved: SavedStateHandle,
    private val ownerId: String,
) : ViewModel() {
    private val mutable = MutableStateFlow(RewardsUiState())
    val state = mutable.asStateFlow()
    private var active = false
    private var request: Job? = null
    private val keyField = "redemptionKey:$ownerId"
    private val rewardField = "redemptionReward:$ownerId"
    private val pendingId: String? get() = saved[rewardField]

    init {
        viewModelScope.launch {
            repository.userId.distinctUntilChanged().collect { userId ->
                active = userId == ownerId
                if (!active) {
                    request?.cancel()
                    mutable.value = RewardsUiState()
                } else {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        if (!active || request?.isActive == true || state.value.busy) return
        mutable.update { it.copy(loading = true, error = null) }
        request = viewModelScope.launch {
            try {
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, loaded = false, error = "Data reward gagal dimuat. Periksa koneksi lalu muat ulang.") }
            }
        }
    }

    private suspend fun load() = coroutineScope {
        val balance = async { repository.balance() }
        val catalog = async { repository.catalog() }
        val history = async { repository.history() }
        val nextBalance = balance.await()
        val rewards = catalog.await()
        val redemptions = history.await()
        ensureActive()
        mutable.update {
            it.copy(
                balance = nextBalance, rewards = rewards, redemptions = redemptions,
                loading = false, loaded = true, error = null, pending = pendingId != null,
                selected = it.selected?.let { selected -> rewards.find { reward -> reward.rewardId == selected.rewardId } },
            )
        }
    }

    fun select(reward: Reward) {
        val state = state.value
        if (!active || state.busy || !state.loaded || state.loading) return
        if (pendingId != null && pendingId != reward.rewardId) return
        if (pendingId == null && (!reward.isActive || (state.balance ?: 0) < reward.requiredPoints)) return
        mutable.update { it.copy(selected = reward, redeemError = null, message = null) }
    }

    fun dismissSelection() {
        if (!state.value.busy) mutable.update { it.copy(selected = null, redeemError = null) }
    }

    fun dismissMessage() { mutable.update { it.copy(message = null) } }

    fun redeem() {
        val current = state.value
        val reward = current.selected ?: return
        if (!active || current.busy || current.loading) return
        if (pendingId == null && (!current.loaded || !reward.isActive || (current.balance ?: 0) < reward.requiredPoints)) return
        val key = saved.get<String>(keyField) ?: UUID.randomUUID().toString().also {
            saved[keyField] = it
            saved[rewardField] = reward.rewardId
        }
        if (pendingId != reward.rewardId) return
        mutable.update { it.copy(busy = true, pending = true, redeemError = null, error = null) }
        request = viewModelScope.launch {
            try {
                val result = repository.redeem(reward.rewardId, key)
                ensureActive()
                saved.remove<String>(keyField)
                saved.remove<String>(rewardField)
                // A replay carries the historical balance, so always fetch the current balance.
                mutable.update {
                    it.copy(
                        selected = null, pending = false, loaded = false, balance = null,
                        redemptions = listOf(result.redemption) + it.redemptions.filter { entry -> entry.redemptionId != result.redemption.redemptionId },
                        message = "${result.redemption.rewardName} berhasil ditukar.",
                    )
                }
                try {
                    load()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutable.update { it.copy(loaded = false, error = "Penukaran berhasil. Saldo terbaru belum dapat dimuat; silakan muat ulang.") }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val definitive = error is HttpException && error.code() in 400..499 && error.code() !in listOf(408, 429)
                if (definitive) {
                    saved.remove<String>(keyField)
                    saved.remove<String>(rewardField)
                }
                mutable.update {
                    it.copy(pending = !definitive, loaded = false, redeemError = redemptionError(error, !definitive))
                }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun resumePending() {
        val id = pendingId ?: return
        val reward = state.value.rewards.find { it.rewardId == id }
            ?: Reward(id, "Reward sebelumnya", "", "gift", 0, "", false)
        if (active && !state.value.busy) mutable.update { it.copy(selected = reward) }
    }
}

private fun redemptionError(error: Exception, uncertain: Boolean): String {
    if (uncertain) return "Hasil penukaran belum dapat dipastikan. Coba periksa lagi dengan permintaan yang sama agar poin tidak terpotong dua kali."
    val code = if (error is HttpException) runCatching {
        JsonParser.parseString(error.response()?.errorBody()?.string()).asJsonObject
            .getAsJsonObject("error")?.get("code")?.asString
    }.getOrNull() else null
    return when (code) {
        "INSUFFICIENT_POINTS" -> "Saldo poin tidak cukup. Muat ulang untuk melihat saldo terbaru."
        "REWARD_NOT_FOUND" -> "Reward sudah tidak tersedia. Muat ulang katalog."
        "IDEMPOTENCY_CONFLICT" -> "Permintaan penukaran bertentangan dengan data server. Muat ulang sebelum melanjutkan."
        else -> "Penukaran ditolak server. Muat ulang atau masuk kembali sebelum mencoba lagi."
    }
}
