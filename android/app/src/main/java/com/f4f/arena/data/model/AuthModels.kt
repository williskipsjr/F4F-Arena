package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("username") val username: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("age") val age: Int,
    @SerializedName("password") val password: String,
    @SerializedName("firebase_id_token") val firebaseIdToken: String? = null
)

data class PhoneLookupRequest(
    @SerializedName("phone") val phone: String
)

data class PhoneLookupResponse(
    @SerializedName("found") val found: Boolean,
    @SerializedName("player_id") val playerId: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("claimed") val claimed: Boolean = false
)

data class ClaimProfileRequest(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class ResetPinRequest(
    @SerializedName("username") val username: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("new_password") val newPassword: String
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class UpdateProfileRequest(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class UpdateProfileResponse(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class ResetPinResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
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
