package com.example.squadapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        val profilePhoto = view.findViewById<ImageView>(R.id.profile_photo)
        val userName = view.findViewById<TextView>(R.id.user_name)
        val discordTag = view.findViewById<TextView>(R.id.discord_tag)
        val editProfileBtn = view.findViewById<MaterialButton>(R.id.edit_profile_btn)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.logout_btn)

        // Get current user from MainActivity safely
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            val currentUser = mainActivity.currentUser

            // Populate with user data
            // Use default placeholder if profile image is not set (0 or invalid)
            val profileImageRes = if (currentUser.profileImage != 0) {
                currentUser.profileImage
            } else {
                R.drawable.user_profile_placeholder
            }
            profilePhoto.setImageResource(profileImageRes)
            userName.text = currentUser.username
            discordTag.text = currentUser.discordTag
        }

        // Set up edit profile button click listener
        editProfileBtn.setOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, EditProfileFragment())
                addToBackStack(null)
                commit()
            }
        }

        // Set up logout button click listener
        logoutBtn.setOnClickListener {
            //TODO: Clear user session data here (e.g., SharedPreferences, database, etc.)
            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }
}

