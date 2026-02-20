package com.example.squadapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.squadapp.entities.GameEntity

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameById(gameId: Int): GameEntity?
}

