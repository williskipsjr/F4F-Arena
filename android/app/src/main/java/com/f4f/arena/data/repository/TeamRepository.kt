package com.f4f.arena.data.repository

import com.f4f.arena.data.api.TeamApi
import com.f4f.arena.data.model.GenerateTeamsRequest
import com.f4f.arena.data.model.MatchTeamsData
import com.f4f.arena.data.model.MovePlayerRequest
import com.f4f.arena.data.model.SwapPlayersRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TeamRepository(
    private val teamApi: TeamApi
) {
    suspend fun generateTeams(token: String, matchId: String, subPolicy: String? = null): Result<MatchTeamsData> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = teamApi.generateTeams(bearer, matchId, GenerateTeamsRequest(subPolicy))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to generate teams: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getTeams(matchId: String): Result<MatchTeamsData> =
        withContext(Dispatchers.IO) {
            try {
                val response = teamApi.getTeams(matchId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to get teams: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun swapPlayers(token: String, matchId: String, playerAId: String, playerBId: String): Result<MatchTeamsData> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = teamApi.swapPlayers(bearer, matchId, SwapPlayersRequest(playerAId, playerBId))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to swap players: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun movePlayer(
        token: String,
        matchId: String,
        playerId: String,
        targetTeamId: String?,
        isSubstitute: Boolean,
        assignedPosition: String? = null
    ): Result<MatchTeamsData> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = teamApi.movePlayer(
                    bearer,
                    matchId,
                    MovePlayerRequest(playerId, targetTeamId, isSubstitute, assignedPosition)
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to move player: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun finaliseMatch(token: String, matchId: String): Result<MatchTeamsData> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = teamApi.finaliseMatch(bearer, matchId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to finalise match: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
