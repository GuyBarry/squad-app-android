package com.example.squadapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

        // Create sample posts
        val samplePosts = listOf(
            Post(
                id = 1,
                postImage = R.drawable.post_image_placeholder_1,
                user = User(
                    id = 1,
                    profileImage = R.drawable.user_profile_placeholder,
                    userName = "John Gamer",
                    discordTag = "JohnG#5678"
                ),
                postText = "Just finished an amazing gaming session! The graphics on this game are absolutely insane. Can't wait to play more tomorrow!"
            ),
            Post(
                id = 2,
                postImage = R.drawable.post_image_placeholder_2,
                user = User(
                    id = 2,
                    profileImage = R.drawable.user_profile_placeholder,
                    userName = "Sarah Pro",
                    discordTag = "SarahPro#1234"
                ),
                postText = "New speedrun world record! 🎮 Finally beat my personal best after weeks of training. Thanks to everyone who supported me!"
            ),
            Post(
                id = 3,
                postImage = R.drawable.post_image_placeholder_3,
                user = User(
                    id = 3,
                    profileImage = R.drawable.user_profile_placeholder,
                    userName = "Mike Gaming",
                    discordTag = "MikeG#9012"
                ),
                postText = "Tournament tomorrow! Really excited to compete with the squad. Let's bring home the trophy!"
            ),
            Post(
                id = 4,
                postImage = R.drawable.post_image_placeholder_4,
                user = User(
                    id = 4,
                    profileImage = R.drawable.user_profile_placeholder,
                    userName = "Alex Stream",
                    discordTag = "AlexS#3456"
                ),
                postText = "Going live in 10 minutes! Come hang out with us for a chill gaming stream. Link in bio!"
            )
        )

        // Set adapter
        val adapter = PostAdapter(samplePosts)
        recyclerView.adapter = adapter
    }
}

