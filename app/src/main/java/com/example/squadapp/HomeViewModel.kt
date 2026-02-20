package com.example.squadapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.Post
import com.example.squadapp.models.Model

class HomeViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPosts() {
        _isLoading.value = true
        Model.shared.getAllPosts { postList ->
            _posts.postValue(postList)
            _isLoading.postValue(false)
        }
    }
}

