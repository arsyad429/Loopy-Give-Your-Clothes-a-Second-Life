package id.ac.loopy.feature.pickup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.ui.theme.LoopyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PickupRequestScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun invalidSubmissionShowsFixInputPromptAndValidSubmissionOpensSuccessDialog() {
        var navigatedToMyDonations = 0
        compose.setContent {
            var state by remember {
                mutableStateOf(
                    PickupRequestUiState(
                        donorName = "Neva",
                        check = ClothingCheck(
                            checkId = "check-1",
                            userId = "user-1",
                            clothingPhoto = "",
                            clothingType = ClothingType.TSHIRT,
                            aiResult = AiResult.POTENTIALLY_ELIGIBLE,
                            aiDescription = "Layak donasi",
                            createdAt = "",
                        ),
                        preferredDate = PickupRequestValidator.todayIsoJakarta(),
                        cleanAndSafeConfirmed = true,
                    ),
                )
            }
            LoopyTheme {
                PickupRequestContent(
                    state = state,
                    onBack = {},
                    onRetryLoadCheck = {},
                    onRecheck = {},
                    onPhoneChange = { state = state.copy(phoneNumber = it, fieldErrors = state.fieldErrors - "phoneNumber") },
                    onAddressChange = { state = state.copy(pickupAddress = it, fieldErrors = state.fieldErrors - "pickupAddress") },
                    onCityChange = { state = state.copy(city = it, fieldErrors = state.fieldErrors - "city") },
                    onIncrementQuantity = { state = state.copy(quantity = state.quantity + 1) },
                    onDecrementQuantity = {},
                    onDateSelected = {},
                    onTimeSelected = { state = state.copy(preferredTime = it, fieldErrors = state.fieldErrors - "preferredTime") },
                    onNotesChange = { state = state.copy(notes = it) },
                    onCleanAndSafeChange = { state = state.copy(cleanAndSafeConfirmed = it) },
                    onSubmit = {
                        val errors = PickupRequestValidator.validate(state)
                        if (errors.isNotEmpty()) {
                            state = state.copy(
                                fieldErrors = errors,
                                showValidationSummary = true,
                                submitError = "Data belum valid. Perbaiki input.",
                            )
                        } else {
                            state = state.copy(
                                submittedDonation = SubmittedPickupSummary(
                                    donationId = "LP777",
                                    status = DonationStatus.PENDING_REVIEW,
                                    quantity = state.quantity,
                                    city = state.city,
                                    preferredDate = state.preferredDate,
                                    preferredTime = state.preferredTime,
                                ),
                            )
                        }
                    },
                    onGoToMyDonations = { navigatedToMyDonations++ },
                )
            }
        }

        // 1. Klik submit saat data kosong -> menampilkan Perbaiki input
        compose.onNodeWithText("Ajukan Penjemputan").performClick()
        compose.onNodeWithText("Perbaiki input").assertIsDisplayed()
        compose.onNodeWithText("Masukkan nomor telepon yang bisa dihubungi.").performScrollTo().assertIsDisplayed()

        // 2. Perbaiki input dan pilih preferensi waktu -> submit -> dialog Pending Review -> My Donations
        compose.onNodeWithContentDescription("Nomor telepon").performScrollTo().performTextInput("081234567890")
        compose.onNodeWithContentDescription("Alamat penjemputan").performScrollTo().performTextInput("Jl. Veteran No. 12")
        compose.onNodeWithContentDescription("Kota / Area").performScrollTo().performTextInput("Malang")
        compose.onNodeWithText(PreferredTimeSlots.first()).performScrollTo().performClick()

        compose.onNodeWithText("Ajukan Penjemputan").performClick()
        compose.onNodeWithText("Pengajuan terkirim!").assertIsDisplayed()
        compose.onNodeWithText("Menunggu verifikasi").assertIsDisplayed()
        compose.onNodeWithText("Lihat My Donations").performClick()
        compose.runOnIdle { assertEquals(1, navigatedToMyDonations) }
    }
}
