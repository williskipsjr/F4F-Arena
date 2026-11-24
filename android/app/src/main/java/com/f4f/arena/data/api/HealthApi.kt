package com.f4f.arena.data.api

import com.f4f.arena.data.model.HealthResponse
import retrofit2.Response
import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>
}
