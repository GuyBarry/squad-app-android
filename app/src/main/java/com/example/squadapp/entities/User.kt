package com.example.squadapp.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val profileImage: String,
    val username: String,
    val email: String,
    val discordTag: String
) : Parcelable {
    companion object {
        const val EXTRA_USER = "extra_user"

        const val USER_ID = "id"
        const val USER_PROFILE_IMAGE = "profileImage"
        const val USER_USERNAME = "username"
        const val USER_EMAIL = "email"
        const val USER_DISCORD_TAG = "discordTag"

        fun deserializeUser(data: Map<String, Any?>): User {
            return User(
                id = data[USER_ID] as? String ?: "",
                profileImage = data[USER_PROFILE_IMAGE] as? String ?: "",
                username = data[USER_USERNAME] as? String ?: "",
                email = data[USER_EMAIL] as? String ?: "",
                discordTag = data[USER_DISCORD_TAG] as? String ?: ""
            )
        }
    }
}
