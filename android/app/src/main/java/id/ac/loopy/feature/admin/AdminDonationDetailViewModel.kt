package id.ac.loopy.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.repository.AdminRepositoryImpl
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal class AdminDonationDetailViewModel(
    private val id: String,
    private val repository: AdminRepositoryImpl,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminDonationDetailUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.busy) return
        _state.value = AdminDonationDetailUiState(busy = true)
        viewModelScope.launch {
            try {
                _state.value = AdminDonationDetailUiState(donation = repository.detail(id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = AdminDonationDetailUiState(error = error.adminMessage("Detail gagal dimuat."))
            }
        }
    }

    fun schedule(at: Long) = decide(DonationStatus.PICKUP_SCHEDULED, "Jadwal penjemputan tersimpan.") {
        repository.schedule(it, at)
    }

    fun reject(reason: String) = decide(DonationStatus.REJECTED, "Penolakan tersimpan.") {
        repository.reject(it, reason)
    }

    fun advance(status: DonationStatus) = decide(status, "Status donasi diperbarui.") {
        repository.advance(it, status)
    }

    private fun decide(status: DonationStatus, message: String, submit: suspend (DonationRequest) -> DonationRequest) {
        val current = _state.value
        val donation = current.donation ?: return
        if (current.busy || !donation.status.canTransitionTo(status)) return

        _state.value = current.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                _state.value = AdminDonationDetailUiState(donation = submit(donation), message = message)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (error is HttpException && error.code() == 409) {
                    reloadAfterConflict()
                } else {
                    _state.update { it.copy(busy = false, error = error.adminMessage("Keputusan gagal disimpan.")) }
                }
            }
        }
    }

    private suspend fun reloadAfterConflict() {
        try {
            _state.value = AdminDonationDetailUiState(
                donation = repository.detail(id),
                error = "Pengajuan sudah berubah. Periksa detail terbaru sebelum melanjutkan.",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.value = AdminDonationDetailUiState(
                error = error.adminMessage("Pengajuan sudah berubah dan detail terbaru gagal dimuat."),
            )
        }
    }
}
