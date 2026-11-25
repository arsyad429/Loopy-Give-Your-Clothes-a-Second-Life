package id.ac.loopy.data.mapper

import id.ac.loopy.data.remote.dto.ClothingReviewDto
import id.ac.loopy.data.remote.dto.DonationDetailDto
import id.ac.loopy.data.remote.dto.DonationItemDto
import id.ac.loopy.data.remote.dto.DonationStatusHistoryDto
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DonationMapperTest {
    @Test
    fun createSubmissionToDtoPreservesEveryField() {
        val submission = CreateDonationSubmission(
            checkId = "check-uuid",
            cleanAndSafeConfirmed = true,
            phoneNumber = "081234567890",
            pickupAddress = "Jl. Merdeka No. 1",
            city = "Bandung",
            quantity = 4,
            preferredDate = "2026-11-27",
            preferredTime = "Siang (11.00–14.00)",
            notes = "Di teras",
        )
        val dto = submission.toDto()
        assertEquals(submission.checkId, dto.checkId)
        assertEquals(submission.cleanAndSafeConfirmed, dto.cleanAndSafeConfirmed)
        assertEquals(submission.phoneNumber, dto.phoneNumber)
        assertEquals(submission.pickupAddress, dto.pickupAddress)
        assertEquals(submission.city, dto.city)
        assertEquals(submission.quantity, dto.quantity)
        assertEquals(submission.preferredDate, dto.preferredDate)
        assertEquals(submission.preferredTime, dto.preferredTime)
        assertEquals(submission.notes, dto.notes)
    }

    @Test
    fun donationItemDtoMapsAllStatusesAndFallsBackOnUnknown() {
        DonationStatus.entries.forEach { expected ->
            val domain = DonationItemDto(
                donationId = "don-${expected.name}",
                donorName = "Neva",
                city = "Malang",
                quantity = 2,
                status = expected.name,
                version = 1,
                createdAt = "2026-11-25T00:00:00Z",
                updatedAt = "2026-11-25T00:00:00Z",
            ).toDomain()
            assertEquals(expected, domain.status)
        }

        val unknown = DonationItemDto(
            donationId = "don-x",
            donorName = "Neva",
            city = "Malang",
            quantity = 1,
            status = "UNKNOWN_STATUS",
            version = 1,
            createdAt = "2026-11-25T00:00:00Z",
            updatedAt = "2026-11-25T00:00:00Z",
        ).toDomain()
        assertEquals(DonationStatus.PENDING_REVIEW, unknown.status)
    }

    @Test
    fun clothingReviewDtoMatchesBackendValueLabelAndFallsBackSafely() {
        assertEquals(
            ClothingType.HOODIE,
            ClothingReviewDto("c1", "/p1", "HOODIE", "POTENTIALLY_ELIGIBLE", "Bagus").toDomain().clothingType,
        )
        assertEquals(
            ClothingType.SHIRT,
            ClothingReviewDto("c2", "/p2", "kemeja", "NEEDS_RECHECK", "Buram").toDomain().clothingType,
        )
        val fallback = ClothingReviewDto("c3", "/p3", "UNKNOWN_KIND", "UNKNOWN_AI", "Desc").toDomain()
        assertEquals(ClothingType.OTHER, fallback.clothingType)
        assertEquals(AiResult.POTENTIALLY_ELIGIBLE, fallback.aiResult)
    }

    @Test
    fun donationDetailDtoMapsScheduleRejectionAndAuditHistory() {
        val dto = DonationDetailDto(
            donationId = "don-100",
            donorName = "Neva",
            city = "Malang",
            quantity = 3,
            status = "PICKUP_SCHEDULED",
            version = 2,
            createdAt = "2026-11-25T00:00:00Z",
            updatedAt = "2026-11-25T01:00:00Z",
            phoneNumber = "081234567890",
            pickupAddress = "Jl. Veteran",
            preferredDate = "2026-11-26",
            preferredTime = "Pagi (08.00–11.00)",
            notes = "Siap pickup",
            clothingCheck = ClothingReviewDto("chk-1", "/v1/checks/chk-1/photo", "JEANS", "POTENTIALLY_ELIGIBLE", "Utuh"),
            scheduledAt = "2026-11-26T02:30:00Z",
            scheduledDate = "2026-11-26",
            scheduledTime = "09:30",
            timezone = "Asia/Jakarta",
            rejectionReason = null,
            history = listOf(
                DonationStatusHistoryDto(1, null, "PENDING_REVIEW", "Neva", "2026-11-25T00:00:00Z"),
                DonationStatusHistoryDto(2, "PENDING_REVIEW", "PICKUP_SCHEDULED", "Admin", "2026-11-25T01:00:00Z"),
            ),
        )

        val mapped = dto.toDomain()
        assertEquals(DonationStatus.PICKUP_SCHEDULED, mapped.status)
        assertEquals(ClothingType.JEANS, mapped.clothingCheck.clothingType)
        assertEquals("09:30", mapped.scheduledTime)
        assertEquals("Asia/Jakarta", mapped.timezone)
        assertEquals(2, mapped.history.size)
        assertNull(mapped.history[0].previousStatus)
        assertEquals(DonationStatus.PENDING_REVIEW, mapped.history[0].newStatus)
        assertEquals(DonationStatus.PENDING_REVIEW, mapped.history[1].previousStatus)
        assertEquals(DonationStatus.PICKUP_SCHEDULED, mapped.history[1].newStatus)
    }
}
