package id.ac.loopy.data.repository

import com.google.gson.JsonParseException
import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.remote.dto.LoginRequest
import id.ac.loopy.data.remote.dto.ProfileUpdateRequest
import id.ac.loopy.data.remote.dto.RefreshRequest
import id.ac.loopy.data.remote.dto.RegisterRequest
import id.ac.loopy.data.remote.dto.RegistrationChallengeDto
import id.ac.loopy.data.remote.dto.ResendRegistrationRequest
import id.ac.loopy.data.remote.dto.SessionDto
import id.ac.loopy.data.remote.dto.VerifyRegistrationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class AuthRepositoryImpl(private val api: AuthApi) {
    private val mutex = Mutex()
    private val current = MutableStateFlow<SessionDto?>(null)
    val session = current.asStateFlow()

    suspend fun login(email: String, password: String) = mutex.withLock {
        current.value = api.login(LoginRequest(email, password))
    }

    suspend fun register(name: String, email: String, password: String, username: String): RegistrationChallengeDto? =
        mutex.withLock {
            val result = api.register(RegisterRequest(name, email, password, username))
            val challengeId = result.challengeId
            if (challengeId != null) {
                RegistrationChallengeDto(
                    challengeId,
                    result.maskedEmail ?: throw JsonParseException("Missing masked registration email."),
                    result.expiresInSeconds ?: throw JsonParseException("Missing code expiry."),
                    result.resendAfterSeconds ?: throw JsonParseException("Missing resend delay."),
                )
            } else {
                current.value = SessionDto(
                    result.accessToken ?: throw JsonParseException("Missing access token."),
                    result.refreshToken ?: throw JsonParseException("Missing refresh token."),
                    result.user ?: throw JsonParseException("Missing user."),
                )
                null
            }
        }

    suspend fun resendRegistration(challengeId: String) =
        api.resendRegistration(ResendRegistrationRequest(challengeId))

    suspend fun verifyRegistration(challengeId: String, code: String) = mutex.withLock {
        current.value = api.verifyRegistration(VerifyRegistrationRequest(challengeId, code))
    }

    suspend fun profile() = withBearer(api::profile)

    suspend fun updateProfile(request: ProfileUpdateRequest) =
        withBearer { bearer -> api.updateProfile(bearer, request) }.also { profile ->
            mutex.withLock {
                val active = current.value
                if (active?.user?.userId == profile.userId) current.value = active.copy(user = profile)
            }
        }

    suspend fun logout() = mutex.withLock {
        val active = current.value ?: return@withLock
        try {
            try {
                api.logout("Bearer ${active.accessToken}")
            } catch (error: HttpException) {
                if (error.code() != 401) throw error
                val renewed = refreshLocked(active)
                api.logout("Bearer ${renewed.accessToken}")
            }
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
        }
        current.value = null
    }

    suspend fun <T> withBearer(request: suspend (String) -> T): T {
        val original = current.value ?: error("Silakan masuk terlebih dahulu.")
        try {
            return request("Bearer ${original.accessToken}")
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
        }

        val renewed = mutex.withLock {
            val active = current.value ?: error("Sesi berakhir. Silakan masuk kembali.")
            check(active.user.userId == original.user.userId) { "Akun telah berubah." }

            if (active.accessToken != original.accessToken) {
                active
            } else {
                refreshLocked(active)
            }
        }

        return request("Bearer ${renewed.accessToken}")
    }

    private suspend fun refreshLocked(active: SessionDto): SessionDto {
        try {
            return api.refresh(RefreshRequest(active.refreshToken)).also { current.value = it }
        } catch (error: HttpException) {
            if (error.code() == 401) current.value = null
            throw error
        }
    }
}
