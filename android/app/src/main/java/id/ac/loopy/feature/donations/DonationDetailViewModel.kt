package id.ac.loopy.feature.donations

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.domain.repository.DonationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DonationDetailViewModel(
    private val donationId: String,
    private val repository: DonationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonationDetailUiState(isLoading = true))
    val uiState: StateFlow<DonationDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    @MainThread
    fun retry() {
        load()
    }

    private fun load() {
        if (donationId.isBlank()) {
            _uiState.value = DonationDetailUiState(error = "ID donasi tidak tersedia.")
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val detail = repository.getDonationDetail(donationId)
                ensureActive()
                _uiState.value = DonationDetailUiState(donation = detail)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = DonationDetailUiState(error = "Detail donasi gagal dimuat. Coba lagi.")
            }
        }
    }
}
