package com.example.squadapp.models

import android.net.Uri
import Game
import com.example.squadapp.base.AuthCompletion
import com.example.squadapp.api.RawgApiClient
import com.example.squadapp.base.GameCompletion
import com.example.squadapp.base.GamesCompletion
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.base.UploadPictureCompletion
import com.example.squadapp.entities.GameEntity
import com.example.squadapp.entities.NewPost
import com.example.squadapp.entities.NewUser
import com.example.squadapp.entities.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Model private constructor() {

    private val rawgApiClient = RawgApiClient()
    private val firebaseModel = FirebaseModel()
    private val firebaseAuthModel = FirebaseAuthModel()
    private val firebaseStorageModel = FirebaseStorageModel()
    private val roomLocalModel = RoomLocalModel()

    companion object {
        val shared = Model()
    }

    fun getCurrentUser(completion: AuthCompletion) {
        firebaseAuthModel.getCurrentUser(completion)
    }

    fun getAllPosts(completion: PostsCompletion) {
        CoroutineScope(Dispatchers.IO).launch {
            val cachedPosts = roomLocalModel.getAllPosts()
            CoroutineScope(Dispatchers.Main).launch {
                completion(cachedPosts)
            }
        }
    }

    fun refreshPosts(completion: PostsCompletion) {
        // Skip cache – go straight to Firestore and replace the local cache
        firebaseModel.getAllPosts { posts ->
            CoroutineScope(Dispatchers.IO).launch {
                roomLocalModel.clearAllPosts()
                roomLocalModel.savePosts(posts)
                CoroutineScope(Dispatchers.Main).launch {
                    completion(posts)
                }
            }
        }
    }

    fun getPostsByUser(userId: String, completion: PostsCompletion) {
        CoroutineScope(Dispatchers.IO).launch {
            val cachedPosts = roomLocalModel.getPostsByUser(userId)
            CoroutineScope(Dispatchers.Main).launch {
                completion(cachedPosts)
            }
        }
    }

    fun addPost(newPost: NewPost,completion: ResultCompletion) {
        firebaseModel.addPost(newPost) { success, message, postId ->
            if (success && postId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    roomLocalModel.savePost(newPost, postId)
                }
            }
            completion(success, message)
        }
    }

    fun searchGames(gameName: String, completion: GamesCompletion) {
        rawgApiClient.searchGamesByName(gameName, completion)
    }

    fun searchGameById(gameId: Int, completion: GameCompletion) {
        CoroutineScope(Dispatchers.IO).launch {
            val cached = roomLocalModel.getGame(gameId)
            if (cached != null) {
                val game = Game(
                    id = cached.id,
                    name = cached.name,
                    platforms = cached.platforms.split(",").filter { it.isNotBlank() },
                    imageResId = android.R.drawable.ic_menu_gallery,
                    imageUrl = null,
                    rating = cached.rating
                )
                CoroutineScope(Dispatchers.Main).launch { completion(game) }
                return@launch
            }

            rawgApiClient.searchGameById(gameId) { fetchedGame: Game? ->
                if (fetchedGame != null) {
                    val entity = GameEntity(
                        id = fetchedGame.id,
                        name = fetchedGame.name,
                        rating = 0.0,
                        platforms = fetchedGame.platforms.joinToString(",")
                    )
                    CoroutineScope(Dispatchers.IO).launch { roomLocalModel.saveGame(entity) }
                }
                completion(fetchedGame)
            }
        }
    }

    fun deletePost(postId: String, imageUrl: String, completion: ResultCompletion) {
        firebaseModel.deletePost(postId) { success, message ->
            if (success) {
                if (imageUrl.isNotEmpty()) {
                    firebaseStorageModel.deletePicture(imageUrl) { _, _ -> }
                }

                // Also delete from local Room cache
                CoroutineScope(Dispatchers.IO).launch {
                    roomLocalModel.deletePost(postId)
                }
            }
            completion(success, message)
        }
    }

    fun updatePost(postId: String, updates: Map<String, Any?>, completion: ResultCompletion) {
        firebaseModel.updatePost(postId, updates) { success, message ->
            if (success) {
                // Also update the local Room cache
                CoroutineScope(Dispatchers.IO).launch {
                    roomLocalModel.updatePost(postId, updates)
                }
            }
            completion(success, message)
        }
    }

    fun signUpUser(password: String, newUser: NewUser, completion: AuthCompletion) {
        firebaseAuthModel.signUpUser(password, newUser) { success, user, message ->
            if (success && user != null) {
                // Save new user to Room cache
                CoroutineScope(Dispatchers.IO).launch {
                    roomLocalModel.saveUser(user)
                }
            }
            completion(success, user, message)
        }
    }

    fun signInUser(email: String, password: String, completion: AuthCompletion) {
        firebaseAuthModel.signInUser(email, password) { success, user, message ->
            if (success && user != null) {
                // Save user to Room cache
                CoroutineScope(Dispatchers.IO).launch {
                    roomLocalModel.saveUser(user)
                }
            }
            completion(success, user, message)
        }
    }

    fun updateUser(
        currentUser: User,
        username: String,
        discordTag: String,
        profileImageUrl: String? = null,
        completion: AuthCompletion
    ) {
        firebaseAuthModel.updateUser(currentUser, username, discordTag, profileImageUrl) { success, user, message ->
            if (success && user != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    roomLocalModel.updateUser(user)
                }
            }
            completion(success, user, message)
        }
    }

    fun signOut() {
        firebaseAuthModel.signOut()
        // Clear local cache on sign out
        CoroutineScope(Dispatchers.IO).launch {
            roomLocalModel.clearAllPosts()
            roomLocalModel.clearAllUsers()
        }
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
