package com.example.squadapp

import Game
import GameListAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.entities.Post
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView
import android.widget.TextView
import com.example.squadapp.models.Model
import java.util.Date

class PostFragment : Fragment(R.layout.fragment_post) {

    private lateinit var imagePreview: ImageView
    private lateinit var imagePlaceholder: android.widget.LinearLayout
    private lateinit var imageBoxContainer: android.widget.FrameLayout
    private lateinit var gameSearchInput: TextInputEditText
    private lateinit var gamesListRecycler: RecyclerView
    private lateinit var gameListAdapter: GameListAdapter
    private lateinit var publishBtn: MaterialButton

    private var selectedImageUri: Uri? = null
    private var isPublishing = false
    private var selectedGame: Game? = null
    private val filteredGames = mutableListOf<Game>()
    private var inputChangeCounter = 0

    // Mock game list with objects
    private val gameList = listOf(
        Game("Valorant", "PC", android.R.drawable.ic_menu_view),
        Game("Counter-Strike 2", "PC", android.R.drawable.ic_menu_view),
        Game("League of Legends", "PC", android.R.drawable.ic_menu_view),
        Game("League of Legends2", "PC", android.R.drawable.ic_menu_view),
        Game("League of Legends3", "PC", android.R.drawable.ic_menu_view),
        Game("League of nothing", "PC", android.R.drawable.ic_menu_view),
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
        gameSearchInput = view.findViewById(R.id.squad_search_input)
        gamesListRecycler = view.findViewById(R.id.games_list_recycler)
        publishBtn = view.findViewById(R.id.publish_btn)

        // Set up games list RecyclerView
        setupGamesList()

        // Set up search input filtering with 2-keystroke delay
        setupSearchInput()

        // Set up image box to open gallery directly on click
        imageBoxContainer.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(galleryIntent)
        }

        // Set up publish button
        publishBtn.setOnClickListener {
            if (!isPublishing) {
                publishPost()
            }
        }
    }

    private fun setupGamesList() {
        gameListAdapter = GameListAdapter(filteredGames) { game ->
            selectedGame = game
            gameSearchInput.setText(game.name, TextView.BufferType.EDITABLE)
            filteredGames.clear()
            gameListAdapter.notifyDataSetChanged()
        }
        gamesListRecycler.layoutManager = LinearLayoutManager(context)
        gamesListRecycler.adapter = gameListAdapter
    }

    private fun setupSearchInput() {
        gameSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                inputChangeCounter++

                // After 2 keystrokes, filter the games
                if (s != null && s.length >= 2 && inputChangeCounter >= 2) {
                    filterGames(s.toString())
                } else if (s != null && s.isEmpty()) {
                    // Clear the list if input is empty
                    filteredGames.clear()
                    gameListAdapter.notifyDataSetChanged()
                    selectedGame = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterGames(query: String) {
        val results = gameList.filter { game ->
            game.name.contains(query, ignoreCase = true)
        }.take(4)

        filteredGames.clear()
        filteredGames.addAll(results)
        gameListAdapter.notifyDataSetChanged()
    }

    private fun publishPost() {
        val descriptionText: TextInputEditText = view?.findViewById(R.id.description_text) ?: return
        val description = descriptionText.text.toString()

        if (selectedGame == null) {
            Toast.makeText(context, "Please select a game from the list", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(context, "Please add a description", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user from MainActivity
        val mainActivity = activity as? MainActivity
        if (mainActivity == null) {
            Toast.makeText(context, "Error: Could not get user information", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = mainActivity.currentUser

        // Create post object
        val post = Post(
            id = "", // Will be generated by Firestore
            image = 0, // No local image resource ID when fetching from gallery
            user = currentUser,
            description = description,
            creationTime = Date(System.currentTimeMillis())
        )

        // Disable publish button and show loading state
        isPublishing = true
        publishBtn.isEnabled = false
        publishBtn.text = "Publishing..."

        // Publish post to server
        Model.shared.addPost(post) { success, message ->
            isPublishing = false
            publishBtn.isEnabled = true
            publishBtn.text = "Publish Post"

            if (success) {
                Log.d("PostFragment", "Post published successfully")
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                // Reset form and navigate back to home
                resetForm()
                navigateToHome()
            } else {
                Log.e("PostFragment", "Failed to publish post: $message")
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                // Form is NOT reset, user can try again
            }
        }
    }

    private fun navigateToHome() {
        // Navigate back to HomeFragment
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, HomeFragment())
            commit()
        }
    }

    private fun resetForm() {
        selectedImageUri = null
        imagePreview.setImageBitmap(null)
        imagePreview.visibility = View.GONE
        imagePlaceholder.visibility = View.VISIBLE
        gameSearchInput.text?.clear()
        filteredGames.clear()
        gameListAdapter.notifyDataSetChanged()
        selectedGame = null
        val descriptionText: TextInputEditText = view?.findViewById(R.id.description_text) ?: return
        descriptionText.text?.clear()
    }
}


