package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

data class GenerateTeamsRequest(
    @SerializedName("sub_policy") val subPolicy: String? = null
)

data class TeamPlayerDetail(
    @SerializedName("id") val id: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("primary_position") val primaryPosition: Position,
    @SerializedName("assigned_position") val assignedPosition: Position,
    @SerializedName("is_emergency_keeper") val isEmergencyKeeper: Boolean = false,
    @SerializedName("is_manual_override") val isManualOverride: Boolean = false
)

data class TeamDetail(
    @SerializedName("id") val id: String,
    @SerializedName("team_number") val teamNumber: Int,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("bib_color") val bibColor: String? = null,
    @SerializedName("players") val players: List<TeamPlayerDetail> = emptyList(),
    @SerializedName("position_counts") val positionCounts: Map<String, Int> = emptyMap()
)

data class MatchTeamsData(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("format") val format: MatchFormat,
    @SerializedName("sub_policy") val subPolicy: SubstitutePolicy,
    @SerializedName("status") val status: MatchStatus,
    @SerializedName("team_count") val teamCount: Int,
    @SerializedName("teams") val teams: List<TeamDetail> = emptyList(),
    @SerializedName("substitutes") val substitutes: List<TeamPlayerDetail> = emptyList(),
    @SerializedName("warnings") val warnings: List<String> = emptyList(),
    @SerializedName("total_attendees") val totalAttendees: Int,
    @SerializedName("assigned_count") val assignedCount: Int,
    @SerializedName("substitutes_count") val substitutesCount: Int
)

data class MatchTeamsResponse(
    @SerializedName("data") val data: MatchTeamsData
)
