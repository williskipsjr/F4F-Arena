package com.f4f.arena.util

import com.f4f.arena.data.model.MatchTeamsData
import com.f4f.arena.data.model.Position

object WhatsAppFormatter {

    /**
     * Formats the latest MatchTeamsData into a WhatsApp-ready plain-text message,
     * grouping players by their assigned position and listing any substitutes.
     */
    fun format(data: MatchTeamsData, matchTitle: String? = null, venue: String? = null): String {
        val sb = StringBuilder()
        sb.appendLine("⚽ *F4F ARENA — MATCH DAY TEAMS*")
        if (!matchTitle.isNullOrBlank()) {
            sb.appendLine("📌 *${matchTitle.trim()}* (${data.format.label})")
        } else {
            sb.appendLine("📌 Format: *${data.format.label}*")
        }
        if (!venue.isNullOrBlank()) {
            sb.appendLine("📍 Venue: ${venue.trim()}")
        }
        sb.appendLine()

        data.teams.forEach { team ->
            val header = if (!team.bibColor.isNullOrBlank()) {
                "*${team.teamName.uppercase()} (${team.bibColor.trim()})*"
            } else {
                "*${team.teamName.uppercase()}*"
            }
            sb.appendLine(header)

            val keepers = mutableListOf<String>()
            val defenders = mutableListOf<String>()
            val midfielders = mutableListOf<String>()
            val strikers = mutableListOf<String>()

            team.players.forEach { player ->
                val name = if (player.isEmergencyKeeper) {
                    "${player.displayName} (Emergency GK)"
                } else {
                    player.displayName
                }
                when (player.assignedPosition) {
                    Position.KEEPER -> keepers.add(name)
                    Position.DEFENDER -> defenders.add(name)
                    Position.MIDFIELDER -> midfielders.add(name)
                    Position.STRIKER -> strikers.add(name)
                }
            }

            if (keepers.isNotEmpty()) {
                sb.appendLine("🧤 Keeper: ${keepers.joinToString(", ")}")
            } else {
                sb.appendLine("🧤 Keeper: _Needs Emergency GK_")
            }
            if (defenders.isNotEmpty()) {
                sb.appendLine("🛡️ Defenders: ${defenders.joinToString(", ")}")
            }
            if (midfielders.isNotEmpty()) {
                sb.appendLine("⚙️ Midfielders: ${midfielders.joinToString(", ")}")
            }
            if (strikers.isNotEmpty()) {
                sb.appendLine("⚡ Strikers: ${strikers.joinToString(", ")}")
            }
            sb.appendLine()
        }

        if (data.substitutes.isNotEmpty()) {
            sb.appendLine("*SUBSTITUTES / BENCH*")
            data.substitutes.forEach { sub ->
                sb.appendLine("- ${sub.displayName} (${positionShort(sub.primaryPosition)})")
            }
            sb.appendLine()
        }

        sb.append("Let's have a great game! ⚽🔥")
        return sb.toString()
    }

    private fun positionShort(pos: Position): String = when (pos) {
        Position.KEEPER -> "GK"
        Position.DEFENDER -> "DEF"
        Position.MIDFIELDER -> "MID"
        Position.STRIKER -> "STR"
    }
}
