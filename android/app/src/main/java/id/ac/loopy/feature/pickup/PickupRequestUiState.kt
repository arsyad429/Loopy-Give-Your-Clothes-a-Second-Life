package id.ac.loopy.feature.pickup

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.DonationStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val PreferredTimeSlots = listOf(
    "Pagi (08.00–11.00)",
    "Siang (11.00–14.00)",
    "Sore (14.00–17.00)",
)

data class SubmittedPickupSummary(
    val donationId: String,
    val status: DonationStatus,
    val quantity: Int,
    val city: String,
    val preferredDate: String,
    val preferredTime: String,
) {
    val shortId: String
        get() = if (donationId.length > 8 && donationId.contains('-')) {
            donationId.substringBefore('-').uppercase(Locale.US)
        } else {
            donationId
        }
}

data class PickupRequestUiState(
    val donorName: String = "",
    val check: ClothingCheck? = null,
    val isLoadingCheck: Boolean = false,
    val checkLoadError: String? = null,
    val phoneNumber: String = "",
    val pickupAddress: String = "",
    val city: String = "",
    val quantity: Int = 1,
    val preferredDate: String = "",
    val preferredTime: String = "",
    val notes: String = "",
    val cleanAndSafeConfirmed: Boolean = true,
    val fieldErrors: Map<String, String> = emptyMap(),
    val showValidationSummary: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submittedDonation: SubmittedPickupSummary? = null,
) {
    val isEligibleCheck: Boolean
        get() = check?.aiResult == AiResult.POTENTIALLY_ELIGIBLE

    val canInteractWithForm: Boolean
        get() = !isLoadingCheck && checkLoadError == null && isEligibleCheck
}

object PickupRequestValidator {
    private val jakartaZone = TimeZone.getTimeZone("Asia/Jakarta")
    private val allowedPhoneChars = setOf('+', '-', ' ', '(', ')')

    fun todayIsoJakarta(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = jakartaZone
        }
        return formatter.format(Date(nowMillis))
    }

    fun formatReadableDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = jakartaZone
        }
        val parsed = runCatching { parser.parse(isoDate) }.getOrNull() ?: return isoDate
        val output = SimpleDateFormat("EEEE, d MMM yyyy", Locale.forLanguageTag("id-ID")).apply {
            timeZone = jakartaZone
        }
        return output.format(parsed)
    }

    fun isDateValidAndNotPast(isoDate: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val trimmed = isoDate.trim()
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(trimmed)) return false
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = jakartaZone
        }
        val parsed = runCatching { parser.parse(trimmed) }.getOrNull() ?: return false
        if (parser.format(parsed) != trimmed) return false

        val todayCal = Calendar.getInstance(jakartaZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedCal = Calendar.getInstance(jakartaZone).apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return !selectedCal.before(todayCal)
    }

    fun validate(
        state: PickupRequestUiState,
        nowMillis: Long = System.currentTimeMillis(),
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (state.check == null || state.check.aiResult != AiResult.POTENTIALLY_ELIGIBLE) {
            errors["checkId"] = "Hanya hasil pemeriksaan berpotensi layak yang dapat diajukan."
        }
        if (!state.cleanAndSafeConfirmed) {
            errors["cleanAndSafeConfirmed"] = "Centang konfirmasi pakaian bersih dan aman digunakan."
        }

        val phone = state.phoneNumber.trim()
        val digits = phone.count { it in '0'..'9' }
        val phoneCharsValid = phone.all { it in '0'..'9' || it in allowedPhoneChars }
        if (phone.isEmpty()) {
            errors["phoneNumber"] = "Masukkan nomor telepon yang bisa dihubungi."
        } else if (!phoneCharsValid || digits < 8 || digits > 15 || phone.length > 20) {
            errors["phoneNumber"] = "Gunakan nomor telepon valid (8–15 digit angka)."
        }

        val address = state.pickupAddress.trim()
        if (address.isEmpty()) {
            errors["pickupAddress"] = "Masukkan alamat lengkap penjemputan."
        } else if (address.length < 5) {
            errors["pickupAddress"] = "Alamat terlalu singkat. Gunakan minimal 5 karakter."
        } else if (address.length > 300 || address.any { it != '\n' && it.isISOControl() }) {
            errors["pickupAddress"] = "Alamat maksimal 300 karakter tanpa karakter kontrol."
        }

        val city = state.city.trim()
        if (city.isEmpty()) {
            errors["city"] = "Masukkan kota atau area penjemputan."
        } else if (city.length < 2) {
            errors["city"] = "Nama kota terlalu singkat."
        } else if (city.length > 100 || city.any { it.isISOControl() }) {
            errors["city"] = "Nama kota maksimal 100 karakter."
        }

        if (state.quantity < 1) {
            errors["quantity"] = "Jumlah pakaian minimal 1."
        } else if (state.quantity > 100) {
            errors["quantity"] = "Jumlah pakaian maksimal 100 per pengajuan."
        }

        val preferredDate = state.preferredDate.trim()
        if (preferredDate.isEmpty()) {
            errors["preferredDate"] = "Pilih tanggal penjemputan pilihanmu."
        } else if (!isDateValidAndNotPast(preferredDate, nowMillis)) {
            errors["preferredDate"] = "Tanggal penjemputan tidak boleh di masa lalu."
        }

        val preferredTime = state.preferredTime.trim()
        if (preferredTime.isEmpty()) {
            errors["preferredTime"] = "Pilih preferensi waktu penjemputan."
        } else if (preferredTime.length > 50 || preferredTime.any { it.isISOControl() }) {
            errors["preferredTime"] = "Preferensi waktu tidak valid."
        }

        val notes = state.notes.trim()
        if (notes.length > 300 || notes.any { it != '\n' && it.isISOControl() }) {
            errors["notes"] = "Catatan maksimal 300 karakter."
        }

        return errors
    }
}
