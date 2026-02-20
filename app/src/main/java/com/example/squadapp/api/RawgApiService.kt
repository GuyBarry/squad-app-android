package com.example.squadapp.api

import com.example.squadapp.entities.RawgGame
import com.example.squadapp.entities.RawgGamesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {

    @GET("games")
    fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") search: String,
        @Query("page_size") pageSize: Int = 10
    ): Call<RawgGamesResponse>

    @GET("games/{id}")
    fun getGameById(
        @Path("id") gameId: Int,
        @Query("key") apiKey: String
    ): Call<RawgGame>
}

