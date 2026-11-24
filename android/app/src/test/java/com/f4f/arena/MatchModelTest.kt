package com.f4f.arena

import com.f4f.arena.data.model.AttendanceSummaryResponse
import com.f4f.arena.data.model.MatchFormat
import com.f4f.arena.data.model.MatchResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MatchModelTest {

    @Test
    fun testMatchFormatProperties() {
        assertEquals(6, MatchFormat.SIX_A_SIDE.playersPerTeam)
        assertEquals(7, MatchFormat.SEVEN_A_SIDE.playersPerTeam)
        assertEquals(9, MatchFormat.NINE_A_SIDE.playersPerTeam)

        assertEquals("6-a-side", MatchFormat.SIX_A_SIDE.label)
        assertEquals(MatchFormat.SEVEN_A_SIDE, MatchFormat.fromString("SEVEN_A_SIDE"))
    }

    @Test
    fun testAttendanceSummaryDeserialization() {
        val json = """
            {
                "data": {
                    "match_id": "m-123",
                    "total_attendees": 28,
                    "keepers": 4,
                    "defenders": 12,
                    "midfielders": 8,
                    "strikers": 4,
                    "suggested_teams": 4,
                    "substitutes_count": 0,
                    "keeper_deficit": 0,
                    "attendees": []
                }
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, AttendanceSummaryResponse::class.java)

        assertNotNull(response)
        assertEquals(28, response.data.totalAttendees)
        assertEquals(4, response.data.suggestedTeams)
        assertEquals(0, response.data.substitutesCount)
        assertEquals(0, response.data.keeperDeficit)
    }
}
