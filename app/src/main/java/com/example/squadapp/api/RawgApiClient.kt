package com.example.squadapp.api

import android.util.Log
import com.example.squadapp.BuildConfig
import com.example.squadapp.models.RawgGamesResponse
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
     * Fetch games with optional filters
     * @param onSuccess Callback when games are successfully fetched
     * @param onError Callback when an error occurs
     * @param pageSize Number of results per page (default: 20)
     * @param page Page number (default: 1)
     * @param search Optional search query
     * @param ordering Optional ordering (e.g., "-released", "name")
     */
    fun getGames(
        onSuccess: (RawgGamesResponse) -> Unit,
        onError: (String) -> Unit,
        pageSize: Int = 20,
        page: Int = 1,
        search: String? = null,
        ordering: String? = null
    ) {
        val call = apiService.getGames(
            apiKey = API_KEY,
            pageSize = pageSize,
            page = page,
            search = search,
            ordering = ordering
        )

        call.enqueue(object : Callback<RawgGamesResponse> {
            override fun onResponse(call: Call<RawgGamesResponse>, response: Response<RawgGamesResponse>) {
                if (response.isSuccessful) {
                    val gameResponse = response.body()
                    if (gameResponse != null) {
                        Log.d(TAG, "Successfully fetched ${gameResponse.results.size} games")
                        onSuccess(gameResponse)
                    } else {
                        Log.e(TAG, "Response body is null")
                        onError("Response body is null")
                    }
                } else {
                    val errorMessage = "Error: ${response.code()} ${response.message()}"
                    Log.e(TAG, errorMessage)
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<RawgGamesResponse>, t: Throwable) {
                val errorMessage = "Network error: ${t.message}"
                Log.e(TAG, errorMessage, t)
                onError(errorMessage)
            }
        })
    }

    /**
     * Search games by name
     * @param query Search query string
     * @param onSuccess Callback when search is successful
     * @param onError Callback when an error occurs
     * @param pageSize Number of results per page (default: 10)
     */
    fun searchGames(
        query: String,
        onSuccess: (RawgGamesResponse) -> Unit,
        onError: (String) -> Unit,
        pageSize: Int = 10
    ) {
        if (query.isEmpty()) {
            onError("Search query cannot be empty")
            return
        }

        val call = apiService.searchGames(
            apiKey = API_KEY,
            search = query,
            pageSize = pageSize
        )

        call.enqueue(object : Callback<RawgGamesResponse> {
            override fun onResponse(call: Call<RawgGamesResponse>, response: Response<RawgGamesResponse>) {
                if (response.isSuccessful) {
                    val gameResponse = response.body()
                    if (gameResponse != null) {
                        Log.d(TAG, "Search found ${gameResponse.results.size} games for query: '$query'")
                        onSuccess(gameResponse)
                    } else {
                        Log.e(TAG, "Response body is null")
                        onError("Response body is null")
                    }
                } else {
                    val errorMessage = "Error: ${response.code()} ${response.message()}"
                    Log.e(TAG, errorMessage)
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<RawgGamesResponse>, t: Throwable) {
                val errorMessage = "Network error: ${t.message}"
                Log.e(TAG, errorMessage, t)
                onError(errorMessage)
            }
        })
    }

    /**
     * Set a custom API key
     * @param key Your RAWG API key
     */
    fun setApiKey(key: String) {
        // Note: In a real app, you'd want to store this securely (e.g., in SharedPreferences or a secure vault)
        // This is a simplified approach for demonstration
    }
}

