package com.example.squadapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for camera-related operations shared across fragments.
 */
object CameraUtils {

    /**
     * Creates a temporary file in the external pictures directory and returns a
     * content URI that can be passed to [MediaStore.ACTION_IMAGE_CAPTURE].
     */
    fun createCameraImageUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    /**
     * Builds a camera [Intent] that writes the captured photo to [outputUri].
     */
    fun buildCameraIntent(outputUri: Uri): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        }
}

