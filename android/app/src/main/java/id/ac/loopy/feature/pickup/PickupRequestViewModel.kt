package id.ac.loopy.feature.pickup

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import id.ac.loopy.data.repository.CompletionDemoRepository
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.repository.ClothingCheckRepository
import id.ac.loopy.domain.repository.DonationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

private val ServerFieldMessages = mapOf(
    "checkId" to "Hasil pemeriksaan tidak valid atau bukan milik akun ini.",
    "cleanAndSafeConfirmed" to "Centang konfirmasi pakaian bersih dan aman digunakan.",
    "phoneNumber" to "Periksa nomor telepon. Gunakan 8–15 digit angka yang valid.",
    "pickupAddress" to "Periksa alamat penjemputan (5–300 karakter).",
    "city" to "Periksa kota atau area penjemputan (2–100 karakter).",
    "quantity" to "Jumlah pakaian minimal 1 dan maksimal 100.",
    "preferredDate" to "Tanggal penjemputan tidak boleh di masa lalu.",
    "preferredTime" to "Pilih preferensi waktu penjemputan.",
    "notes" to "Catatan maksimal 300 karakter.",
)

class PickupRequestViewModel(
    private val checkId: String,
    private val donorName: String,
    private val clothingCheckRepository: ClothingCheckRepository?,
    private val donationRepository: DonationRepository?,
    private val demoRepository: CompletionDemoRepository? = null,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PickupRequestUiState(
            donorName = donorName.ifBlank { "Donatur Loopy" },
            isDemoMode = donationRepository == null && demoRepository != null,
            isLoadingCheck = checkId.isNotBlank() && clothingCheckRepository != null,
            cleanAndSafeConfirmed = checkId.isNotBlank(),
        ),
    )
    val uiState: StateFlow<PickupRequestUiState> = _uiState.asStateFlow()

    init {
        if (donationRepository == null && demoRepository != null && checkId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoadingCheck = false,
                    check = sampleDemoCheck(donorName),
                )
            }
        } else {
            loadClothingCheck()
        }
    }

    @MainThread
    fun retryLoadCheck() {
        loadClothingCheck()
    }

    @MainThread
    fun onPhoneNumberChange(value: String) {
        updateField("phoneNumber") { it.copy(phoneNumber = value) }
    }

    @MainThread
    fun onPickupAddressChange(value: String) {
        updateField("pickupAddress") { it.copy(pickupAddress = value) }
    }

    @MainThread
    fun onCityChange(value: String) {
        updateField("city") { it.copy(city = value) }
    }

    @MainThread
    fun onQuantityChange(quantity: Int) {
        val clamped = quantity.coerceIn(0, 100)
        updateField("quantity") { it.copy(quantity = clamped) }
    }

    @MainThread
    fun incrementQuantity() {
        val next = (_uiState.value.quantity + 1).coerceAtMost(100)
        updateField("quantity") { it.copy(quantity = next) }
    }

    @MainThread
    fun decrementQuantity() {
        val next = (_uiState.value.quantity - 1).coerceAtLeast(1)
        updateField("quantity") { it.copy(quantity = next) }
    }

    @MainThread
    fun onPreferredDateChange(isoDate: String) {
        updateField("preferredDate") { it.copy(preferredDate = isoDate) }
    }

    @MainThread
    fun onPreferredTimeChange(slot: String) {
        updateField("preferredTime") { it.copy(preferredTime = slot) }
    }

    @MainThread
    fun onNotesChange(value: String) {
        updateField("notes") { it.copy(notes = value) }
    }

    @MainThread
    fun onCleanAndSafeChange(confirmed: Boolean) {
        updateField("cleanAndSafeConfirmed") { it.copy(cleanAndSafeConfirmed = confirmed) }
    }

    @MainThread
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(submittedDonation = null) }
    }

    @MainThread
    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting || current.submittedDonation != null || !current.canInteractWithForm) {
            return
        }

        val errors = PickupRequestValidator.validate(current, clockMillis())
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    showValidationSummary = true,
                    submitError = "Data belum valid. Perbaiki input yang ditandai sebelum mengirim.",
                )
            }
            return
        }

        val check = current.check ?: return
        _uiState.update {
            it.copy(
                isSubmitting = true,
                fieldErrors = emptyMap(),
                showValidationSummary = false,
                submitError = null,
            )
        }

        viewModelScope.launch {
            try {
                if (donationRepository != null) {
                    val created = donationRepository.createDonation(
                        CreateDonationSubmission(
                            checkId = check.checkId,
                            cleanAndSafeConfirmed = current.cleanAndSafeConfirmed,
                            phoneNumber = current.phoneNumber.trim(),
                            pickupAddress = current.pickupAddress.trim(),
                            city = current.city.trim(),
                            quantity = current.quantity,
                            preferredDate = current.preferredDate.trim(),
                            preferredTime = current.preferredTime.trim(),
                            notes = current.notes.trim(),
                        ),
                    )
                    ensureActive()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submittedDonation = SubmittedPickupSummary(
                                donationId = created.donationId,
                                status = created.status,
                                quantity = created.quantity,
                                city = created.city,
                                preferredDate = created.preferredDate,
                                preferredTime = created.preferredTime,
                                isLocalDemo = false,
                            ),
                        )
                    }
                } else if (demoRepository != null) {
                    val readableDate = PickupRequestValidator.formatReadableDate(current.preferredDate.trim())
                    val scheduleLabel = "Preferensi: $readableDate · ${current.preferredTime.trim()}"
                    val fullAddress = "${current.pickupAddress.trim()}, ${current.city.trim()}"
                    val created = demoRepository.createPendingDonation(
                        quantity = current.quantity,
                        schedule = scheduleLabel,
                        address = fullAddress,
                    )
                    ensureActive()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submittedDonation = SubmittedPickupSummary(
                                donationId = created.id,
                                status = DonationStatus.PENDING_REVIEW,
                                quantity = created.quantity,
                                city = current.city.trim(),
                                preferredDate = current.preferredDate.trim(),
                                preferredTime = current.preferredTime.trim(),
                                isLocalDemo = true,
                            ),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = "Layanan pengajuan donasi belum tersedia.",
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isSubmitting = false) }
                throw cancelled
            } catch (http: HttpException) {
                val (message, fields) = pickupHttpFeedback(http)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        fieldErrors = fields,
                        showValidationSummary = fields.isNotEmpty(),
                        submitError = message,
                    )
                }
            } catch (_: SocketTimeoutException) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = "Koneksi terlalu lama. Coba kirim ulang pengajuan.",
                    )
                }
            } catch (_: IOException) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = "Tidak bisa terhubung ke server. Periksa koneksimu, lalu coba lagi.",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = e.message ?: "Pengajuan gagal disimpan. Coba lagi.",
                    )
                }
            }
        }
    }

    private fun loadClothingCheck() {
        val repo = clothingCheckRepository
        if (checkId.isBlank() || repo == null) {
            _uiState.update {
                it.copy(
                    isLoadingCheck = false,
                    checkLoadError = "Mulai dari pemeriksaan pakaian AI terlebih dahulu.",
                )
            }
            return
        }

        _uiState.update { it.copy(isLoadingCheck = true, checkLoadError = null) }
        viewModelScope.launch {
            try {
                val loaded = repo.getCheckById(checkId)
                ensureActive()
                if (loaded.aiResult != AiResult.POTENTIALLY_ELIGIBLE) {
                    _uiState.update {
                        it.copy(
                            isLoadingCheck = false,
                            check = loaded,
                            checkLoadError = "Hanya pakaian berstatus Berpotensi Layak yang dapat diajukan pickup.",
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingCheck = false,
                            check = loaded,
                            checkLoadError = null,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isLoadingCheck = false) }
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCheck = false,
                        checkLoadError = "Data pemeriksaan pakaian gagal dimuat. Coba lagi.",
                    )
                }
            }
        }
    }

    private inline fun updateField(
        fieldKey: String,
        transform: (PickupRequestUiState) -> PickupRequestUiState,
    ) {
        _uiState.update { state ->
            val updated = transform(state)
            val remainingErrors = updated.fieldErrors - fieldKey
            updated.copy(
                fieldErrors = remainingErrors,
                submitError = if (remainingErrors.isEmpty() && state.fieldErrors.isNotEmpty()) null else updated.submitError,
                showValidationSummary = remainingErrors.isNotEmpty() && updated.showValidationSummary,
            )
        }
    }

    private fun sampleDemoCheck(name: String) = ClothingCheck(
        checkId = "demo-check-01",
        userId = name.ifBlank { "demo-user" },
        clothingPhoto = "",
        clothingType = ClothingType.SHIRT,
        aiResult = AiResult.POTENTIALLY_ELIGIBLE,
        aiDescription = "Pakaian terlihat utuh dan bersih pada foto (Mode pratinjau demo).",
        createdAt = "",
    )
}

