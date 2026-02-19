package com.example.squadapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.NewUser
import com.example.squadapp.entities.User
import com.example.squadapp.models.Model

class SignUpViewModel : ViewModel() {

    private val _isSigningUp = MutableLiveData(false)
    val isSigningUp: LiveData<Boolean> = _isSigningUp

    private val _signUpResult = MutableLiveData<Pair<Boolean, String?>>()
    val signUpResult: LiveData<Pair<Boolean, String?>> = _signUpResult

    fun signUp(password: String, newUser: NewUser, onSuccess: (User) -> Unit) {
        _isSigningUp.value = true
        Model.shared.signUpUser(password, newUser) { success, user, message ->
            _isSigningUp.postValue(false)
            if (success && user != null) {
                _signUpResult.postValue(Pair(true, message))
                onSuccess(user)
            } else {
                _signUpResult.postValue(Pair(false, message ?: "Sign up failed"))
            }
        }
    }
}

