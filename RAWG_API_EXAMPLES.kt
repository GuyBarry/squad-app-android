package com.example.squadapp

/**
 * EXAMPLE: How to integrate RAWG API with PostFragment
 *
 * This file shows optional enhancements to use real game data from RAWG API
 * instead of the mock game list.
 *
 * To use this, copy the methods into your PostFragment class and modify
 * setupGamesList() to call loadGamesFromAPI() instead.
 */

import android.util.Log
import com.example.squadapp.api.RawgApiClient
import com.example.squadapp.models.RawgGamesResponse

// ============================================================================
// EXAMPLE 1: Load Popular Games on Fragment Load
// ============================================================================

fun loadPopularGamesFromAPI() {
    val apiClient = RawgApiClient()

    apiClient.getGames(
        onSuccess = { response: RawgGamesResponse ->
            // Convert RAWG games to local Game objects
            val apiGames = response.results.map { rawgGame ->
                Game(
                    name = rawgGame.name,
                    platform = rawgGame.platforms
                        ?.firstOrNull()?.platform?.name ?: "PC",
                    imageResId = android.R.drawable.ic_menu_gallery
                )
            }

            // Update game list
            gameList.clear()
            gameList.addAll(apiGames)

            Log.d("PostFragment", "Loaded ${apiGames.size} games from RAWG API")
        },
        onError = { error: String ->
            Log.e("PostFragment", "Failed to load games from RAWG: $error")
            // Falls back to mock data if available
        },
        pageSize = 20,
        ordering = "-rating"  // Load highest rated games first
    )
}

// ============================================================================
// EXAMPLE 2: Real-time Search as User Types
// ============================================================================

fun setupSearchWithRawgAPI() {
    val apiClient = RawgApiClient()

    squadSearchInput.addTextChangedListener(object : TextWatcher {
        private var searchJob: Runnable? = null

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Only search after 2 characters AND 500ms delay to avoid too many API calls
            if (s != null && s.length >= 2) {
                // Cancel previous search
                searchJob?.let {
                    squadSearchInput.removeCallbacks(it)
                }

                // Schedule new search with delay
                searchJob = Runnable {
                    searchGamesFromRAWG(s.toString(), apiClient)
                }
                squadSearchInput.postDelayed(searchJob!!, 500)
            } else if (s != null && s.isEmpty()) {
                filteredGames.clear()
                gameListAdapter.notifyDataSetChanged()
                selectedGame = null
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    })
}

fun searchGamesFromRAWG(query: String, apiClient: RawgApiClient) {
    apiClient.searchGames(
        query = query,
        onSuccess = { response: RawgGamesResponse ->
            // Take only first 4 results (max 4 games as per requirement)
            val results = response.results.take(4).map { rawgGame ->
                Game(
                    name = rawgGame.name,
                    platform = rawgGame.platforms
                        ?.firstOrNull()?.platform?.name ?: "PC",
                    imageResId = android.R.drawable.ic_menu_gallery
                )
            }

            filteredGames.clear()
            filteredGames.addAll(results)
            gameListAdapter.notifyDataSetChanged()

            Log.d("PostFragment", "Search found ${results.size} games for query: '$query'")
        },
        onError = { error: String ->
            Log.e("PostFragment", "Search error: $error")
            // Keep previous results visible
        },
        pageSize = 10
    )
}

// ============================================================================
// EXAMPLE 3: Smart Game Selection with Rating Display
// ============================================================================

/**
 * Enhanced game selection that stores more info about selected game
 * (You can extend Post entity to store game metadata)
 */
fun setupGamesListWithMetadata() {
    gameListAdapter = GameListAdapter(filteredGames) { game ->
        selectedGame = game
        squadSearchInput.setText(game.name, TextView.BufferType.EDITABLE)

        // Optional: Show additional info about selected game
        Toast.makeText(
            context,
            "Selected: ${game.name} (${game.platform})",
            Toast.LENGTH_SHORT
        ).show()

        // Clear list
        filteredGames.clear()
        gameListAdapter.notifyDataSetChanged()
    }
    gamesListRecycler.layoutManager = LinearLayoutManager(context)
    gamesListRecycler.adapter = gameListAdapter
}

// ============================================================================
// EXAMPLE 4: Hybrid Approach (Local + API)
// ============================================================================

/**
 * Load local games first, then supplement with API results
 * This provides better UX with instant results
 */
fun setupHybridGameSearch(apiClient: RawgApiClient) {
    squadSearchInput.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null && s.length >= 2) {
                // First: Search local list immediately
                val localResults = gameList.filter { game ->
                    game.name.contains(s.toString(), ignoreCase = true)
                }.take(4)

                filteredGames.clear()
                filteredGames.addAll(localResults)
                gameListAdapter.notifyDataSetChanged()

                // Then: Search API for more results
                apiClient.searchGames(
                    query = s.toString(),
                    onSuccess = { response ->
                        // Get API results
                        val apiResults = response.results
                            .take(4)
                            .map { rawgGame ->
                                Game(
                                    name = rawgGame.name,
                                    platform = rawgGame.platforms
                                        ?.firstOrNull()?.platform?.name ?: "PC",
                                    imageResId = android.R.drawable.ic_menu_gallery
                                )
                            }

                        // Merge with local (avoid duplicates)
                        val merged = (localResults + apiResults)
                            .distinctBy { it.name }
                            .take(4)

                        filteredGames.clear()
                        filteredGames.addAll(merged)
                        gameListAdapter.notifyDataSetChanged()
                    },
                    onError = { error ->
                        Log.d("PostFragment", "API search failed: $error, using local results")
                    },
                    pageSize = 10
                )
            } else if (s != null && s.isEmpty()) {
                filteredGames.clear()
                gameListAdapter.notifyDataSetChanged()
                selectedGame = null
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    })
}

