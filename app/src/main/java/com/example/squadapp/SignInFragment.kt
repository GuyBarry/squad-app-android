package com.example.squadapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.squadapp.utils.SpannableUtils

class SignInFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()
    private val signInViewModel: SignInViewModel by viewModels()
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_sign_in, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupSignUpLink()
        setupButtonListeners()
        observeViewModel(view)
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews(view: View) {
        emailEditText = view.findViewById(R.id.signin_email)
        passwordEditText = view.findViewById(R.id.signin_password)
        signInButton = view.findViewById(R.id.signin_button)
        signUpLink = view.findViewById(R.id.signin_signup_link)
    }

    private fun setupButtonListeners() {
        signInButton.setOnClickListener { handleSignIn() }
    }

    private fun setupSignUpLink() {
        SpannableUtils.setClickableLink(
            textView = signUpLink,
            fullText = "Don't have an account? Sign up here",
            clickableSubstring = "Sign up here",
            onClick = { findNavController().navigate(R.id.action_signInFragment_to_signUpFragment) }
        )
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel(view: View) {
        signInViewModel.isSigningIn.observe(viewLifecycleOwner) { isLoading ->
            setFormEnabled(!isLoading, view)
            signInButton.text = if (isLoading) getString(R.string.signing_in) else getString(R.string.sign_in)
        }

        signInViewModel.signInResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Form state helpers ────────────────────────────────────────────────────

    private fun setFormEnabled(enabled: Boolean, rootView: View) {
        signInButton.isEnabled = enabled
        emailEditText.isEnabled = enabled
        passwordEditText.isEnabled = enabled
        if (!enabled) rootView.clearFocus()
    }

    // ── Sign in ───────────────────────────────────────────────────────────────

    private fun handleSignIn() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (!validateSignInFields(email, password)) return

        signInViewModel.signIn(email, password) { user ->
            authViewModel.onAuthSuccess(user)
        }
    }

    private fun validateSignInFields(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}
