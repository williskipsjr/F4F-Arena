package com.f4f.arena.data.api

import com.f4f.arena.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MatchApi {

    @POST("api/v1/matches")
    suspend fun createMatch(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateMatchRequest
    ): Response<MatchResponse>

    @GET("api/v1/matches")
    suspend fun listMatches(
        @Header("Authorization") bearerToken: String? = null,
        @Query("status") status: String? = null
    ): Response<MatchListResponse>

    @PATCH("api/v1/matches/{id}")
    suspend fun updateMatch(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: UpdateMatchRequest
    ): Response<MatchResponse>

    @POST("api/v1/matches/{id}/vote")
    suspend fun castVote(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: CastVoteRequest
    ): Response<MatchResponse>

    @GET("api/v1/matches/{id}/votes")
    suspend fun getMatchVotes(
        @Path("id") id: String
    ): Response<MatchVotesResponse>

    @GET("api/v1/matches/{id}")
    suspend fun getMatch(
        @Path("id") id: String
    ): Response<MatchResponse>

    @PUT("api/v1/matches/{id}/attendees")
    suspend fun syncAttendees(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: SyncAttendeesRequest
    ): Response<AttendanceSummaryResponse>

    @GET("api/v1/matches/{id}/attendees")
    suspend fun getAttendees(
        @Path("id") id: String
    ): Response<AttendanceSummaryResponse>

    @POST("api/v1/matches/{id}/attendees/late")
    suspend fun addLateArrival(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: AddLateArrivalRequest
    ): Response<AttendanceSummaryResponse>

    @DELETE("api/v1/matches/{id}/attendees/{playerId}")
    suspend fun removeAttendee(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Path("playerId") playerId: String
    ): Response<AttendanceSummaryResponse>
}
