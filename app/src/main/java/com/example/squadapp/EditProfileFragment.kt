package com.example.squadapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.squadapp.models.Model
import com.google.android.material.button.MaterialButton

class EditProfileFragment : Fragment() {

    private lateinit var profilePhoto: ImageView
    private lateinit var cameraButton: ImageButton
    private lateinit var userNameInput: EditText
    private lateinit var discordTagInput: EditText
    private lateinit var cancelBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton

    private var selectedImageUri: Uri? = null

    // Activity result launcher for gallery
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            profilePhoto.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profilePhoto = view.findViewById(R.id.edit_profile_photo)
        cameraButton = view.findViewById(R.id.camera_button)
        userNameInput = view.findViewById(R.id.user_name_input)
        discordTagInput = view.findViewById(R.id.discord_tag_input)
        cancelBtn = view.findViewById(R.id.cancel_btn)
        saveBtn = view.findViewById(R.id.save_btn)

        // Get current user from MainActivity safely
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            val currentUser = mainActivity.currentUser

            // Load existing user data from MainActivity
            // Use default placeholder if profile image is not set (0 or invalid)
            val profileImageRes = if (currentUser.profileImage != 0) {
                currentUser.profileImage
            } else {
                R.drawable.user_profile_placeholder
            }
            profilePhoto.setImageResource(profileImageRes)
            userNameInput.setText(currentUser.username)
            discordTagInput.setText(currentUser.discordTag)
        }

        // Set up click listeners
        cameraButton.setOnClickListener {
            // Open gallery to select image
            galleryLauncher.launch("image/*")
        }

        cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        saveBtn.setOnClickListener {
            handleSaveProfile()
        }
    }

    private fun handleSaveProfile() {
        val newUsername = userNameInput.text.toString().trim()
        val newDiscordTag = discordTagInput.text.toString().trim()

        val mainActivity = activity as? MainActivity
        if (mainActivity == null) {
            Toast.makeText(context, "Error: Could not get user information", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = mainActivity.currentUser

        // Validation
        if (newUsername.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (newDiscordTag.isEmpty()) {
            Toast.makeText(context, "Discord tag cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if anything changed
        if (newUsername == currentUser.username &&
            newDiscordTag == currentUser.discordTag &&
            selectedImageUri == null) {
            Toast.makeText(context, "No changes to save", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Disable save button while saving
        saveBtn.isEnabled = false
        saveBtn.text = "Saving..."

        // Update user in Firebase
        Model.shared.updateUser(currentUser.id, newUsername, newDiscordTag) { success, updatedUser, message ->
            saveBtn.isEnabled = true
            saveBtn.text = "Save Changes"

            if (success && updatedUser != null) {
                // Update MainActivity's current user
                mainActivity.currentUser = updatedUser

                Toast.makeText(context, message ?: "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                // Go back to profile
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, message ?: "Failed to update profile", Toast.LENGTH_LONG).show()
            }
        }
    }
}




