package com.example.squadapp

import Game
import GameAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView

class PostFragment : Fragment() {

    private lateinit var imagePreview: ImageView
    private lateinit var imagePlaceholder: android.widget.LinearLayout
    private lateinit var imageBoxContainer: android.widget.FrameLayout
    private lateinit var squadDropdown: MaterialAutoCompleteTextView
    private lateinit var descriptionText: TextInputEditText
    private lateinit var publishBtn: MaterialButton

    private var selectedImageUri: Uri? = null

    // Mock game list with objects
    private val gameList = listOf(
        Game("Valorant", "PC", android.R.drawable.ic_menu_view),
        Game("Counter-Strike 2", "PC", android.R.drawable.ic_menu_view),
        Game("League of Legends", "PC", android.R.drawable.ic_menu_view),
        Game("Dota 2", "PC", android.R.drawable.ic_menu_view),
        Game("Fortnite", "PC", android.R.drawable.ic_menu_view),
        Game("Apex Legends", "PC", android.R.drawable.ic_menu_view),
        Game("Call of Duty", "PC", android.R.drawable.ic_menu_view),
        Game("Overwatch 2", "PC", android.R.drawable.ic_menu_view)
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                imagePreview.setImageURI(selectedImageUri)
                imagePreview.visibility = View.VISIBLE
                imagePlaceholder.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        imagePreview = view.findViewById(R.id.post_image_preview)
        imagePlaceholder = view.findViewById(R.id.image_placeholder)
        imageBoxContainer = view.findViewById(R.id.image_box_container)
        squadDropdown = view.findViewById(R.id.squad_dropdown)
        descriptionText = view.findViewById(R.id.description_text)
        publishBtn = view.findViewById(R.id.publish_btn)

        // Set up game dropdown with filtering
        setupGameDropdown()

        // Set up image box to open gallery directly on click
        imageBoxContainer.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(galleryIntent)
        }

        // Set up publish button
        publishBtn.setOnClickListener {
            publishPost()
        }
    }

    private fun setupGameDropdown() {
        val adapter = GameAdapter(requireContext(), gameList)
        squadDropdown.setAdapter(adapter)
        squadDropdown.dropDownHeight = 850
        squadDropdown.setOnItemClickListener { _, _, position, _ ->
            squadDropdown.setText(gameList[position].name, false)
        }
    }

    private fun publishPost() {
        val game = squadDropdown.text.toString()
        val description = descriptionText.text.toString()

        if (game.isEmpty()) {
            Toast.makeText(context, "Please select a game", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(context, "Please add a description", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: add here loader + publish post functionallity+ on close fragment refresh the posts list

//        val post: Post = Post()

        Toast.makeText(
            context,
            "Post published for $game!",
            Toast.LENGTH_SHORT
        ).show()

        // Reset form
        resetForm() // TODO: maybe close the fragment and return to home page
    }

    private fun resetForm() {
        selectedImageUri = null
        imagePreview.setImageBitmap(null)
        imagePreview.visibility = View.GONE
        imagePlaceholder.visibility = View.VISIBLE
        squadDropdown.text.clear()
        descriptionText.text?.clear()
    }
}


