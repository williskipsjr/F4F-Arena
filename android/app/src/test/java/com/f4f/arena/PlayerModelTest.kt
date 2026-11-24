package com.f4f.arena

import com.f4f.arena.data.model.Player
import com.f4f.arena.data.model.PlayerListResponse
import com.f4f.arena.data.model.Position
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerModelTest {

    @Test
    fun testPositionEnumMapping() {
        assertEquals(Position.KEEPER, Position.fromString("KEEPER"))
        assertEquals(Position.DEFENDER, Position.fromString("defender"))
        assertEquals(Position.MIDFIELDER, Position.fromString("MIDFIELDER"))
        assertEquals(Position.STRIKER, Position.fromString("striker"))
        assertEquals("🧤", Position.KEEPER.emoji)
        assertEquals("GK", Position.KEEPER.shortLabel)
    }

    @Test
    fun testPlayerListDeserialization() {
        val json = """
            {
                "data": [
                    {
                        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                        "display_name": "Lenpu",
                        "primary_position": "KEEPER",
                        "is_active": true
                    },
                    {
                        "id": "c9bf9e57-1685-4c89-bafb-ff5af830be8a",
                        "display_name": "Joseph",
                        "primary_position": "STRIKER",
                        "is_active": true
                    }
                ],
                "total": 2
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, PlayerListResponse::class.java)

        assertNotNull(response)
        assertEquals(2, response.total)
        assertEquals(2, response.data.size)
        assertEquals("Lenpu", response.data[0].displayName)
        assertEquals(Position.KEEPER, response.data[0].primaryPosition)
        assertTrue(response.data[0].isActive)
    }
}
