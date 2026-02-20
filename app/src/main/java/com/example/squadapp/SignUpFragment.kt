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
import com.example.squadapp.entities.NewUser
import com.example.squadapp.utils.SpannableUtils

class SignUpFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()
    private val signUpViewModel: SignUpViewModel by viewModels()
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var discordTagEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var signInLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_sign_up, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupSignInLink()
        setupButtonListeners()
        observeViewModel(view)
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews(view: View) {
        usernameEditText = view.findViewById(R.id.signup_username)
        emailEditText = view.findViewById(R.id.signup_email)
        discordTagEditText = view.findViewById(R.id.signup_discord_tag)
        passwordEditText = view.findViewById(R.id.signup_password)
        confirmPasswordEditText = view.findViewById(R.id.signup_confirm_password)
        signUpButton = view.findViewById(R.id.signup_button)
        signInLink = view.findViewById(R.id.signup_signin_link)
    }

    private fun setupButtonListeners() {
        signUpButton.setOnClickListener { handleSignUp() }
    }

    private fun setupSignInLink() {
        SpannableUtils.setClickableLink(
            textView = signInLink,
            fullText = getString(R.string.already_have_account_sign_in),
            clickableSubstring = getString(R.string.sign_in_here),
            onClick = { findNavController().navigate(R.id.action_signUpFragment_to_signInFragment) }
        )
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel(view: View) {
        signUpViewModel.isSigningUp.observe(viewLifecycleOwner) { isLoading ->
            setFormEnabled(!isLoading, view)
            signUpButton.text = if (isLoading) getString(R.string.signing_up) else getString(R.string.sign_up)
        }

        signUpViewModel.signUpResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, getString(R.string.sign_up_successful), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Form state helpers ────────────────────────────────────────────────────

    private fun setFormEnabled(enabled: Boolean, rootView: View) {
        signUpButton.isEnabled = enabled
        usernameEditText.isEnabled = enabled
        emailEditText.isEnabled = enabled
        discordTagEditText.isEnabled = enabled
        passwordEditText.isEnabled = enabled
        confirmPasswordEditText.isEnabled = enabled
        if (!enabled) rootView.clearFocus()
    }

    // ── Sign up ───────────────────────────────────────────────────────────────

    private fun handleSignUp() {
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val discordTag = discordTagEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (!validateSignUpFields(username, email, discordTag, password, confirmPassword)) return

        val newUser = NewUser(profileImage = "", username = username, email = email, discordTag = discordTag)
        signUpViewModel.signUp(password, newUser) { user ->
            authViewModel.onAuthSuccess(user)
        }
    }

    private fun validateSignUpFields(
        username: String,
        email: String,
        discordTag: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (username.isEmpty()) {
            Toast.makeText(context, getString(R.string.username_cannot_be_empty), Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty()) {
            Toast.makeText(context, getString(R.string.email_cannot_be_empty), Toast.LENGTH_SHORT).show()
            return false
        }
        if (discordTag.isEmpty()) {
            Toast.makeText(context, getString(R.string.discord_tag_cannot_be_empty), Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(context, getString(R.string.password_cannot_be_empty), Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(context, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(context, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}
