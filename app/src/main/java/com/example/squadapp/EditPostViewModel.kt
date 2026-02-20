package com.example.squadapp

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.squadapp.entities.Post
import com.example.squadapp.entities.RawgGame
import com.example.squadapp.models.Model

/**
 * EditPostViewModel - Manages game search and post editing logic.
 */
class EditPostViewModel(application: Application) : AndroidViewModel(application) {

    private val _games = MutableLiveData<List<RawgGame>>()
    val games: LiveData<List<RawgGame>> = _games

    private val _isPublishing = MutableLiveData<Boolean>(false)
    val isPublishing: LiveData<Boolean> = _isPublishing

    private val _publishProgress = MutableLiveData<String?>()
    val publishProgress: LiveData<String?> = _publishProgress

    /** Emits true on update success, false on failure with a message. */
    private val _publishResult = MutableLiveData<Pair<Boolean, String>>()
    val publishResult: LiveData<Pair<Boolean, String>> = _publishResult

    /** True while the initial game data is being fetched from the API. */
    private val _isLoadingData = MutableLiveData<Boolean>(true)
    val isLoadingData: LiveData<Boolean> = _isLoadingData

    var selectedGame: RawgGame? = null
    var inputChangeCounter = 0

    /** Fetches game data for the given gameId and marks loading complete. */
    fun loadPostData(gameId: Int) {
        _isLoadingData.value = true
        Model.shared.searchGameById(gameId) { rawgGame ->
            if (rawgGame != null) {
                selectedGame = rawgGame
                inputChangeCounter = 0
            }
            _isLoadingData.postValue(false)
        }
    }

    fun searchGames(query: String) {
        Model.shared.searchGames(query) { rawgGames ->
            _games.postValue(rawgGames.take(4))
        }
    }

    fun clearGames() {
        _games.postValue(emptyList())
    }

    /**
     * Update an existing post. If a new image URI is provided, upload it first.
     * Otherwise, just update the description and/or game.
     */
    fun updatePost(
        post: Post,
        newImageUri: Uri?,
        description: String
    ) {
        val game = selectedGame ?: return
        val ctx = getApplication<Application>()
        _isPublishing.value = true
        _publishProgress.value = null

        if (newImageUri != null) {
            // Upload new image, then update post
            Log.d("EditPostViewModel", "Uploading new image to Firebase Storage...")
            Model.shared.uploadPostPicture(
                newImageUri,
                post.id,
                { success, downloadUrl, message ->
                    if (success && downloadUrl != null) {
                        Log.d("EditPostViewModel", "New image uploaded: $downloadUrl")
                        // Delete old image if it exists
                        if (post.image.isNotEmpty()) {
                            Model.shared.deletePicture(post.image) { _, _ -> }
                        }
                        val updates = mapOf(
                            Post.POST_IMAGE to downloadUrl,
                            Post.POST_DESCRIPTION to description,
                            Post.POST_GAME_ID to game.id
                        )
                        Model.shared.updatePost(post.id, updates) { updateSuccess, updateMessage ->
                            _isPublishing.postValue(false)
                            _publishProgress.postValue(null)
                            _publishResult.postValue(Pair(updateSuccess, updateMessage))
                        }
                    } else {
                        Log.e("EditPostViewModel", "Image upload failed: $message")
                        _isPublishing.postValue(false)
                        _publishProgress.postValue(null)
                        _publishResult.postValue(Pair(false, ctx.getString(R.string.failed_to_upload_image_post, message)))
                    }
                },
                onProgress = { progress ->
                    Log.d("EditPostViewModel", "Upload progress: $progress%")
                    _publishProgress.postValue(ctx.getString(R.string.uploading_progress_post, progress))
                }
            )
        } else {
            // No new image, just update description and game
            val updates = mapOf(
                Post.POST_DESCRIPTION to description,
                Post.POST_GAME_ID to game.id
            )
            Model.shared.updatePost(post.id, updates) { updateSuccess, updateMessage ->
                _isPublishing.postValue(false)
                _publishProgress.postValue(null)
                _publishResult.postValue(Pair(updateSuccess, updateMessage))
            }
        }
    }
}
