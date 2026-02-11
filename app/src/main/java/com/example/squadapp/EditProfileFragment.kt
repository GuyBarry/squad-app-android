package com.example.squadapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class EditProfileFragment : Fragment() {

    private lateinit var profilePhoto: ImageView
    private lateinit var cameraButton: ImageButton
    private lateinit var userNameInput: EditText
    private lateinit var discordTagInput: EditText
    private lateinit var cancelBtn: MaterialButton
    private lateinit var saveBtn: MaterialButton

    // Activity result launcher for gallery
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
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

        // Load existing user data
        val currentUserName = requireContext().getString(R.string.user_name)
        val currentDiscordTag = requireContext().getString(R.string.discord_tag)

        userNameInput.setText(currentUserName)
        discordTagInput.setText(currentDiscordTag)

        // Set up click listeners
        cameraButton.setOnClickListener {
            // Open gallery to select image
            galleryLauncher.launch("image/*")
        }

        cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        saveBtn.setOnClickListener {
            // TODO: Save changes (user name, discord tag, and profile photo)
            parentFragmentManager.popBackStack()
        }
    }
}




