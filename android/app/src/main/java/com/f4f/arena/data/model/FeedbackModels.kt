package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

data class FeedbackItem(
    @SerializedName("id") val id: String,
    @SerializedName("player_id") val playerId: String? = null,
    @SerializedName("author_name") val authorName: String,
    @SerializedName("category") val category: String,
    @SerializedName("message") val message: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreateFeedbackRequest(
    @SerializedName("category") val category: String,
    @SerializedName("message") val message: String
)

data class FeedbackResponse(
    @SerializedName("data") val data: FeedbackItem
)

data class FeedbackListResponse(
    @SerializedName("data") val data: List<FeedbackItem>
)
