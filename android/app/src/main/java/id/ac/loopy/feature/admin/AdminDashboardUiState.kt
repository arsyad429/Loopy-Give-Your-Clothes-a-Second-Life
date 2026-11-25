package id.ac.loopy.feature.admin

import id.ac.loopy.data.remote.dto.AdminDonationSummary
import id.ac.loopy.domain.model.DonationSummaryItem

internal data class AdminDashboardUiState(
    val summary: AdminDonationSummary? = null,
    val pending: List<DonationSummaryItem>? = null,
    val loading: Boolean = true,
    val error: String? = null,
)
