package com.example.squadapp.entities

import java.util.Date

data class NewPost(
    val image: String,
    val userId: String,
    val description: String,
    val creationTime: Date,
    val gameId: Int
) {
    companion object {
        fun serialize(newPost: NewPost): Map<String, Any?> {
            return hashMapOf(
                Post.POST_IMAGE to newPost.image,
                Post.POST_USER to newPost.userId,
                Post.POST_DESCRIPTION to newPost.description,
                Post.POST_CREATION_TIME to newPost.creationTime,
                Post.POST_GAME_ID to newPost.gameId
            )
        }
    }
}

