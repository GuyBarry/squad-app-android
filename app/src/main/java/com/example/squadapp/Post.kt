package com.example.squadapp

import java.util.Date

data class Post(
    val id: Int,
    val image: Int,  // drawable resource ID
    val user: User,
    val description: String,
    val creationTime: Date
)



