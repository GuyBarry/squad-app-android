package com.example.squadapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.entities.PostAdapter
import com.example.squadapp.models.Model

class HomeFragment : Fragment() {

    private var recyclerView: RecyclerView? = null

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
        recyclerView = view.findViewById(R.id.posts_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        // Load posts
        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        // Refresh posts when returning to home (e.g., after profile edit)
        loadPosts()
    }

    private fun loadPosts() {
        // Fetch all posts from Firebase
        Model.shared.getAllPosts { posts ->
            val adapter = PostAdapter(posts)
            recyclerView?.adapter = adapter
        }
    }
}

