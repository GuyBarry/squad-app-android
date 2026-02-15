package com.example.squadapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.squadapp.entities.User
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.entities.PostAdapter
import com.example.squadapp.models.Model
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
        val postsCount = view.findViewById<TextView>(R.id.posts_count)
        val editProfileBtn = view.findViewById<MaterialButton>(R.id.edit_profile_btn)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.logout_btn)
        val userPostsRecyclerView = view.findViewById<RecyclerView>(R.id.user_posts_recycler_view)

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

            // Set up user posts RecyclerView
            setupUserPosts(userPostsRecyclerView, currentUser, postsCount)
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

    private fun setupUserPosts(
        recyclerView: RecyclerView,
        currentUser: User,
        postsCountTextView: TextView
    ) {
        // Fetch all posts from Firebase
        Model.shared.getPostsByUser(currentUser.id, { posts ->
            // Set up RecyclerView
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Set adapter with delete callback
            val adapter = PostAdapter(posts) { postToDelete ->
                // Show confirmation and delete post
                deletePost(postToDelete, recyclerView, currentUser, postsCountTextView)
            }
            recyclerView.adapter = adapter

            // Update posts count
            postsCountTextView.text = adapter.getItemCount().toString()
        })
    }

    private fun deletePost(
        post: com.example.squadapp.entities.Post,
        recyclerView: RecyclerView,
        currentUser: User,
        postsCountTextView: TextView
    ) {
        // Call Model to delete the post
        Model.shared.deletePost(post.id) { success, message ->
            if (success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                // Refresh the posts list
                setupUserPosts(recyclerView, currentUser, postsCountTextView)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to delete post: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

