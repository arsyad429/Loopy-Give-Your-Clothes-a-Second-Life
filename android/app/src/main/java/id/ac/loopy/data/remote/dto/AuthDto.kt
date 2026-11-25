package id.ac.loopy.data.remote.dto

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val username: String = "",
)

data class RegistrationChallengeDto(
    val challengeId: String,
    val maskedEmail: String,
    val expiresInSeconds: Int,
    val resendAfterSeconds: Int,
)

data class RegistrationStartDto(
    val challengeId: String? = null,
    val maskedEmail: String? = null,
    val expiresInSeconds: Int? = null,
    val resendAfterSeconds: Int? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: AuthUserDto? = null,
)

data class ResendRegistrationRequest(val challengeId: String)
data class VerifyRegistrationRequest(val challengeId: String, val code: String)

data class ProfileUpdateRequest(
    val name: String,
    val username: String,
    val currentPassword: String? = null,
    val newPassword: String? = null,
)

data class RefreshRequest(val refreshToken: String)

data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserDto,
)

data class AuthUserDto(
    val userId: String,
    val name: String,
    val role: String,
    val email: String = "",
    val username: String? = null,
    val totalPoints: Int = 0,
    val emailVerified: Boolean = false,
)
