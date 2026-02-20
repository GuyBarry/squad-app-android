package com.example.squadapp.entities


data class NewUser(
    val profileImage: String,
    val username: String,
    val email: String,
    val discordTag: String
) {
    companion object {
        fun serialize(newUser: NewUser): Map<String, Any?> {
            return hashMapOf(
                User.USER_PROFILE_IMAGE to newUser.profileImage,
                User.USER_USERNAME to newUser.username,
                User.USER_EMAIL to newUser.email,
                User.USER_DISCORD_TAG to newUser.discordTag
            )
        }
    }
}
