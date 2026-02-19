package com.example.squadapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.models.Model

class SignInViewModel : ViewModel() {

    private val _isSigningIn = MutableLiveData(false)
    val isSigningIn: LiveData<Boolean> = _isSigningIn

    private val _signInResult = MutableLiveData<Pair<Boolean, String?>>()
    val signInResult: LiveData<Pair<Boolean, String?>> = _signInResult

    fun signIn(email: String, password: String, onSuccess: (com.example.squadapp.entities.User) -> Unit) {
        _isSigningIn.value = true
        Model.shared.signInUser(email, password) { success, user, message ->
            _isSigningIn.postValue(false)
            if (success && user != null) {
                _signInResult.postValue(Pair(true, message))
                onSuccess(user)
            } else {
                _signInResult.postValue(Pair(false, message ?: "Sign in failed"))
            }
        }
    }
}

