package com.f4f.arena.data.model

import com.google.gson.annotations.SerializedName

enum class MatchFormat(val label: String, val templateDescription: String, val playersPerTeam: Int) {
    @SerializedName("SIX_A_SIDE")
    SIX_A_SIDE("6-a-side", "1 GK • 2 DEF • 2 MID • 1 STR", 6),

    @SerializedName("SEVEN_A_SIDE")
    SEVEN_A_SIDE("7-a-side", "1 GK • 3 DEF • 2 MID • 1 STR", 7),

    @SerializedName("NINE_A_SIDE")
    NINE_A_SIDE("9-a-side", "1 GK • 3 DEF • 3 MID • 2 STR", 9);

    companion object {
        fun fromString(str: String): MatchFormat {
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) } ?: SEVEN_A_SIDE
        }
    }
}

enum class MatchStatus(val label: String) {
    @SerializedName("DRAFT")
    DRAFT("Draft / Check-In Open"),

    @SerializedName("ATTENDANCE_LOCKED")
    ATTENDANCE_LOCKED("Attendance Locked"),

    @SerializedName("TEAMS_GENERATED")
    TEAMS_GENERATED("Teams Generated"),

    @SerializedName("FINALISED")
    FINALISED("Finalised"),

    @SerializedName("COMPLETED")
    COMPLETED("Completed"),

    @SerializedName("CANCELLED")
    CANCELLED("Cancelled")
}

enum class SubstitutePolicy(val label: String) {
    @SerializedName("STRICT_SUBSTITUTES")
    STRICT_SUBSTITUTES("Strict Substitutes (Rotating Bench)"),

    @SerializedName("DISTRIBUTE_ALL")
    DISTRIBUTE_ALL("Distribute All (Uneven Team Expansion)")
}

data class Match(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("match_date") val matchDate: String? = null,
    @SerializedName("kickoff_time") val kickoffTime: String? = null,
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("format") val format: MatchFormat,
    @SerializedName("status") val status: MatchStatus,
    @SerializedName("sub_policy") val subPolicy: SubstitutePolicy,
    @SerializedName("is_poll_open") val isPollOpen: Boolean = false,
    @SerializedName("in_count") val inCount: Int = 0,
    @SerializedName("out_count") val outCount: Int = 0,
    @SerializedName("my_vote") val myVote: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class MatchListResponse(
    @SerializedName("data") val data: List<Match>,
    @SerializedName("total") val total: Int
)

data class MatchResponse(
    @SerializedName("data") val data: Match
)

data class MatchAttendee(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("player_id") val playerId: String,
    @SerializedName("player") val player: Player,
    @SerializedName("is_late_arrival") val isLateArrival: Boolean = false,
    @SerializedName("checked_in_at") val checkedInAt: String? = null
)

data class AttendanceSummary(
    @SerializedName("match_id") val matchId: String,
    @SerializedName("total_attendees") val totalAttendees: Int,
    @SerializedName("keepers") val keepers: Int,
    @SerializedName("defenders") val defenders: Int,
    @SerializedName("midfielders") val midfielders: Int,
    @SerializedName("strikers") val strikers: Int,
    @SerializedName("suggested_teams") val suggestedTeams: Int,
    @SerializedName("substitutes_count") val substitutesCount: Int,
    @SerializedName("keeper_deficit") val keeperDeficit: Int,
    @SerializedName("attendees") val attendees: List<MatchAttendee>
)

data class AttendanceSummaryResponse(
    @SerializedName("data") val data: AttendanceSummary
)

data class CreateMatchRequest(
    @SerializedName("title") val title: String,
    @SerializedName("match_date") val matchDate: String? = null,
    @SerializedName("kickoff_time") val kickoffTime: String? = null,
    @SerializedName("venue") val venue: String? = null,
    @SerializedName("format") val format: String,
    @SerializedName("sub_policy") val subPolicy: String? = null,
    @SerializedName("is_poll_open") val isPollOpen: Boolean = true,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateMatchRequest(
    @SerializedName("format") val format: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("is_poll_open") val isPollOpen: Boolean? = null
)

data class CastVoteRequest(
    @SerializedName("vote") val vote: String
)

data class MatchVoteDetail(
    @SerializedName("player_id") val playerId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("primary_position") val primaryPosition: Position,
    @SerializedName("vote") val vote: String,
    @SerializedName("voted_at") val votedAt: String
)

data class MatchVotesResponse(
    @SerializedName("data") val data: List<MatchVoteDetail>
)

data class SyncAttendeesRequest(
    @SerializedName("player_ids") val playerIds: List<String>
)

data class AddLateArrivalRequest(
    @SerializedName("player_id") val playerId: String
)
