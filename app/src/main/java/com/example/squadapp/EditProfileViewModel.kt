package com.example.squadapp

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.User
import com.example.squadapp.models.Model

/**
 * EditProfileViewModel - Manages profile editing: image upload/delete and user data update.
 */
class EditProfileViewModel : ViewModel() {

    private val _isSaving = MutableLiveData<Boolean>(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveProgress = MutableLiveData<String?>()
    val saveProgress: LiveData<String?> = _saveProgress

    /** Emits updated User on success, or null + message on failure. */
    private val _saveResult = MutableLiveData<Triple<Boolean, User?, String?>>()
    val saveResult: LiveData<Triple<Boolean, User?, String?>> = _saveResult

    fun saveProfile(
        currentUser: User,
        newUsername: String,
        newDiscordTag: String,
        selectedImageUri: Uri?,
        isImageDeleted: Boolean,
        originalImageUrl: String
    ) {
        _isSaving.value = true

        when {
            isImageDeleted && originalImageUrl.isNotEmpty() -> {
                _saveProgress.value = "Deleting image..."
                Log.d("EditProfileViewModel", "Deleting profile image...")
                Model.shared.deletePicture(originalImageUrl) { success, message ->
                    if (success) {
                        _saveProgress.postValue("Updating profile...")
                        updateProfile(currentUser, newUsername, newDiscordTag, "")
                    } else {
                        _isSaving.postValue(false)
                        _saveProgress.postValue(null)
                        Log.e("EditProfileViewModel", "Failed to delete image: $message")
                        _saveResult.postValue(Triple(false, null, "Failed to delete image: $message"))
                    }
                }
            }

            selectedImageUri != null -> {
                _saveProgress.value = "Uploading image..."
                Log.d("EditProfileViewModel", "Uploading profile image...")
                Model.shared.uploadProfilePicture(
                    selectedImageUri,
                    currentUser.id,
                    { success, downloadUrl, message ->
                        if (success && downloadUrl != null) {
                            _saveProgress.postValue("Updating profile...")
                            updateProfile(currentUser, newUsername, newDiscordTag, downloadUrl)
                        } else {
                            _isSaving.postValue(false)
                            _saveProgress.postValue(null)
                            Log.e("EditProfileViewModel", "Failed to upload image: $message")
                            _saveResult.postValue(Triple(false, null, "Failed to upload image: $message"))
                        }
                    },
                    onProgress = { progress ->
                        _saveProgress.postValue("Uploading... $progress%")
                    }
                )
            }

            else -> {
                updateProfile(currentUser, newUsername, newDiscordTag, null)
            }
        }
    }

    private fun updateProfile(
        currentUser: User,
        username: String,
        discordTag: String,
        profileImageUrl: String?
    ) {
        Model.shared.updateUser(currentUser, username, discordTag, profileImageUrl) { success, updatedUser, message ->
            _isSaving.postValue(false)
            _saveProgress.postValue(null)
            _saveResult.postValue(Triple(success, updatedUser, message))
        }
    }
}

