# RAWG API Integration Guide

## Overview
This guide explains how to use the RAWG API client to fetch game data from the RAWG.io API.

## Files Created

1. **RawgApiService.kt** - Retrofit service interface defining API endpoints
2. **RawgApiClient.kt** - Main API client class for making requests
3. **RawgGame.kt** - Data models for API responses

## Setup

### 1. Get Your API Key
1. Go to https://rawg.io/api/
2. Sign up for a free account
3. Generate your API key
4. Replace `YOUR_API_KEY_HERE` in `RawgApiClient.kt` with your actual key

### 2. Basic Usage

#### Fetch Games (with optional filters)
```kotlin
val apiClient = RawgApiClient()

apiClient.getGames(
    onSuccess = { response ->
        val games = response.results
        for (game in games) {
            println("Game: ${game.name}")
            println("Rating: ${game.rating}")
            println("Release Date: ${game.released}")
        }
    },
    onError = { error ->
        println("Error: $error")
    },
    pageSize = 20,
    page = 1,
    search = "valorant",
    ordering = "-rating"  // Order by rating descending
)
```

#### Search Games
```kotlin
val apiClient = RawgApiClient()

apiClient.searchGames(
    query = "valorant",
    onSuccess = { response ->
        val games = response.results
        // Handle search results
    },
    onError = { error ->
        println("Error: $error")
    },
    pageSize = 10
)
```

## API Endpoints

### GET /games
Fetch games with optional filters

**Parameters:**
- `pageSize`: Number of results per page (default: 20, max: 40)
- `page`: Page number (default: 1)
- `search`: Search query string (optional)
- `ordering`: Sort order (optional)
  - `-released`: Most recent first
  - `-rating`: Highest rated first
  - `name`: Alphabetically by name
  - `-metacritic`: Highest Metacritic score first

**Returns:** `RawgGamesResponse` containing:
- `count`: Total number of games matching criteria
- `next`: URL to next page (if available)
- `previous`: URL to previous page (if available)
- `results`: List of `RawgGame` objects

## Data Models

### RawgGame
```kotlin
data class RawgGame(
    val id: Int,                           // Game ID
    val name: String,                      // Game name
    val slug: String,                      // URL slug
    val released: String?,                 // Release date (YYYY-MM-DD)
    val backgroundImage: String?,          // Cover image URL
    val rating: Double?,                   // Average rating (0-5)
    val ratingsCount: Int?,                // Number of ratings
    val metacritic: Int?,                  // Metacritic score
    val platforms: List<PlatformInfo>?,    // Available platforms
    val genres: List<GenreInfo>?,          // Game genres
    val description: String?,              // Game description
    val shortScreenshots: List<ScreenshotInfo>?  // Screenshot URLs
)
```

### PlatformInfo
```kotlin
data class PlatformInfo(
    val platform: Platform?
)

data class Platform(
    val id: Int,
    val name: String,      // e.g., "PC", "PlayStation 5"
    val slug: String
)
```

### GenreInfo
```kotlin
data class GenreInfo(
    val id: Int,
    val name: String,      // e.g., "Action", "Sports"
    val slug: String
)
```

### ScreenshotInfo
```kotlin
data class ScreenshotInfo(
    val id: Int,
    val image: String      // Screenshot image URL
)
```

## Integration with PostFragment

To use the RAWG API in PostFragment to populate the games list:

```kotlin
import com.example.squadapp.api.RawgApiClient

// In PostFragment class
private val rawgApiClient = RawgApiClient()

// In setupGamesList() or a new setup method
private fun loadGamesFromAPI() {
    rawgApiClient.searchGames(
        query = "",  // Start with empty to get popular games
        onSuccess = { response ->
            val apiGames = response.results.take(20).map { rawgGame ->
                Game(
                    name = rawgGame.name,
                    platform = rawgGame.platforms?.firstOrNull()?.platform?.name ?: "PC",
                    imageResId = android.R.drawable.ic_menu_gallery  // Placeholder
                )
            }
            gameList.addAll(apiGames)
            gameListAdapter.notifyDataSetChanged()
        },
        onError = { error ->
            Log.e("PostFragment", "Failed to load games: $error")
        }
    )
}
```

## Features

✅ Asynchronous API calls using Retrofit
✅ Proper error handling
✅ Logging interceptor for debugging
✅ Timeout configuration (30 seconds)
✅ GSON serialization/deserialization
✅ Type-safe API endpoints
✅ Callback-based API (can be extended to use Coroutines/Flow)

## Error Handling

The API client provides error callbacks with descriptive messages:

```kotlin
onError = { errorMessage ->
    when {
        errorMessage.contains("Network error") -> {
            // Handle network connectivity issues
        }
        errorMessage.contains("404") -> {
            // Handle not found errors
        }
        else -> {
            // Handle other errors
        }
    }
}
```

## Logging

HTTP requests and responses are automatically logged. Check Android Studio's Logcat with tag "RawgApiClient" to see:
- Request/response headers
- Request/response body
- Error messages

## Future Enhancements

1. **Caching**: Add local caching to reduce API calls
2. **Pagination**: Implement pagination UI for browsing games
3. **Coroutines**: Convert callbacks to suspend functions
4. **Room Database**: Cache game data locally
5. **Advanced Search**: Add filters for genre, platform, rating
6. **Image Loading**: Integrate with Glide/Coil for loading game images

## API Rate Limiting

RAWG API has rate limits. Free tier allows:
- 20 requests per minute (with key)
- No requests without key

## Documentation

For more information about RAWG API, visit:
https://api.rawg.io/api/


