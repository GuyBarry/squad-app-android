package com.example.squadapp

import android.content.Intent
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

class SignInFragment : Fragment() {
    private lateinit var usernameEditText: EditText
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

        usernameEditText = view.findViewById(R.id.signin_username)
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
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validation
        if (username.isEmpty()) {
            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement actual sign-in logic (e.g., validate credentials with database/API)
        Toast.makeText(context, "Sign in successful for $username", Toast.LENGTH_SHORT).show()

        // Navigate to MainActivity after successful sign-in
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToSignUp() {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, SignUpFragment())
            addToBackStack(null)
            commit()
        }
    }
}


