package com.example.squadapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Date

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

    private fun setupUserPosts(recyclerView: RecyclerView, currentUser: User, postsCountTextView: TextView) {
        // Create sample posts (same as HomeFragment)
        val currentTime = System.currentTimeMillis()
        val allPosts = listOf(
            Post(
                id = 1,
                image = R.drawable.post_image_placeholder_1,
                user = User(
                    id = 1,
                    profileImage = R.drawable.user_profile_placeholder,
                    username = "John Gamer",
                    discordTag = "JohnG#5678",
                    password = "5678"
                ),
                description = "Just finished an amazing gaming session! The graphics on this game are absolutely insane. Can't wait to play more tomorrow!",
                creationTime = Date(currentTime - (2 * 3_600_000)) // 2 hours ago
            ),
            Post(
                id = 2,
                image = R.drawable.post_image_placeholder_2,
                user = User(
                    id = 2,
                    profileImage = R.drawable.user_profile_placeholder,
                    username = "Sarah Pro",
                    discordTag = "SarahPro#1234",
                    password = "1234"
                ),
                description = "New speedrun world record! 🎮 Finally beat my personal best after weeks of training. Thanks to everyone who supported me!",
                creationTime = Date(currentTime - (30 * 60_000)) // 30 minutes ago
            ),
            Post(
                id = 3,
                image = R.drawable.post_image_placeholder_3,
                user = User(
                    id = 3,
                    profileImage = R.drawable.user_profile_placeholder,
                    username = "Mike Gaming",
                    discordTag = "MikeG#9012",
                    password = "9012"
                ),
                description = "Tournament tomorrow! Really excited to compete with the squad. Let's bring home the trophy!",
                creationTime = Date(currentTime - (1 * 86_400_000)) // 1 day ago
            ),
            Post(
                id = 4,
                image = R.drawable.post_image_placeholder_4,
                user = User(
                    id = 4,
                    profileImage = R.drawable.user_profile_placeholder,
                    username = "Alex Stream",
                    discordTag = "AlexS#3456",
                    password = "3456"
                ),
                description = "Going live in 10 minutes! Come hang out with us for a chill gaming stream. Link in bio!",
                creationTime = Date(currentTime - (5 * 60_000)) // 5 minutes ago
            )
        )

        // Filter posts by current user
        val userPosts = allPosts.filter { it.user.id == currentUser.id }.sortedByDescending { it.creationTime }


        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set adapter
        val adapter = PostAdapter(userPosts)

        // Update posts count
        postsCountTextView.text = adapter.getItemCount().toString()

        recyclerView.adapter = adapter
    }
}

