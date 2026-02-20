package com.example.squadapp.entities

import androidx.room.Embedded
import androidx.room.Relation
import java.util.Date

data class PostWithUser(
    @Embedded val postEntity: PostEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: User
) {
    fun toPost(): Post {
        return Post(
            id = postEntity.id,
            image = postEntity.image,
            user = user,
            description = postEntity.description,
            creationTime = Date(postEntity.creationTime),
            gameId = postEntity.gameId
        )
    }
}

