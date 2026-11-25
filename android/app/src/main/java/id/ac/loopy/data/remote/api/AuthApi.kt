package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.LoginRequest
import id.ac.loopy.data.remote.dto.RefreshRequest
import id.ac.loopy.data.remote.dto.RegisterRequest
import id.ac.loopy.data.remote.dto.SessionDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): SessionDto

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): SessionDto

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): SessionDto

    @POST("v1/auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String)
}
