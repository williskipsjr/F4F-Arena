package com.f4f.arena.data.repository

import com.f4f.arena.data.api.PlayerApi
import com.f4f.arena.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerRepository(
    private val playerApi: PlayerApi
) {
    suspend fun getPlayers(active: Boolean? = null, position: String? = null, query: String? = null): Result<List<Player>> =
        withContext(Dispatchers.IO) {
            try {
                val response = playerApi.getPlayers(active, position, query)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to fetch players: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createPlayer(token: String, request: CreatePlayerRequest): Result<Player> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = playerApi.createPlayer(bearer, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Admin sign-in required or session expired (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to create player: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updatePlayer(token: String, id: String, request: UpdatePlayerRequest): Result<Player> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = playerApi.updatePlayer(bearer, id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Admin sign-in required or session expired (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to update player: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
