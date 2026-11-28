package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

enum class Position(val label: String, val shortLabel: String, val emoji: String) {
    @SerializedName("KEEPER")
    KEEPER("Keeper", "GK", "🧤"),

    @SerializedName("DEFENDER")
    DEFENDER("Defender", "DEF", "🛡️"),

    @SerializedName("MIDFIELDER")
    MIDFIELDER("Midfielder", "MID", "⚙️"),

    @SerializedName("STRIKER")
    STRIKER("Striker", "STR", "⚡");

    companion object {
        fun fromString(str: String): Position? {
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) }
        }
    }
}

data class Player(
    @SerializedName("id") val id: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("primary_position") val primaryPosition: Position,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("avatar_emoji") val avatarEmoji: String = "⚽",
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class PlayerListResponse(
    @SerializedName("data") val data: List<Player>,
    @SerializedName("total") val total: Int
)

data class CreatePlayerRequest(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("primary_position") val primaryPosition: String,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("notes") val notes: String? = null
)

data class CreatePlayerResponse(
    @SerializedName("data") val data: Player,
    @SerializedName("warnings") val warnings: List<String>? = null
)

data class UpdatePlayerRequest(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("primary_position") val primaryPosition: String? = null,
    @SerializedName("avatar_emoji") val avatarEmoji: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdatePlayerResponse(
    @SerializedName("data") val data: Player
)
