package com.example.squadapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.User

/**
 * MainViewModel - Shared ViewModel scoped to MainActivity.
 * Holds the currently authenticated user, making it accessible to all
 * child fragments without casting to the Activity.
 */
class MainViewModel : ViewModel() {

    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User> = _currentUser

    fun setUser(user: User) {
        _currentUser.value = user
    }

    fun updateUser(user: User) {
        _currentUser.value = user
    }
}

