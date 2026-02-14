package com.example.squadapp.models

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
}
