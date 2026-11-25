package id.ac.loopy.feature.pickup

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationCheckReview
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import id.ac.loopy.domain.repository.ClothingCheckRepository
import id.ac.loopy.domain.repository.DonationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class PickupRequestValidationTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixedNowMillis: Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
        set(2026, Calendar.NOVEMBER, 25, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validatorRejectsPastDatesInvalidPhoneZeroQuantityAndUnconfirmedCheck() {
        val invalid = validState().copy(
            phoneNumber = "0812",
            pickupAddress = "Jl",
            city = "A",
            quantity = 0,
            preferredDate = "2026-11-24",
            preferredTime = "",
            notes = "x".repeat(301),
            cleanAndSafeConfirmed = false,
        )

        val errors = PickupRequestValidator.validate(invalid, fixedNowMillis)
        assertTrue("phoneNumber" in errors)
        assertTrue("pickupAddress" in errors)
        assertTrue("city" in errors)
        assertTrue("quantity" in errors)
        assertTrue("preferredDate" in errors)
        assertTrue("preferredTime" in errors)
        assertTrue("notes" in errors)
        assertTrue("cleanAndSafeConfirmed" in errors)
    }

    @Test
    fun validatorRejectsNonEligibleCheckAndImpossibleCalendarDate() {
        val ineligible = validState().copy(
            check = sampleCheck(AiResult.NEEDS_RECHECK),
            preferredDate = "2026-02-30",
        )
        val errors = PickupRequestValidator.validate(ineligible, fixedNowMillis)
        assertTrue("checkId" in errors)
        assertTrue("preferredDate" in errors)
    }

    @Test
    fun validatorAcceptsTodayAndValidInputs() {
        val errors = PickupRequestValidator.validate(validState(), fixedNowMillis)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun validatorEnforcesPhoneFormattingAndLengthBoundaries() {
        assertTrue(
            PickupRequestValidator.validate(
                validState().copy(phoneNumber = "+62 (812) 3456-7890"),
                fixedNowMillis,
            ).isEmpty(),
        )
        assertTrue(
            "phoneNumber" in PickupRequestValidator.validate(
                validState().copy(phoneNumber = "0812345678901234"), // 16 digits
                fixedNowMillis,
            ),
        )
        assertTrue(
            "phoneNumber" in PickupRequestValidator.validate(
                validState().copy(phoneNumber = "+62 - 812 - 3456 - 78"), // 21 chars
                fixedNowMillis,
            ),
        )
        assertTrue(
            "phoneNumber" in PickupRequestValidator.validate(
                validState().copy(phoneNumber = "0812abcdef"),
                fixedNowMillis,
            ),
        )
    }

    @Test
    fun validatorEnforcesTextQuantityAndControlCharBoundaries() {
        val exactMin = validState().copy(
            pickupAddress = "Jl. A",
            city = "BD",
            quantity = 1,
            notes = "",
        )
        assertTrue(PickupRequestValidator.validate(exactMin, fixedNowMillis).isEmpty())

        val exactMax = validState().copy(
            pickupAddress = "a\n" + "b".repeat(298),
            city = "c".repeat(100),
            quantity = 100,
            notes = "n\n" + "d".repeat(298),
        )
        assertTrue(PickupRequestValidator.validate(exactMax, fixedNowMillis).isEmpty())

        val overMax = validState().copy(
            pickupAddress = "a".repeat(301),
            city = "Malang\nJatim",
            quantity = 101,
            notes = "ok\u0007bad",
        )
        val errors = PickupRequestValidator.validate(overMax, fixedNowMillis)
        assertTrue("pickupAddress" in errors)
        assertTrue("city" in errors)
        assertTrue("quantity" in errors)
        assertTrue("notes" in errors)

        val controlAddress = validState().copy(pickupAddress = "Jl. Raya\u000012")
        assertTrue("pickupAddress" in PickupRequestValidator.validate(controlAddress, fixedNowMillis))
    }

    @Test
    fun validatorUsesJakartaTimezoneForTodayAndLeapYearDates() {
        // 2026-11-25 18:00 UTC == 2026-11-26 01:00 WIB (Asia/Jakarta)
        val wibNextDayMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 25, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals("2026-11-26", PickupRequestValidator.todayIsoJakarta(wibNextDayMillis))
        assertFalse(PickupRequestValidator.isDateValidAndNotPast("2026-11-25", wibNextDayMillis))
        assertTrue(PickupRequestValidator.isDateValidAndNotPast("2026-11-26", wibNextDayMillis))

        assertFalse(PickupRequestValidator.isDateValidAndNotPast("2026-02-29", fixedNowMillis))
        assertTrue(PickupRequestValidator.isDateValidAndNotPast("2028-02-29", fixedNowMillis))
        assertEquals("", PickupRequestValidator.formatReadableDate("  "))
        assertEquals("invalid-date", PickupRequestValidator.formatReadableDate("invalid-date"))
    }

    @Test
    fun viewModelBlocksInvalidSubmissionAndClearsFieldErrorOnEdit() = runTest(dispatcher) {
        var submitCalls = 0
        val vm = PickupRequestViewModel(
            checkId = "check-1",
            donorName = "Neva",
            clothingCheckRepository = fakeCheckRepo(sampleCheck(AiResult.POTENTIALLY_ELIGIBLE)),
            donationRepository = object : DonationRepository {
                override suspend fun createDonation(submission: CreateDonationSubmission): DonationRequest {
                    submitCalls++
                    return sampleDonation()
                }

                override suspend fun listMyDonations(status: DonationStatus?, query: String?) = emptyList<DonationSummaryItem>()
                override suspend fun getDonationDetail(donationId: String) = sampleDonation()
            },
            clockMillis = { fixedNowMillis },
        )
        advanceUntilIdle()

        vm.submit()
        assertEquals(0, submitCalls)
        assertTrue(vm.uiState.value.showValidationSummary)
        assertTrue("phoneNumber" in vm.uiState.value.fieldErrors)
        assertTrue("pickupAddress" in vm.uiState.value.fieldErrors)

        vm.onPhoneNumberChange("081234567890")
        assertNull(vm.uiState.value.fieldErrors["phoneNumber"])
    }

    @Test
    fun viewModelClampsQuantityAndPreventsConcurrentDuplicateSubmit() = runTest(dispatcher) {
        val gate = CompletableDeferred<DonationRequest>()
        var submitCalls = 0
        val vm = PickupRequestViewModel(
            checkId = "check-1",
            donorName = "Neva",
            clothingCheckRepository = fakeCheckRepo(sampleCheck(AiResult.POTENTIALLY_ELIGIBLE)),
            donationRepository = object : DonationRepository {
                override suspend fun createDonation(submission: CreateDonationSubmission): DonationRequest {
                    submitCalls++
                    return gate.await()
                }

                override suspend fun listMyDonations(status: DonationStatus?, query: String?) = emptyList<DonationSummaryItem>()
                override suspend fun getDonationDetail(donationId: String) = sampleDonation()
            },
            clockMillis = { fixedNowMillis },
        )
        advanceUntilIdle()

        vm.decrementQuantity()
        assertEquals(1, vm.uiState.value.quantity)
        vm.onQuantityChange(100)
        vm.incrementQuantity()
        assertEquals(100, vm.uiState.value.quantity)
        vm.onQuantityChange(2)

        vm.onPhoneNumberChange("081234567890")
        vm.onPickupAddressChange("Jl. Veteran No. 12, Lowokwaru")
        vm.onCityChange("Malang")
        vm.onPreferredDateChange("2026-11-26")
        vm.onPreferredTimeChange(PreferredTimeSlots.first())

        vm.submit()
        runCurrent()
        assertTrue(vm.uiState.value.isSubmitting)

        // Second click while inflight must be ignored
        vm.submit()
        runCurrent()
        assertEquals(1, submitCalls)

        gate.complete(sampleDonation())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertNotNull(vm.uiState.value.submittedDonation)

        // Subsequent submit after dialog opens must also be blocked
        vm.submit()
        assertEquals(1, submitCalls)

        vm.dismissSuccessDialog()
        assertNull(vm.uiState.value.submittedDonation)
    }

    @Test
    fun viewModelHandlesCheckLoadErrorIneligibleCheckAndRetryRecovery() = runTest(dispatcher) {
        var shouldFail = true
        var currentResult = AiResult.POTENTIALLY_ELIGIBLE
        val repo = object : ClothingCheckRepository {
            override suspend fun analyzeClothing(photoUri: String, clothingType: ClothingType) = sampleCheck(currentResult)
            override suspend fun getCheckById(checkId: String): ClothingCheck {
                if (shouldFail) throw IOException("offline")
                return sampleCheck(currentResult)
            }
            override fun observeCheck(checkId: String) = flowOf(null)
        }

        val vm = PickupRequestViewModel(
            checkId = "check-1",
            donorName = "Neva",
            clothingCheckRepository = repo,
            donationRepository = null,
            clockMillis = { fixedNowMillis },
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canInteractWithForm)
        assertNotNull(vm.uiState.value.checkLoadError)

        shouldFail = false
        currentResult = AiResult.NOT_RECOMMENDED
        vm.retryLoadCheck()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canInteractWithForm)
        assertNotNull(vm.uiState.value.checkLoadError)

        currentResult = AiResult.POTENTIALLY_ELIGIBLE
        vm.retryLoadCheck()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canInteractWithForm)
        assertNull(vm.uiState.value.checkLoadError)
    }

    @Test
    fun viewModelSubmitsValidFormAndEmitsPendingReviewSummary() = runTest(dispatcher) {
        val vm = PickupRequestViewModel(
            checkId = "check-1",
            donorName = "Neva",
            clothingCheckRepository = fakeCheckRepo(sampleCheck(AiResult.POTENTIALLY_ELIGIBLE)),
            donationRepository = object : DonationRepository {
                override suspend fun createDonation(submission: CreateDonationSubmission): DonationRequest {
                    assertEquals("check-1", submission.checkId)
                    assertEquals(2, submission.quantity)
                    assertTrue(submission.cleanAndSafeConfirmed)
                    return sampleDonation()
                }

                override suspend fun listMyDonations(status: DonationStatus?, query: String?) = emptyList<DonationSummaryItem>()
                override suspend fun getDonationDetail(donationId: String) = sampleDonation()
            },
            clockMillis = { fixedNowMillis },
        )
        advanceUntilIdle()

        vm.onPhoneNumberChange("081234567890")
        vm.onPickupAddressChange("Jl. Veteran No. 12, Lowokwaru")
        vm.onCityChange("Malang")
        vm.incrementQuantity()
        vm.onPreferredDateChange("2026-11-26")
        vm.onPreferredTimeChange(PreferredTimeSlots.first())
        vm.submit()
        advanceUntilIdle()

        val summary = vm.uiState.value.submittedDonation
        assertNotNull(summary)
        assertEquals("donation-123", summary?.donationId)
        assertEquals(DonationStatus.PENDING_REVIEW, summary?.status)
    }

    @Test
    fun viewModelSurfacesNetworkTimeoutsAndIoErrors() = runTest(dispatcher) {
        var exceptionToThrow: Throwable = SocketTimeoutException("timeout")
        val vm = PickupRequestViewModel(
            checkId = "check-1",
            donorName = "Neva",
            clothingCheckRepository = fakeCheckRepo(sampleCheck(AiResult.POTENTIALLY_ELIGIBLE)),
            donationRepository = object : DonationRepository {
                override suspend fun createDonation(submission: CreateDonationSubmission): DonationRequest {
                    throw exceptionToThrow
                }

                override suspend fun listMyDonations(status: DonationStatus?, query: String?) = emptyList<DonationSummaryItem>()
                override suspend fun getDonationDetail(donationId: String) = sampleDonation()
            },
            clockMillis = { fixedNowMillis },
        )
        advanceUntilIdle()
        vm.onPhoneNumberChange("081234567890")
        vm.onPickupAddressChange("Jl. Veteran No. 12, Lowokwaru")
        vm.onCityChange("Malang")
        vm.onPreferredDateChange("2026-11-26")
        vm.onPreferredTimeChange(PreferredTimeSlots.first())

        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.submitError.orEmpty().contains("lama", ignoreCase = true))

        exceptionToThrow = IOException("no network")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.submitError.orEmpty().contains("terhubung", ignoreCase = true))
    }

    @Test
    fun serverValidationAndErrorCodesMapToActionableFeedback() {
        val validation = httpError(
            422,
            """{"error":{"code":"VALIDATION_ERROR","fieldErrors":{"preferredDate":"past","quantity":"min"}}}""",
        )
        val (message, fields) = pickupHttpFeedback(validation)
        assertTrue(message.isNotBlank())
        assertTrue("preferredDate" in fields)
        assertTrue("quantity" in fields)

        assertTrue(pickupHttpFeedback(httpError(401)).first.contains("Sesi", ignoreCase = true))
        assertTrue(pickupHttpFeedback(httpError(404)).first.contains("tidak ditemukan", ignoreCase = true))
        assertTrue(
            pickupHttpFeedback(httpError(409, """{"error":{"code":"IDEMPOTENCY_CONFLICT"}}"""))
                .first.contains("berubah", ignoreCase = true),
        )
        assertTrue(
            pickupHttpFeedback(httpError(409, """{"error":{"code":"VERSION_CONFLICT"}}"""))
                .first.contains("konflik", ignoreCase = true),
        )
        assertTrue(
            pickupHttpFeedback(httpError(422, """{"error":{"code":"CHECK_NOT_ELIGIBLE"}}"""))
                .first.contains("Berpotensi Layak", ignoreCase = true),
        )
        assertTrue(pickupHttpFeedback(httpError(422, "not-json{")).second.isEmpty())
        assertTrue(pickupHttpFeedback(httpError(500)).first.isNotBlank())
    }

    private fun httpError(status: Int, body: String = "{}") = HttpException(
        Response.error<Unit>(status, body.toResponseBody()),
    )

    private fun validState() = PickupRequestUiState(
        donorName = "Neva",
        check = sampleCheck(AiResult.POTENTIALLY_ELIGIBLE),
        phoneNumber = "081234567890",
        pickupAddress = "Jl. Veteran No. 12, Lowokwaru",
        city = "Malang",
        quantity = 2,
        preferredDate = "2026-11-25",
        preferredTime = PreferredTimeSlots.first(),
        notes = "Sudah dicuci",
        cleanAndSafeConfirmed = true,
    )

    private fun sampleCheck(result: AiResult) = ClothingCheck(
        checkId = "check-1",
        userId = "user-1",
        clothingPhoto = "",
        clothingType = ClothingType.TSHIRT,
        aiResult = result,
        aiDescription = "Layak",
        createdAt = "2026-11-25T00:00:00Z",
    )

    private fun fakeCheckRepo(check: ClothingCheck) = object : ClothingCheckRepository {
        override suspend fun analyzeClothing(photoUri: String, clothingType: ClothingType) = check
        override suspend fun getCheckById(checkId: String) = check
        override fun observeCheck(checkId: String) = flowOf(check)
    }

    private fun sampleDonation() = DonationRequest(
        donationId = "donation-123",
        donorName = "Neva",
        phoneNumber = "081234567890",
        pickupAddress = "Jl. Veteran No. 12, Lowokwaru",
        city = "Malang",
        quantity = 2,
        preferredDate = "2026-11-26",
        preferredTime = PreferredTimeSlots.first(),
        notes = "",
        status = DonationStatus.PENDING_REVIEW,
        version = 1,
        clothingCheck = DonationCheckReview(
            checkId = "check-1",
            photoKey = "/v1/checks/check-1/photo",
            clothingType = ClothingType.TSHIRT,
            aiResult = AiResult.POTENTIALLY_ELIGIBLE,
            aiDescription = "Layak",
        ),
        createdAt = "2026-11-25T00:00:00Z",
        updatedAt = "2026-11-25T00:00:00Z",
    )
}
