package com.example.squadapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.squadapp.entities.PostAdapter

class HomeFragment : Fragment() {
    private val homeViewModel: HomeViewModel by viewModels()
    private var recyclerView: RecyclerView? = null
    private var loadingIndicator: View? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)

        swipeRefreshLayout?.setOnRefreshListener {
            homeViewModel.refreshPosts()
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val isPullRefreshing = swipeRefreshLayout?.isRefreshing == true
            if (!isLoading) {
                swipeRefreshLayout?.isRefreshing = false
            }

            // Only show the full-screen spinner on the initial load (not during swipe-refresh)
            loadingIndicator?.visibility = if (isLoading && !isPullRefreshing) View.VISIBLE else View.GONE
            recyclerView?.visibility = if (isLoading && !isPullRefreshing) View.GONE else View.VISIBLE
        }

        homeViewModel.posts.observe(viewLifecycleOwner) { posts ->
            recyclerView?.adapter = PostAdapter(posts)
        }

        homeViewModel.refreshPosts()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadPosts()
    }
}
