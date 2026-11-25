package id.ac.loopy.data.mapper

import id.ac.loopy.data.remote.dto.ClothingReviewDto
import id.ac.loopy.data.remote.dto.CreateDonationRequestDto
import id.ac.loopy.data.remote.dto.DonationDetailDto
import id.ac.loopy.data.remote.dto.DonationItemDto
import id.ac.loopy.data.remote.dto.DonationStatusHistoryDto
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationCheckReview
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationStatusHistory
import id.ac.loopy.domain.model.DonationSummaryItem

fun CreateDonationSubmission.toDto(): CreateDonationRequestDto = CreateDonationRequestDto(
    checkId = checkId,
    cleanAndSafeConfirmed = cleanAndSafeConfirmed,
    phoneNumber = phoneNumber,
    pickupAddress = pickupAddress,
    city = city,
    quantity = quantity,
    preferredDate = preferredDate,
    preferredTime = preferredTime,
    notes = notes,
)

fun DonationItemDto.toDomain(): DonationSummaryItem = DonationSummaryItem(
    donationId = donationId,
    donorName = donorName,
    city = city,
    quantity = quantity,
    status = parseDonationStatus(status),
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ClothingReviewDto.toDomain(): DonationCheckReview = DonationCheckReview(
    checkId = checkId,
    photoKey = photoKey,
    clothingType = ClothingType.entries.firstOrNull {
        it.backendValue == clothingType || it.label.equals(clothingType, ignoreCase = true)
    } ?: ClothingType.OTHER,
    aiResult = AiResult.entries.firstOrNull { it.name == aiResult } ?: AiResult.POTENTIALLY_ELIGIBLE,
    aiDescription = aiDescription,
)

fun DonationStatusHistoryDto.toDomain(): DonationStatusHistory = DonationStatusHistory(
    version = version,
    previousStatus = previousStatus?.let(::parseDonationStatus),
    newStatus = parseDonationStatus(newStatus),
    updatedBy = updatedBy,
    updatedAt = updatedAt,
)

fun DonationDetailDto.toDomain(): DonationRequest = DonationRequest(
    donationId = donationId,
    donorName = donorName,
    phoneNumber = phoneNumber,
    pickupAddress = pickupAddress,
    city = city,
    quantity = quantity,
    preferredDate = preferredDate,
    preferredTime = preferredTime,
    notes = notes,
    status = parseDonationStatus(status),
    version = version,
    clothingCheck = clothingCheck.toDomain(),
    scheduledAt = scheduledAt,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    timezone = timezone,
    rejectionReason = rejectionReason,
    history = history.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun parseDonationStatus(raw: String): DonationStatus =
    DonationStatus.entries.firstOrNull { it.name == raw } ?: DonationStatus.PENDING_REVIEW
