package com.example.squadapp.models

import com.example.squadapp.base.Completion
import com.example.squadapp.base.PostsCompletion
import com.example.squadapp.entities.Post

class Model private constructor() {

    private val firebaseModel = FirebaseModel()
//    private val firebaseAuth = FirebaseAuthModel()

//    private val executor = Executors.newSingleThreadExecutor()
//    private val mainHandler = Handler.createAsync(Looper.getMainLooper())
//
//    private val database: AppLocalDbRepository = AppLocalDB.db

    companion object {
        val shared = Model()
    }

    fun getAllPosts(completion: PostsCompletion) {


//        firebaseAuth.signIn("tal.ziii@colman.ac.il", "123456") {
//
//        }

        firebaseModel.getAllPosts(completion)
//
//        executor.execute {
//            val Posts = database.PostDao.getAllPosts()
//            mainHandler.post {
//                completion(Posts)
//            }
//        }
    }

    fun addPost(Post: Post, completion: Completion) {
        firebaseModel.addPost(Post, completion)

//        executor.execute {
//            database.PostDao.insertPosts(Post)
//            mainHandler.post {
//                completion()
//            }
//        }
    }
}
