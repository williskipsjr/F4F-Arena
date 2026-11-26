package com.f4f.arena.data.api

import com.f4f.arena.data.model.AuthVerifyResponse
import com.f4f.arena.data.model.LoginRequest
import com.f4f.arena.data.model.LoginResponse
import com.f4f.arena.data.model.RegisterRequest
import com.f4f.arena.data.model.ResetPinRequest
import com.f4f.arena.data.model.ResetPinResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("api/v1/auth/reset-pin")
    suspend fun resetPin(@Body request: ResetPinRequest): Response<ResetPinResponse>

    @GET("api/v1/auth/verify")
    suspend fun verifyAuth(@Header("Authorization") bearerToken: String): Response<AuthVerifyResponse>
}
