package com.example.squadapp.models

import android.util.Log
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.entities.Post
import com.example.squadapp.entities.Post.Companion.POST_CREATION_TIME
import com.example.squadapp.entities.Post.Companion.POST_DESCRIPTION
import com.example.squadapp.entities.Post.Companion.POST_IMAGE
import com.example.squadapp.entities.Post.Companion.POST_USER
import com.example.squadapp.entities.NewPost
import com.example.squadapp.entities.User
import com.example.squadapp.entities.User.Companion.deserializeUser
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import java.sql.Date

class FirebaseModel {
    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val POSTS = "posts"
        const val USERS = "users"
    }

    fun getAllPosts(completion: PostsCompletion) {
        db.collection(POSTS).get().addOnSuccessListener { querySnapshot ->
            val postDocuments = querySnapshot.documents

            if (postDocuments.isEmpty()) {
                completion(emptyList())
                return@addOnSuccessListener
            }

            val userIds = mutableSetOf<String>()
            val postsData = mutableListOf<Map<String, Any?>>()

            postDocuments.forEach { postDocument ->
                // Timestamp must be cast explicitly before converting to java.sql.Date
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

            if (userIds.isNotEmpty()) {
                fetchAllUsers(userIds.toList()) { usersMap, foundUserIds ->
                    // Filter out posts whose authors have been deleted
                    val filteredPostsData = postsData.filter { postData ->
                        val userId = postData["user"] as? String
                        userId != null && foundUserIds.contains(userId)
                    }

                    val posts = combinePostsWithUsers(filteredPostsData, usersMap).toMutableList()
                    posts.sortByDescending { it.creationTime }
                    completion(posts)
                }
            } else {
                val posts = combinePostsWithUsers(postsData, emptyMap()).toMutableList()
                completion(posts)
            }

        }.addOnFailureListener { exception ->
            Log.e("FirebaseModel", "Error fetching posts: ${exception.message}")
            completion(emptyList())
        }
    }

    fun getPostsByUser(userId: String, completion: PostsCompletion) {
        db.collection(POSTS)
            .whereEqualTo(POST_USER, userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val postDocuments = querySnapshot.documents

                if (postDocuments.isEmpty()) {
                    completion(emptyList())
                    return@addOnSuccessListener
                }

                val postsData = mutableListOf<Map<String, Any?>>()

                postDocuments.forEach { postDocument ->
                    // Timestamp must be cast explicitly before converting to java.sql.Date
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

                db.collection(USERS).document(userId).get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument.exists()) {
                            val user = deserializeUser(userDocument.data ?: emptyMap()).copy(id = userId)
                            val usersMap = mapOf(userId to user)
                            val posts = combinePostsWithUsers(postsData, usersMap).toMutableList()
                            posts.sortByDescending { it.creationTime }
                            completion(posts)
                        } else {
                            Log.w("FirebaseModel", "User document not found: $userId - returning empty list")
                            completion(emptyList())
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseModel", "Error fetching user $userId: ${exception.message}")
                        completion(emptyList())
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
                        val user = deserializeUser(userDocument.data ?: emptyMap()).copy(id = userId)
                        usersMap[userId] = user
                        foundUserIds.add(userId)
                    } else {
                        Log.w("FirebaseModel", "User document not found: $userId")
                    }
                    fetchedCount++
                    if (fetchedCount == userIds.size) {
                        onComplete(usersMap, foundUserIds)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseModel", "Error fetching user $userId: ${exception.message}")
                    fetchedCount++
                    if (fetchedCount == userIds.size) {
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

            val user = if (userId != null && usersMap.containsKey(userId)) {
                usersMap[userId]!!
            } else {
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

    fun addPost(newPost: NewPost, completion: (Boolean, String, String?) -> Unit) {
        val docRef = db.collection(POSTS).document()
        val postData = NewPost.serialize(newPost)

        docRef.set(postData)
            .addOnSuccessListener {
                completion(true, "Post published successfully!", docRef.id)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error adding post: ${exception.message}")
                completion(false, "Failed to publish post: ${exception.message}", null)
            }
    }

    fun deletePost(postId: String, completion: ResultCompletion) {
        db.collection(POSTS).document(postId).delete()
            .addOnSuccessListener {
                completion(true, "Post deleted successfully!")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error deleting post: ${exception.message}")
                completion(false, "Failed to delete post: ${exception.message}")
            }
    }

    fun updatePost(postId: String, updates: Map<String, Any?>, completion: ResultCompletion) {
        db.collection(POSTS).document(postId).update(updates)
            .addOnSuccessListener {
                completion(true, "Post updated successfully!")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Error updating post: ${exception.message}")
                completion(false, "Failed to update post: ${exception.message}")
            }
    }
}