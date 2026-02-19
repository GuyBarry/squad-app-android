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
import androidx.navigation.fragment.findNavController
import com.example.squadapp.models.Model

class SignInFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var signUpLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailEditText = view.findViewById(R.id.signin_email)
        passwordEditText = view.findViewById(R.id.signin_password)
        signInButton = view.findViewById(R.id.signin_button)
        signUpLink = view.findViewById(R.id.signin_signup_link)

        signInButton.setOnClickListener {
            handleSignIn()
        }

        // Create clickable and underlined "Sign up here" text
        setupSignUpLink()
    }

    private fun setupSignUpLink() {
        val fullText = "Don't have an account? Sign up here"
        val spannableString = SpannableString(fullText)
        
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToSignUp()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // Remove default underline, we'll add our own
            }
        }

        // Find the position of "Sign up here"
        val startIndex = fullText.indexOf("Sign up here")
        val endIndex = startIndex + "Sign up here".length

        // Apply clickable span
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        // Apply underline span
        spannableString.setSpan(UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signUpLink.text = spannableString
        signUpLink.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun handleSignIn() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validation
        if (email.isEmpty()) {
            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button to prevent multiple clicks
        signInButton.isEnabled = false
        signInButton.text = getString(R.string.signing_in)

        // Authenticate user with Firebase
        Model.shared.signInUser(email, password) { success, user, message ->
            signInButton.isEnabled = true
            signInButton.text = getString(R.string.sign_in)

            if (success && user != null) {
                Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
                authViewModel.onAuthSuccess(user)
            } else {
                Toast.makeText(context, message ?: "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToSignUp() {
        findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
    }
}
