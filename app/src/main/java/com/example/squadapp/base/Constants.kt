package com.example.squadapp.base

import com.example.squadapp.entities.Post
import com.example.squadapp.entities.RawgGame

typealias PostsCompletion = (List<Post>) -> Unit
typealias ResultCompletion = (success: Boolean, message: String) -> Unit
typealias RawgGamesCompletion = (List<RawgGame>) -> Unit
