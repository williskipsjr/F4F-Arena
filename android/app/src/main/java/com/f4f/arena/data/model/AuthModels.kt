package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class AuthVerifyResponse(
    @SerializedName("status") val status: String,
    @SerializedName("user") val user: String,
    @SerializedName("role") val role: String
)
