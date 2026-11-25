package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.DonationItemDto
import retrofit2.http.GET
import retrofit2.http.Header

internal interface HomeApi {
    @GET("v1/points/balance")
    suspend fun points(@Header("Authorization") bearer: String): HomePointsDto

    @GET("v1/donations/latest")
    suspend fun latestDonation(@Header("Authorization") bearer: String): HomeDonationDto
}

internal data class HomePointsDto(val balance: Int)
internal data class HomeDonationDto(val item: DonationItemDto?)
