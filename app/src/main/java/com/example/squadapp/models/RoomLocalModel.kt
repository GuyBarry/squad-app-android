package com.example.squadapp.models

import com.example.squadapp.SquadApplication
import com.example.squadapp.dao.PostDao
import com.example.squadapp.dao.UserDao
import com.example.squadapp.entities.NewPost
import com.example.squadapp.entities.Post
import com.example.squadapp.entities.PostEntity
import com.example.squadapp.entities.User

class RoomLocalModel {

    private val userDao: UserDao = SquadApplication.instance.database.userDao()
    private val postDao: PostDao = SquadApplication.instance.database.postDao()

    suspend fun saveUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun savePosts(posts: List<Post>) {
        // Extract unique users and save them first
        val users = posts.map { it.user }.distinctBy { it.id }
        userDao.insertUsers(users)
        postDao.insertPosts(posts.map { PostEntity.fromPost(it) })
    }

    suspend fun savePost(newPost: NewPost, postId: String) {
        postDao.insertPosts(listOf(
            PostEntity(
                id = postId,
                image = newPost.image,
                userId = newPost.userId,
                description = newPost.description,
                creationTime = newPost.creationTime.time,
                gameId = newPost.gameId
            )
        ))
    }

    suspend fun getAllPosts(): List<Post> {
        return postDao.getAllPostsWithUsers().map { it.toPost() }
    }

    suspend fun getPostsByUser(userId: String): List<Post> {
        return postDao.getPostsByUserWithUser(userId).map { it.toPost() }
    }

    suspend fun deletePost(postId: String) {
        postDao.deletePost(postId)
    }

    suspend fun updatePost(postId: String, updates: Map<String, Any?>) {
        val existing = postDao.getPostById(postId) ?: return
        val updated = existing.copy(
            description = updates["description"] as? String ?: existing.description,
            image = updates["imageUrl"] as? String ?: existing.image,
            gameId = (updates["gameId"] as? Number)?.toInt() ?: existing.gameId
        )
        postDao.insertPosts(listOf(updated))
    }

    suspend fun clearAllPosts() {
        postDao.deleteAllPosts()
    }

    suspend fun clearAllUsers() {
        userDao.deleteAllUsers()
    }
}

