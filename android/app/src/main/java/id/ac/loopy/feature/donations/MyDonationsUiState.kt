package id.ac.loopy.feature.donations

import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem

enum class DonationListFilter(val label: String) {
    ALL("Semua"),
    ACTIVE("Aktif"),
    COMPLETED("Selesai"),
    REJECTED("Ditolak"),
}

data class MyDonationsUiState(
    val items: List<DonationSummaryItem> = emptyList(),
    val filter: DonationListFilter = DonationListFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val filteredItems: List<DonationSummaryItem>
        get() {
            val q = searchQuery.trim().lowercase()
            return items.filter { item ->
                val matchesFilter = when (filter) {
                    DonationListFilter.ALL -> true
                    DonationListFilter.ACTIVE -> item.status != DonationStatus.COMPLETED && item.status != DonationStatus.REJECTED
                    DonationListFilter.COMPLETED -> item.status == DonationStatus.COMPLETED
                    DonationListFilter.REJECTED -> item.status == DonationStatus.REJECTED
                }
                val matchesQuery = q.isEmpty() ||
                    item.donationId.lowercase().contains(q) ||
                    item.city.lowercase().contains(q)
                matchesFilter && matchesQuery
            }
        }
}
