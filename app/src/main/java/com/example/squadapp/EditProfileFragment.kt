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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import java.util.Date

class EditProfileFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val editProfileViewModel: EditProfileViewModel by viewModels()

    private lateinit var profilePhoto: ImageView
    private lateinit var galleryButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var cameraButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var cancelImageButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var deleteImageButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var userNameInput: EditText
    private lateinit var discordTagInput: EditText
    private lateinit var cancelBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton

    private var selectedImageUri: Uri? = null
    private var originalImageUrl: String = ""
    private var isImageDeleted: Boolean = false

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isImageDeleted = false
            Glide.with(this).load(uri)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop().into(profilePhoto)
            cancelImageButton.visibility = View.VISIBLE
            galleryButton.visibility = View.GONE
            cameraButton.visibility = View.GONE
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && selectedImageUri != null) {
            isImageDeleted = false
            Glide.with(this).load(selectedImageUri)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop().into(profilePhoto)
            cancelImageButton.visibility = View.VISIBLE
            galleryButton.visibility = View.GONE
            cameraButton.visibility = View.GONE
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) launchCamera()
        else Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profilePhoto = view.findViewById(R.id.edit_profile_photo)
        galleryButton = view.findViewById(R.id.gallery_button)
        cameraButton = view.findViewById(R.id.camera_button)
        cancelImageButton = view.findViewById(R.id.cancel_image_button)
        deleteImageButton = view.findViewById(R.id.delete_image_button)
        userNameInput = view.findViewById(R.id.user_name_input)
        discordTagInput = view.findViewById(R.id.discord_tag_input)
        cancelBtn = view.findViewById(R.id.cancel_btn)
        saveBtn = view.findViewById(R.id.save_btn)

        // Populate fields from the shared MainViewModel
        mainViewModel.currentUser.value?.let { user ->
            originalImageUrl = user.profileImage
            Glide.with(this).load(user.profileImage)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop().into(profilePhoto)
            userNameInput.setText(user.username)
            discordTagInput.setText(user.discordTag)
            if (user.profileImage.isNotEmpty()) deleteImageButton.visibility = View.VISIBLE
        }

        galleryButton.setOnClickListener { galleryLauncher.launch("image/*") }
        cameraButton.setOnClickListener { openCamera() }
        cancelImageButton.setOnClickListener { cancelImageChange() }
        deleteImageButton.setOnClickListener { deleteImageChange() }
        cancelBtn.setOnClickListener { findNavController().popBackStack() }
        saveBtn.setOnClickListener { handleSaveProfile() }

        // Observe saving state
        editProfileViewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            saveBtn.isEnabled = !isSaving
        }

        editProfileViewModel.saveProgress.observe(viewLifecycleOwner) { progress ->
            saveBtn.text = progress ?: "Save Changes"
        }

        // Observe save result
        editProfileViewModel.saveResult.observe(viewLifecycleOwner) { (success, updatedUser, message) ->
            if (success && updatedUser != null) {
                // Push the updated user into MainViewModel — all fragments will update automatically
                mainViewModel.updateUser(updatedUser)
                Toast.makeText(context, message ?: "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, message ?: "Failed to update profile", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(Date())
            val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val photoFile = java.io.File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            selectedImageUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", photoFile
            )
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri)
            takePictureLauncher.launch(cameraIntent)
        } catch (ex: Exception) {
            Log.e("EditProfileFragment", "Error opening camera", ex)
            Toast.makeText(context, "Error opening camera: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelImageChange() {
        selectedImageUri = null
        isImageDeleted = false
        Glide.with(this).load(originalImageUrl)
            .placeholder(R.drawable.user_profile_placeholder)
            .error(R.drawable.user_profile_placeholder)
            .circleCrop().into(profilePhoto)
        cancelImageButton.visibility = View.GONE
        galleryButton.visibility = View.VISIBLE
        cameraButton.visibility = View.VISIBLE
        if (originalImageUrl.isNotEmpty()) deleteImageButton.visibility = View.VISIBLE
    }

    private fun deleteImageChange() {
        isImageDeleted = true
        selectedImageUri = null
        profilePhoto.setImageResource(R.drawable.user_profile_placeholder)
        deleteImageButton.visibility = View.GONE
        cancelImageButton.visibility = View.VISIBLE
        galleryButton.visibility = View.GONE
        cameraButton.visibility = View.GONE
    }

    private fun handleSaveProfile() {
        val newUsername = userNameInput.text.toString().trim()
        val newDiscordTag = discordTagInput.text.toString().trim()
        val currentUser = mainViewModel.currentUser.value ?: run {
            Toast.makeText(context, "Error: Could not get user information", Toast.LENGTH_SHORT).show()
            return
        }

        if (newUsername.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (newDiscordTag.isEmpty()) {
            Toast.makeText(context, "Discord tag cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (newUsername == currentUser.username &&
            newDiscordTag == currentUser.discordTag &&
            selectedImageUri == null && !isImageDeleted) {
            Toast.makeText(context, "No changes to save", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        editProfileViewModel.saveProfile(
            currentUser, newUsername, newDiscordTag,
            selectedImageUri, isImageDeleted, originalImageUrl
        )
    }
}
