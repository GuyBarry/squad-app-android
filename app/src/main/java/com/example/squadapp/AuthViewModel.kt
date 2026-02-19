package com.example.squadapp

import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AuthViewModel : ViewModel() {
    private val _navigateToMain = MutableSharedFlow<User>(extraBufferCapacity = 1)
    val navigateToMain = _navigateToMain.asSharedFlow()

    fun onAuthSuccess(user: User) {
        _navigateToMain.tryEmit(user)
    }
}
