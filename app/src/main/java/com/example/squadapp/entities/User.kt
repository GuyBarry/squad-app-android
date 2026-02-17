package com.example.squadapp.entities

data class User(
    val id: String,
    val profileImage: Int,  // drawable resource ID
    val username: String,
    val password: String,
    val discordTag: String
) {
    companion object {
        const val USER_ID = "id"
        const val USER_PROFILE_IMAGE = "profileImage"
        const val USER_USERNAME = "username"
        const val USER_PASSWORD = "password"
        const val USER_DISCORD_TAG = "discordTag"

        fun deserializeUser(data: Map<String, Any?>): User {
            return User(
                id = data[USER_ID] as? String ?: "",
                profileImage = (data[USER_PROFILE_IMAGE] as? Long)?.toInt()
                    ?: (data[USER_PROFILE_IMAGE] as? Int) ?: 0,
                username = data[USER_USERNAME] as? String ?: "",
                password = data[USER_PASSWORD] as? String ?: "",
                discordTag = data[USER_DISCORD_TAG] as? String ?: ""
            )
        }

        fun serializeUser(user: User): Map<String, Any?> {
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
