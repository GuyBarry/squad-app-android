package com.example.squadapp.base

import com.example.squadapp.entities.Post

typealias PostsCompletion = (List<Post>) -> Unit
typealias PostCompletion = (Post) -> Unit
typealias Completion = () -> Unit