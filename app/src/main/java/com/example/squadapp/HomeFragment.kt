package com.example.squadapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.posts_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create sample posts with timestamps
        val currentTime = System.currentTimeMillis()
        val samplePosts = listOf(
            Post(
                id = 1,
                image = R.drawable.post_image_placeholder_1,
                user = User(
                    id = 1,
                    profileImage = R.drawable.user_profile_placeholder,
                    username = "John Gamer",
                    discordTag = "JohnG#5678"
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
                    discordTag = "SarahPro#1234"
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
                    discordTag = "MikeG#9012"
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
                    discordTag = "AlexS#3456"
                ),
                description = "Going live in 10 minutes! Come hang out with us for a chill gaming stream. Link in bio!",
                creationTime = Date(currentTime - (5 * 60_000)) // 5 minutes ago
            )
        ).sortedByDescending { it.creationTime }

        // Set adapter
        val adapter = PostAdapter(samplePosts)
        recyclerView.adapter = adapter
    }
}

