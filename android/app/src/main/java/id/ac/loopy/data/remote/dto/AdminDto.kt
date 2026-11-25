package id.ac.loopy.data.remote.dto

internal data class AdminDonationSummary(
    val total: Long,
    val pending: Long,
    val scheduled: Long,
    val pickedUp: Long,
    val completed: Long,
    val rejected: Long,
)

internal data class ScheduleDonationRequest(
    val scheduledDate: String,
    val scheduledTime: String,
    val timezone: String,
    val expectedVersion: Long,
)

internal data class RejectDonationRequest(
    val rejectionReason: String,
    val expectedVersion: Long,
)

internal data class AdvanceDonationRequest(
    val targetStatus: String,
    val expectedVersion: Long,
)
