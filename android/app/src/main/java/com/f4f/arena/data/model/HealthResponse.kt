package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String,
    @SerializedName("service") val service: String,
    @SerializedName("database") val database: String,
    @SerializedName("timestamp") val timestamp: String
)
