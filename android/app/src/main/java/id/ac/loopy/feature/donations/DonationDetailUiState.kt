package id.ac.loopy.feature.donations

import id.ac.loopy.domain.model.DonationRequest

data class DonationDetailUiState(
    val donation: DonationRequest? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
