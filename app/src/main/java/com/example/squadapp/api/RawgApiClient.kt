package com.example.squadapp.api

import android.util.Log
import com.example.squadapp.BuildConfig
import com.example.squadapp.base.GameCompletion
import com.example.squadapp.base.GamesCompletion
import com.example.squadapp.entities.RawgGame
import com.example.squadapp.entities.RawgGamesResponse
import com.example.squadapp.utils.GameUtils
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RawgApiClient {
    companion object {
        private const val BASE_URL = "https://api.rawg.io/api/"
        private const val API_KEY = BuildConfig.RAWG_API_KEY
        private const val TAG = "RawgApiClient"

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        private val apiService: RawgApiService by lazy {
            retrofit.create(RawgApiService::class.java)
        }
    }

    fun searchGamesByName(
        gameName: String,
        onSuccess: GamesCompletion
    ) {
        val call = apiService.searchGames(
            apiKey = API_KEY,
            search = gameName,
            pageSize = 4
        )

        call.enqueue(object : Callback<RawgGamesResponse> {
            override fun onResponse(call: Call<RawgGamesResponse>, response: Response<RawgGamesResponse>) {
                if (response.isSuccessful) {
                    val gameResponse = response.body()
                    if (gameResponse != null) {
                        onSuccess(GameUtils.mapRawgGamesToUiGames(gameResponse.results))
                    } else {
                        Log.e(TAG, "Response body is null")
                    }
                } else {
                    Log.e(TAG, "Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RawgGamesResponse>, t: Throwable) {
                Log.e(TAG, "Network error: ${t.message}", t)
            }
        })
    }

    fun searchGameById(
        gameId: Int,
        onSuccess: GameCompletion
    ) {
        val call = apiService.getGameById(
            gameId = gameId,
            apiKey = API_KEY
        )

        call.enqueue(object : Callback<RawgGame> {
            override fun onResponse(call: Call<RawgGame>, response: Response<RawgGame>) {
                if (response.isSuccessful) {
                    val game = response.body()
                    if (game != null) {
                        onSuccess(with(GameUtils) { game.toGame() })
                    } else {
                        Log.e(TAG, "Response body is null for game ID: $gameId")
                        onSuccess(null)
                    }
                } else {
                    Log.e(TAG, "Error fetching game ID $gameId: ${response.code()} ${response.message()}")
                    onSuccess(null)
                }
            }

            override fun onFailure(call: Call<RawgGame>, t: Throwable) {
                Log.e(TAG, "Network error fetching game ID $gameId: ${t.message}", t)
                onSuccess(null)
            }
        })
    }
}

