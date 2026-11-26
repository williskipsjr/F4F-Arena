package com.f4f.arena.ui.state

enum class UserRole { PLAYER, ADMIN }

enum class PlayerPosition(val shortCode: String, val displayName: String) {
    GK("GK", "Keeper"),
    DEF("DEF", "Defender"),
    MID("MID", "Midfielder"),
    STR("STR", "Striker")
}

enum class MatchFormat(val size: Int, val label: String, val formation: String) {
    SIX_A_SIDE(6, "6-a-side", "1 GK · 2 DEF · 2 MID · 1 STR"),
    SEVEN_A_SIDE(7, "7-a-side", "1 GK · 3 DEF · 2 MID · 1 STR"),
    NINE_A_SIDE(9, "9-a-side", "1 GK · 3 DEF · 3 MID · 2 STR");

    fun mathSummary(playerCount: Int): String {
        val teams = playerCount / size
        val subs = playerCount % size
        val subWord = if (subs == 1) "Sub" else "Subs"
        return "$teams Teams, $subs $subWord"
    }
}

enum class TeamPolicy { STRICT_SUBS, DISTRIBUTE_ALL }

data class UserSessionUi(
    val id: String,
    val name: String,
    val username: String,
    val role: UserRole,
    val playerId: String?
)

data class PlayerUi(
    val id: String,
    val name: String,
    val position: PlayerPosition,
    val isActive: Boolean = true,
    val notes: String = ""
)

data class PollVoteUi(
    val playerId: String,
    val playerName: String,
    val position: PlayerPosition,
    val isIn: Boolean,
    val votedAtLabel: String
)

data class MatchCardUi(
    val id: String,
    val title: String,
    val kickoffTime: String,
    val dateLabel: String,
    val timelineLabel: String,
    val isOngoing: Boolean = false,
    val isEnded: Boolean = false,
    val venue: String,
    val format: MatchFormat,
    val pollOpen: Boolean,
    val inCount: Int,
    val outCount: Int,
    val currentUserVoteIn: Boolean?,
    val teamsReady: Boolean,
    val finalised: Boolean,
    val voterInitials: List<String> = emptyList()
)

data class FeedbackItemUi(
    val id: String,
    val authorName: String,
    val category: String,
    val message: String,
    val createdAtLabel: String
)

data class TeamSlotUi(
    val playerId: String,
    val name: String,
    val position: PlayerPosition,
    val isManual: Boolean = false
)

data class GeneratedTeamUi(
    val id: String,
    val name: String,
    val kitColorName: String,
    val kitColorHex: Long,
    val players: List<TeamSlotUi>,
    val needsEmergencyGk: Boolean
)

data class SystemHealthUi(
    val baseUrl: String,
    val apiConnected: Boolean,
    val postgresConnected: Boolean,
    val latencyMs: Int,
    val lastChecked: String
)
