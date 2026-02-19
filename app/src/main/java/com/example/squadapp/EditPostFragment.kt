package com.example.squadapp

import Game
import com.example.squadapp.entities.GameListAdapter
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.squadapp.entities.RawgGame
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView
import android.widget.TextView
import java.util.Date

class EditPostFragment : Fragment(R.layout.fragment_edit_post) {
    private val editPostViewModel: EditPostViewModel by viewModels()
    private val args: EditPostFragmentArgs by navArgs()

    private lateinit var imagePreview: ImageView
    private lateinit var imagePlaceholder: android.widget.LinearLayout
    private lateinit var imageBoxContainer: android.widget.FrameLayout
    private lateinit var cancelImageButton: MaterialButton
    private lateinit var galleryButton: MaterialButton
    private lateinit var cameraButton: MaterialButton
    private lateinit var gameSearchInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var gamesListRecycler: RecyclerView
    private lateinit var gameListAdapter: GameListAdapter
    private lateinit var publishBtn: MaterialButton

    private var selectedImageUri: Uri? = null
    private var imageChanged = false
    private val displayedGames = mutableListOf<Game>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                imageChanged = true
                imagePreview.setImageURI(selectedImageUri)
                imagePreview.visibility = View.VISIBLE
                imagePlaceholder.visibility = View.GONE
                cancelImageButton.visibility = View.VISIBLE
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (selectedImageUri != null) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri!!)
                    if (inputStream != null && inputStream.available() > 0) {
                        inputStream.close()
                        imageChanged = true
                        imagePreview.setImageURI(selectedImageUri)
                        imagePreview.visibility = View.VISIBLE
                        imagePlaceholder.visibility = View.GONE
                        cancelImageButton.visibility = View.VISIBLE
                    } else {
                        inputStream?.close()
                        selectedImageUri = null
                    }
                } catch (e: Exception) {
                    Log.e("EditPostFragment", "Error reading camera image", e)
                    selectedImageUri = null
                }
            }
        } else {
            selectedImageUri = null
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) launchCamera()
        else Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val post = args.post
        val user = args.user

        imagePreview = view.findViewById(R.id.post_image_preview)
        imagePlaceholder = view.findViewById(R.id.image_placeholder)
        imageBoxContainer = view.findViewById(R.id.image_box_container)
        cancelImageButton = view.findViewById(R.id.cancel_image_button)
        galleryButton = view.findViewById(R.id.gallery_button)
        cameraButton = view.findViewById(R.id.camera_button)
        gameSearchInput = view.findViewById(R.id.squad_search_input)
        descriptionInput = view.findViewById(R.id.description_text)
        gamesListRecycler = view.findViewById(R.id.games_list_recycler)
        publishBtn = view.findViewById(R.id.publish_btn)

        setupGamesList()
        setupSearchInput()
        initializeWithPostData(post)

        galleryButton.setOnClickListener { openGallery() }
        cameraButton.setOnClickListener { openCamera() }
        cancelImageButton.setOnClickListener { cancelImage() }

        publishBtn.setOnClickListener {
            if (editPostViewModel.isPublishing.value != true) updatePost(post)
        }

        // Observe game search results
        editPostViewModel.games.observe(viewLifecycleOwner) { rawgGames ->
            val games = rawgGames.map { rawgGame ->
                val platforms = rawgGame.platforms?.mapNotNull { it.platform?.name } ?: emptyList()
                Game(
                    name = rawgGame.name,
                    platforms = platforms,
                    imageResId = android.R.drawable.ic_menu_gallery,
                    id = rawgGame.id,
                    imageUrl = rawgGame.backgroundImage
                )
            }
            val oldSize = displayedGames.size
            displayedGames.clear()
            if (oldSize > 0) gameListAdapter.notifyItemRangeRemoved(0, oldSize)
            displayedGames.addAll(games)
            if (games.isNotEmpty()) gameListAdapter.notifyItemRangeInserted(0, games.size)
        }

        // Observe publishing state
        editPostViewModel.isPublishing.observe(viewLifecycleOwner) { isPublishing ->
            publishBtn.isEnabled = !isPublishing
            descriptionInput.isEnabled = !isPublishing
            gameSearchInput.isEnabled = !isPublishing
            galleryButton.isEnabled = !isPublishing
            cameraButton.isEnabled = !isPublishing
            cancelImageButton.isEnabled = !isPublishing
            if (isPublishing) {
                view.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        editPostViewModel.publishProgress.observe(viewLifecycleOwner) { progress ->
            publishBtn.text = progress ?: getString(R.string.update_post)
        }

        // Observe update result
        editPostViewModel.publishResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, "Post updated successfully!", Toast.LENGTH_SHORT).show()
                navigateToProfile(user)
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeWithPostData(post: com.example.squadapp.entities.Post) {
        // Load existing image
        Glide.with(this)
            .load(post.image)
            .placeholder(R.drawable.post_image_placeholder_2)
            .error(R.drawable.post_image_placeholder_2)
            .into(imagePreview)
        imagePreview.visibility = View.VISIBLE
        imagePlaceholder.visibility = View.GONE
        cancelImageButton.visibility = View.VISIBLE

        // Set description
        descriptionInput.setText(post.description)

        // Load game data from API and pre-select it
        com.example.squadapp.models.Model.shared.searchGameById(post.gameId) { rawgGame ->
            if (rawgGame != null) {
                editPostViewModel.selectedGame = rawgGame
                editPostViewModel.inputChangeCounter = 0
                activity?.runOnUiThread {
                    gameSearchInput.setText(rawgGame.name, TextView.BufferType.EDITABLE)
                }
            }
        }
    }

    private fun setupGamesList() {
        gameListAdapter = GameListAdapter(displayedGames) { game ->
            editPostViewModel.selectedGame = game.toRawgGame()
            editPostViewModel.inputChangeCounter = 0
            gameSearchInput.setText(game.name, TextView.BufferType.EDITABLE)
        }
        gamesListRecycler.layoutManager = LinearLayoutManager(context)
        gamesListRecycler.adapter = gameListAdapter
    }

    private fun setupSearchInput() {
        gameSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                editPostViewModel.inputChangeCounter++
                val selectedGame = editPostViewModel.selectedGame

                if (s != null && selectedGame != null && s.toString() == selectedGame.name) {
                    val oldSize = displayedGames.size
                    displayedGames.clear()
                    if (oldSize > 0) gameListAdapter.notifyItemRangeRemoved(0, oldSize)
                    displayedGames.add(Game(
                        name = selectedGame.name,
                        platforms = selectedGame.platforms?.mapNotNull { it.platform?.name } ?: emptyList(),
                        imageResId = android.R.drawable.ic_menu_gallery,
                        id = selectedGame.id,
                        imageUrl = selectedGame.backgroundImage
                    ))
                    gameListAdapter.notifyItemInserted(0)
                    return
                }

                if (s != null && s.length >= 2 && editPostViewModel.inputChangeCounter >= 2) {
                    editPostViewModel.searchGames(s.toString())
                } else if (s != null && s.isEmpty()) {
                    editPostViewModel.clearGames()
                    editPostViewModel.selectedGame = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(galleryIntent)
    }

    private fun openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(Date())
            val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val photoFile = java.io.File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            selectedImageUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", photoFile
            )
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri)
            takePictureLauncher.launch(cameraIntent)
        } catch (ex: Exception) {
            Log.e("EditPostFragment", "Error opening camera", ex)
            Toast.makeText(context, "Error opening camera: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelImage() {
        selectedImageUri = null
        imageChanged = true
        imagePreview.setImageURI(null)
        imagePreview.visibility = View.GONE
        cancelImageButton.visibility = View.GONE
        imagePlaceholder.visibility = View.VISIBLE
    }

    private fun updatePost(post: com.example.squadapp.entities.Post) {
        val description = descriptionInput.text.toString()

        // If image was changed and no new image selected, require one
        if (imageChanged && selectedImageUri == null) {
            Toast.makeText(context, "Please select or take an image", Toast.LENGTH_LONG).show()
            return
        }
        if (editPostViewModel.selectedGame == null) {
            Toast.makeText(context, "Please select a game from the list", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(context, "Please add a description", Toast.LENGTH_SHORT).show()
            return
        }

        // Pass null for imageUri if image wasn't changed (keep existing)
        val imageUri = if (imageChanged) selectedImageUri else null
        editPostViewModel.updatePost(post, imageUri, description)
    }

    private fun navigateToProfile(user: com.example.squadapp.entities.User) {
        findNavController().navigate(
            R.id.profileFragment,
            ProfileFragmentArgs(user = user).toBundle()
        )
        val bottomNavigation = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.selectedItemId = R.id.nav_profile
    }

    // Helper to convert a Game (UI model) back to RawgGame for the ViewModel
    private fun Game.toRawgGame(): RawgGame = RawgGame(
        id = this.id,
        name = this.name,
        slug = "",
        released = null,
        backgroundImage = this.imageUrl,
        rating = null,
        ratingsCount = null,
        metacritic = null,
        platforms = this.platforms.map { platformName ->
            com.example.squadapp.entities.PlatformInfo(
                com.example.squadapp.entities.Platform(0, platformName, "")
            )
        },
        genres = null,
        description = null,
        shortScreenshots = null
    )
}

