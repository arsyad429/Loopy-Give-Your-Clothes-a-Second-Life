package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.AuthUserDto
import id.ac.loopy.data.remote.dto.LoginRequest
import id.ac.loopy.data.remote.dto.ProfileUpdateRequest
import id.ac.loopy.data.remote.dto.RefreshRequest
import id.ac.loopy.data.remote.dto.RegisterRequest
import id.ac.loopy.data.remote.dto.RegistrationChallengeDto
import id.ac.loopy.data.remote.dto.RegistrationStartDto
import id.ac.loopy.data.remote.dto.ResendRegistrationRequest
import id.ac.loopy.data.remote.dto.SessionDto
import id.ac.loopy.data.remote.dto.VerifyRegistrationRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): SessionDto

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegistrationStartDto

    @POST("v1/auth/register/resend")
    suspend fun resendRegistration(@Body request: ResendRegistrationRequest): RegistrationChallengeDto

    @POST("v1/auth/register/verify")
    suspend fun verifyRegistration(@Body request: VerifyRegistrationRequest): SessionDto

    @GET("v1/auth/me")
    suspend fun profile(@Header("Authorization") bearer: String): AuthUserDto

    @PATCH("v1/auth/me")
    suspend fun updateProfile(@Header("Authorization") bearer: String, @Body request: ProfileUpdateRequest): AuthUserDto

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): SessionDto

    @POST("v1/auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String)
}
