package id.ac.loopy.data.repository

import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.remote.api.DonationApi
import id.ac.loopy.data.remote.dto.AuthUserDto
import id.ac.loopy.data.remote.dto.ClothingReviewDto
import id.ac.loopy.data.remote.dto.CreateDonationRequestDto
import id.ac.loopy.data.remote.dto.DonationDetailDto
import id.ac.loopy.data.remote.dto.DonationItemDto
import id.ac.loopy.data.remote.dto.DonationListResponseDto
import id.ac.loopy.data.remote.dto.LoginRequest
import id.ac.loopy.data.remote.dto.ProfileUpdateRequest
import id.ac.loopy.data.remote.dto.RefreshRequest
import id.ac.loopy.data.remote.dto.RegisterRequest
import id.ac.loopy.data.remote.dto.RegistrationStartDto
import id.ac.loopy.data.remote.dto.ResendRegistrationRequest
import id.ac.loopy.data.remote.dto.SessionDto
import id.ac.loopy.data.remote.dto.VerifyRegistrationRequest
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DonationRepositoryImplTest {
    @Test
    fun createDonationRequiresActiveSession() = runBlocking {
        val repo = DonationRepositoryImpl(FakeDonationApi(), AuthRepositoryImpl(FakeAuthApi()))
        val error = runCatching { repo.createDonation(sampleSubmission()) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun createDonationReusesIdempotencyKeyAcrossFailureRetriesAndRotatesAfterSuccess() = runBlocking {
        val donationApi = FakeDonationApi()
        val auth = AuthRepositoryImpl(FakeAuthApi()).apply { login("neva@example.test", "secret-passphrase-123") }
        val repo = DonationRepositoryImpl(donationApi, auth)
        val submission = sampleSubmission()

        // First attempt fails with IOException
        donationApi.failCreateWith = IOException("network drop")
        assertTrue(runCatching { repo.createDonation(submission) }.isFailure)

        // Second attempt with identical submission & same user must keep identical Idempotency-Key
        donationApi.failCreateWith = null
        val firstCreated = repo.createDonation(submission)
        assertEquals(2, donationApi.recordedKeys.size)
        assertEquals(donationApi.recordedKeys[0], donationApi.recordedKeys[1])
        assertTrue(donationApi.recordedKeys[0].length in 8..128)
        assertEquals("don-1", firstCreated.donationId)

        // Subsequent submission after success must mint a new Idempotency-Key
        repo.createDonation(submission)
        assertEquals(3, donationApi.recordedKeys.size)
        assertNotEquals(donationApi.recordedKeys[1], donationApi.recordedKeys[2])

        // Modified payload after failure must mint a new Idempotency-Key
        donationApi.failCreateWith = IOException("temporary")
        assertTrue(runCatching { repo.createDonation(submission.copy(quantity = 4)) }.isFailure)
        donationApi.failCreateWith = null
        repo.createDonation(submission.copy(quantity = 5))
        assertEquals(5, donationApi.recordedKeys.size)
        assertNotEquals(donationApi.recordedKeys[3], donationApi.recordedKeys[4])
    }

    @Test
    fun createAndListRefreshExpiredBearerTokenTransparently() = runBlocking {
        val authApi = FakeAuthApi()
        val donationApi = FakeDonationApi().apply { rejectToken = "token-1" }
        val auth = AuthRepositoryImpl(authApi).apply { login("neva@example.test", "secret-passphrase-123") }
        val repo = DonationRepositoryImpl(donationApi, auth)

        repo.createDonation(sampleSubmission())
        assertEquals(1, authApi.refreshCalls)
        assertEquals("Bearer token-2", donationApi.lastBearer)

        val list = repo.listMyDonations(status = DonationStatus.PENDING_REVIEW, query = "   ")
        assertEquals(1, list.size)
        assertEquals("PENDING_REVIEW", donationApi.lastListStatus)
        assertNull(donationApi.lastListQuery)
    }

    @Test
    fun logoutRefreshesExpiredAccessTokenAndClearsSessionAfterRevocation() = runBlocking {
        val api = FakeAuthApi().apply { expireAccessTokenOnLogout = true }
        val auth = AuthRepositoryImpl(api)
        auth.login("neva@example.test", "secret-passphrase-123")

        auth.logout()

        assertEquals(listOf("Bearer token-1", "Bearer token-2"), api.logoutBearers)
        assertEquals(1, api.refreshCalls)
        assertNull(auth.session.value)
    }

    private fun sampleSubmission() = CreateDonationSubmission(
        checkId = "chk-1",
        cleanAndSafeConfirmed = true,
        phoneNumber = "081234567890",
        pickupAddress = "Jl. Veteran 12",
        city = "Malang",
        quantity = 2,
        preferredDate = "2026-11-26",
        preferredTime = "Pagi (08.00–11.00)",
        notes = "",
    )

    private class FakeAuthApi : AuthApi {
        var refreshCalls = 0
        var expireAccessTokenOnLogout = false
        val logoutBearers = mutableListOf<String>()

        override suspend fun logout(bearer: String) {
            logoutBearers += bearer
            if (expireAccessTokenOnLogout && bearer == "Bearer token-1") {
                throw HttpException(Response.error<Unit>(401, "".toResponseBody()))
            }
        }

        override suspend fun login(request: LoginRequest) = SessionDto(
            accessToken = "token-1",
            refreshToken = "refresh-1",
            user = AuthUserDto("user-1", "Neva", "USER"),
        )

        override suspend fun register(request: RegisterRequest): RegistrationStartDto = error("Unexpected registration")

        override suspend fun resendRegistration(request: ResendRegistrationRequest) = error("Unexpected code resend")

        override suspend fun verifyRegistration(request: VerifyRegistrationRequest) = error("Unexpected verification")

        override suspend fun profile(bearer: String) = error("Unexpected profile read")

        override suspend fun updateProfile(bearer: String, request: ProfileUpdateRequest) = error("Unexpected profile update")

        override suspend fun refresh(request: RefreshRequest): SessionDto {
            refreshCalls++
            return SessionDto(
                accessToken = "token-2",
                refreshToken = "refresh-2",
                user = AuthUserDto("user-1", "Neva", "USER"),
            )
        }
    }

    private class FakeDonationApi : DonationApi {
        val recordedKeys = mutableListOf<String>()
        var failCreateWith: Throwable? = null
        var rejectToken: String? = null
        var lastBearer: String? = null
        var lastListStatus: String? = null
        var lastListQuery: String? = "sentinel"

        override suspend fun create(
            bearer: String,
            key: String,
            request: CreateDonationRequestDto,
        ): DonationDetailDto {
            lastBearer = bearer
            if (rejectToken != null && bearer == "Bearer $rejectToken") {
                throw HttpException(Response.error<Unit>(401, "{}".toResponseBody()))
            }
            recordedKeys += key
            failCreateWith?.let { throw it }
            return sampleDetailDto()
        }

        override suspend fun listOwn(
            bearer: String,
            status: String?,
            query: String?,
            limit: Int?,
            cursor: String?,
        ): DonationListResponseDto {
            lastBearer = bearer
            lastListStatus = status
            lastListQuery = query
            return DonationListResponseDto(
                items = listOf(
                    DonationItemDto("don-1", "Neva", "Malang", 2, "PENDING_REVIEW", 1, "2026-11-25T00:00:00Z", "2026-11-25T00:00:00Z"),
                ),
            )
        }

        override suspend fun detail(bearer: String, donationId: String) = sampleDetailDto()

        private fun sampleDetailDto() = DonationDetailDto(
            donationId = "don-1",
            donorName = "Neva",
            city = "Malang",
            quantity = 2,
            status = "PENDING_REVIEW",
            version = 1,
            createdAt = "2026-11-25T00:00:00Z",
            updatedAt = "2026-11-25T00:00:00Z",
            phoneNumber = "081234567890",
            pickupAddress = "Jl. Veteran 12",
            preferredDate = "2026-11-26",
            preferredTime = "Pagi (08.00–11.00)",
            notes = "",
            clothingCheck = ClothingReviewDto("chk-1", "/v1/checks/chk-1/photo", "TSHIRT", "POTENTIALLY_ELIGIBLE", "Layak"),
        )
    }
}
