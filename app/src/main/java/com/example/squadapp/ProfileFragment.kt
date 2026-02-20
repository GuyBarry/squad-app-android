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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.entities.PostAdapter
import com.example.squadapp.entities.User
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private val args: ProfileFragmentArgs by navArgs()

    private lateinit var profilePhoto: ImageView
    private lateinit var userName: TextView
    private lateinit var discordTag: TextView
    private lateinit var postsCount: TextView
    private lateinit var editProfileBtn: MaterialButton
    private lateinit var logoutBtn: MaterialButton
    private lateinit var userPostsRecyclerView: RecyclerView
    private lateinit var noPostsMessage: TextView
    private lateinit var loadingIndicator: View
    private lateinit var contentContainer: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = args.user

        bindViews(view)
        setupRecyclerView()
        populateUserHeader(user)
        setupButtonListeners(user)
        observeViewModel(user)

        profileViewModel.loadUserPosts(user.id)
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadUserPosts(args.user.id)
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews(view: View) {
        profilePhoto = view.findViewById(R.id.profile_photo)
        userName = view.findViewById(R.id.user_name)
        discordTag = view.findViewById(R.id.discord_tag)
        postsCount = view.findViewById(R.id.posts_count)
        editProfileBtn = view.findViewById(R.id.edit_profile_btn)
        logoutBtn = view.findViewById(R.id.logout_btn)
        userPostsRecyclerView = view.findViewById(R.id.user_posts_recycler_view)
        noPostsMessage = view.findViewById(R.id.no_posts_message)
        loadingIndicator = view.findViewById(R.id.posts_loading_indicator)
        contentContainer = view.findViewById(R.id.posts_content_container)
    }

    private fun setupRecyclerView() {
        userPostsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun populateUserHeader(user: User) {
        Glide.with(this)
            .load(user.profileImage)
            .placeholder(R.drawable.user_profile_placeholder)
            .error(R.drawable.user_profile_placeholder)
            .circleCrop()
            .into(profilePhoto)
        userName.text = user.username
        discordTag.text = user.discordTag
    }

    // ── Button listeners ──────────────────────────────────────────────────────

    private fun setupButtonListeners(user: User) {
        editProfileBtn.setOnClickListener { navigateToEditProfile(user) }
        logoutBtn.setOnClickListener { signOutAndReturnToAuth() }
    }

    private fun navigateToEditProfile(user: User) {
        val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(user = user)
        findNavController().navigate(action)
    }

    private fun signOutAndReturnToAuth() {
        profileViewModel.signOut()
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel(user: User) {
        profileViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            contentContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        profileViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postsCount.text = posts.size.toString()
            updatePostsList(posts, user)
        }

        profileViewModel.deleteResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to delete post: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Posts list ────────────────────────────────────────────────────────────

    private fun updatePostsList(posts: List<com.example.squadapp.entities.Post>, user: User) {
        if (posts.isEmpty()) {
            noPostsMessage.visibility = View.VISIBLE
            userPostsRecyclerView.visibility = View.GONE
        } else {
            noPostsMessage.visibility = View.GONE
            userPostsRecyclerView.visibility = View.VISIBLE
            userPostsRecyclerView.adapter = PostAdapter(
                posts,
                onDeletePost = { postToDelete ->
                    profileViewModel.deletePost(postToDelete.id, postToDelete.image, user.id)
                },
                onEditPost = { postToEdit ->
                    navigateToEditPost(postToEdit, user)
                }
            )
        }
    }

    private fun navigateToEditPost(post: com.example.squadapp.entities.Post, user: User) {
        val action = ProfileFragmentDirections.actionProfileFragmentToEditPostFragment(
            post = post,
            user = user
        )
        findNavController().navigate(action)
    }
}
