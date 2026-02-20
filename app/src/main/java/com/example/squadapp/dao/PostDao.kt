package com.example.squadapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.squadapp.entities.PostEntity
import com.example.squadapp.entities.PostWithUser

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Transaction
    @Query("SELECT * FROM posts ORDER BY creationTime DESC")
    suspend fun getAllPostsWithUsers(): List<PostWithUser>

    @Transaction
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY creationTime DESC")
    suspend fun getPostsByUserWithUser(userId: String): List<PostWithUser>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}


