package com.f4f.arena.data.api

import com.f4f.arena.data.model.AuthVerifyResponse
import com.f4f.arena.data.model.ChangePasswordRequest
import com.f4f.arena.data.model.ClaimProfileRequest
import com.f4f.arena.data.model.LoginRequest
import com.f4f.arena.data.model.LoginResponse
import com.f4f.arena.data.model.PhoneLookupRequest
import com.f4f.arena.data.model.PhoneLookupResponse
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

    @POST("api/v1/auth/phone-lookup")
    suspend fun phoneLookup(@Body request: PhoneLookupRequest): Response<PhoneLookupResponse>

    @POST("api/v1/auth/claim-profile")
    suspend fun claimProfile(@Body request: ClaimProfileRequest): Response<LoginResponse>

    @POST("api/v1/auth/reset-pin")
    suspend fun resetPin(@Body request: ResetPinRequest): Response<ResetPinResponse>

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") bearerToken: String,
        @Body request: ChangePasswordRequest
    ): Response<ResetPinResponse>

    @GET("api/v1/auth/verify")
    suspend fun verifyAuth(@Header("Authorization") bearerToken: String): Response<AuthVerifyResponse>
}
