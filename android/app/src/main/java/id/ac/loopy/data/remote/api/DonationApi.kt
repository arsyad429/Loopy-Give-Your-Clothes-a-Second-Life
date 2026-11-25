package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.CreateDonationRequestDto
import id.ac.loopy.data.remote.dto.DonationDetailDto
import id.ac.loopy.data.remote.dto.DonationListResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DonationApi {
    @POST("v1/donations")
    suspend fun create(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") key: String,
        @Body request: CreateDonationRequestDto,
    ): DonationDetailDto

    @GET("v1/donations")
    suspend fun listOwn(
        @Header("Authorization") bearer: String,
        @Query("status") status: String? = null,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
    ): DonationListResponseDto

    @GET("v1/donations/{id}")
    suspend fun detail(
        @Header("Authorization") bearer: String,
        @Path("id") donationId: String,
    ): DonationDetailDto
}
