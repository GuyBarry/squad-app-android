package com.example.squadapp

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.squadapp.utils.CameraUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EditProfileFragment : Fragment() {
    private val editProfileViewModel: EditProfileViewModel by viewModels()
    private val args: EditProfileFragmentArgs by navArgs()

    private lateinit var profilePhoto: ImageView
    private lateinit var galleryButton: FloatingActionButton
    private lateinit var cameraButton: FloatingActionButton
    private lateinit var cancelImageButton: FloatingActionButton
    private lateinit var deleteImageButton: FloatingActionButton
    private lateinit var userNameInput: EditText
    private lateinit var discordTagInput: EditText
    private lateinit var cancelBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton

    private var selectedImageUri: Uri? = null
    private var originalImageUrl: String = ""
    private var isImageDeleted: Boolean = false

    // ── Activity-result launchers ─────────────────────────────────────────────

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isImageDeleted = false
            loadProfileImageFromUri(uri)
            showCancelImageButton()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && selectedImageUri != null) {
            isImageDeleted = false
            loadProfileImageFromUri(selectedImageUri!!)
            showCancelImageButton()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera()
        else Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_edit_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        populateFieldsFromArgs()
        setupButtonListeners()
        observeViewModel()
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews(view: View) {
        profilePhoto = view.findViewById(R.id.edit_profile_photo)
        galleryButton = view.findViewById(R.id.gallery_button)
        cameraButton = view.findViewById(R.id.camera_button)
        cancelImageButton = view.findViewById(R.id.cancel_image_button)
        deleteImageButton = view.findViewById(R.id.delete_image_button)
        userNameInput = view.findViewById(R.id.user_name_input)
        discordTagInput = view.findViewById(R.id.discord_tag_input)
        cancelBtn = view.findViewById(R.id.cancel_btn)
        saveBtn = view.findViewById(R.id.save_btn)
    }

    private fun populateFieldsFromArgs() {
        val user = args.user
        originalImageUrl = user.profileImage
        loadProfileImageFromUrl(user.profileImage)
        userNameInput.setText(user.username)
        discordTagInput.setText(user.discordTag)
        if (user.profileImage.isNotEmpty()) deleteImageButton.visibility = View.VISIBLE
    }

    private fun setupButtonListeners() {
        galleryButton.setOnClickListener { galleryLauncher.launch("image/*") }
        cameraButton.setOnClickListener { openCamera() }
        cancelImageButton.setOnClickListener { cancelImageChange() }
        deleteImageButton.setOnClickListener { deleteImageChange() }
        cancelBtn.setOnClickListener { findNavController().popBackStack() }
        saveBtn.setOnClickListener { handleSaveProfile() }
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel() {
        editProfileViewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            setFormEnabled(!isSaving)
            if (isSaving) view?.clearFocus()
        }

        editProfileViewModel.saveProgress.observe(viewLifecycleOwner) { progress ->
            saveBtn.text = progress ?: "Save Changes"
        }

        editProfileViewModel.saveResult.observe(viewLifecycleOwner) { (success, updatedUser, message) ->
            if (success && updatedUser != null) {
                (activity as? MainActivity)?.onUserUpdated(updatedUser)
                Toast.makeText(context, message ?: "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                val action = EditProfileFragmentDirections
                    .actionEditProfileFragmentToProfileFragment(user = updatedUser)
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, message ?: "Failed to update profile", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Camera / image helpers ────────────────────────────────────────────────

    private fun openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) launchCamera()
        else requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        try {
            selectedImageUri = CameraUtils.createCameraImageUri(requireContext())
            takePictureLauncher.launch(CameraUtils.buildCameraIntent(selectedImageUri!!))
        } catch (ex: Exception) {
            Log.e("EditProfileFragment", "Error opening camera", ex)
            Toast.makeText(context, "Error opening camera: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Loads a remote/local URL into the profile photo using Glide (circle-cropped). */
    private fun loadProfileImageFromUrl(url: String) {
        Glide.with(this).load(url)
            .placeholder(R.drawable.user_profile_placeholder)
            .error(R.drawable.user_profile_placeholder)
            .circleCrop().into(profilePhoto)
    }

    /** Loads a local content [Uri] into the profile photo using Glide (circle-cropped). */
    private fun loadProfileImageFromUri(uri: Uri) {
        Glide.with(this).load(uri)
            .placeholder(R.drawable.user_profile_placeholder)
            .error(R.drawable.user_profile_placeholder)
            .circleCrop().into(profilePhoto)
    }

    /** Shows the cancel button and hides gallery/camera buttons. */
    private fun showCancelImageButton() {
        cancelImageButton.visibility = View.VISIBLE
        galleryButton.visibility = View.GONE
        cameraButton.visibility = View.GONE
    }

    private fun cancelImageChange() {
        selectedImageUri = null
        isImageDeleted = false
        loadProfileImageFromUrl(originalImageUrl)
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

    // ── Form state helpers ────────────────────────────────────────────────────

    private fun setFormEnabled(enabled: Boolean) {
        saveBtn.isEnabled = enabled
        cancelBtn.isEnabled = enabled
        userNameInput.isEnabled = enabled
        discordTagInput.isEnabled = enabled
        galleryButton.isEnabled = enabled
        cameraButton.isEnabled = enabled
        cancelImageButton.isEnabled = enabled
        deleteImageButton.isEnabled = enabled
    }

    // ── Save profile ──────────────────────────────────────────────────────────

    private fun handleSaveProfile() {
        val currentUser = args.user
        val newUsername = userNameInput.text.toString().trim()
        val newDiscordTag = discordTagInput.text.toString().trim()

        if (!validateInputFields(newUsername, newDiscordTag)) return

        if (hasNoChanges(currentUser, newUsername, newDiscordTag)) {
            Toast.makeText(context, "No changes to save", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        editProfileViewModel.saveProfile(
            currentUser, newUsername, newDiscordTag,
            selectedImageUri, isImageDeleted, originalImageUrl
        )
    }

    private fun validateInputFields(username: String, discordTag: String): Boolean {
        if (username.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (discordTag.isEmpty()) {
            Toast.makeText(context, "Discord tag cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun hasNoChanges(
        currentUser: com.example.squadapp.entities.User,
        newUsername: String,
        newDiscordTag: String
    ): Boolean =
        newUsername == currentUser.username &&
        newDiscordTag == currentUser.discordTag &&
        selectedImageUri == null && !isImageDeleted
}
