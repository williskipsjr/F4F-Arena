package com.f4f.arena.data.repository

import com.f4f.arena.data.api.MatchApi
import com.f4f.arena.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MatchRepository(
    private val matchApi: MatchApi
) {
    suspend fun createMatch(token: String, request: CreateMatchRequest): Result<Match> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.createMatch(bearer, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to create match: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listMatches(status: String? = null): Result<List<Match>> =
        withContext(Dispatchers.IO) {
            try {
                val response = matchApi.listMatches(status)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to list matches: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMatch(id: String): Result<Match> =
        withContext(Dispatchers.IO) {
            try {
                val response = matchApi.getMatch(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to get match: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun syncAttendees(token: String, matchId: String, playerIds: List<String>): Result<AttendanceSummary> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.syncAttendees(bearer, matchId, SyncAttendeesRequest(playerIds))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to sync attendees: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAttendees(matchId: String): Result<AttendanceSummary> =
        withContext(Dispatchers.IO) {
            try {
                val response = matchApi.getAttendees(matchId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to get attendees: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun addLateArrival(token: String, matchId: String, playerId: String): Result<AttendanceSummary> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.addLateArrival(bearer, matchId, AddLateArrivalRequest(playerId))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to add late arrival: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun removeAttendee(token: String, matchId: String, playerId: String): Result<AttendanceSummary> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.removeAttendee(bearer, matchId, playerId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to remove attendee: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
