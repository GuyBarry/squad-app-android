package com.example.squadapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.Post
import com.example.squadapp.models.Model

/**
 * ProfileViewModel - Manages user profile data and their posts.
 */
class ProfileViewModel : ViewModel() {

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /** Emits a one-shot message (success or error) after a delete operation. */
    private val _deleteResult = MutableLiveData<Pair<Boolean, String>>()
    val deleteResult: LiveData<Pair<Boolean, String>> = _deleteResult

    fun loadUserPosts(userId: String) {
        _isLoading.value = true
        Model.shared.getPostsByUser(userId) { posts ->
            _userPosts.postValue(posts)
            _isLoading.postValue(false)
        }
    }

    fun deletePost(postId: String, imageUrl: String, userId: String) {
        Model.shared.deletePost(postId, imageUrl) { success, message ->
            _deleteResult.postValue(Pair(success, message))
            if (success) {
                // Reload user posts after deletion
                loadUserPosts(userId)
            }
        }
    }

    fun signOut() {
        Model.shared.signOut()
    }
}

