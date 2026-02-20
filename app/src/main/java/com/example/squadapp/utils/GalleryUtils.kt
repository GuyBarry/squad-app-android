package com.example.squadapp.utils

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Utility for opening the system image gallery in a consistent way across fragments.
 *
 * Usage:
 *  1. Call [registerGalleryLauncher] in your Fragment's field initialisation (before onAttach),
 *     passing a callback that receives the chosen [Uri].
 *  2. Call [openGallery] on the returned launcher whenever you want to show the picker.
 */
object GalleryUtils {

    /**
     * Registers an [ActivityResultLauncher] that opens the system image picker and delivers
     * the selected [Uri] to [onImagePicked].  Must be called during fragment initialisation
     * (i.e. as a property delegate, not inside a lifecycle method).
     */
    fun registerGalleryLauncher(
        fragment: Fragment,
        onImagePicked: (Uri) -> Unit
    ): ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) onImagePicked(uri)
        }

    /**
     * Launches the gallery picker using a launcher previously obtained from
     * [registerGalleryLauncher].
     */
    fun openGallery(launcher: ActivityResultLauncher<String>) {
        launcher.launch("image/*")
    }
}

