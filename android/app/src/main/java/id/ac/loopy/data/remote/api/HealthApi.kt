package id.ac.loopy.data.remote.api

import id.ac.loopy.data.remote.dto.HealthDto
import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun health(): HealthDto
}
