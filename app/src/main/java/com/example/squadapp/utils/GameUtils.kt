package com.example.squadapp.utils

import Game
import com.example.squadapp.entities.RawgGame

object GameUtils {

    fun mapRawgGamesToUiGames(rawgGames: List<RawgGame>): List<Game> =
        rawgGames.map { it.toGame() }

    fun RawgGame.toGame(): Game = Game(
        id = this.id,
        name = this.name,
        platforms = this.platforms?.mapNotNull { it.platform?.name } ?: emptyList(),
        imageResId = android.R.drawable.ic_menu_gallery,
        imageUrl = this.backgroundImage,
        rating = this.rating ?: 0.0
    )
}
