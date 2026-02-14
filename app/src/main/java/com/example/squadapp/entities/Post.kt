package com.example.squadapp.entities

import java.util.Date

data class Post(
    val id: String,
    val image: Int,  // drawable resource ID
    val user: User,
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
            val id = json["id"] as String
            val image = json["image"] as Int
            val description = json["description"] as String
            val creationTime = json["creationTime"] as Date
            val user = json["user"] as User

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
                POST_USER to post.user.id.toString(), // Store user ID as foreign key reference
                POST_DESCRIPTION to post.description,
                POST_CREATION_TIME to post.creationTime
            )
        }

    }
}
