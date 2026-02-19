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
import com.example.squadapp.entities.User

class Model private constructor() {

    private val rawgApiClient = RawgApiClient()
    private val firebaseModel = FirebaseModel()
    private val firebaseAuthModel = FirebaseAuthModel()
    private val firebaseStorageModel = FirebaseStorageModel()

    companion object {
        val shared = Model()
    }

    fun getCurrentUser(completion: AuthCompletion) {
        firebaseAuthModel.getCurrentUser(completion)
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

    fun deletePost(postId: String, imageUrl: String, completion: ResultCompletion) {
        if (imageUrl.isNotEmpty()) {
            firebaseStorageModel.deletePicture(imageUrl) { _, _ -> }
        }

        firebaseModel.deletePost(postId, completion)
    }

    fun signUpUser(password: String, newUser: NewUser, completion: AuthCompletion) {
        firebaseAuthModel.signUpUser(password, newUser, completion)
    }

    fun signInUser(email: String, password: String, completion: AuthCompletion) {
        firebaseAuthModel.signInUser(email, password, completion)
    }

    fun updateUser(
        currentUser: User,
        username: String,
        discordTag: String,
        profileImageUrl: String? = null,
        completion: AuthCompletion
    ) {
        firebaseAuthModel.updateUser(currentUser, username, discordTag, profileImageUrl, completion)
    }

    fun signOut() {
        firebaseAuthModel.signOut()
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
