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
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private val args: ProfileFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = args.user

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

        userPostsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        Glide.with(this)
            .load(user.profileImage)
            .placeholder(R.drawable.user_profile_placeholder)
            .error(R.drawable.user_profile_placeholder)
            .circleCrop()
            .into(profilePhoto)
        userName.text = user.username
        discordTag.text = user.discordTag

        profileViewModel.loadUserPosts(user.id)

        profileViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            contentContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        profileViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postsCount.text = posts.size.toString()
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
                        val action = ProfileFragmentDirections.actionProfileFragmentToEditPostFragment(
                            post = postToEdit,
                            user = user
                        )
                        findNavController().navigate(action)
                    }
                )
            }
        }

        profileViewModel.deleteResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to delete post: $message", Toast.LENGTH_LONG).show()
            }
        }

        editProfileBtn.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(user = user)
            findNavController().navigate(action)
        }

        logoutBtn.setOnClickListener {
            profileViewModel.signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadUserPosts(args.user.id)
    }
}
