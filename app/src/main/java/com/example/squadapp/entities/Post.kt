package com.example.squadapp.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Post(
    val id: String,
    val image: String,  // image URL from Firebase Storage
    val user: User,  // Uses User (without password) instead of UserDTO
    val description: String,
    val creationTime: Date,
    val gameId: Int
) : Parcelable {

    companion object {
        // Post JSON keys
        const val POST_IMAGE = "image"
        const val POST_USER = "user"
        const val POST_DESCRIPTION = "description"
        const val POST_CREATION_TIME = "creationTime"
        const val POST_GAME_ID = "gameId"

    }
}
