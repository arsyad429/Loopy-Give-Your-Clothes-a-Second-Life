package id.ac.loopy.feature.home

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    @MainThread
    fun refresh() {
        if (refreshJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = true) }
        refreshJob = viewModelScope.launch {
            val pointsRequest = async { load(repository::getPoints) }
            val donationRequest = async { load(repository::getLatestDonation) }
            val points = pointsRequest.await()
            val donation = donationRequest.await()
            _uiState.update { old ->
                HomeUiState(
                    points = points.getOrElse { old.points },
                    latestDonation = donation.getOrElse { old.latestDonation },
                    isLoading = false,
                    pointsUnavailable = points.isFailure,
                    donationUnavailable = donation.isFailure,
                )
            }
        }
    }

    private suspend fun <T> load(request: suspend () -> T): Result<T> = try {
        Result.success(request())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
