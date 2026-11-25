package id.ac.loopy.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.repository.AdminRepositoryImpl
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DonationRequestsViewModel(private val repository: AdminRepositoryImpl) : ViewModel() {
    private val _state = MutableStateFlow(DonationRequestsUiState())
    val state = _state.asStateFlow()
    private var request: Job? = null

    fun refresh() = load(_state.value)

    fun search(query: String) = load(_state.value.copy(query = query.take(100)), debounce = true)

    fun filter(status: DonationStatus) {
        if (status != _state.value.status) load(_state.value.copy(status = status))
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (!current.loading) load(current, cursor)
    }

    fun retry() {
        if (_state.value.nextCursor == null) refresh() else loadMore()
    }

    private fun load(state: DonationRequestsUiState, cursor: String? = null, debounce: Boolean = false) {
        request?.cancel()
        val items = if (cursor == null) emptyList() else state.items
        _state.value = state.copy(items = items, nextCursor = cursor, loading = true, error = null)
        request = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val page = repository.list(state.status, state.query, cursor)
                ensureActive()
                _state.value = state.copy(
                    items = (items + page.items).distinctBy { it.donationId },
                    nextCursor = page.nextCursor,
                    loading = false,
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ensureActive()
                _state.value = _state.value.copy(loading = false, error = error.adminMessage("Antrean gagal dimuat."))
            }
        }
    }
}
