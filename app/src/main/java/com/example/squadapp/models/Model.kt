package com.example.squadapp.models

import android.net.Uri
import com.example.squadapp.base.AuthCompletion
import com.example.squadapp.api.RawgApiClient
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.RawgGameCompletion
import com.example.squadapp.base.RawgGamesCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.base.UploadPictureCompletion
import com.example.squadapp.entities.NewPost
import com.example.squadapp.entities.NewUser

class Model private constructor() {

    private val rawgApiClient = RawgApiClient()
    private val firebaseModel = FirebaseModel()
    private val firebaseStorageModel = FirebaseStorageModel()

    companion object {
        val shared = Model()
    }

    fun getAllPosts(completion: PostsCompletion) {
        firebaseModel.getAllPosts(completion)
    }

    fun getPostsByUser(userId: String, completion: PostsCompletion) {
        firebaseModel.getPostsByUser(userId, completion)
    }

    fun addPost(newPost: NewPost, completion: ResultCompletion) {
        firebaseModel.addPost(newPost, completion)
    }

    fun searchGames(gameName: String, completion: RawgGamesCompletion) {
        rawgApiClient.searchGamesByName(gameName, completion)
    }

    fun searchGameById(gameId: Int, completion: RawgGameCompletion) {
        rawgApiClient.searchGameById(gameId, completion)
    }

    fun deletePost(postId: String, completion: ResultCompletion) {
        firebaseModel.deletePost(postId, completion)
    }

    fun signUpUser(newUser: NewUser, completion: AuthCompletion) {
        firebaseModel.signUpUser(newUser, completion)
    }

    fun signInUser(username: String, password: String, completion: AuthCompletion) {
        firebaseModel.signInUser(username, password, completion)
    }

    fun updateUser(userId: String, username: String, discordTag: String, completion: AuthCompletion) {
        firebaseModel.updateUser(userId, username, discordTag, completion)
    }

    // Storage functions
    fun uploadPicture(
        imageUri: Uri,
        folderPath: String,
        fileName: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        firebaseStorageModel.uploadPicture(imageUri, folderPath, fileName, completion, onProgress)
    }

    fun uploadProfilePicture(
        imageUri: Uri,
        userId: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        firebaseStorageModel.uploadProfilePicture(imageUri, userId, completion, onProgress)
    }

    fun uploadPostPicture(
        imageUri: Uri,
        postId: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        firebaseStorageModel.uploadPostPicture(imageUri, postId, completion, onProgress)
    }

    fun deletePicture(downloadUrl: String, completion: ResultCompletion) {
        firebaseStorageModel.deletePicture(downloadUrl, completion)
    }
}
