package id.ac.loopy.feature.clothingcheck

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck

data class CheckResultUiState(
    val check: ClothingCheck? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val cleanAndSafeConfirmed: Boolean = false,
) {
    val isEligibleForPickup: Boolean
        get() = check?.aiResult == AiResult.POTENTIALLY_ELIGIBLE

    val canContinueToPickup: Boolean
        get() = isEligibleForPickup && cleanAndSafeConfirmed && !isLoading && error == null
}
