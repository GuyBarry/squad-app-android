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

        // Firestore field keys
        const val USER_ID = "id"
        const val USER_PROFILE_IMAGE = "profileImage"
        const val USER_USERNAME = "username"
        const val USER_DISCORD_TAG = "discordTag"

        /**
         * Creates a User from a map of Firestore document fields
         */
        fun deserializeUser(data: Map<String, Any?>): User {
            return User(
                id = data[USER_ID] as? String ?: "",
                profileImage = data[USER_PROFILE_IMAGE] as? String ?: "",
                username = data[USER_USERNAME] as? String ?: "",
                discordTag = data[USER_DISCORD_TAG] as? String ?: ""
            )
        }

        /**
         * Converts a User to a map of Firestore document fields
         */
        fun serializeUser(user: User): Map<String, Any?> {
            return hashMapOf(
                USER_ID to user.id,
                USER_PROFILE_IMAGE to user.profileImage,
                USER_USERNAME to user.username,
                USER_DISCORD_TAG to user.discordTag
            )
        }
    }
}
