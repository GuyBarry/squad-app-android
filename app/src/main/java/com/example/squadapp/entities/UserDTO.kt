package com.example.squadapp.entities

/**
 * UserDTO - Data Transfer Object for database operations
 * Contains password field for authentication and database storage
 * Should NOT be passed between activities or used in UI layer
 */
data class UserDTO(
    val id: String,
    val profileImage: Int,  // drawable resource ID
    val username: String,
    val password: String,  // Hashed password for database
    val discordTag: String
) {
    companion object {
        const val USER_ID = "id"
        const val USER_PROFILE_IMAGE = "profileImage"
        const val USER_USERNAME = "username"
        const val USER_PASSWORD = "password"
        const val USER_DISCORD_TAG = "discordTag"

        fun deserializeUser(data: Map<String, Any?>): UserDTO {
            return UserDTO(
                id = data[USER_ID] as? String ?: "",
                profileImage = (data[USER_PROFILE_IMAGE] as? Long)?.toInt()
                    ?: (data[USER_PROFILE_IMAGE] as? Int) ?: 0,
                username = data[USER_USERNAME] as? String ?: "",
                password = data[USER_PASSWORD] as? String ?: "",
                discordTag = data[USER_DISCORD_TAG] as? String ?: ""
            )
        }

        fun serializeUser(user: UserDTO): Map<String, Any?> {
            return hashMapOf(
                USER_ID to user.id,
                USER_PROFILE_IMAGE to user.profileImage,
                USER_USERNAME to user.username,
                USER_PASSWORD to user.password,
                USER_DISCORD_TAG to user.discordTag
            )
        }
    }
}
