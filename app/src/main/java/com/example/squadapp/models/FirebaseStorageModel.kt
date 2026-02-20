package com.example.squadapp.models

import android.net.Uri
import android.util.Log
import com.example.squadapp.base.ResultCompletion
import com.example.squadapp.base.UploadPictureCompletion
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage

class FirebaseStorageModel {
    private val storage = Firebase.storage
    private val storageRef = storage.reference

    companion object {
        const val PROFILE_PICTURES_FOLDER = "profile_pictures"
        const val POST_PICTURES_FOLDER = "post_pictures"

        private const val TAG = "FirebaseStorageModel"
    }

    fun uploadPicture(
        imageUri: Uri,
        folderPath: String,
        fileName: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val imageRef: StorageReference = storageRef.child("$folderPath/$fileName")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            onProgress?.invoke(progress)
        }.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val downloadUrl = downloadUri.toString()
                completion(true, downloadUrl, "Image uploaded successfully")
            }.addOnFailureListener { exception ->
                val errorMessage = "Failed to get download URL: ${exception.message}"
                Log.e(TAG, errorMessage)
                completion(false, null, errorMessage)
            }
        }.addOnFailureListener { exception ->
            val errorMessage = "Upload failed: ${exception.message}"
            Log.e(TAG, errorMessage)
            completion(false, null, errorMessage)
        }
    }

    fun uploadProfilePicture(
        imageUri: Uri,
        userId: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val fileName = "profile_$userId.jpg"
        uploadPicture(imageUri, PROFILE_PICTURES_FOLDER, fileName, completion, onProgress)
    }

    fun uploadPostPicture(
        imageUri: Uri,
        postId: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val fileName = "post_${postId}_${System.currentTimeMillis()}.jpg"
        uploadPicture(imageUri, POST_PICTURES_FOLDER, fileName, completion, onProgress)
    }

    fun deletePicture(
        downloadUrl: String,
        completion: ResultCompletion
    ) {
        try {
            val imageRef = storage.getReferenceFromUrl(downloadUrl)
            imageRef.delete()
                .addOnSuccessListener {
                    completion(true, "Image deleted successfully")
                }
                .addOnFailureListener { exception ->
                    val errorMessage = "Failed to delete image: ${exception.message}"
                    Log.e(TAG, errorMessage)
                    completion(false, errorMessage)
                }
        } catch (e: Exception) {
            val errorMessage = "Invalid storage URL: ${e.message}"
            Log.e(TAG, errorMessage)
            completion(false, errorMessage)
        }
    }
}

