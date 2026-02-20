package com.example.squadapp

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.squadapp.entities.NewPost
import com.example.squadapp.entities.RawgGame
import com.example.squadapp.models.Model
import java.util.Date

class PostViewModel : ViewModel() {

    private val _games = MutableLiveData<List<RawgGame>>()
    val games: LiveData<List<RawgGame>> = _games

    private val _isPublishing = MutableLiveData<Boolean>(false)
    val isPublishing: LiveData<Boolean> = _isPublishing

    private val _publishProgress = MutableLiveData<String?>()
    val publishProgress: LiveData<String?> = _publishProgress

    /** Emits true on publish success, false on failure. */
    private val _publishResult = MutableLiveData<Pair<Boolean, String>>()
    val publishResult: LiveData<Pair<Boolean, String>> = _publishResult

    var selectedGame: RawgGame? = null
    var inputChangeCounter = 0

    fun searchGames(query: String) {
        Model.shared.searchGames(query) { rawgGames ->
            _games.postValue(rawgGames.take(4))
        }
    }

    fun clearGames() {
        _games.postValue(emptyList())
    }

    fun publishPost(
        imageUri: Uri,
        userId: String,
        description: String
    ) {
        val game = selectedGame ?: return
        _isPublishing.value = true
        _publishProgress.value = null

        val tempPostId = "${userId}_${System.currentTimeMillis()}"

        Model.shared.uploadPostPicture(
            imageUri,
            tempPostId,
            { success, downloadUrl, message ->
                if (success && downloadUrl != null) {
                    val newPost = NewPost(
                        image = downloadUrl,
                        userId = userId,
                        description = description,
                        creationTime = Date(System.currentTimeMillis()),
                        gameId = game.id
                    )
                    Model.shared.addPost(newPost) { postSuccess, postMessage ->
                        _isPublishing.postValue(false)
                        _publishProgress.postValue(null)
                        _publishResult.postValue(Pair(postSuccess, postMessage))
                    }
                } else {
                    _isPublishing.postValue(false)
                    _publishProgress.postValue(null)
                    _publishResult.postValue(Pair(false, "Failed to upload image: $message"))
                }
            },
            onProgress = { progress ->
                _publishProgress.postValue("Uploading... $progress%")
            }
        )
    }
}

