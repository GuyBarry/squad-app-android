# RAWG API Integration - Complete Setup Guide

## Summary

I've successfully created a complete RAWG API integration for your Squad App Android project. This integration includes:

1. **HTTP Client Setup** - Retrofit + OkHttp with proper dependency configuration
2. **API Service Interface** - Type-safe API endpoints
3. **Data Models** - GSON serialization/deserialization for API responses
4. **API Client** - Async API communication with callbacks
5. **UI Integration** - Updated PostFragment with text input + filtered game list

## Files Created/Modified

### New Files Created:
1. **RawgApiClient.kt** - Main API client for RAWG API communication
2. **RawgApiService.kt** - Retrofit service interface
3. **RawgGame.kt** - Data models for API responses
4. **GameListAdapter.kt** - RecyclerView adapter for displaying games
5. **RAWG_API_INTEGRATION.md** - This documentation file

### Files Modified:
1. **gradle/libs.versions.toml** - Added Retrofit and OkHttp dependencies
2. **app/build.gradle.kts** - Added Retrofit and OkHttp implementations
3. **app/src/main/res/layout/fragment_post.xml** - Replaced dropdown with text input + RecyclerView
4. **app/src/main/java/com/example/squadapp/PostFragment.kt** - Updated to use new game selection UI
5. **gradle/wrapper/gradle-wrapper.properties** - Created missing wrapper properties file

## Key Features Implemented

### 1. Text Input with Filtering (PostFragment)
- ✅ TextInputEditText for game search
- ✅ Filters games after 2 keystrokes minimum
- ✅ Shows maximum 4 matching games
- ✅ Text input only changes when a game is selected
- ✅ Scrollable list with max height of 300dp

### 2. RAWG API Client
```kotlin
val apiClient = RawgApiClient()

// Search games
apiClient.searchGames(
    query = "valorant",
    onSuccess = { response ->
        val games = response.results
        // Handle success
    },
    onError = { error ->
        // Handle error
    }
)
```

### 3. Retrofit Configuration
- Base URL: `https://api.rawg.io/api/`
- HTTP Logging Interceptor for debugging
- 30-second timeouts
- GSON serialization

## Setup Instructions