// ============================================================================
// EXAMPLE 5: Error Handling Best Practices
// ============================================================================

fun robustGameSearch(query: String, apiClient: RawgApiClient) {
    // Show loading state
    publishBtn.isEnabled = false

    apiClient.searchGames(
        query = query,
        onSuccess = { response ->
            try {
                val games = response.results.take(4).map { rawgGame ->
                    Game(
                        name = rawgGame.name ?: "Unknown",
                        platform = rawgGame.platforms
                            ?.firstOrNull()?.platform?.name ?: "Unknown",
                        imageResId = android.R.drawable.ic_menu_gallery
                    )
                }

                if (games.isEmpty()) {
                    Toast.makeText(context, "No games found", Toast.LENGTH_SHORT).show()
                } else {
                    filteredGames.clear()
                    filteredGames.addAll(games)
                    gameListAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("PostFragment", "Error processing games", e)
                Toast.makeText(context, "Error processing results", Toast.LENGTH_SHORT).show()
            } finally {
                publishBtn.isEnabled = true
            }
        },
        onError = { error ->
            Log.e("PostFragment", "Search failed: $error")

            val userFriendlyMessage = when {
                error.contains("Network") -> "Check your internet connection"
                error.contains("404") -> "Game not found"
                error.contains("401") -> "API key issue"
                error.contains("429") -> "Too many requests, please try again later"
                else -> "Search failed, try again"
            }

            Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_SHORT).show()
            publishBtn.isEnabled = true
        },
        pageSize = 10
    )
}

// ============================================================================
// USAGE NOTES
// ============================================================================

/**
 * To use these examples:
 *
 * 1. Copy any of the functions above into PostFragment.kt
 *
 * 2. Call them from onViewCreated():
 *    - loadPopularGamesFromAPI() - Load on startup
 *    - setupSearchWithRawgAPI() - Setup search
 *
 * 3. Or use setupHybridGameSearch() for best UX
 *
 * 4. Don't forget to:
 *    - Import RawgApiClient
 *    - Set up your API key in RawgApiClient.kt
 *    - Handle responses on main thread (already done in callbacks)
 *
 * Example in onViewCreated():
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *
 *         // ... existing code ...
 *
 *         // Option 1: Load popular games
 *         loadPopularGamesFromAPI()
 *
 *         // Option 2: Setup search
 *         val apiClient = RawgApiClient()
 *         setupHybridGameSearch(apiClient)
 *     }
 */

