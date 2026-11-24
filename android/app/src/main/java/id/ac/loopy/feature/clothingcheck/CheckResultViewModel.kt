package id.ac.loopy.feature.clothingcheck

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.domain.repository.ClothingCheckRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckResultViewModel(
    private val checkId: String,
    private val repository: ClothingCheckRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckResultUiState())
    val uiState: StateFlow<CheckResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    @MainThread
    fun retry() {
        loadResult()
    }

    @MainThread
    fun setCleanAndSafeConfirmed(confirmed: Boolean) {
        _uiState.update { state ->
            if (state.isEligibleForPickup && !state.isLoading && state.error == null) {
                state.copy(cleanAndSafeConfirmed = confirmed)
            } else {
                state
            }
        }
    }

    private fun loadResult() {
        val state = _uiState.value
        if (state.isLoading || state.check != null) return

        if (checkId.isBlank()) {
            _uiState.value = CheckResultUiState(error = "ID pemeriksaan tidak tersedia.")
            return
        }

        _uiState.value = CheckResultUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val result = repository.getCheckById(checkId)
                ensureActive()
                check(result.checkId == checkId)
                _uiState.value = CheckResultUiState(check = result)
            } catch (cancelled: CancellationException) {
                _uiState.value = CheckResultUiState()
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = CheckResultUiState(
                    error = "Hasil pemeriksaan gagal dimuat. Coba lagi.",
                )
            }
        }
    }
}
