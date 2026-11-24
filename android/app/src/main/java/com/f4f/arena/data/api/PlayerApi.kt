package com.f4f.arena.data.api

import com.f4f.arena.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PlayerApi {

    @GET("api/v1/players")
    suspend fun getPlayers(
        @Query("active") active: Boolean? = null,
        @Query("position") position: String? = null,
        @Query("query") query: String? = null
    ): Response<PlayerListResponse>

    @GET("api/v1/players/{id}")
    suspend fun getPlayer(
        @Path("id") id: String
    ): Response<Player>

    @POST("api/v1/players")
    suspend fun createPlayer(
        @Header("Authorization") bearerToken: String,
        @Body request: CreatePlayerRequest
    ): Response<CreatePlayerResponse>

    @PATCH("api/v1/players/{id}")
    suspend fun updatePlayer(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Body request: UpdatePlayerRequest
    ): Response<UpdatePlayerResponse>
}
