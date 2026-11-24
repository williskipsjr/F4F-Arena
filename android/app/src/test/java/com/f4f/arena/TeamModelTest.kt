package com.f4f.arena

import com.f4f.arena.data.model.MatchFormat
import com.f4f.arena.data.model.MatchTeamsResponse
import com.f4f.arena.data.model.Position
import com.f4f.arena.data.model.SubstitutePolicy
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamModelTest {

    @Test
    fun testMatchTeamsResponseDeserialization() {
        val json = """
            {
                "data": {
                    "match_id": "match-uuid-1",
                    "format": "SEVEN_A_SIDE",
                    "sub_policy": "STRICT_SUBSTITUTES",
                    "status": "TEAMS_GENERATED",
                    "team_count": 2,
                    "teams": [
                        {
                            "id": "team-uuid-1",
                            "team_number": 1,
                            "team_name": "Team 1",
                            "bib_color": "Orange",
                            "players": [
                                {
                                    "id": "p-1",
                                    "display_name": "Lenpu",
                                    "primary_position": "KEEPER",
                                    "assigned_position": "KEEPER",
                                    "is_emergency_keeper": false,
                                    "is_manual_override": false
                                },
                                {
                                    "id": "p-2",
                                    "display_name": "Thonglam",
                                    "primary_position": "DEFENDER",
                                    "assigned_position": "DEFENDER",
                                    "is_emergency_keeper": false,
                                    "is_manual_override": false
                                }
                            ],
                            "position_counts": {
                                "KEEPER": 1,
                                "DEFENDER": 1
                            }
                        }
                    ],
                    "substitutes": [
                        {
                            "id": "p-15",
                            "display_name": "Jack",
                            "primary_position": "MIDFIELDER",
                            "assigned_position": "MIDFIELDER",
                            "is_emergency_keeper": false,
                            "is_manual_override": false
                        }
                    ],
                    "warnings": [
                        "Team 2 has no natural goalkeeper (Needs Emergency GK)"
                    ],
                    "total_attendees": 15,
                    "assigned_count": 14,
                    "substitutes_count": 1
                }
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, MatchTeamsResponse::class.java)

        assertNotNull(response)
        assertEquals("match-uuid-1", response.data.matchId)
        assertEquals(MatchFormat.SEVEN_A_SIDE, response.data.format)
        assertEquals(SubstitutePolicy.STRICT_SUBSTITUTES, response.data.subPolicy)
        assertEquals(2, response.data.teamCount)
        assertEquals(1, response.data.teams.size)
        assertEquals("Team 1", response.data.teams[0].teamName)
        assertEquals("Orange", response.data.teams[0].bibColor)
        assertEquals(2, response.data.teams[0].players.size)
        assertEquals(Position.KEEPER, response.data.teams[0].players[0].assignedPosition)
        assertEquals(1, response.data.substitutes.size)
        assertEquals("Jack", response.data.substitutes[0].displayName)
        assertEquals(1, response.data.warnings.size)
        assertTrue(response.data.warnings[0].contains("Needs Emergency GK"))
        assertEquals(15, response.data.totalAttendees)
        assertEquals(14, response.data.assignedCount)
        assertEquals(1, response.data.substitutesCount)
    }

    @Test
    fun testSwapAndMovePlayerSerialization() {
        val gson = Gson()

        val swap = com.f4f.arena.data.model.SwapPlayersRequest("p-1", "p-2")
        val swapJson = gson.toJson(swap)
        assertTrue(swapJson.contains(""""player_a_id":"p-1""""))
        assertTrue(swapJson.contains(""""player_b_id":"p-2""""))

        val move = com.f4f.arena.data.model.MovePlayerRequest(
            playerId = "p-3",
            targetTeamId = "t-1",
            isSubstitute = false,
            assignedPosition = "DEFENDER"
        )
        val moveJson = gson.toJson(move)
        assertTrue(moveJson.contains(""""player_id":"p-3""""))
        assertTrue(moveJson.contains(""""target_team_id":"t-1""""))
        assertTrue(moveJson.contains(""""is_substitute":false"""))
    }
}
