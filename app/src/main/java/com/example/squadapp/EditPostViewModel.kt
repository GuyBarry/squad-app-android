package com.example.squadapp

import Game
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.squadapp.entities.Post
import com.example.squadapp.models.Model

class EditPostViewModel(application: Application) : AndroidViewModel(application) {

    private val _games = MutableLiveData<List<Game>>()
    val games: LiveData<List<Game>> = _games

    private val _isPublishing = MutableLiveData<Boolean>(false)
    val isPublishing: LiveData<Boolean> = _isPublishing

    private val _publishProgress = MutableLiveData<String?>()
    val publishProgress: LiveData<String?> = _publishProgress

    /** Emits true on update success, false on failure with a message. */
    private val _publishResult = MutableLiveData<Pair<Boolean, String>>()
    val publishResult: LiveData<Pair<Boolean, String>> = _publishResult

    /** True while the initial game data is being fetched. */
    private val _isLoadingData = MutableLiveData(true)
    val isLoadingData: LiveData<Boolean> = _isLoadingData

    var selectedGame: Game? = null
    var inputChangeCounter = 0

    fun loadPostData(gameId: Int) {
        _isLoadingData.value = true
        Model.shared.searchGameById(gameId) { game ->
            if (game != null) {
                selectedGame = game
                inputChangeCounter = 0
            }
            _isLoadingData.postValue(false)
        }
    }

    fun searchGames(query: String) {
        Model.shared.searchGames(query) { games ->
            _games.postValue(games.take(4))
        }
    }

    fun clearGames() {
        _games.postValue(emptyList())
    }

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
            Model.shared.uploadPostPicture(
                newImageUri,
                post.id,
                { success, downloadUrl, message ->
                    if (success && downloadUrl != null) {
                        // Replace old image in Storage before updating the post document
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
                        _isPublishing.postValue(false)
                        _publishProgress.postValue(null)
                        _publishResult.postValue(Pair(false, ctx.getString(R.string.failed_to_upload_image_post, message)))
                    }
                },
                onProgress = { progress ->
                    _publishProgress.postValue(ctx.getString(R.string.uploading_progress_post, progress))
                }
            )
        } else {
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