### Step 1: Get Your RAWG API Key
1. Visit https://rawg.io/api/
2. Create an account
3. Generate an API key
4. Keep it safe (don't commit to version control)

### Step 2: Configure API Key

**Option A - Hardcode (Development Only):**
Edit `RawgApiClient.kt` line 17:
```kotlin
private const val API_KEY = "YOUR_API_KEY_HERE"  // Replace with your key
```

**Option B - Secure Storage (Recommended for Production):**
Use Android Keystore or Gradle BuildConfig:

```gradle
// In app/build.gradle.kts
android {
    ...
    buildTypes {
        debug {
            buildConfigField "String", "RAWG_API_KEY", "\"your_api_key_here\""
        }
    }
}
```

Then in RawgApiClient.kt:
```kotlin
private const val API_KEY = BuildConfig.RAWG_API_KEY
```

### Step 3: Sync Gradle
- Android Studio will automatically sync and download dependencies
- Or manually run: `./gradlew build`

## Usage Examples

### Example 1: Search for Popular Games
```kotlin
private val apiClient = RawgApiClient()

private fun loadPopularGames() {
    apiClient.getGames(
        onSuccess = { response ->
            val games = response.results.map { rawgGame ->
                Game(
                    name = rawgGame.name,
                    platform = rawgGame.platforms?.firstOrNull()?.platform?.name ?: "PC",
                    imageResId = android.R.drawable.ic_menu_gallery
                )
            }
            gameList.addAll(games)
            gameListAdapter.notifyDataSetChanged()
        },
        onError = { error ->
            Log.e("PostFragment", "Failed to load games: $error")
        },
        pageSize = 20,
        ordering = "-rating"
    )
}
```

### Example 2: Real-time Game Search
```kotlin
private fun setupSearchInput() {
    squadSearchInput.addTextChangedListener(object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null && s.length >= 2) {
                searchGamesFromAPI(s.toString())
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    })
}

private fun searchGamesFromAPI(query: String) {
    apiClient.searchGames(
        query = query,
        onSuccess = { response ->
            val apiGames = response.results.take(4).map { rawgGame ->
                Game(
                    name = rawgGame.name,
                    platform = rawgGame.platforms?.firstOrNull()?.platform?.name ?: "PC",
                    imageResId = android.R.drawable.ic_menu_gallery
                )
            }
            filteredGames.clear()
            filteredGames.addAll(apiGames)
            gameListAdapter.notifyDataSetChanged()
        },
        onError = { error ->
            Log.e("PostFragment", "API search error: $error")
        },
        pageSize = 4
    )
}
```

## API Endpoints Available

### GET /games
Fetch games with optional filters
```kotlin
apiClient.getGames(
    pageSize = 20,              // Results per page
    page = 1,                   // Page number
    search = "valorant",        // Optional search
    ordering = "-rating",       // Optional sorting
    onSuccess = { response -> },
    onError = { error -> }
)
```

**Supported Ordering:**
- `-released` - Most recent first
- `-rating` - Highest rated first
- `-metacritic` - Highest Metacritic score
- `name` - Alphabetically

## Data Models

### RawgGame Object
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

## Debugging

### View API Requests/Responses
Check Android Studio Logcat with filter: `RawgApiClient`

The HttpLoggingInterceptor will show:
- Request headers and body
- Response headers and body
- Status codes and errors

### Common Issues

**Issue:** "401 Unauthorized"
- **Solution:** Check your API key is correct

**Issue:** "429 Too Many Requests"
- **Solution:** You've exceeded rate limit (20 req/min). Wait or use pagination

**Issue:** No results
- **Solution:** Check your search query or try different filters

## Performance Tips

1. **Implement Caching**
   - Use Room Database to cache game data locally
   - Reduces API calls and improves performance

2. **Pagination**
   - Use `page` parameter for large result sets
   - Load more on scroll

3. **Coroutines** (Future Enhancement)
   - Convert callbacks to suspend functions
   - Better async handling

4. **Image Loading**
   - Use Glide or Coil for loading game images from URLs
   - Don't store large images in memory

## Next Steps

### Optional: Load RAWG Images
Currently, the app uses placeholder images. To load real game images:

```kotlin
// Add Glide dependency
implementation("com.github.bumptech.glide:glide:4.16.0")

// In GameListAdapter
Glide.with(itemView.context)
    .load(game.backgroundImage)
    .placeholder(android.R.drawable.ic_menu_gallery)
    .into(gameImage)
```

### Optional: Add Game Details Fragment
Display detailed information about selected games:
- Rating, genres, platforms
- Screenshots
- Release date, description

## Dependencies Added

- **Retrofit 2.11.0** - REST client
- **OkHttp 4.12.0** - HTTP client
- **Gson** - JSON serialization (automatically included with Retrofit)
- **OkHttp Logging Interceptor** - Request/response logging

## Rate Limits

RAWG API Free Tier:
- 20 requests per minute (with API key)
- Unlimited requests per day (but slow)

## Troubleshooting

### Build Issues
If dependencies don't resolve:
1. Run `./gradlew clean`
2. Sync Gradle in Android Studio
3. Invalidate caches and restart Android Studio

### Runtime Issues
Check Logcat for "RawgApiClient" messages
All errors are logged with full details

## Security Notes

⚠️ **Important:** Never commit API keys to version control!

Use one of these approaches:
1. **Gradle BuildConfig** (recommended)
2. **Environment variables**
3. **Secure configuration file** (not in repo)
4. **Firebase Remote Config** (for production apps)

## References

- RAWG API Docs: https://api.rawg.io/api/
- Retrofit: https://square.github.io/retrofit/
- OkHttp: https://square.github.io/okhttp/

## Support

For issues with:
- **RAWG API:** Check https://api.rawg.io/api/
- **Retrofit:** See https://square.github.io/retrofit/
- **Your App:** Review the code comments and error logs

---

**Created:** February 14, 2026
**Version:** 1.0
**Status:** ✅ Complete and ready to use

