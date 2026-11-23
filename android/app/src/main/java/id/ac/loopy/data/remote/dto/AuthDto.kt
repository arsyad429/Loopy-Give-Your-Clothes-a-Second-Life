package id.ac.loopy.data.remote.dto

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
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
)
