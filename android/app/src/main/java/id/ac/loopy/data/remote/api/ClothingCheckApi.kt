package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.ClothingCheckDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ClothingCheckApi {
    @Multipart
    @POST("v1/checks")
    suspend fun analyze(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") key: String,
        @Part("clothingType") clothingType: RequestBody,
        @Part image: MultipartBody.Part,
    ): ClothingCheckDto

    @GET("v1/checks/{id}")
    suspend fun detail(
        @Header("Authorization") bearer: String,
        @Path("id") checkId: String,
    ): ClothingCheckDto

    @Streaming
    @GET("v1/checks/{id}/photo")
    suspend fun photo(
        @Header("Authorization") bearer: String,
        @Path("id") checkId: String,
    ): ResponseBody
}