internal fun pickupHttpFeedback(error: HttpException): Pair<String, Map<String, String>> {
    val details = error.extractApiError()
    val code = details?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
    return when (error.code()) {
        401 -> "Sesi berakhir. Silakan masuk kembali untuk mengirim pengajuan." to emptyMap()
        404 -> "Hasil pemeriksaan pakaian tidak ditemukan pada akun ini." to emptyMap()
        409 -> if (code == "IDEMPOTENCY_CONFLICT") {
            "Data pengajuan berubah. Periksa ulang lalu kirim kembali." to emptyMap()
        } else {
            "Pengajuan tidak dapat diproses karena konflik versi." to emptyMap()
        }
        422 -> {
            if (code == "CHECK_NOT_ELIGIBLE") {
                "Hanya pemeriksaan berstatus Berpotensi Layak yang dapat diajukan." to emptyMap()
            } else {
                val rawFields = details?.get("fieldErrors")
                    ?.takeIf { it.isJsonObject }?.asJsonObject?.keySet().orEmpty()
                val mapped = ServerFieldMessages.filterKeys { it in rawFields }
                val message = if (mapped.isEmpty()) {
                    "Periksa kembali data dan preferensi jadwal yang dikirim."
                } else {
                    "Data belum valid. Perbaiki input yang ditandai."
                }
                message to mapped
            }
        }
        else -> "Gagal mengirim pengajuan ke server. Coba lagi sebentar." to emptyMap()
    }
}

private fun HttpException.extractApiError(): JsonObject? = try {
    response()?.errorBody()?.use { body ->
        JsonParser.parseReader(body.charStream())
            .takeIf { it.isJsonObject }?.asJsonObject
            ?.get("error")?.takeIf { it.isJsonObject }?.asJsonObject
    }
} catch (_: JsonParseException) {
    null
} catch (_: IOException) {
    null
}
