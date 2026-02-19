package com.example.squadapp

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
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

        emailEditText = view.findViewById(R.id.signin_email)
        passwordEditText = view.findViewById(R.id.signin_password)
        signInButton = view.findViewById(R.id.signin_button)
        signUpLink = view.findViewById(R.id.signin_signup_link)

        setupSignUpLink()

        signInButton.setOnClickListener { handleSignIn() }

        signInViewModel.isSigningIn.observe(viewLifecycleOwner) { isLoading ->
            signInButton.isEnabled = !isLoading
            signInButton.text = if (isLoading) getString(R.string.signing_in) else getString(R.string.sign_in)
            emailEditText.isEnabled = !isLoading
            passwordEditText.isEnabled = !isLoading
            if (isLoading) {
                view.clearFocus()
            }
        }

        signInViewModel.signInResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        signInViewModel.signIn(email, password) { user ->
            authViewModel.onAuthSuccess(user)
        }
    }

    private fun setupSignUpLink() {
        val fullText = "Don't have an account? Sign up here"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val startIndex = fullText.indexOf("Sign up here")
        val endIndex = startIndex + "Sign up here".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        signUpLink.text = spannableString
        signUpLink.movementMethod = LinkMovementMethod.getInstance()
    }
}
