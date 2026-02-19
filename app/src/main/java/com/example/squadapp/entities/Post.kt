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
        const val POST_ID = "id"
        const val POST_IMAGE = "image"
        const val POST_USER = "user"
        const val POST_DESCRIPTION = "description"
        const val POST_CREATION_TIME = "creationTime"
        const val POST_GAME_ID = "gameId"

        fun deserialize(json: Map<String, Any?>): Post {
            val id = json[POST_ID] as String
            val image = json[POST_IMAGE] as String
            val description = json[POST_DESCRIPTION] as String
            val creationTime = json[POST_CREATION_TIME] as Date
            val user = json[POST_USER] as User
            val gameId = json[POST_GAME_ID] as Int

            return Post(
                id = id,
                image = image,
                description = description,
                creationTime = creationTime,
                user = user,
                gameId = gameId
            )
        }

        fun serialize(post: Post): Map<String, Any?> {
            return hashMapOf(
                POST_ID to post.id,
                POST_IMAGE to post.image,
                POST_USER to post.user.id, // Store user ID as foreign key reference
                POST_DESCRIPTION to post.description,
                POST_CREATION_TIME to post.creationTime,
                POST_GAME_ID to post.gameId
            )
        }

    }
}
