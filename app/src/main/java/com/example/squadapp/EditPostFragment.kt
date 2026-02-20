package com.example.squadapp

import Game
import com.example.squadapp.entities.GameListAdapter
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.net.Uri
import android.os.Bundle
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
import com.example.squadapp.utils.CameraUtils
import com.example.squadapp.utils.GameUiUtils.mapRawgGamesToUiGames
import com.example.squadapp.utils.GameUiUtils.toRawgGame
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView
import android.widget.TextView

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
    private lateinit var loadingIndicator: View
    private lateinit var contentView: View

    private var selectedImageUri: Uri? = null
    private var imageChanged = false
    private val displayedGames = mutableListOf<Game>()

    // ── Activity-result launchers ─────────────────────────────────────────────

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageUri = uri
                imageChanged = true
                showSelectedImage(uri)
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
                        showSelectedImage(selectedImageUri!!)
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_edit_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val post = args.post
        val user = args.user

        bindViews(view)
        setupGamesList()
        setupSearchInput()
        setupButtonListeners(post)
        initializeWithPostData(post)
        observeViewModel(post, user)

        editPostViewModel.loadPostData(post.gameId)
    }

    // ── View binding & setup ──────────────────────────────────────────────────

    private fun bindViews(view: View) {
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
        loadingIndicator = view.findViewById(R.id.edit_post_loading_indicator)
        contentView = view.findViewById(R.id.edit_post_content)
    }

    private fun setupButtonListeners(post: com.example.squadapp.entities.Post) {
        galleryButton.setOnClickListener { openGallery() }
        cameraButton.setOnClickListener { openCamera() }
        cancelImageButton.setOnClickListener { cancelImage() }
        publishBtn.setOnClickListener {
            if (editPostViewModel.isPublishing.value != true) updatePost(post)
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
                    replaceDisplayedGamesWithSelected(selectedGame)
                    return
                }

                when {
                    s != null && s.length >= 2 && editPostViewModel.inputChangeCounter >= 2 ->
                        editPostViewModel.searchGames(s.toString())
                    s != null && s.isEmpty() -> {
                        editPostViewModel.clearGames()
                        editPostViewModel.selectedGame = null
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initializeWithPostData(post: com.example.squadapp.entities.Post) {
        Glide.with(this)
            .load(post.image)
            .placeholder(R.drawable.post_image_placeholder_2)
            .error(R.drawable.post_image_placeholder_2)
            .into(imagePreview)
        imagePreview.visibility = View.VISIBLE
        imagePlaceholder.visibility = View.GONE
        cancelImageButton.visibility = View.VISIBLE

        descriptionInput.setText(post.description)

        // Fill the game search input once the initial game data finishes loading
        editPostViewModel.isLoadingData.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                val gameName = editPostViewModel.selectedGame?.name ?: return@observe
                if (gameSearchInput.text.toString() != gameName) {
                    gameSearchInput.setText(gameName, TextView.BufferType.EDITABLE)
                }
            }
        }
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel(
        @Suppress("UNUSED_PARAMETER") post: com.example.squadapp.entities.Post,
        user: com.example.squadapp.entities.User
    ) {
        editPostViewModel.isLoadingData.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            contentView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        editPostViewModel.games.observe(viewLifecycleOwner) { rawgGames ->
            updateDisplayedGames(mapRawgGamesToUiGames(rawgGames))
        }

        editPostViewModel.isPublishing.observe(viewLifecycleOwner) { isPublishing ->
            setFormEnabled(!isPublishing)
            if (isPublishing) {
                view?.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
            }
        }

        editPostViewModel.publishProgress.observe(viewLifecycleOwner) { progress ->
            publishBtn.text = progress ?: getString(R.string.update_post)
        }

        editPostViewModel.publishResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, "Post updated successfully!", Toast.LENGTH_SHORT).show()
                navigateToProfile(user)
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Image helpers ─────────────────────────────────────────────────────────

    /** Shows the picked/captured image and reveals the cancel button. */
    private fun showSelectedImage(uri: Uri) {
        imagePreview.setImageURI(uri)
        imagePreview.visibility = View.VISIBLE
        imagePlaceholder.visibility = View.GONE
        cancelImageButton.visibility = View.VISIBLE
    }

    private fun openGallery() {
        pickImageLauncher.launch(CameraUtils.buildGalleryIntent())
    }

    private fun openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) launchCamera()
        else requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        try {
            selectedImageUri = CameraUtils.createCameraImageUri(requireContext())
            takePictureLauncher.launch(CameraUtils.buildCameraIntent(selectedImageUri!!))
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

    // ── Games list helpers ────────────────────────────────────────────────────

    /** Replaces the games list with just the already-selected game. */
    private fun replaceDisplayedGamesWithSelected(selectedGame: com.example.squadapp.entities.RawgGame) {
        val oldSize = displayedGames.size
        displayedGames.clear()
        if (oldSize > 0) gameListAdapter.notifyItemRangeRemoved(0, oldSize)
        displayedGames.add(
            Game(
                name = selectedGame.name,
                platforms = selectedGame.platforms?.mapNotNull { it.platform?.name } ?: emptyList(),
                imageResId = android.R.drawable.ic_menu_gallery,
                id = selectedGame.id,
                imageUrl = selectedGame.backgroundImage
            )
        )
        gameListAdapter.notifyItemInserted(0)
    }

    private fun updateDisplayedGames(games: List<Game>) {
        val oldSize = displayedGames.size
        displayedGames.clear()
        if (oldSize > 0) gameListAdapter.notifyItemRangeRemoved(0, oldSize)
        displayedGames.addAll(games)
        if (games.isNotEmpty()) gameListAdapter.notifyItemRangeInserted(0, games.size)
    }

    // ── Form state helpers ────────────────────────────────────────────────────

    private fun setFormEnabled(enabled: Boolean) {
        publishBtn.isEnabled = enabled
        descriptionInput.isEnabled = enabled
        gameSearchInput.isEnabled = enabled
        galleryButton.isEnabled = enabled
        cameraButton.isEnabled = enabled
        cancelImageButton.isEnabled = enabled
    }

    // ── Update & navigation ───────────────────────────────────────────────────

    private fun updatePost(post: com.example.squadapp.entities.Post) {
        val description = descriptionInput.text.toString()

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

        val imageUri = if (imageChanged) selectedImageUri else null
        editPostViewModel.updatePost(post, imageUri, description)
    }

    private fun navigateToProfile(user: com.example.squadapp.entities.User) {
        findNavController().navigate(R.id.profileFragment, ProfileFragmentArgs(user = user).toBundle())
        activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            ?.selectedItemId = R.id.nav_profile
    }
}

