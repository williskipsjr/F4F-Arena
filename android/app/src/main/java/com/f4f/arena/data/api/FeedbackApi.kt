package com.f4f.arena.data.api

import com.f4f.arena.data.model.CreateFeedbackRequest
import com.f4f.arena.data.model.FeedbackListResponse
import com.f4f.arena.data.model.FeedbackResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface FeedbackApi {
    @POST("api/v1/feedback")
    suspend fun submitFeedback(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateFeedbackRequest
    ): Response<FeedbackResponse>

    @GET("api/v1/feedback")
    suspend fun listFeedback(
        @Header("Authorization") bearerToken: String
    ): Response<FeedbackListResponse>
}
