package com.example.squadapp.models

import com.example.squadapp.base.AuthCompletion
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.entities.Post

class Model private constructor() {

    private val firebaseModel = FirebaseModel()

    companion object {
        val shared = Model()
    }

    fun getAllPosts(completion: PostsCompletion) {
        firebaseModel.getAllPosts(completion)
    }

    fun getPostsByUser(userId: String, completion: PostsCompletion) {
        firebaseModel.getPostsByUser(userId, completion)
    }

    fun addPost(post: Post, completion: ResultCompletion) {
        firebaseModel.addPost(post, completion)
    }

    fun deletePost(postId: String, completion: ResultCompletion) {
        firebaseModel.deletePost(postId, completion)
    }

    fun signUpUser(username: String, password: String, discordTag: String, completion: AuthCompletion) {
        firebaseModel.signUpUser(username, password, discordTag, completion)
    }

    fun signInUser(username: String, password: String, completion: AuthCompletion) {
        firebaseModel.signInUser(username, password, completion)
    }

    fun updateUser(userId: String, username: String, discordTag: String, completion: AuthCompletion) {
        firebaseModel.updateUser(userId, username, discordTag, completion)
    }
}
