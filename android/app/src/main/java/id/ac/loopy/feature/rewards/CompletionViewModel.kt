package id.ac.loopy.feature.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.ac.loopy.data.local.DemoDonation
import id.ac.loopy.data.local.DemoRedemption
import id.ac.loopy.data.repository.CompletionDemoRepository
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompletionUiState(
    val donations: List<DemoDonation> = emptyList(),
    val balance: Int = 0,
    val redemptions: List<DemoRedemption> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class CompletionViewModel(private val repository: CompletionDemoRepository) : ViewModel() {
    private val feedback = MutableStateFlow(CompletionUiState())
    val state = combine(repository.donations, repository.balance, repository.redemptions, feedback) { donations, balance, redemptions, ui ->
        ui.copy(donations = donations, balance = balance, redemptions = redemptions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompletionUiState())

    init { prepare() }
    fun prepare() = action(null) { repository.prepare() }
    fun dismissMessage() { feedback.value = feedback.value.copy(error = null, message = null) }
    fun advance(id: String, target: DonationStatus) = action(
        if (target == DonationStatus.COMPLETED) "Donasi selesai. 10 poin ditambahkan satu kali." else "Pakaian ditandai sudah dijemput.",
    ) { repository.advance(id, target) }
    fun redeem(id: String, key: String) = action("Penukaran berhasil! Reward tersimpan di Reward-ku.") { repository.redeem(id, key) }

    private fun action(success: String?, block: suspend () -> Unit) {
        if (feedback.value.busy) return
        feedback.value = feedback.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                block()
                feedback.value = feedback.value.copy(loading = false, message = success)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                feedback.value = feedback.value.copy(loading = false, error = if (error is IllegalArgumentException || error is IllegalStateException) error.message else "Penyimpanan lokal gagal. Coba lagi.")
            } finally {
                feedback.value = feedback.value.copy(busy = false)
            }
        }
    }

    companion object {
        fun factory(repository: CompletionDemoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CompletionViewModel(repository) }
        }
    }
}
