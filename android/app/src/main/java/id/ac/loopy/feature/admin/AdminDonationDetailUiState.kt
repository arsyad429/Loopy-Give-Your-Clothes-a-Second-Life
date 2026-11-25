package id.ac.loopy.feature.admin

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import java.text.SimpleDateFormat
import java.util.Locale

internal data class AdminDonationDetailUiState(
    val donation: DonationRequest? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val scheduleDraft: PickupScheduleSelection = PickupScheduleSelection(),
    val savingDraft: Boolean = false,
    val draftError: PickupScheduleDraftError? = null,
)

internal enum class PickupScheduleDraftError(val message: String) {
    LOAD("Draft di perangkat ini belum bisa dimuat."),
    SAVE("Perubahan draft belum tersimpan di perangkat ini."),
}

internal data class PickupScheduleSelection(
    val dateMillis: Long? = null,
    val minutesOfDay: Int? = null,
)

internal fun DonationRequest.toReview(): DonationReview = DonationReview(
    id = donationId,
    submittedAt = formatAdminTimestamp(createdAt),
    donor = donorName,
    city = city,
    phone = phoneNumber,
    address = pickupAddress,
    clothing = clothingCheck.clothingType.label,
    quantity = quantity,
    assessment = when (clothingCheck.aiResult) {
        AiResult.POTENTIALLY_ELIGIBLE -> "Berpotensi layak"
        AiResult.NEEDS_RECHECK -> "Perlu dicek ulang"
        AiResult.NOT_RECOMMENDED -> "Tidak direkomendasikan"
    },
    assessmentDescription = clothingCheck.aiDescription,
    preference = "$preferredDate · $preferredTime",
    notes = notes,
    status = status,
    schedule = scheduledAt?.let(::formatAdminTimestamp),
    rejectionReason = rejectionReason,
)

internal fun formatAdminTimestamp(value: String): String {
    val withoutFraction = if (value.endsWith("Z") && '.' in value) value.substringBefore('.') + "Z" else value
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).apply { isLenient = false }
    val at = runCatching { parser.parse(withoutFraction)?.time }.getOrNull()
    return at?.let(::formatPickupSchedule) ?: value
}

internal data class DonationReview(
    val id: String,
    val submittedAt: String,
    val donor: String,
    val city: String,
    val phone: String,
    val address: String,
    val clothing: String,
    val quantity: Int,
    val assessment: String,
    val assessmentDescription: String,
    val preference: String,
    val notes: String,
    val status: DonationStatus = DonationStatus.PENDING_REVIEW,
    val schedule: String? = null,
    val rejectionReason: String? = null,
)

internal val SampleDonationReviews = listOf(
    DonationReview(
        id = "LP024",
        submittedAt = "11 Okt, 09.35",
        donor = "Arsyad Setiawan",
        city = "Malang",
        phone = "0812 3456 7890",
        address = "Jl. Contoh No. 12",
        clothing = "Kaus",
        quantity = 3,
        assessment = "Berpotensi layak",
        assessmentDescription = "Tidak tampak sobekan besar pada foto. Hasil AI merupakan pemeriksaan awal.",
        preference = "Pagi",
        notes = "Pakaian telah dicuci",
    ),
    DonationReview(
        id = "LP025",
        submittedAt = "12 Okt, 10.15",
        donor = "Nadia Putri",
        city = "Malang",
        phone = "0812 3456 7891",
        address = "Jl. Contoh No. 23",
        clothing = "Kemeja",
        quantity = 2,
        assessment = "Berpotensi layak",
        assessmentDescription = "Kancing tampak lengkap pada foto. Hasil AI merupakan pemeriksaan awal.",
        preference = "Siang",
        notes = "",
    ),
    DonationReview(
        id = "LP026",
        submittedAt = "12 Okt, 14.10",
        donor = "Raka Pratama",
        city = "Batu",
        phone = "0812 3456 7892",
        address = "Jl. Contoh No. 34",
        clothing = "Jaket",
        quantity = 1,
        assessment = "Berpotensi layak",
        assessmentDescription = "Tidak tampak kerusakan besar pada foto. Hasil AI merupakan pemeriksaan awal.",
        preference = "Sore",
        notes = "",
        status = DonationStatus.COMPLETED,
        schedule = "16 Okt, 13.00 WIB",
    ),
)
