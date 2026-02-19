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
import com.example.squadapp.entities.RawgGame
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView
import android.widget.TextView
import java.util.Date

class PostFragment : Fragment(R.layout.fragment_post) {
    private val postViewModel: PostViewModel by viewModels()
    private val args: PostFragmentArgs by navArgs()

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
    private val displayedGames = mutableListOf<Game>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
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
                        imagePreview.setImageURI(selectedImageUri)
                        imagePreview.visibility = View.VISIBLE
                        imagePlaceholder.visibility = View.GONE
                        cancelImageButton.visibility = View.VISIBLE
                    } else {
                        inputStream?.close()
                        selectedImageUri = null
                    }
                } catch (e: Exception) {
                    Log.e("PostFragment", "Error reading camera image", e)
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
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        galleryButton.setOnClickListener { openGallery() }
        cameraButton.setOnClickListener { openCamera() }
        cancelImageButton.setOnClickListener { cancelImage() }

        publishBtn.setOnClickListener {
            if (postViewModel.isPublishing.value != true) publishPost()
        }

        // Observe game search results
        postViewModel.games.observe(viewLifecycleOwner) { rawgGames ->
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
        postViewModel.isPublishing.observe(viewLifecycleOwner) { isPublishing ->
            publishBtn.isEnabled = !isPublishing
            descriptionInput.isEnabled = !isPublishing
            gameSearchInput.isEnabled = !isPublishing
            galleryButton.isEnabled = !isPublishing
            cameraButton.isEnabled = !isPublishing
            cancelImageButton.isEnabled = !isPublishing
            if (isPublishing) {
                view.clearFocus()
            }
        }

        postViewModel.publishProgress.observe(viewLifecycleOwner) { progress ->
            publishBtn.text = progress ?: getString(R.string.publish_post)
        }

        // Observe publish result
        postViewModel.publishResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, "Post published successfully!", Toast.LENGTH_SHORT).show()
                resetForm()
                navigateToHome()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupGamesList() {
        gameListAdapter = GameListAdapter(displayedGames) { game ->
            postViewModel.selectedGame = game.toRawgGame()
            postViewModel.inputChangeCounter = 0
            gameSearchInput.setText(game.name, TextView.BufferType.EDITABLE)
        }
        gamesListRecycler.layoutManager = LinearLayoutManager(context)
        gamesListRecycler.adapter = gameListAdapter
    }

    private fun setupSearchInput() {
        gameSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                postViewModel.inputChangeCounter++
                val selectedGame = postViewModel.selectedGame

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

                if (s != null && s.length >= 2 && postViewModel.inputChangeCounter >= 2) {
                    postViewModel.searchGames(s.toString())
                } else if (s != null && s.isEmpty()) {
                    postViewModel.clearGames()
                    postViewModel.selectedGame = null
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
            Log.e("PostFragment", "Error opening camera", ex)
            Toast.makeText(context, "Error opening camera: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelImage() {
        selectedImageUri = null
        imagePreview.setImageURI(null)
        imagePreview.visibility = View.GONE
        cancelImageButton.visibility = View.GONE
        imagePlaceholder.visibility = View.VISIBLE
    }

    private fun publishPost() {
        val descriptionText: TextInputEditText = view?.findViewById(R.id.description_text) ?: return
        val description = descriptionText.text.toString()

        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select or take an image", Toast.LENGTH_LONG).show()
            return
        }
        if (postViewModel.selectedGame == null) {
            Toast.makeText(context, "Please select a game from the list", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(context, "Please add a description", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = args.user.id
        postViewModel.publishPost(selectedImageUri!!, userId, description)
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment, HomeFragmentArgs(user = args.user).toBundle())
        val bottomNavigation = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.selectedItemId = R.id.nav_home
    }

    private fun resetForm() {
        selectedImageUri = null
        imagePreview.setImageBitmap(null)
        imagePreview.visibility = View.GONE
        cancelImageButton.visibility = View.GONE
        imagePlaceholder.visibility = View.VISIBLE
        gameSearchInput.text?.clear()
        val size = displayedGames.size
        displayedGames.clear()
        if (size > 0) gameListAdapter.notifyItemRangeRemoved(0, size)
        postViewModel.selectedGame = null
        view?.findViewById<TextInputEditText>(R.id.description_text)?.text?.clear()
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