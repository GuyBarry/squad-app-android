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
        // Folder paths for different image types
        const val PROFILE_PICTURES_FOLDER = "profile_pictures"
        const val POST_PICTURES_FOLDER = "post_pictures"

        private const val TAG = "FirebaseStorageModel"
    }

    /**
     * Upload a picture to Firebase Storage
     * @param imageUri The local URI of the image to upload
     * @param folderPath The folder path where the image should be stored (use constants from companion)
     * @param fileName The name to give the file in storage
     * @param completion Callback with success status, download URL (if successful), and message
     * @param onProgress Optional callback for upload progress (0-100)
     */
    fun uploadPicture(
        imageUri: Uri,
        folderPath: String,
        fileName: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val imageRef: StorageReference = storageRef.child("$folderPath/$fileName")

        val uploadTask = imageRef.putFile(imageUri)

        // Track upload progress
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            Log.d(TAG, "Upload progress: $progress%")
            onProgress?.invoke(progress)
        }.addOnSuccessListener { taskSnapshot ->
            // Get download URL
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val downloadUrl = downloadUri.toString()
                Log.d(TAG, "Image uploaded successfully to: $downloadUrl")
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

    /**
     * Upload a profile picture
     * @param imageUri The local URI of the image to upload
     * @param userId The user's ID to create a unique filename
     * @param completion Callback with success status, download URL (if successful), and message
     * @param onProgress Optional callback for upload progress (0-100)
     */
    fun uploadProfilePicture(
        imageUri: Uri,
        userId: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val fileName = "profile_$userId.jpg"
        uploadPicture(imageUri, PROFILE_PICTURES_FOLDER, fileName, completion, onProgress)
    }

    /**
     * Upload a post picture
     * @param imageUri The local URI of the image to upload
     * @param postId The post's ID to create a unique filename
     * @param completion Callback with success status, download URL (if successful), and message
     * @param onProgress Optional callback for upload progress (0-100)
     */
    fun uploadPostPicture(
        imageUri: Uri,
        postId: String,
        completion: UploadPictureCompletion,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val fileName = "post_${postId}_${System.currentTimeMillis()}.jpg"
        uploadPicture(imageUri, POST_PICTURES_FOLDER, fileName, completion, onProgress)
    }

    /**
     * Delete a picture from Firebase Storage
     * @param downloadUrl The download URL of the image to delete
     * @param completion Callback with success status and message
     */
    fun deletePicture(
        downloadUrl: String,
        completion: ResultCompletion
    ) {
        try {
            val imageRef = storage.getReferenceFromUrl(downloadUrl)
            imageRef.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Image deleted successfully: $downloadUrl")
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

