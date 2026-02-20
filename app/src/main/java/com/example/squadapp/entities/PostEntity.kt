package com.example.squadapp.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class PostEntity(
    @PrimaryKey
    val id: String,
    val image: String,
    val userId: String,
    val description: String,
    val creationTime: Long,
    val gameId: Int
) {
    companion object {
        fun fromPost(post: Post): PostEntity {
            return PostEntity(
                id = post.id,
                image = post.image,
                userId = post.user.id,
                description = post.description,
                creationTime = post.creationTime.time,
                gameId = post.gameId
            )
        }
    }
}

