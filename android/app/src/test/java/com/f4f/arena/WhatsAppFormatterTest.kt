package com.f4f.arena

import com.f4f.arena.data.model.*
import com.f4f.arena.util.WhatsAppFormatter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppFormatterTest {

    @Test
    fun testFormatWithTeamsAndSubstitutes() {
        val data = MatchTeamsData(
            matchId = "m-1",
            format = MatchFormat.SEVEN_A_SIDE,
            subPolicy = SubstitutePolicy.STRICT_SUBSTITUTES,
            status = MatchStatus.FINALISED,
            teamCount = 2,
            teams = listOf(
                TeamDetail(
                    id = "t-1",
                    teamNumber = 1,
                    teamName = "Team 1",
                    bibColor = "Orange",
                    players = listOf(
                        TeamPlayerDetail("p-1", "Lenpu", Position.KEEPER, Position.KEEPER),
                        TeamPlayerDetail("p-2", "Thonglam", Position.DEFENDER, Position.DEFENDER),
                        TeamPlayerDetail("p-3", "Eric", Position.MIDFIELDER, Position.MIDFIELDER),
                        TeamPlayerDetail("p-4", "Joseph", Position.STRIKER, Position.STRIKER)
                    )
                ),
                TeamDetail(
                    id = "t-2",
                    teamNumber = 2,
                    teamName = "Team 2",
                    bibColor = null,
                    players = listOf(
                        TeamPlayerDetail("p-5", "Emergency", Position.MIDFIELDER, Position.KEEPER, isEmergencyKeeper = true),
                        TeamPlayerDetail("p-6", "Robert", Position.DEFENDER, Position.DEFENDER)
                    )
                )
            ),
            substitutes = listOf(
                TeamPlayerDetail("p-7", "Jack", Position.MIDFIELDER, Position.MIDFIELDER)
            ),
            totalAttendees = 7,
            assignedCount = 6,
            substitutesCount = 1
        )

        val formatted = WhatsAppFormatter.format(data, matchTitle = "Sunday Kickoff", venue = "Turf Arena")

        assertTrue(formatted.contains("⚽ *F4F ARENA — MATCH DAY TEAMS*"))
        assertTrue(formatted.contains("📌 *Sunday Kickoff* (7-a-side)"))
        assertTrue(formatted.contains("📍 Venue: Turf Arena"))
        assertTrue(formatted.contains("*TEAM 1 (Orange)*"))
        assertTrue(formatted.contains("🧤 Keeper: Lenpu"))
        assertTrue(formatted.contains("🛡️ Defenders: Thonglam"))
        assertTrue(formatted.contains("⚙️ Midfielders: Eric"))
        assertTrue(formatted.contains("⚡ Strikers: Joseph"))
        assertTrue(formatted.contains("*TEAM 2*"))
        assertTrue(formatted.contains("🧤 Keeper: Emergency (Emergency GK)"))
        assertTrue(formatted.contains("*SUBSTITUTES / BENCH*"))
        assertTrue(formatted.contains("- Jack (MID)"))
        assertTrue(formatted.contains("Let's have a great game! ⚽🔥"))
    }

    @Test
    fun testFormatWithoutSubstitutesOrKeeper() {
        val data = MatchTeamsData(
            matchId = "m-2",
            format = MatchFormat.SIX_A_SIDE,
            subPolicy = SubstitutePolicy.DISTRIBUTE_ALL,
            status = MatchStatus.TEAMS_GENERATED,
            teamCount = 1,
            teams = listOf(
                TeamDetail(
                    id = "t-1",
                    teamNumber = 1,
                    teamName = "Team 1",
                    players = listOf(
                        TeamPlayerDetail("p-1", "Bob", Position.DEFENDER, Position.DEFENDER)
                    )
                )
            ),
            substitutes = emptyList(),
            totalAttendees = 1,
            assignedCount = 1,
            substitutesCount = 0
        )

        val formatted = WhatsAppFormatter.format(data)

        assertTrue(formatted.contains("🧤 Keeper: _Needs Emergency GK_"))
        assertFalse(formatted.contains("SUBSTITUTES"))
    }
}
