package id.ac.loopy.domain.model

data class DonationStatusHistory(
    val version: Long,
    val previousStatus: DonationStatus?,
    val newStatus: DonationStatus,
    val updatedBy: String,
    val updatedAt: String,
)
