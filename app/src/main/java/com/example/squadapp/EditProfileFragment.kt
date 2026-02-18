package com.example.squadapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.squadapp.models.Model
import com.google.android.material.button.MaterialButton
import java.util.Date

class EditProfileFragment : Fragment() {

    private lateinit var profilePhoto: ImageView
    private lateinit var galleryButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var cameraButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var cancelImageButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var userNameInput: EditText
    private lateinit var discordTagInput: EditText
    private lateinit var cancelBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton

    private var selectedImageUri: Uri? = null
    private var originalImageUrl: String = ""  // Store the original profile image URL

    // Activity result launcher for gallery
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            // Load selected image as circle using Glide
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop()
                .into(profilePhoto)
            // Show cancel button and hide gallery/camera buttons
            cancelImageButton.visibility = View.VISIBLE
            galleryButton.visibility = View.GONE
            cameraButton.visibility = View.GONE
        }
    }

    // Activity result launcher for camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (selectedImageUri != null) {
                // Load captured image as circle using Glide
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.user_profile_placeholder)
                    .error(R.drawable.user_profile_placeholder)
                    .circleCrop()
                    .into(profilePhoto)
                // Show cancel button and hide gallery/camera buttons
                cancelImageButton.visibility = View.VISIBLE
                galleryButton.visibility = View.GONE
                cameraButton.visibility = View.GONE
            }
        }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
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
        galleryButton = view.findViewById(R.id.gallery_button)
        cameraButton = view.findViewById(R.id.camera_button)
        cancelImageButton = view.findViewById(R.id.cancel_image_button)
        userNameInput = view.findViewById(R.id.user_name_input)
        discordTagInput = view.findViewById(R.id.discord_tag_input)
        cancelBtn = view.findViewById(R.id.cancel_btn)
        saveBtn = view.findViewById(R.id.save_btn)

        // Get current user from MainActivity safely
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            val currentUser = mainActivity.currentUser

            // Store original image URL
            originalImageUrl = currentUser.profileImage

            // Load existing user profile image using Glide
            Glide.with(this)
                .load(currentUser.profileImage)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop()
                .into(profilePhoto)

            userNameInput.setText(currentUser.username)
            discordTagInput.setText(currentUser.discordTag)
        }

        // Set up click listeners
        galleryButton.setOnClickListener {
            // Open gallery to select image
            galleryLauncher.launch("image/*")
        }

        cameraButton.setOnClickListener {
            openCamera()
        }

        cancelImageButton.setOnClickListener {
            cancelImageChange()
        }

        cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        saveBtn.setOnClickListener {
            handleSaveProfile()
        }
    }

    private fun openCamera() {
        // Check if camera permission is granted
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            // Request permission
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            // Create a temporary file for the camera to save the image
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(Date())
            val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val photoFile = java.io.File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )

            selectedImageUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            // Create camera intent
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri)

            Log.d("EditProfileFragment", "Launching camera with URI: $selectedImageUri")
            takePictureLauncher.launch(cameraIntent)
        } catch (ex: Exception) {
            Log.e("EditProfileFragment", "Error opening camera", ex)
            Toast.makeText(context, "Error opening camera: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelImageChange() {
        // Clear the selected image
        selectedImageUri = null

        // Restore the original image
        Glide.with(this)
            .load(originalImageUrl)
            .placeholder(R.drawable.user_profile_placeholder)
            .error(R.drawable.user_profile_placeholder)
            .circleCrop()
            .into(profilePhoto)

        // Hide cancel button and show gallery/camera buttons again
        cancelImageButton.visibility = View.GONE
        galleryButton.visibility = View.VISIBLE
        cameraButton.visibility = View.VISIBLE

        Log.d("EditProfileFragment", "Image change cancelled, restored original")
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

        // If image is selected, upload it first
        if (selectedImageUri != null) {
            Log.d("EditProfileFragment", "Uploading profile image to Firebase Storage...")
            saveBtn.text = "Uploading image..."

            Model.shared.uploadProfilePicture(
                selectedImageUri!!,
                currentUser.id,
                { success, downloadUrl, message ->
                    if (success && downloadUrl != null) {
                        Log.d("EditProfileFragment", "Image uploaded successfully: $downloadUrl")

                        // Update user with the uploaded image URL
                        saveBtn.text = "Updating profile..."
                        updateUserProfile(currentUser.id, newUsername, newDiscordTag, downloadUrl, mainActivity)
                    } else {
                        // Image upload failed
                        saveBtn.isEnabled = true
                        saveBtn.text = "Save Changes"

                        Log.e("EditProfileFragment", "Failed to upload image: $message")
                        Toast.makeText(context, "Failed to upload image: $message", Toast.LENGTH_LONG).show()
                    }
                },
                onProgress = { progress ->
                    Log.d("EditProfileFragment", "Upload progress: $progress%")
                    activity?.runOnUiThread {
                        saveBtn.text = "Uploading... $progress%"
                    }
                }
            )
        } else {
            // No image selected, just update user info
            updateUserProfile(currentUser.id, newUsername, newDiscordTag, null, mainActivity)
        }
    }

    private fun updateUserProfile(
        userId: String,
        username: String,
        discordTag: String,
        profileImageUrl: String?,
        mainActivity: MainActivity
    ) {
        Model.shared.updateUser(userId, username, discordTag, profileImageUrl) { success, updatedUser, message ->
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




