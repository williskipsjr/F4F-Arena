package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("username") val username: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("password") val password: String,
    @SerializedName("primary_position") val primaryPosition: String? = null,
    @SerializedName("player_id") val playerId: String? = null
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("player_id") val playerId: String? = null,
    @SerializedName("display_name") val displayName: String? = null
)

data class AuthVerifyResponse(
    @SerializedName("status") val status: String,
    @SerializedName("user") val user: String,
    @SerializedName("role") val role: String
)
