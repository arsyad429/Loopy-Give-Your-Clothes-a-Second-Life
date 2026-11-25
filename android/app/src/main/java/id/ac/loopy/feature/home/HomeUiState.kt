package id.ac.loopy.feature.home

import id.ac.loopy.domain.model.DonationSummaryItem

internal data class HomeUiState(
    val points: Int? = null,
    val latestDonation: DonationSummaryItem? = null,
    val isLoading: Boolean = true,
    val pointsUnavailable: Boolean = false,
    val donationUnavailable: Boolean = false,
)
