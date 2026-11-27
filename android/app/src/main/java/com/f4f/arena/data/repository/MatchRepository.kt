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
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Admin sign-in required or session expired (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to create match: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listMatches(status: String? = null, token: String? = null): Result<List<Match>> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = token?.takeIf { it.isNotBlank() }?.let {
                    if (it.startsWith("Bearer ")) it else "Bearer $it"
                }
                val response = matchApi.listMatches(bearer, status)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to list matches: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateMatch(token: String, id: String, request: UpdateMatchRequest): Result<Match> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.updateMatch(bearer, id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Admin sign-in required (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to update match: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteMatch(token: String, id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.deleteMatch(bearer, id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Admin sign-in required (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to delete poll: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun castVote(token: String, matchId: String, vote: String): Result<Match> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.castVote(bearer, matchId, CastVoteRequest(vote))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Please sign in to vote (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to record vote: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMatchVotes(matchId: String): Result<List<MatchVoteDetail>> =
        withContext(Dispatchers.IO) {
            try {
                val response = matchApi.getMatchVotes(matchId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to load votes: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMatchComments(matchId: String): Result<List<MatchComment>> =
        withContext(Dispatchers.IO) {
            try {
                val response = matchApi.getMatchComments(matchId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception("Failed to load comments: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun postMatchComment(token: String, matchId: String, message: String): Result<MatchComment> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.postMatchComment(bearer, matchId, CreateCommentRequest(message))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Please sign in to comment (HTTP 401)"))
                } else {
                    Result.failure(Exception("Failed to post comment: HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteMatchComment(token: String, matchId: String, commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = matchApi.deleteMatchComment(bearer, matchId, commentId)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete comment: HTTP ${response.code()}"))
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
                } else if (response.code() == 401) {
                    com.f4f.arena.data.auth.SessionManager.onUnauthorized()
                    Result.failure(Exception("Admin sign-in required or session expired (HTTP 401)"))
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
