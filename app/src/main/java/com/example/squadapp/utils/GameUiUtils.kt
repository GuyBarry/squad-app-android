package com.example.squadapp.utils

import Game
import com.example.squadapp.entities.Platform
import com.example.squadapp.entities.PlatformInfo
import com.example.squadapp.entities.RawgGame

/**
 * Utility functions for converting between Game UI models and RawgGame API models.
 */
object GameUiUtils {

    /**
     * Maps a list of [RawgGame] API results to [Game] UI models.
     */
    fun mapRawgGamesToUiGames(rawgGames: List<RawgGame>): List<Game> =
        rawgGames.map { rawgGame ->
            val platforms = rawgGame.platforms?.mapNotNull { it.platform?.name } ?: emptyList()
            Game(
                name = rawgGame.name,
                platforms = platforms,
                imageResId = android.R.drawable.ic_menu_gallery,
                id = rawgGame.id,
                imageUrl = rawgGame.backgroundImage
            )
        }

    /**
     * Converts a [Game] UI model back to a [RawgGame] API model.
     */
    fun Game.toRawgGame(): RawgGame = RawgGame(
        id = this.id,
        name = this.name,
        slug = "",
        released = null,
        backgroundImage = this.imageUrl,
        rating = null,
        ratingsCount = null,
        metacritic = null,
        platforms = this.platforms.map { platformName ->
            PlatformInfo(Platform(0, platformName, ""))
        },
        genres = null,
        description = null,
        shortScreenshots = null
    )
}

