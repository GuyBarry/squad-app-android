package com.example.squadapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.squadapp.entities.User
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        user = intent.getParcelableExtra(User.EXTRA_USER, User::class.java)!!

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navController = navHostFragment.navController

        navController.setGraph(R.navigation.nav_main, HomeFragmentArgs(user = user).toBundle())

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.homeFragment, HomeFragmentArgs(user = user).toBundle())
                    true
                }
                R.id.nav_post -> {
                    navController.navigate(R.id.postFragment, PostFragmentArgs(user = user).toBundle())
                    true
                }
                R.id.nav_profile -> {
                    navController.navigate(R.id.profileFragment, ProfileFragmentArgs(user = user).toBundle())
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> bottomNavigation.menu.findItem(R.id.nav_home)?.isChecked = true
                R.id.postFragment -> bottomNavigation.menu.findItem(R.id.nav_post)?.isChecked = true
                R.id.profileFragment, R.id.editProfileFragment ->
                    bottomNavigation.menu.findItem(R.id.nav_profile)?.isChecked = true
            }
        }
    }

    /** Called by fragments when the user object is updated (e.g. after edit profile). */
    fun onUserUpdated(updatedUser: User) {
        user = updatedUser
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
