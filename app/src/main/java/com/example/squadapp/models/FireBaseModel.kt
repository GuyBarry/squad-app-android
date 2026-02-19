package com.example.squadapp.models

import android.util.Log
import com.example.squadapp.base.AuthCompletion
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.entities.Post
import com.example.squadapp.entities.Post.Companion.POST_CREATION_TIME
import com.example.squadapp.entities.Post.Companion.POST_DESCRIPTION
import com.example.squadapp.entities.Post.Companion.POST_IMAGE
import com.example.squadapp.entities.Post.Companion.POST_USER
import com.example.squadapp.entities.NewPost
import com.example.squadapp.entities.NewUser
import com.example.squadapp.entities.User
import com.example.squadapp.entities.User.Companion.deserializeUser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import java.sql.Date

class FirebaseModel {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private companion object COLLECTIONS {
        const val POSTS = "posts"
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
                    Log.d("FirebaseModel", "Restored session for user: ${user.username}")
                    completion(true, user, null)
                } else {
                    Log.w("FirebaseModel", "Auth session found but Firestore profile missing: $uid")
                    completion(false, null, null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error fetching current user profile: ${exception.message}")
                completion(false, null, exception.message)
            }
    }

    fun getAllPosts(completion: PostsCompletion) {
        // Step 1: Fetch all posts
        db.collection(POSTS).get().addOnSuccessListener { querySnapshot ->
            val postDocuments = querySnapshot.documents

            if (postDocuments.isEmpty()) {
                Log.d("FirebaseModel", "No posts found")
                completion(emptyList())
                return@addOnSuccessListener
            }

            // Step 2: Extract all unique user IDs from posts
            val userIds = mutableSetOf<String>()
            val postsData = mutableListOf<Map<String, Any?>>()

            postDocuments.forEach { postDocument ->
                // Properly convert Timestamp from Firebase to Date
                val timestamp = postDocument.get(POST_CREATION_TIME) as? Timestamp
                val creationTime = if (null != timestamp)
                    Date(timestamp.toDate().time) else Date(System.currentTimeMillis())

                val postData = mapOf(
                    "id" to postDocument.id,
                    "image" to (postDocument.get(POST_IMAGE) as? String ?: ""),
                    "description" to (postDocument.get(POST_DESCRIPTION) as? String ?: ""),
                    "creationTime" to creationTime,
                    "user" to (postDocument.get(POST_USER) as? String),
                    "gameId" to ((postDocument.get(Post.POST_GAME_ID) as? Long)?.toInt() ?: 0)
                )
                postsData.add(postData)

                val userId = postDocument.get(POST_USER) as? String
                if (userId != null) {
                    userIds.add(userId)
                }
            }

            Log.d(
                "FirebaseModel",
                "Fetched ${postDocuments.size} posts with ${userIds.size} unique users"
            )

            // Step 3: Fetch all users in batch
            if (userIds.isNotEmpty()) {
                fetchAllUsers(userIds.toList()) { usersMap, foundUserIds ->
                    // Step 4: Filter out posts whose users don't exist
                    val filteredPostsData = postsData.filter { postData ->
                        val userId = postData["user"] as? String
                        userId != null && foundUserIds.contains(userId)
                    }

                    Log.d(
                        "FirebaseModel",
                        "Filtered posts: ${filteredPostsData.size}/${postsData.size} (removed ${postsData.size - filteredPostsData.size} posts with missing users)"
                    )

                    // Step 5: Populate posts with user data
                    val posts = combinePostsWithUsers(filteredPostsData, usersMap).toMutableList()
                    posts.sortByDescending { it.creationTime }
                    Log.d(
                        "FirebaseModel",
                        "Successfully created ${posts.size} posts with user data"
                    )
                    completion(posts)
                }
            } else {
                // No users to fetch, create posts with fallback users
                val posts = combinePostsWithUsers(postsData, emptyMap()).toMutableList()
                completion(posts)
            }

        }.addOnFailureListener { exception ->
            Log.e("FirebaseModel", "Error fetching posts: ${exception.message}")
            completion(emptyList())
        }
    }

