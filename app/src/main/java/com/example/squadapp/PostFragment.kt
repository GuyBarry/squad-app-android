package com.example.squadapp

import Game
import com.example.squadapp.entities.GameListAdapter
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
import com.example.squadapp.utils.CameraUtils
import com.example.squadapp.utils.GalleryUtils
import com.example.squadapp.utils.GameUiUtils.mapRawgGamesToUiGames
import com.example.squadapp.utils.GameUiUtils.toRawgGame
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView
import android.widget.TextView

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

    // ── Activity-result launchers ─────────────────────────────────────────────

    private val pickImageLauncher = GalleryUtils.registerGalleryLauncher(this) { uri ->
        selectedImageUri = uri
        showSelectedImage(uri)
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
                        showSelectedImage(selectedImageUri!!)
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
        else Toast.makeText(context, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupGamesList()
        setupSearchInput()
        setupButtonListeners()
        observeViewModel()
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
    }

    private fun setupButtonListeners() {
        galleryButton.setOnClickListener { openGallery() }
        cameraButton.setOnClickListener { openCamera() }
        cancelImageButton.setOnClickListener { cancelImage() }
        publishBtn.setOnClickListener {
            if (postViewModel.isPublishing.value != true) publishPost()
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
                    replaceDisplayedGamesWithSelected(selectedGame)
                    return
                }

                when {
                    s != null && s.length >= 2 && postViewModel.inputChangeCounter >= 2 ->
                        postViewModel.searchGames(s.toString())
                    s != null && s.isEmpty() -> {
                        postViewModel.clearGames()
                        postViewModel.selectedGame = null
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private fun observeViewModel() {
        postViewModel.games.observe(viewLifecycleOwner) { rawgGames ->
            updateDisplayedGames(mapRawgGamesToUiGames(rawgGames))
        }

        postViewModel.isPublishing.observe(viewLifecycleOwner) { isPublishing ->
            setFormEnabled(!isPublishing)
            if (isPublishing) view?.clearFocus()
        }

        postViewModel.publishProgress.observe(viewLifecycleOwner) { progress ->
            publishBtn.text = progress ?: getString(R.string.publish_post)
        }

        postViewModel.publishResult.observe(viewLifecycleOwner) { (success, message) ->
            if (success) {
                Toast.makeText(context, getString(R.string.post_published_successfully), Toast.LENGTH_SHORT).show()
                navigateToHome()
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
        GalleryUtils.openGallery(pickImageLauncher)
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
            Log.e("PostFragment", "Error opening camera", ex)
            Toast.makeText(context, getString(R.string.error_opening_camera, ex.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelImage() {
        selectedImageUri = null
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

    // ── Publish & navigation ──────────────────────────────────────────────────

    private fun publishPost() {
        val description = descriptionInput.text.toString()

        if (selectedImageUri == null) {
            Toast.makeText(context, getString(R.string.please_select_image), Toast.LENGTH_LONG).show()
            return
        }
        if (postViewModel.selectedGame == null) {
            Toast.makeText(context, getString(R.string.please_select_game), Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(context, getString(R.string.please_add_description), Toast.LENGTH_SHORT).show()
            return
        }

        postViewModel.publishPost(selectedImageUri!!, args.user.id, description)
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment, HomeFragmentArgs(user = args.user).toBundle())
        activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            ?.selectedItemId = R.id.nav_home
    }
}