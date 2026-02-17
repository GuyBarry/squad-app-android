package com.example.squadapp.entities

import java.util.Date

data class Post(
    val id: String,
    val image: Int,  // drawable resource ID
    val user: User,  // Uses User (without password) instead of UserDTO
    val description: String,
    val creationTime: Date
) {

    companion object {
        // Post JSON keys
        const val POST_ID = "id"
        const val POST_IMAGE = "image"
        const val POST_USER = "user"
        const val POST_DESCRIPTION = "description"
        const val POST_CREATION_TIME = "creationTime"

        fun deserialize(json: Map<String, Any?>): Post {
            val id = json[POST_ID] as String
            val image = json[POST_IMAGE] as Int
            val description = json[POST_DESCRIPTION] as String
            val creationTime = json[POST_CREATION_TIME] as Date
            val user = json[POST_USER] as User

            return Post(
                id = id,
                image = image,
                description = description,
                creationTime = creationTime,
                user = user
            )
        }

        fun serialize(post: Post): Map<String, Any?> {
            return hashMapOf(
                POST_ID to post.id,
                POST_IMAGE to post.image,
                POST_USER to post.user.id, // Store user ID as foreign key reference
                POST_DESCRIPTION to post.description,
                POST_CREATION_TIME to post.creationTime
            )
        }

    }
}
