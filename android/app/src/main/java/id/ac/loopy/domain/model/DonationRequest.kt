package id.ac.loopy.domain.model

data class DonationSummaryItem(
    val donationId: String,
    val donorName: String,
    val city: String,
    val quantity: Int,
    val status: DonationStatus,
    val version: Long,
    val createdAt: String,
    val updatedAt: String,
)

data class DonationCheckReview(
    val checkId: String,
    val photoKey: String,
    val clothingType: ClothingType,
    val aiResult: AiResult,
    val aiDescription: String,
)

data class DonationRequest(
    val donationId: String,
    val donorName: String,
    val phoneNumber: String,
    val pickupAddress: String,
    val city: String,
    val quantity: Int,
    val preferredDate: String,
    val preferredTime: String,
    val notes: String,
    val status: DonationStatus,
    val version: Long,
    val clothingCheck: DonationCheckReview,
    val scheduledAt: String? = null,
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val timezone: String? = null,
    val rejectionReason: String? = null,
    val history: List<DonationStatusHistory> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

data class CreateDonationSubmission(
    val checkId: String,
    val cleanAndSafeConfirmed: Boolean,
    val phoneNumber: String,
    val pickupAddress: String,
    val city: String,
    val quantity: Int,
    val preferredDate: String,
    val preferredTime: String,
    val notes: String = "",
)