    fun getPostsByUser(userId: String, completion: PostsCompletion) {
        // Step 1: Query posts where POST_USER equals userId
        db.collection(POSTS)
            .whereEqualTo(POST_USER, userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val postDocuments = querySnapshot.documents

                if (postDocuments.isEmpty()) {
                    Log.d("FirebaseModel", "No posts found for user: $userId")
                    completion(emptyList())
                    return@addOnSuccessListener
                }

                // Step 2: Extract post data
                val postsData = mutableListOf<Map<String, Any?>>()

                postDocuments.forEach { postDocument ->
                    // Properly convert Timestamp from Firebase to Date
                    val timestamp = postDocument.get(POST_CREATION_TIME) as? Timestamp
                    val creationTime = if (null != timestamp)
                        Date(timestamp.toDate().time) else Date(System.currentTimeMillis())

                    val postData = mapOf(
                        "id" to postDocument.id,
                        "image" to (postDocument.get(POST_IMAGE) as? String ?: ""),
                        "description" to (postDocument.get(POST_DESCRIPTION) as? String ?: ""),
                        "creationTime" to creationTime,
                        "user" to (postDocument.get(POST_USER) as? String),
                        "gameId" to ((postDocument.get(Post.POST_GAME_ID) as? Long)?.toInt() ?: 0)
                    )
                    postsData.add(postData)
                }

                Log.d("FirebaseModel", "Fetched ${postDocuments.size} posts for user: $userId")

                // Step 3: Fetch the user data
                db.collection(USERS).document(userId).get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument.exists()) {
                            val user = deserializeUser(userDocument.data ?: emptyMap()).copy(id = userId)

                            // Step 4: Create posts with user data
                            val usersMap = mapOf(userId to user)
                            val posts = combinePostsWithUsers(postsData, usersMap).toMutableList()
                            posts.sortByDescending { it.creationTime }

                            Log.d("FirebaseModel", "Successfully created ${posts.size} posts for user: $userId")
                            completion(posts)
                        } else {
                            Log.w("FirebaseModel", "User document not found: $userId - returning empty list")
                            completion(emptyList())
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseModel", "Error fetching user $userId: ${exception.message}")
                        // Return empty list instead of creating posts with fallback user
                        completion(emptyList())
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "FirebaseModel",
                    "Error fetching posts for user $userId: ${exception.message}"
                )
                completion(emptyList())
            }
    }

    private fun fetchAllUsers(
        userIds: List<String>,
        onComplete: (Map<String, User>, Set<String>) -> Unit
    ) {
        val usersMap = mutableMapOf<String, User>()
        val foundUserIds = mutableSetOf<String>()
        var fetchedCount = 0

        if (userIds.isEmpty()) {
            onComplete(usersMap, foundUserIds)
            return
        }

        userIds.forEach { userId ->
            db.collection(USERS).document(userId).get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument.exists()) {
                        val user = deserializeUser(userDocument.data ?: emptyMap()).copy(id = userId)
                        usersMap[userId] = user
                        foundUserIds.add(userId)
                        Log.d("FirebaseModel", "Fetched user: $userId")
                    } else {
                        Log.w("FirebaseModel", "User document not found: $userId")
                    }
                    fetchedCount++
                    if (fetchedCount == userIds.size) {
                        Log.d(
                            "FirebaseModel",
                            "All users fetched. Found: ${usersMap.size}/${userIds.size}"
                        )
                        onComplete(usersMap, foundUserIds)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseModel", "Error fetching user $userId: ${exception.message}")
                    fetchedCount++
                    if (fetchedCount == userIds.size) {
                        Log.d(
                            "FirebaseModel",
                            "All users fetched (with failures). Found: ${usersMap.size}/${userIds.size}"
                        )
                        onComplete(usersMap, foundUserIds)
                    }
                }
        }
    }

    private fun combinePostsWithUsers(
        postsData: List<Map<String, Any?>>,
        usersMap: Map<String, User>
    ): List<Post> {
        return postsData.mapNotNull { postData ->
            val postId = postData["id"] as? String ?: return@mapNotNull null
            val image = postData["image"] as? String ?: ""
            val description = postData["description"] as? String ?: ""
            val creationTime = postData["creationTime"] as? Date ?: Date(System.currentTimeMillis())
            val userId = postData["user"] as? String
            val gameId = postData["gameId"] as? Int ?: 0

            // Only create post if user exists in usersMap
            val user = if (userId != null && usersMap.containsKey(userId)) {
                usersMap[userId]!!
            } else {
                // Return null to filter out this post
                Log.d("FirebaseModel", "Filtering out post $postId - user not found: $userId")
                return@mapNotNull null
            }

            Post(
                id = postId,
                image = image,
                user = user,
                description = description,
                creationTime = creationTime,
                gameId = gameId
            )
        }
    }

    fun addPost(newPost: NewPost, completion: ResultCompletion) {
        val postData = NewPost.serialize(newPost)

        db.collection(POSTS).add(postData)
            .addOnSuccessListener {
                Log.d("FirebaseModel", "Post added successfully with ID: ${it.id}")
                completion(true, "Post published successfully!")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error adding post: ${exception.message}")
                completion(false, "Failed to publish post: ${exception.message}")
            }
    }

    fun deletePost(postId: String, completion: ResultCompletion) {
        db.collection(POSTS).document(postId).delete()
            .addOnSuccessListener {
                Log.d("FirebaseModel", "Post deleted successfully with ID: $postId")
                completion(true, "Post deleted successfully!")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error deleting post: ${exception.message}")
                completion(false, "Failed to delete post: ${exception.message}")
            }
    }

    fun signUpUser(password: String, newUser: NewUser, completion: AuthCompletion) {
        // Step 1: Check if username is already taken
        db.collection(USERS)
            .whereEqualTo(User.USER_USERNAME, newUser.username)
            .get()
            .addOnSuccessListener { usernameSnapshot ->
                if (!usernameSnapshot.isEmpty) {
                    Log.d("FirebaseModel", "Username already exists: ${newUser.username}")
                    completion(false, null, "Username already taken")
                    return@addOnSuccessListener
                }

                // Step 2: Check if email is already registered in Firestore
                db.collection(USERS)
                    .whereEqualTo(User.USER_EMAIL, newUser.email)
                    .get()
                    .addOnSuccessListener { emailSnapshot ->
                        if (!emailSnapshot.isEmpty) {
                            Log.d("FirebaseModel", "Email already registered: ${newUser.email}")
                            completion(false, null, "An account with this email already exists")
                            return@addOnSuccessListener
                        }

                        // Step 3: Create Firebase Auth account
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
                                        Log.d("FirebaseModel", "User created successfully with ID: $uid")
                                        completion(true, user, "Sign up successful!")
                                    }
                                    .addOnFailureListener { exception ->
                                        // Auth account was created but Firestore save failed – clean up auth user
                                        authResult.user?.delete()
                                        Log.e("FirebaseModel", "Error saving user profile: ${exception.message}")
                                        completion(false, null, "Failed to create account: ${exception.message}")
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FirebaseModel", "Error creating auth user: ${exception.message}")
                                completion(false, null, exception.localizedMessage ?: "Failed to create account")
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseModel", "Error checking email: ${exception.message}")
                        completion(false, null, "Failed to check email: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error checking username: ${exception.message}")
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
                            Log.d("FirebaseModel", "User signed in successfully: ${user.username}")
                            completion(true, user, "Sign in successful!")
                        } else {
                            Log.w("FirebaseModel", "Auth succeeded but user profile not found: $uid")
                            completion(false, null, "User profile not found")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseModel", "Error fetching user profile: ${exception.message}")
                        completion(false, null, "Failed to load user profile: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error signing in: ${exception.message}")
                completion(false, null, exception.localizedMessage ?: "Invalid email or password")
            }
    }

    fun updateUser(
        userId: String,
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
                val usernameExists = querySnapshot.documents.any { it.id != userId }

                if (usernameExists) {
                    Log.d("FirebaseModel", "Username already taken: $username")
                    completion(false, null, "Username already taken")
                } else {
                    // Update user document
                    val updates = hashMapOf<String, Any>(
                        User.USER_USERNAME to username,
                        User.USER_DISCORD_TAG to discordTag
                    )

                    // Add profile image URL to updates if provided
                    if (profileImageUrl != null) {
                        updates[User.USER_PROFILE_IMAGE] = profileImageUrl
                    }

                    db.collection(USERS).document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            // Fetch updated user data
                            db.collection(USERS).document(userId).get()
                                .addOnSuccessListener { userDocument ->
                                    if (userDocument.exists()) {
                                        val user =
                                            deserializeUser(userDocument.data ?: emptyMap())
                                                .copy(id = userId)
                                        Log.d("FirebaseModel", "User updated successfully: $userId")
                                        completion(true, user, "Profile updated successfully!")
                                    } else {
                                        completion(false, null, "Failed to fetch updated user data")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(
                                        "FirebaseModel",
                                        "Error fetching updated user: ${exception.message}"
                                    )
                                    completion(
                                        false,
                                        null,
                                        "Failed to fetch updated data: ${exception.message}"
                                    )
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirebaseModel", "Error updating user: ${exception.message}")
                            completion(
                                false,
                                null,
                                "Failed to update profile: ${exception.message}"
                            )
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error checking username: ${exception.message}")
                completion(false, null, "Failed to check username: ${exception.message}")
            }
    }
}