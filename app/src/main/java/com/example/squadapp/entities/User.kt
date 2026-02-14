package com.example.squadapp.entities

data class User(
    val id: Int,
    val profileImage: Int,  // drawable resource ID
    val username: String,
    val password: String,
    val discordTag: String
)

