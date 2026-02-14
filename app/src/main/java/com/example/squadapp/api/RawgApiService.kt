package com.example.squadapp.api

import com.example.squadapp.models.RawgGamesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RawgApiService {
    @GET("games")
    fun getGames(
        @Query("key") apiKey: String,
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null
    ): Call<RawgGamesResponse>

    @GET("games")
    fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") search: String,
        @Query("page_size") pageSize: Int = 10
    ): Call<RawgGamesResponse>
}

