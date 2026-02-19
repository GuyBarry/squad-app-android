package com.example.squadapp

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
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

        usernameEditText = view.findViewById(R.id.signup_username)
        emailEditText = view.findViewById(R.id.signup_email)
        discordTagEditText = view.findViewById(R.id.signup_discord_tag)
        passwordEditText = view.findViewById(R.id.signup_password)
        confirmPasswordEditText = view.findViewById(R.id.signup_confirm_password)
        signUpButton = view.findViewById(R.id.signup_button)
        signInLink = view.findViewById(R.id.signup_signin_link)

        setupSignInLink()

        signUpButton.setOnClickListener { handleSignUp() }

        signUpViewModel.isSigningUp.observe(viewLifecycleOwner) { isLoading ->
            signUpButton.isEnabled = !isLoading
            signUpButton.text = if (isLoading) getString(R.string.signing_up) else getString(R.string.sign_up)
        }

        signUpViewModel.signUpResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, "Sign up successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignUp() {
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val discordTag = discordTagEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isEmpty()) {
            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (discordTag.isEmpty()) {
            Toast.makeText(context, "Discord tag cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val newUser = NewUser(
            profileImage = "",
            username = username,
            email = email,
            discordTag = discordTag
        )

        signUpViewModel.signUp(password, newUser) { user ->
            authViewModel.onAuthSuccess(user)
        }
    }

    private fun setupSignInLink() {
        val fullText = "Already have an account? Sign in here"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val startIndex = fullText.indexOf("Sign in here")
        val endIndex = startIndex + "Sign in here".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signInLink.text = spannableString
        signInLink.movementMethod = LinkMovementMethod.getInstance()
    }
}
