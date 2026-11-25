package id.ac.loopy.feature.admin

import id.ac.loopy.data.remote.dto.AdminDonationSummary

internal data class AdminDashboardUiState(
    val summary: AdminDonationSummary? = null,
    val loading: Boolean = true,
    val error: String? = null,
)
