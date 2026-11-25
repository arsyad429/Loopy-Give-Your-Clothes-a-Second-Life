package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface RewardApi {
    @GET("v1/points/balance")
    suspend fun balance(@Header("Authorization") bearer: String): PointsBalanceDto

    @GET("v1/rewards")
    suspend fun catalog(@Header("Authorization") bearer: String): RewardListDto

    @GET("v1/redemptions")
    suspend fun history(@Header("Authorization") bearer: String): RedemptionListDto

    @POST("v1/redemptions")
    suspend fun redeem(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") requestKey: String,
        @Body request: RedeemRequestDto,
    ): RedemptionResultDto
}
