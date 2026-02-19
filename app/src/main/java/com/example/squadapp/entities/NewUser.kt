package com.example.squadapp.entities

/**
 * NewUser - Entity for creating new users WITHOUT an ID
 * Used for inserting new users into Firestore after Firebase Auth creates the account.
 * Password is handled entirely by Firebase Auth; it is NOT stored in Firestore.
 */
data class NewUser(
    val profileImage: String,  // profile image URL from Firebase Storage
    val username: String,
    val email: String,
    val discordTag: String
) {
    companion object {
        /**
         * Serializes NewUser to Map for Firestore insertion (without ID or password)
         */
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
