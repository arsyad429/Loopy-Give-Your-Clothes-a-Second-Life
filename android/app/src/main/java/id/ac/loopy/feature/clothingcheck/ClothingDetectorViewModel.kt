package id.ac.loopy.feature.clothingcheck

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.repository.ClothingCheckRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClothingAnalysisUiState(
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val checkId: String? = null,
)

class ClothingDetectorViewModel(
    private val repository: ClothingCheckRepository,
) : ViewModel() {

    private val _analysisState = MutableStateFlow(ClothingAnalysisUiState())
    val analysisState: StateFlow<ClothingAnalysisUiState> =
        _analysisState.asStateFlow()

    @MainThread
    fun analyze(photoUri: String, clothingType: ClothingType) {
        val current = _analysisState.value
        if (photoUri.isBlank() || current.isAnalyzing || current.checkId != null) {
            return
        }

        _analysisState.value = ClothingAnalysisUiState(isAnalyzing = true)

        viewModelScope.launch {
            try {
                val check = repository.analyzeClothing(photoUri, clothingType)
                ensureActive()
                _analysisState.value = ClothingAnalysisUiState(checkId = check.checkId)
            } catch (cancelled: CancellationException) {
                _analysisState.value = ClothingAnalysisUiState()
                throw cancelled
            } catch (_: Exception) {
                _analysisState.value = ClothingAnalysisUiState(
                    error = "Pemeriksaan gagal. Coba lagi.",
                )
            }
        }
    }

    @MainThread
    fun onResultHandled() {
        _analysisState.update { it.copy(checkId = null) }
    }
}
