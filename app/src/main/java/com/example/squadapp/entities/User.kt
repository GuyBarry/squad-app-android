package com.example.squadapp.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * User - Main user entity for application layer
 * Does NOT contain password - safe for passing between activities and UI operations
 * This is the primary user type used throughout the app
 * Implements Parcelable to be passed via Intent
 */
@Parcelize
data class User(
    val id: String,
    val profileImage: String,  // profile image URL from Firebase Storage
    val username: String,
    val discordTag: String
) : Parcelable {
    companion object {
        const val EXTRA_USER = "extra_user"

        /**
         * Creates a User from a UserDTO (removes password)
         */
        fun fromUserDTO(userDTO: UserDTO): User {
            return User(
                id = userDTO.id,
                profileImage = userDTO.profileImage,
                username = userDTO.username,
                discordTag = userDTO.discordTag
            )
        }
    }
}


