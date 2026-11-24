package com.f4f.arena.data.api

import com.f4f.arena.data.model.GenerateTeamsRequest
import com.f4f.arena.data.model.MatchTeamsResponse
import com.f4f.arena.data.model.MovePlayerRequest
import com.f4f.arena.data.model.SwapPlayersRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface TeamApi {

    @POST("api/v1/matches/{id}/generate-teams")
    suspend fun generateTeams(
        @Header("Authorization") bearerToken: String,
        @Path("id") matchId: String,
        @Body request: GenerateTeamsRequest
    ): Response<MatchTeamsResponse>

    @GET("api/v1/matches/{id}/teams")
    suspend fun getTeams(
        @Path("id") matchId: String
    ): Response<MatchTeamsResponse>

    @POST("api/v1/matches/{id}/assignments/swap")
    suspend fun swapPlayers(
        @Header("Authorization") bearerToken: String,
        @Path("id") matchId: String,
        @Body request: SwapPlayersRequest
    ): Response<MatchTeamsResponse>

    @POST("api/v1/matches/{id}/assignments/move")
    suspend fun movePlayer(
        @Header("Authorization") bearerToken: String,
        @Path("id") matchId: String,
        @Body request: MovePlayerRequest
    ): Response<MatchTeamsResponse>

    @POST("api/v1/matches/{id}/finalise")
    suspend fun finaliseMatch(
        @Header("Authorization") bearerToken: String,
        @Path("id") matchId: String
    ): Response<MatchTeamsResponse>
}
