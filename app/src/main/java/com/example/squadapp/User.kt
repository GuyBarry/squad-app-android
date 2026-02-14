package com.example.squadapp

data class User(
    val id: Int,
    val profileImage: Int,  // drawable resource ID
    val username: String,
    val password: String,
    val discordTag: String
)

