package com.example.squadapp.models

import android.util.Log
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.entities.Post
import com.example.squadapp.entities.Post.Companion.POST_CREATION_TIME
import com.example.squadapp.entities.Post.Companion.POST_DESCRIPTION
import com.example.squadapp.entities.Post.Companion.POST_IMAGE
import com.example.squadapp.entities.Post.Companion.POST_USER
import com.example.squadapp.entities.Post.Companion.serialize
import com.example.squadapp.entities.User
import com.example.squadapp.entities.User.Companion.deserializeUser
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.sql.Date

class FirebaseModel {
    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val POSTS = "posts"
        const val USERS = "users"
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
                val postData = mapOf(
                    "id" to postDocument.id,
                    "image" to ((postDocument.get(POST_IMAGE) as? Long)?.toInt() ?: 0),
                    "description" to (postDocument.get(POST_DESCRIPTION) as? String ?: ""),
                    "creationTime" to (postDocument.get(POST_CREATION_TIME) as? Date
                        ?: Date(System.currentTimeMillis())),
                    "user" to (postDocument.get(POST_USER) as? String)
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
                    val posts = createPostsWithUsers(filteredPostsData, usersMap).toMutableList()
                    posts.sortByDescending { it.creationTime }
                    Log.d(
                        "FirebaseModel",
                        "Successfully created ${posts.size} posts with user data"
                    )
                    completion(posts)
                }
            } else {
                // No users to fetch, create posts with fallback users
                val posts = createPostsWithUsers(postsData, emptyMap()).toMutableList()
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
                    val postData = mapOf(
                        "id" to postDocument.id,
                        "image" to ((postDocument.get(POST_IMAGE) as? Long)?.toInt() ?: 0),
                        "description" to (postDocument.get(POST_DESCRIPTION) as? String ?: ""),
                        "creationTime" to (postDocument.get(POST_CREATION_TIME) as? Date
                            ?: Date(System.currentTimeMillis())),
                        "user" to (postDocument.get(POST_USER) as? String)
                    )
                    postsData.add(postData)
                }

                Log.d("FirebaseModel", "Fetched ${postDocuments.size} posts for user: $userId")

                // Step 3: Fetch the user data
                db.collection(USERS).document(userId).get()
                    .addOnSuccessListener { userDocument ->
                        val user = if (userDocument.exists()) {
                            deserializeUser(userDocument.data ?: emptyMap())
                        } else {
                            Log.w("FirebaseModel", "User document not found: $userId")
                            User(
                                id = "0",
                                profileImage = 0,
                                username = "Unknown",
                                password = "",
                                discordTag = "Unknown"
                            )
                        }

                        // Step 4: Create posts with user data
                        val usersMap = mapOf(userId to user)
                        val posts = createPostsWithUsers(postsData, usersMap).toMutableList()
                        posts.sortByDescending { it.creationTime }

                        Log.d("FirebaseModel", "Successfully created ${posts.size} posts for user: $userId")
                        completion(posts)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseModel", "Error fetching user $userId: ${exception.message}")
                        // Still create posts with fallback user
                        val posts = createPostsWithUsers(postsData, emptyMap()).toMutableList()
                        posts.sortByDescending { it.creationTime }
                        completion(posts)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error fetching posts for user $userId: ${exception.message}")
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
                        val user = deserializeUser(userDocument.data ?: emptyMap())
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

    private fun createPostsWithUsers(
        postsData: List<Map<String, Any?>>,
        usersMap: Map<String, User>
    ): List<Post> {
        return postsData.mapNotNull { postData ->
            val postId = postData["id"] as? String ?: return@mapNotNull null
            val image = postData["image"] as? Int ?: 0
            val description = postData["description"] as? String ?: ""
            val creationTime = postData["creationTime"] as? Date ?: Date(System.currentTimeMillis())
            val userId = postData["user"] as? String

            val user = if (userId != null && usersMap.containsKey(userId)) {
                usersMap[userId]!!
            } else {
                User(
                    id = "0",
                    profileImage = 0,
                    username = "Unknown",
                    password = "",
                    discordTag = "Unknown"
                )
            }

            Post(
                id = postId,
                image = image,
                user = user,
                description = description,
                creationTime = creationTime
            )
        }
    }

    fun addPost(post: Post, completion: ResultCompletion) {
        val postData = serialize(post)

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
}