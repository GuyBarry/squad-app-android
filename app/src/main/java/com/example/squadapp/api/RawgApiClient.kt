package com.example.squadapp.api

import android.util.Log
import com.example.squadapp.BuildConfig
import com.example.squadapp.base.RawgGamesCompletion
import com.example.squadapp.entities.RawgGamesResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            OkHttpClient.Builder()
                .addInterceptor(logging)
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

    /**
     * Search games by name
     * @param gameName Name of the game to search for
     * @param onSuccess Callback with list of games when search is successful
     */
    fun searchGamesByName(
        gameName: String,
        onSuccess: RawgGamesCompletion
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
                        Log.d(TAG, "Search found ${gameResponse.results.size} games for: '$gameName'")
                        onSuccess(gameResponse.results)
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
}

