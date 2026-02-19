package com.example.squadapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.entities.PostAdapter

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModels()

    private var recyclerView: RecyclerView? = null
    private var loadingIndicator: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.posts_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        loadingIndicator = view.findViewById(R.id.home_loading_indicator)

        // Observe loading state
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
            recyclerView?.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        // Observe posts
        homeViewModel.posts.observe(viewLifecycleOwner) { posts ->
            recyclerView?.adapter = PostAdapter(posts)
        }

        homeViewModel.loadPosts()
    }

    override fun onResume() {
        super.onResume()
        // Refresh posts when returning to home (e.g., after posting)
        homeViewModel.loadPosts()
    }
}

