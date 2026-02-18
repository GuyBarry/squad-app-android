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
import com.bumptech.glide.Glide
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
        val noPostsMessage = view.findViewById<TextView>(R.id.no_posts_message)
        val loadingIndicator = view.findViewById<View>(R.id.posts_loading_indicator)
        val contentContainer = view.findViewById<View>(R.id.posts_content_container)

        loadUserProfile(profilePhoto, userName, discordTag, postsCount, userPostsRecyclerView,
            noPostsMessage, loadingIndicator, contentContainer)

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
            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user profile when returning from EditProfileFragment
        val profilePhoto = view?.findViewById<ImageView>(R.id.profile_photo)
        val userName = view?.findViewById<TextView>(R.id.user_name)
        val discordTag = view?.findViewById<TextView>(R.id.discord_tag)
        val postsCount = view?.findViewById<TextView>(R.id.posts_count)
        val userPostsRecyclerView = view?.findViewById<RecyclerView>(R.id.user_posts_recycler_view)
        val noPostsMessage = view?.findViewById<TextView>(R.id.no_posts_message)
        val loadingIndicator = view?.findViewById<View>(R.id.posts_loading_indicator)
        val contentContainer = view?.findViewById<View>(R.id.posts_content_container)

        if (profilePhoto != null && userName != null && discordTag != null &&
            postsCount != null && userPostsRecyclerView != null && noPostsMessage != null &&
            loadingIndicator != null && contentContainer != null) {
            loadUserProfile(profilePhoto, userName, discordTag, postsCount, userPostsRecyclerView,
                noPostsMessage, loadingIndicator, contentContainer)
        }
    }

    private fun loadUserProfile(
        profilePhoto: ImageView,
        userName: TextView,
        discordTag: TextView,
        postsCount: TextView,
        userPostsRecyclerView: RecyclerView,
        noPostsMessage: TextView,
        loadingIndicator: View,
        contentContainer: View
    ) {
        // Get current user from MainActivity safely
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            val currentUser = mainActivity.currentUser

            // Load user profile image using Glide
            Glide.with(this)
                .load(currentUser.profileImage)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop()
                .into(profilePhoto)

            userName.text = currentUser.username
            discordTag.text = currentUser.discordTag

            // Set up user posts RecyclerView
            setupUserPosts(userPostsRecyclerView, currentUser, postsCount, noPostsMessage,
                loadingIndicator, contentContainer)
        }
    }

    private fun setupUserPosts(
        recyclerView: RecyclerView,
        currentUser: User,
        postsCountTextView: TextView,
        noPostsMessage: TextView,
        loadingIndicator: View,
        contentContainer: View
    ) {
        // Show loading state
        loadingIndicator.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE

        // Fetch all posts from Firebase
        Model.shared.getPostsByUser(currentUser.id, { posts ->
            // Hide loading, show content
            loadingIndicator.visibility = View.GONE
            contentContainer.visibility = View.VISIBLE

            // Update posts count with actual number
            postsCountTextView.text = posts.size.toString()

            if (posts.isEmpty()) {
                // No posts - show message, hide RecyclerView
                noPostsMessage.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                // Has posts - show RecyclerView, hide message
                noPostsMessage.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Set up RecyclerView
                recyclerView.layoutManager = LinearLayoutManager(requireContext())

                // Set adapter with delete callback
                val adapter = PostAdapter(posts) { postToDelete ->
                    // Show confirmation and delete post
                    deletePost(postToDelete, recyclerView, currentUser, postsCountTextView,
                        noPostsMessage, loadingIndicator, contentContainer)
                }
                recyclerView.adapter = adapter
            }
        })
    }

    private fun deletePost(
        post: com.example.squadapp.entities.Post,
        recyclerView: RecyclerView,
        currentUser: User,
        postsCountTextView: TextView,
        noPostsMessage: TextView,
        loadingIndicator: View,
        contentContainer: View
    ) {
        Model.shared.deletePost(post.id, post.image) { success, message ->
            if (success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                // Refresh the posts list
                setupUserPosts(recyclerView, currentUser, postsCountTextView, noPostsMessage,
                    loadingIndicator, contentContainer)
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

