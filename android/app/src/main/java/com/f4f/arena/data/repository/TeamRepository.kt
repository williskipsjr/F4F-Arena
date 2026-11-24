package com.f4f.arena.data.repository

import com.f4f.arena.data.api.TeamApi
import com.f4f.arena.data.model.GenerateTeamsRequest
import com.f4f.arena.data.model.MatchTeamsData
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
}
