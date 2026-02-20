package com.example.squadapp.models

import android.util.Log
import com.example.squadapp.base.AuthCompletion
import com.example.squadapp.entities.NewUser
import com.example.squadapp.entities.User
import com.example.squadapp.entities.User.Companion.deserializeUser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class FirebaseAuthModel {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private companion object {
        const val USERS = "users"
    }

    fun getCurrentUser(completion: AuthCompletion) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            completion(false, null, null)
            return
        }
        val uid = firebaseUser.uid

        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val user = deserializeUser(userDocument.data ?: emptyMap()).copy(id = uid)
                    completion(true, user, null)
                } else {
                    Log.w("FirebaseAuthModel", "Auth session found but Firestore profile missing: $uid")
                    completion(false, null, null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseAuthModel", "Error fetching current user profile: ${exception.message}")
                completion(false, null, exception.message)
            }
    }

    fun signUpUser(password: String, newUser: NewUser, completion: AuthCompletion) {
        db.collection(USERS)
            .whereEqualTo(User.USER_USERNAME, newUser.username)
            .get()
            .addOnSuccessListener { usernameSnapshot ->
                if (!usernameSnapshot.isEmpty) {
                    completion(false, null, "Username already taken")
                    return@addOnSuccessListener
                }

                db.collection(USERS)
                    .whereEqualTo(User.USER_EMAIL, newUser.email)
                    .get()
                    .addOnSuccessListener { emailSnapshot ->
                        if (!emailSnapshot.isEmpty) {
                            completion(false, null, "An account with this email already exists")
                            return@addOnSuccessListener
                        }

                        auth.createUserWithEmailAndPassword(newUser.email, password)
                            .addOnSuccessListener { authResult ->
                                val uid = authResult.user!!.uid
                                val userData = NewUser.serialize(newUser)

                                db.collection(USERS).document(uid).set(userData)
                                    .addOnSuccessListener {
                                        val user = User(
                                            id = uid,
                                            profileImage = newUser.profileImage,
                                            username = newUser.username,
                                            email = newUser.email,
                                            discordTag = newUser.discordTag
                                        )
                                        completion(true, user, "Sign up successful!")
                                    }
                                    .addOnFailureListener { exception ->
                                        // Auth account was created but Firestore save failed — roll back auth user
                                        authResult.user?.delete()
                                        Log.e("FirebaseAuthModel", "Error saving user profile: ${exception.message}")
                                        completion(false, null, "Failed to create account: ${exception.message}")
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FirebaseAuthModel", "Error creating auth user: ${exception.message}")
                                completion(false, null, exception.localizedMessage ?: "Failed to create account")
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseAuthModel", "Error checking email: ${exception.message}")
                        completion(false, null, "Failed to check email: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseAuthModel", "Error checking username: ${exception.message}")
                completion(false, null, "Failed to check username: ${exception.message}")
            }
    }

    fun signInUser(email: String, password: String, completion: AuthCompletion) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user!!.uid
                db.collection(USERS).document(uid).get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument.exists()) {
                            val user = deserializeUser(userDocument.data ?: emptyMap()).copy(id = uid)
                            completion(true, user, "Sign in successful!")
                        } else {
                            Log.w("FirebaseAuthModel", "Auth succeeded but user profile not found: $uid")
                            completion(false, null, "User profile not found")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseAuthModel", "Error fetching user profile: ${exception.message}")
                        completion(false, null, "Failed to load user profile: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseAuthModel", "Error signing in: ${exception.message}")
                completion(false, null, exception.localizedMessage ?: "Invalid email or password")
            }
    }

    fun updateUser(
        currentUser: User,
        username: String,
        discordTag: String,
        profileImageUrl: String? = null,
        completion: AuthCompletion
    ) {
        // Check if new username is already taken by another user
        db.collection(USERS)
            .whereEqualTo(User.USER_USERNAME, username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val usernameExists = querySnapshot.documents.any { it.id != currentUser.id }

                if (usernameExists) {
                    completion(false, null, "Username already taken")
                    return@addOnSuccessListener
                }

                val updates = hashMapOf<String, Any>(
                    User.USER_USERNAME to username,
                    User.USER_DISCORD_TAG to discordTag
                )

                if (profileImageUrl != null) {
                    updates[User.USER_PROFILE_IMAGE] = profileImageUrl
                }

                db.collection(USERS).document(currentUser.id)
                    .update(updates)
                    .addOnSuccessListener {
                        // Build the updated User from known values — avoids an extra Firestore fetch
                        val updatedUser = currentUser.copy(
                            username = username,
                            discordTag = discordTag,
                            profileImage = profileImageUrl ?: currentUser.profileImage
                        )
                        completion(true, updatedUser, "Profile updated successfully!")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseAuthModel", "Error updating user: ${exception.message}")
                        completion(false, null, "Failed to update profile: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseAuthModel", "Error checking username: ${exception.message}")
                completion(false, null, "Failed to check username: ${exception.message}")
            }
    }

    fun signOut() {
        auth.signOut()
    }
}


