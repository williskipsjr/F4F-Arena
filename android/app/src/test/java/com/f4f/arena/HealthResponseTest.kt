package com.f4f.arena

import com.f4f.arena.data.model.HealthResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HealthResponseTest {

    @Test
    fun testHealthResponseJsonDeserialization() {
        val json = """
            {
                "status": "healthy",
                "version": "1.0.0",
                "service": "f4f-arena-api",
                "database": "connected",
                "timestamp": "2026-11-24T10:00:00Z"
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, HealthResponse::class.java)

        assertNotNull(response)
        assertEquals("healthy", response.status)
        assertEquals("1.0.0", response.version)
        assertEquals("f4f-arena-api", response.service)
        assertEquals("connected", response.database)
        assertEquals("2026-11-24T10:00:00Z", response.timestamp)
    }
}
