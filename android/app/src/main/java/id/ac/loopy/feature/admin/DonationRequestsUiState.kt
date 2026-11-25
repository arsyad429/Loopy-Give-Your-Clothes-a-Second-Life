package id.ac.loopy.feature.admin

import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem

internal data class DonationRequestsUiState(
    val status: DonationStatus = DonationStatus.PENDING_REVIEW,
    val query: String = "",
    val items: List<DonationSummaryItem> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
)
