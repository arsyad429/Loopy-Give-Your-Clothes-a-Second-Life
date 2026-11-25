package id.ac.loopy.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.local.dao.AdminPickupScheduleDraftDao
import id.ac.loopy.data.local.entity.AdminPickupScheduleDraftEntity
import id.ac.loopy.data.repository.AdminRepositoryImpl
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal class AdminDonationDetailViewModel(
    private val id: String,
    private val repository: AdminRepositoryImpl,
    private val adminId: String,
    private val drafts: AdminPickupScheduleDraftDao,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminDonationDetailUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.busy || _state.value.savingDraft) return
        _state.value = AdminDonationDetailUiState(busy = true)
        viewModelScope.launch {
            try {
                showDonation(repository.detail(id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = AdminDonationDetailUiState(error = error.adminMessage("Detail gagal dimuat."))
            }
        }
    }

    fun updateScheduleDraft(selection: PickupScheduleSelection) {
        val current = _state.value
        if (current.busy || current.savingDraft || current.donation?.status != DonationStatus.PENDING_REVIEW) return

        _state.value = current.copy(scheduleDraft = selection, savingDraft = true, draftError = null)
        viewModelScope.launch {
            try {
                // Let the local write finish if the admin immediately leaves the screen.
                withContext(NonCancellable) {
                    if (selection.dateMillis == null && selection.minutesOfDay == null) {
                        drafts.delete(adminId, id)
                    } else {
                        drafts.save(AdminPickupScheduleDraftEntity(adminId, id, selection.dateMillis, selection.minutesOfDay))
                    }
                }
                _state.update { it.copy(savingDraft = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update {
                    it.copy(savingDraft = false, draftError = PickupScheduleDraftError.SAVE)
                }
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
        if (current.busy || current.savingDraft || !donation.status.canTransitionTo(status)) return

        _state.value = current.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                showDonation(submit(donation), message = message)
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
            showDonation(
                repository.detail(id),
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

    private suspend fun showDonation(donation: DonationRequest, message: String? = null, error: String? = null) {
        val pending = donation.status == DonationStatus.PENDING_REVIEW
        var draftError: PickupScheduleDraftError? = null
        val stored = try {
            if (pending) {
                drafts.find(adminId, id)
            } else {
                drafts.delete(adminId, id)
                null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            draftError = PickupScheduleDraftError.LOAD
            null
        }
        val current = _state.value
        val selection = when {
            pending && current.draftError == PickupScheduleDraftError.SAVE -> {
                draftError = current.draftError
                current.scheduleDraft
            }
            stored != null -> PickupScheduleSelection(stored.dateMillis, stored.minutesOfDay)
            else -> PickupScheduleSelection()
        }
        _state.value = AdminDonationDetailUiState(
            donation = donation,
            message = message,
            error = error,
            scheduleDraft = selection,
            draftError = draftError,
        )
    }
}
