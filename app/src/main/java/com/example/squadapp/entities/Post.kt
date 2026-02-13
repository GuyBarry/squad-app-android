package com.example.squadapp.entities

import java.util.Date

data class Post(
    val id: String,
    val image: Int,  // drawable resource ID
    val user: User,
    val description: String,
    val creationTime: Date
){

companion object {
    fun fromJson(json: Map<String, Any?>): Post {
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
}
}
