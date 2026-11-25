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

class MyDonationsViewModel(
    private val repository: DonationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyDonationsUiState(isLoading = true))
    val uiState: StateFlow<MyDonationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    @MainThread
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = repository.listMyDonations()
                    .sortedByDescending { it.createdAt }
                ensureActive()
                _uiState.update {
                    it.copy(
                        items = items,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Daftar donasi gagal dimuat. Coba lagi.",
                    )
                }
            }
        }
    }

    @MainThread
    fun onFilterChange(filter: DonationListFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    @MainThread
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
