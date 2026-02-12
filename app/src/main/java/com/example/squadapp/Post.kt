package com.example.squadapp

data class Post(
    val id: Int,
    val postImage: Int,  // drawable resource ID
    val user: User,
    val postText: String
)

