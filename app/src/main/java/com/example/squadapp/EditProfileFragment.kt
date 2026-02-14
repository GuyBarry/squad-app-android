package com.example.squadapp

import android.net.Uri
import android.os.Bundle
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
            // TODO: Save changes (user name, discord tag, and profile photo)
            parentFragmentManager.popBackStack()
        }
    }
}




