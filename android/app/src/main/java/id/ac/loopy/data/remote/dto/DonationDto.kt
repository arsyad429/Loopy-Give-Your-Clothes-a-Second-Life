package id.ac.loopy.data.remote.dto

data class CreateDonationRequestDto(
    val checkId: String,
    val cleanAndSafeConfirmed: Boolean,
    val phoneNumber: String,
    val pickupAddress: String,
    val city: String,
    val quantity: Int,
    val preferredDate: String,
    val preferredTime: String,
    val notes: String,
)

data class DonationItemDto(
    val donationId: String,
    val donorName: String,
    val city: String,
    val quantity: Int,
    val status: String,
    val version: Long,
    val createdAt: String,
    val updatedAt: String,
)

data class DonationListResponseDto(
    val items: List<DonationItemDto>,
    val nextCursor: String? = null,
)

data class ClothingReviewDto(
    val checkId: String,
    val photoKey: String,
    val clothingType: String,
    val aiResult: String,
    val aiDescription: String,
)

data class DonationStatusHistoryDto(
    val version: Long,
    val previousStatus: String? = null,
    val newStatus: String,
    val updatedBy: String,
    val updatedAt: String,
)

data class DonationDetailDto(
    val donationId: String,
    val donorName: String,
    val city: String,
    val quantity: Int,
    val status: String,
    val version: Long,
    val createdAt: String,
    val updatedAt: String,
    val phoneNumber: String,
    val pickupAddress: String,
    val preferredDate: String,
    val preferredTime: String,
    val notes: String,
    val clothingCheck: ClothingReviewDto,
    val scheduledAt: String? = null,
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val timezone: String? = null,
    val rejectionReason: String? = null,
    val history: List<DonationStatusHistoryDto> = emptyList(),
)
