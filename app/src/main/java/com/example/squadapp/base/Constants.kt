package com.example.squadapp.base

import Game
import com.example.squadapp.entities.Post
import com.example.squadapp.entities.User

typealias PostsCompletion = (List<Post>) -> Unit
typealias ResultCompletion = (success: Boolean, message: String) -> Unit
typealias AuthCompletion = (success: Boolean, user: User?, message: String?) -> Unit
typealias GamesCompletion = (List<Game>) -> Unit
typealias GameCompletion = (Game?) -> Unit
typealias UploadPictureCompletion = (success: Boolean, downloadUrl: String?, message: String) -> Unit
