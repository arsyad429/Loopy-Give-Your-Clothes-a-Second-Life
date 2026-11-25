package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.AdminDonationSummary
import id.ac.loopy.data.remote.dto.AdvanceDonationRequest
import id.ac.loopy.data.remote.dto.DonationDetailDto
import id.ac.loopy.data.remote.dto.DonationListResponseDto
import id.ac.loopy.data.remote.dto.RejectDonationRequest
import id.ac.loopy.data.remote.dto.ScheduleDonationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface AdminApi {
    @GET("v1/admin/summary")
    suspend fun summary(@Header("Authorization") bearer: String): AdminDonationSummary

    @GET("v1/admin/donations")
    suspend fun list(
        @Header("Authorization") bearer: String,
        @Query("status") status: String,
        @Query("query") query: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int? = null,
    ): DonationListResponseDto

    @GET("v1/admin/donations/{id}")
    suspend fun detail(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
    ): DonationDetailDto

    @POST("v1/admin/donations/{id}/schedule")
    suspend fun schedule(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body request: ScheduleDonationRequest,
    ): DonationDetailDto

    @POST("v1/admin/donations/{id}/reject")
    suspend fun reject(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body request: RejectDonationRequest,
    ): DonationDetailDto

    @POST("v1/admin/donations/{id}/status")
    suspend fun advance(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body request: AdvanceDonationRequest,
    ): DonationDetailDto
}
