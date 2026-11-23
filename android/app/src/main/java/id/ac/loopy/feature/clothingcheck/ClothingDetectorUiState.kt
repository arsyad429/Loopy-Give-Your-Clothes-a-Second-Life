package id.ac.loopy.feature.clothingcheck

import androidx.compose.ui.graphics.ImageBitmap
import id.ac.loopy.domain.model.ClothingType

/** Immutable presentation state. A sample illustration is never treated as an uploaded photo. */
data class ClothingDetectorUiState(
    val photo: ImageBitmap? = null,
    val photoName: String? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val clothingType: ClothingType = ClothingType.TSHIRT,
) {
    val canAnalyze: Boolean get() = photo != null && !isLoading && !isAnalyzing && error == null
}
