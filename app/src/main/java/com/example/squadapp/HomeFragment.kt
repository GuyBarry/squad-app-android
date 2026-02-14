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

        // Fetch all posts from Firebase
        Model.shared.getAllPosts { posts ->
            val adapter = PostAdapter(posts)
            recyclerView.adapter = adapter
        }
    }
}

