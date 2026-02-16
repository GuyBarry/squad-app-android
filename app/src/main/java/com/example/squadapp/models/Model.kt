package com.example.squadapp.models

import com.example.squadapp.api.RawgApiClient
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.RawgGamesCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.entities.Post

class Model private constructor() {

    private val rawgApiClient = RawgApiClient()
    private val firebaseModel = FirebaseModel()

    companion object {
        val shared = Model()
    }

    fun getAllPosts(completion: PostsCompletion) {
        firebaseModel.getAllPosts(completion)
    }

    fun addPost(post: Post, completion: ResultCompletion) {
        firebaseModel.addPost(post, completion)
    }

    fun searchGames(gameName: String, completion: RawgGamesCompletion) {
        rawgApiClient.searchGamesByName(gameName, completion)
    }

}
