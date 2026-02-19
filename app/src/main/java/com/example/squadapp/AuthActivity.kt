package com.example.squadapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.squadapp.entities.User
import com.example.squadapp.models.Model
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loadingIndicator = findViewById<ProgressBar>(R.id.auth_loading_indicator)
        val fragmentContainer = findViewById<View>(R.id.nav_host_fragment_auth)

        // Observe navigation events from fragments
        lifecycleScope.launch {
            authViewModel.navigateToMain.collect { user ->
                navigateToMain(user)
            }
        }

        // Show loading, hide fragment container while checking session
        loadingIndicator.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE

        Model.shared.getCurrentUser { success, user, _ ->
            runOnUiThread {
                loadingIndicator.visibility = View.GONE
                if (success && user != null) {
                    // Already logged in — go straight to MainActivity
                    authViewModel.onAuthSuccess(user)
                } else {
                    // No active session — show sign-in screen
                    fragmentContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateToMain(user: User) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(User.EXTRA_USER, user)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
