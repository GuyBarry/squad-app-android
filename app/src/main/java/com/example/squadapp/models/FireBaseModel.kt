package com.example.squadapp.models

import com.example.squadapp.base.Completion
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.entities.Post
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FirebaseModel {
    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val POSTS = "posts"
    }

    fun getAllPosts(completion: PostsCompletion) {
        db.collection(POSTS).get()
            .addOnCompleteListener { result ->
                when (result.isSuccessful) {
                    true -> completion(result.result.map { Post.fromJson(it.data) })
                    false -> completion(emptyList())
                }
            }
    }

    fun addPost(Post: Post, completion: Completion) {

    }
}