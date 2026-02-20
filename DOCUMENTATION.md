# SquadApp – Technical Documentation

> **SquadApp** is a social gaming Android application that lets users create posts about games they are playing,
> browse other players' posts, and manage their profile. The backend is powered entirely by Firebase (Authentication,
> Firestore, Storage). Game metadata is fetched from the [RAWG Video Games Database API](https://rawg.io/apidocs).

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Activity & Lifecycle](#2-activity--lifecycle)
3. [Intents](#3-intents)
4. [Views, Widgets & Layouts](#4-views-widgets--layouts)
5. [RecyclerView & Adapters](#5-recyclerview--adapters)
6. [Navigation Graph, Menus & Dialogs](#6-navigation-graph-menus--dialogs)
7. [SQLite & Room](#7-sqlite--room)
8. [Firebase – Firestore, Storage & Authentication](#8-firebase--firestore-storage--authentication)
9. [MVVM – ViewModel, LiveData & SharedFlow](#9-mvvm--viewmodel-livedata--sharedflow)
10. [Fragment & Fragment ViewModel](#10-fragment--fragment-viewmodel)
11. [Architecture Diagram](#11-architecture-diagram)
12. [Key Dependencies](#12-key-dependencies)

---

## 1. Project Overview

| Item | Value |
|---|---|
| Package | `com.example.squadapp` |
| Min SDK | 33 (Android 13) |
| Target SDK | 36 |
| Language | Kotlin |
| Build System | Gradle (KTS) |
| Architecture | **MVVM** + Repository (`Model` singleton) + **Room offline-first cache** |

### Package Structure

```
com.example.squadapp/
├── api/                   # Retrofit client & service interface (RAWG)
├── base/                  # Kotlin typealias completion callbacks (Constants.kt)
├── dao/                   # Room DAO interfaces (PostDao, UserDao, GameDao)
├── database/              # AppDatabase – Room database singleton
├── entities/              # Data classes + Room @Entity classes + RecyclerView adapters
├── models/                # Firebase wrappers + RoomLocalModel (Model, FirebaseAuthModel, FirebaseModel, FirebaseStorageModel, RoomLocalModel)
├── utils/                 # Camera, Gallery, Game UI, Spannable, Time utilities
├── AuthActivity.kt        # Auth entry point
├── AuthViewModel.kt
├── MainActivity.kt        # Main shell (bottom nav + NavController)
├── HomeFragment.kt / HomeViewModel.kt
├── PostFragment.kt / PostViewModel.kt
├── ProfileFragment.kt / ProfileViewModel.kt
├── EditPostFragment.kt / EditPostViewModel.kt
├── EditProfileFragment.kt / EditProfileViewModel.kt
├── SignInFragment.kt / SignInViewModel.kt
├── SignUpFragment.kt / SignUpViewModel.kt
└── SquadApplication.kt    # Custom Application subclass – initialises the Room database
```

---

## 2. Activity & Lifecycle

### What is an Activity?
An **Activity** is a single, focused screen that the user can interact with. Every Android app must have at least one Activity declared in `AndroidManifest.xml`. Activities own a **lifecycle** — a series of callbacks (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) that Android calls as the screen comes in and out of focus.

### Activities in this project

#### `AuthActivity` (`activity_auth.xml`)
- **Declared as the launcher Activity** in `AndroidManifest.xml` (the entry point of the app).
- On `onCreate` it immediately calls `Model.shared.getCurrentUser(...)` to check whether a Firebase session already exists.
  - If a session is found → emits the user through `AuthViewModel.navigateToMain` → calls `startActivity(Intent(this, MainActivity::class.java))`.
  - If no session → shows the `NavHostFragment` containing `SignInFragment` / `SignUpFragment`.
- Lifecycle used:
  - `onCreate` – sets up edge-to-edge display, wires the `ProgressBar` visibility, and launches the session check.
  - `lifecycleScope.launch` – collects the `navigateToMain` SharedFlow safely within the Activity lifecycle.

#### `MainActivity` (`activity_main.xml`)
- Hosts the **main application shell**: a `FragmentContainerView` (NavHostFragment) + a `BottomNavigationView`.
- Receives the authenticated `User` object via `Intent.getParcelableExtra(User.EXTRA_USER)`.
- On `onCreate`:
  - Inflates the layout, applies window insets (edge-to-edge).
  - Initialises `NavController` and sets the nav graph to `nav_main.xml`, passing the `User` as a Safe Args argument to the start destination (`HomeFragment`).
  - Registers a `BottomNavigationView.setOnItemSelectedListener` to navigate between `Home`, `Post`, and `Profile` fragments.
  - Registers `navController.addOnDestinationChangedListener` to keep the bottom nav item checked-state in sync even when navigating programmatically (e.g., from `EditProfileFragment` back to `ProfileFragment`).
- Exposes `onUserUpdated(updatedUser: User)` so `EditProfileFragment` can bubble an updated user back up.
- `onSupportNavigateUp` delegates to the `NavController` to support the system back button.

### Lifecycle Methods Used in Fragments

| Callback | Where it's used |
|---|---|
| `onCreateView` | All fragments — inflates the XML layout |
| `onViewCreated` | All fragments — binds views, sets up listeners, starts observing LiveData |
| `onResume` | `HomeFragment`, `ProfileFragment` — reloads posts from the local Room cache so the list is always fresh |

---

## 3. Intents

### What is an Intent?
An **Intent** is a messaging object used to request an action from another component. **Explicit intents** target a specific class; **implicit intents** ask the OS to find the right app to handle a given action.

### Intents in this project

#### Navigating between Activities (Explicit Intent)
In `AuthActivity.navigateToMain(user)`:
```kotlin
val intent = Intent(this, MainActivity::class.java)
intent.putExtra(User.EXTRA_USER, user)          // passes the Parcelable User
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
```
- `FLAG_ACTIVITY_CLEAR_TASK` ensures the back stack is cleared — the user cannot press back and return to the auth screen.
- The `User` data class implements `Parcelable` (via the `@Parcelize` annotation) so it can travel inside an Intent extra.

#### Camera Intent (Implicit Intent)
In `CameraUtils.buildCameraIntent(outputUri)`:
```kotlin
Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
    putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
}
```
- Used in `PostFragment`, `EditPostFragment`, and `EditProfileFragment` to open the device camera. The result URI is created with `FileProvider` to stay compatible with Android's file permission model.

#### ActivityResultLaunchers (Modern Intent pattern)
All three image-related fragments use `registerForActivityResult(...)` — the modern replacement for `startActivityForResult`:
- `ActivityResultContracts.StartActivityForResult` → camera capture.
- Gallery images use `GalleryUtils.registerGalleryLauncher(this) { uri -> ... }`.
- `ActivityResultContracts.RequestPermission` → runtime camera permission request.

---

## 4. Views, Widgets & Layouts

### What are Views, Widgets & Layouts?
In Android, every UI element is a **View**. Widgets are the interactive `View` subclasses (buttons, text fields, images). **Layouts** are special `ViewGroup` subclasses that position and size child Views.

### Layouts used in this project

| Layout | File(s) | Purpose |
|---|---|---|
| `ConstraintLayout` | `activity_main.xml` | Root layout of `MainActivity`; positions the `FragmentContainerView` above the `BottomNavigationView` with constraints. |
| `LinearLayout` | `fragment_home.xml`, `item_post.xml`, and most fragment layouts | Arranges children vertically or horizontally in a simple flow. |
| `FrameLayout` | `item_post.xml` (post image area), `activity_auth.xml` | Stacks children on top of each other; used to overlay the game info gradient over the post image. |
| `SwipeRefreshLayout` | `fragment_home.xml` | Wraps the `RecyclerView` to provide pull-to-refresh functionality. |

### Widgets used in this project

| Widget | Where | Purpose |
|---|---|---|
| `TextView` | Almost every layout | Displays read-only text (username, discord tag, post description, time, game info). |
| `EditText` | Sign in, sign up, edit profile, edit post | Receives text input from the user. |
| `TextInputEditText` (Material) | Post / edit post game search & description | Material-styled text input with floating label. |
| `ImageView` | All fragments | Displays profile pictures and post images (loaded with **Glide**). |
| `Button` | Sign in / sign up forms | Triggers form submission. |
| `MaterialButton` (Material) | Post, edit post, profile, post item | Action buttons with Material 3 ripple and styling. |
| `FloatingActionButton` (Material) | Edit profile | Camera / gallery / cancel / delete image floating action buttons. |
| `ProgressBar` | `item_post.xml` (image loader) | Spinner shown while post image loads via Glide. |
| `CircularProgressIndicator` (Material) | `fragment_home.xml`, `activity_auth.xml` | Full-screen loading spinner shown during initial data fetch. |
| `BottomNavigationView` (Material) | `activity_main.xml` | Bottom navigation bar with Home, Post, and Profile tabs. |
| `RecyclerView` | Home, Profile, Post (game search) | Scrollable list of posts or game search results (see §5). |

### Edge-to-Edge Display
Both activities call `enableEdgeToEdge()` and apply `ViewCompat.setOnApplyWindowInsetsListener` to add padding for system bars, ensuring the layout is drawn behind the status and navigation bars on Android 13+.

---

## 5. RecyclerView & Adapters

### What is a RecyclerView?
A `RecyclerView` is an efficient, flexible list/grid widget. Instead of creating a new View for every item in the data set, it **recycles** (reuses) the off-screen item views. Each item's appearance is defined by a **ViewHolder** pattern, and the connection between data and views is managed by an **Adapter**.

### RecyclerViews in this project

#### `PostAdapter` + `PostViewHolder` (`entities/PostAdapter.kt`)
- Displays a list of `Post` objects.
- Used in both `HomeFragment` (all posts) and `ProfileFragment` (user's own posts).
- **ViewHolder** binds:
  - Post image → loaded with **Glide** (with a `ProgressBar` shown while loading).
  - User's profile image → Glide with `circleCrop()`.
  - Username, discord tag, relative time (`TimeUtils`), description.
  - Game name, rating, and platforms fetched via `Model.shared.searchGameById(...)` inside `onBindViewHolder` (checks the Room game cache first, then falls back to the RAWG API).
  - Edit / Delete buttons (conditionally shown based on whether callbacks are provided by the hosting fragment — on `ProfileFragment` the user sees edit and delete; on `HomeFragment` they do not).
  - Copy Discord button — uses `ClipboardManager` to copy the discord tag.
- `LinearLayoutManager` (vertical) is set in both `HomeFragment` and `ProfileFragment`.

#### `GameListAdapter` + `GameViewHolder` (`entities/GameListAdapter.kt`)
- Displays a short list of `Game` objects as search suggestions while the user types a game name.
- Used in `PostFragment` and `EditPostFragment` below the game search `TextInputEditText`.
- **ViewHolder** binds:
  - Game thumbnail image (Glide).
  - Game name and platforms list.
  - `itemView.setOnClickListener` → calls `onGameSelected(game)` to select the game for the post.
- `LinearLayoutManager` (vertical) is set in both fragments.

### SwipeRefreshLayout
`HomeFragment` wraps its `RecyclerView` in a `SwipeRefreshLayout`. When the user pulls down, `homeViewModel.refreshPosts()` is called — this bypasses the Room cache and fetches fresh data directly from Firestore, then updates the cache. The `swipeRefreshLayout.isRefreshing` spinner is controlled by the `isLoading` LiveData so it auto-hides when the fetch completes.

---

## 6. Navigation Graph, Menus & Dialogs

### What is a Navigation Graph?
The **Jetpack Navigation Component** lets you define all screens and the transitions between them in a single XML file (the **nav graph**). A `NavHostFragment` acts as the container, and a `NavController` handles all navigation operations. **Safe Args** (a Gradle plugin) generates type-safe classes for passing arguments between destinations.

### Navigation Graphs in this project

#### `nav_auth.xml` — Auth flow
- **Start destination**: `signInFragment`
- Destinations: `SignInFragment`, `SignUpFragment`
- Action: `action_signInFragment_to_signUpFragment` (go to sign up)
- Action: `action_signUpFragment_to_signInFragment` with `popUpTo` + `popUpToInclusive=true` (go back to sign in and clear sign up from the back stack)
- Hosted in `activity_auth.xml` → `nav_host_fragment_auth`

#### `nav_main.xml` — Main app flow
- **Start destination**: `homeFragment`
- Destinations: `HomeFragment`, `PostFragment`, `ProfileFragment`, `EditProfileFragment`, `EditPostFragment`
- Each fragment destination declares a typed `<argument>` for the `User` (using Safe Args):
  ```xml
  <argument android:name="user" app:argType="com.example.squadapp.entities.User" />
  ```
- Arguments are passed with generated `*Args` classes, e.g. `HomeFragmentArgs(user = user).toBundle()`.
- Fragments retrieve arguments with `private val args: ProfileFragmentArgs by navArgs()`.
- Hosted in `activity_main.xml` → `nav_host_fragment_main`

#### Navigation in code
```kotlin
// Push to next destination
findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, EditProfileFragmentArgs(user = user).toBundle())

// Pop back stack
findNavController().popBackStack()

// Navigate from MainActivity (bottom nav)
navController.navigate(R.id.homeFragment, HomeFragmentArgs(user = user).toBundle())
```

### Menus

#### `bottom_nav_menu.xml`
Defines the three items of the `BottomNavigationView` in `MainActivity`:
- `nav_home` → Home (house icon)
- `nav_post` → Post (add-circle icon)
- `nav_profile` → Profile (person icon)

The selected item is kept in sync via `navController.addOnDestinationChangedListener` — so navigating programmatically from, say, `EditProfileFragment` back to `ProfileFragment` also highlights the Profile tab.

### Dialogs
An **AlertDialog** is used in `ProfileFragment` to confirm post deletion before actually calling `profileViewModel.deletePost(...)`. This prevents accidental deletion with a two-step confirm / cancel interaction:
```kotlin
AlertDialog.Builder(requireContext())
    .setTitle(...)
    .setMessage(...)
    .setPositiveButton("Delete") { _, _ -> profileViewModel.deletePost(...) }
    .setNegativeButton("Cancel", null)
    .show()
```

---

## 7. SQLite & Room

### What is SQLite?
**SQLite** is a lightweight, file-based relational database built into Android. It stores data as tables with rows and columns and is queried with SQL.

### What is Room?
**Room** is the official Android Jetpack abstraction layer over SQLite. It removes the need to write raw `Cursor` / `ContentValues` boilerplate and instead lets you:
- Define tables with `@Entity` data classes.
- Write queries with `@Dao` interfaces and annotated methods (`@Query`, `@Insert`, `@Delete`, …).
- Access the database via a `@Database`-annotated abstract class.
- Integrate natively with **LiveData** and **Kotlin Coroutines / Flow**.

### Room in this project
Room is **fully implemented** as the offline cache layer for posts, users, and game metadata. It is the primary data source for reads; Firebase Firestore is the source of truth and is used for writes and explicit refreshes.

#### `AppDatabase` (`database/AppDatabase.kt`)
The Room database singleton, implemented as a thread-safe `companion object` with double-checked locking. It references all three entities and exposes the three DAOs:
```kotlin
@Database(
    entities = [User::class, PostEntity::class, GameEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun gameDao(): GameDao
}
```
The database file is named `squad_app_database`. `fallbackToDestructiveMigration(true)` is set for development convenience.

#### `SquadApplication` (`SquadApplication.kt`)
A custom `Application` subclass registered in `AndroidManifest.xml`. It initialises the `AppDatabase` instance lazily and exposes it as a global singleton so `RoomLocalModel` can access it without needing a `Context` parameter at every call site:
```kotlin
class SquadApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    companion object {
        lateinit var instance: SquadApplication
            private set
    }
    override fun onCreate() { super.onCreate(); instance = this }
}
```

#### Entities (Room tables)

| Entity class | Table | Key fields |
|---|---|---|
| `User` (`entities/User.kt`) | `users` | `id` (PK), `username`, `email`, `profileImage`, `discordTag` |
| `PostEntity` (`entities/PostEntity.kt`) | `posts` | `id` (PK), `image`, `userId` (FK → `users.id`), `description`, `creationTime` (Long epoch ms), `gameId` |
| `GameEntity` (`entities/GameEntity.kt`) | `games` | `id` (PK, Int), `name`, `rating` (Double), `platforms` (comma-separated String) |

`PostEntity` declares a **foreign key** to `User` with `onDelete = CASCADE` and an index on `userId`. The companion `PostEntity.fromPost(post)` factory converts the domain `Post` object for storage.

`PostWithUser` (`entities/PostWithUser.kt`) is a Room **relation** class that uses `@Embedded` + `@Relation` to join `PostEntity` with its `User` in a single query. Its `toPost()` method converts it back to the domain `Post` object.

#### DAOs (`dao/`)

| DAO | Interface | Key operations |
|---|---|---|
| `PostDao` | `dao/PostDao.kt` | `insertPosts`, `getAllPostsWithUsers` (`@Transaction`), `getPostsByUserWithUser` (`@Transaction`), `getPostById`, `deletePost`, `deleteAllPosts` |
| `UserDao` | `dao/UserDao.kt` | `insertUser`, `insertUsers`, `updateUser`, `getUserById`, `deleteAllUsers` |
| `GameDao` | `dao/GameDao.kt` | `insertGame`, `getGameById` |

All DAO methods are `suspend` functions, executed on coroutine-managed threads.

#### `RoomLocalModel` (`models/RoomLocalModel.kt`)
Wraps all DAO calls and provides the bridge between the domain layer and Room:

| Method | Description |
|---|---|
| `saveUser(user)` | Inserts or replaces a single `User`. |
| `updateUser(user)` | Updates an existing `User` record. |
| `savePosts(posts)` | Extracts unique users from the list, inserts them, then inserts `PostEntity` rows. |
| `savePost(newPost, postId)` | Inserts a single new post after it has been written to Firestore. |
| `getAllPosts()` | Returns all posts (with embedded users) sorted by `creationTime` descending. |
| `getPostsByUser(userId)` | Returns posts for a specific user. |
| `deletePost(postId)` | Deletes a single post row. |
| `updatePost(postId, updates)` | Applies a partial-update map (description, imageUrl, gameId) to an existing `PostEntity`. |
| `clearAllPosts()` | Deletes all rows from `posts`. |
| `clearAllUsers()` | Deletes all rows from `users`. |
| `getGame(gameId)` | Returns a cached `GameEntity` or `null` if not yet cached. |
| `saveGame(game)` | Inserts or replaces a `GameEntity`. |

### Offline-first data flow
The `Model` singleton coordinates Firebase and Room to implement an **offline-first** pattern:

1. **Read (fast path):** `Model.shared.getAllPosts` → reads directly from `RoomLocalModel` → instant local result.
2. **Refresh (network path):** `Model.shared.refreshPosts` → fetches all posts from Firestore → clears the Room `posts` table → saves fresh data → returns to UI.
3. **Write:** `addPost`, `deletePost`, `updatePost` write to Firestore first; on success they mirror the change into Room.
4. **Game cache:** `searchGameById` checks `GameDao` first; on a cache miss it calls the RAWG API and stores the result in `GameEntity` for future hits.
5. **Sign-out:** `Model.shared.signOut` calls `clearAllPosts()` and `clearAllUsers()` to wipe cached data for the next user.

---

## 8. Firebase – Firestore, Storage & Authentication

All Firebase interactions are encapsulated in the `models/` package. The public `Model` singleton delegates to three private Firebase model classes.

### Firebase Authentication (`FirebaseAuthModel.kt`)
**What it is:** Firebase Auth manages user identity. It handles email/password account creation, sign-in, and session persistence (a token is stored on-device automatically).

**Used for:**
- `signUpUser(password, newUser, completion)` — validates uniqueness of username and email in Firestore first, then calls `auth.createUserWithEmailAndPassword(...)`. On success writes the user profile to Firestore. If Firestore write fails it rolls back the Auth account with `user.delete()`. The `Model` layer also saves the new user to Room on success.
- `signInUser(email, password, completion)` — calls `auth.signInWithEmailAndPassword(...)` then fetches the Firestore profile. The `Model` layer saves the user to Room on success.
- `getCurrentUser(completion)` — called on app launch in `AuthActivity`. Checks `auth.currentUser` (persisted token). If a UID is found, fetches the full profile from Firestore.
- `updateUser(...)` — updates username, discord tag, and optionally a new profile image URL in Firestore. The `Model` layer mirrors the change to Room on success.
- `signOut()` — calls `auth.signOut()`. The `Model` layer clears the Room cache.

**Where it's triggered from:**
- `AuthActivity.onCreate` → `Model.shared.getCurrentUser`
- `SignInViewModel` → `Model.shared.signInUser`
- `SignUpViewModel` → `Model.shared.signUpUser`
- `ProfileFragment` (logout button) → `profileViewModel.signOut()` → `Model.shared.signOut()`

### Firebase Firestore (`FirebaseModel.kt`)
**What it is:** Cloud Firestore is a NoSQL document database. Data is organised into **collections** of **documents** (JSON-like objects).

**Collections in SquadApp:**

| Collection | Fields | Purpose |
|---|---|---|
| `users` | `id`, `username`, `email`, `profileImage`, `discordTag` | User profiles |
| `posts` | `image`, `user` (userId), `description`, `creationTime`, `gameId` | Posts |

**Operations:**
- `getAllPosts` — fetches all documents from `posts`, then batch-fetches the corresponding `users` documents using `whereIn`. Joins them in memory and sorts by `creationTime` descending. Called only by `Model.refreshPosts`.
- `getPostsByUser(userId)` — queries `posts` with `.whereEqualTo("user", userId)`.
- `addPost(newPost)` — adds a document to `posts` using `db.collection(POSTS).add(...)`.
- `deletePost(postId)` — deletes a document by ID.
- `updatePost(postId, updates)` — partial update using `document.update(updates)`.

**Where it's triggered from:** `Model.shared` (which is called by `HomeViewModel`, `PostViewModel`, `ProfileViewModel`, `EditPostViewModel`).

### Firebase Storage (`FirebaseStorageModel.kt`)
**What it is:** Firebase Storage stores binary files (images, videos). Files are referenced by a path, and a **download URL** (HTTPS) is stored in Firestore for retrieval.

**Used for:**
- `uploadProfilePicture(uri, userId)` → saves to `profile_pictures/profile_{userId}.jpg`
- `uploadPostPicture(uri, postId)` → saves to `post_pictures/post_{postId}_{timestamp}.jpg`
- `deletePicture(downloadUrl)` → deletes the file at the given URL.
- Upload progress is reported via an `onProgress: ((Int) -> Unit)?` callback, which the ViewModels forward to the UI (e.g., `"Uploading... 42%"` text on the publish button).

**File URI → FileProvider:** Before uploading, camera images are written to a `FileProvider`-backed URI (see `CameraUtils.kt`) to avoid direct file path sharing between apps — required since Android 7.

---

## 9. MVVM – ViewModel, LiveData & SharedFlow

### What is MVVM?
**Model-View-ViewModel** is the architectural pattern recommended by Google for Android. It separates concerns:
- **Model** — data and business logic (Firebase, RAWG API, Room).
- **View** — Activities and Fragments; only observes and renders.
- **ViewModel** — holds and processes UI state; survives configuration changes (e.g., screen rotation) because it is **not** tied to the Activity/Fragment lifecycle.

### ViewModel
Every screen in SquadApp has a dedicated `ViewModel`:

| ViewModel | Owned by | Responsibilities |
|---|---|---|
| `AuthViewModel` | `AuthActivity` (`by viewModels()`) | Holds a `MutableSharedFlow<User>` that fires once on successful auth, driving the `AuthActivity → MainActivity` transition. Shared with child fragments via `activityViewModels()`. |
| `SignInViewModel` | `SignInFragment` | Calls `Model.shared.signInUser`, exposes `isSigningIn: LiveData<Boolean>` and `signInResult: LiveData<Pair<Boolean,String>>`. |
| `SignUpViewModel` | `SignUpFragment` | Same pattern for sign-up. |
| `HomeViewModel` | `HomeFragment` | Exposes `posts: LiveData<List<Post>>` and `isLoading: LiveData<Boolean>`. Has two load methods: `loadPosts()` reads from the Room cache (fast, used on `onResume`); `refreshPosts()` fetches fresh data from Firestore and updates the cache (used on initial load and swipe-to-refresh). |
| `PostViewModel` | `PostFragment` | Manages game search (`games: LiveData<List<RawgGame>>`), upload progress, and publish result. |
| `ProfileViewModel` | `ProfileFragment` | Loads user posts from Room cache, handles delete (Firestore + Room), sign-out. Exposes `deleteResult: LiveData<Pair<Boolean,String>>`. |
| `EditProfileViewModel` | `EditProfileFragment` | Extends `AndroidViewModel` (needs `Application` context for string resources). Manages image upload/delete and profile update. |
| `EditPostViewModel` | `EditPostFragment` | Loads existing post game data, manages game search, update logic (Firestore + Room). |

### LiveData
**`LiveData`** is a lifecycle-aware observable data holder. Fragments observe it with `viewLifecycleOwner` to automatically stop receiving updates when the view is destroyed (preventing memory leaks and null-pointer crashes).

```kotlin
// In HomeViewModel
val posts: LiveData<List<Post>> = _posts          // exposed as immutable

// In HomeFragment
homeViewModel.posts.observe(viewLifecycleOwner) { posts ->
    recyclerView?.adapter = PostAdapter(posts)     // runs only while the fragment view is alive
}
```

Pattern used throughout:
- Private `MutableLiveData` (`_posts`, `_isLoading`) is mutated inside the ViewModel.
- Public `LiveData` property exposes a read-only view to the Fragment.
- `.postValue(...)` is used from background threads (Firebase / Room coroutine callbacks); `.value = ...` is used from the main thread.

### SharedFlow (AuthViewModel)
`AuthViewModel` uses `MutableSharedFlow<User>` instead of `LiveData` for the one-shot navigation event. `SharedFlow` does not re-emit to late subscribers (unlike `LiveData`), which is important for navigation: if the Activity is recreated, it should not navigate again due to a replayed event.

```kotlin
// AuthViewModel
private val _navigateToMain = MutableSharedFlow<User>(extraBufferCapacity = 1)
val navigateToMain = _navigateToMain.asSharedFlow()

// AuthActivity
lifecycleScope.launch {
    authViewModel.navigateToMain.collect { user -> navigateToMain(user) }
}
```

---

## 10. Fragment & Fragment ViewModel

### What is a Fragment?
A **Fragment** represents a modular portion of UI that lives inside an Activity. Fragments have their own lifecycle (closely tied to the Activity's), can be added/removed/replaced dynamically, and are the standard unit of navigation in single-Activity architectures.

### Fragments in this project

#### Auth Fragments (inside `AuthActivity`)
| Fragment | Layout | Purpose |
|---|---|---|
| `SignInFragment` | `fragment_sign_in.xml` | Email + password form. Navigates to `SignUpFragment`. On success, emits user via shared `AuthViewModel`. |
| `SignUpFragment` | `fragment_sign_up.xml` | Username, email, discord tag, password, confirm-password form. On success, emits user via shared `AuthViewModel`. |

Both fragments access the **activity-scoped** `AuthViewModel` via `activityViewModels()` — this ensures the same ViewModel instance is shared with `AuthActivity`, which collects the `navigateToMain` flow.

#### Main Fragments (inside `MainActivity`)
| Fragment | Layout | Key Features |
|---|---|---|
| `HomeFragment` | `fragment_home.xml` | Displays all posts in a `RecyclerView` with pull-to-refresh. On `onViewCreated` calls `refreshPosts()` (Firestore). On `onResume` calls `loadPosts()` (Room cache). |
| `PostFragment` | `fragment_post.xml` | Create a new post: pick/take a photo, type a description, search and select a game. |
| `ProfileFragment` | `fragment_profile.xml` | Shows the current user's info and their posts in a `RecyclerView`. Edit/delete post buttons. Logout. |
| `EditProfileFragment` | `fragment_edit_profile.xml` | Edit username, discord tag, profile picture (gallery or camera). |
| `EditPostFragment` | `fragment_edit_post.xml` | Edit an existing post's description, image, and game. |

### Fragment ViewModel Pattern (per-fragment `by viewModels()`)
Each fragment creates its own `ViewModel` instance using the `by viewModels()` delegate:
```kotlin
private val homeViewModel: HomeViewModel by viewModels()
```
The ViewModel lives as long as the Fragment's view exists (scoped to the Fragment back-stack entry). It survives screen rotations but is destroyed when the fragment is permanently removed.

### Passing Data Between Fragments (Safe Args)
Fragments receive data through Safe Args bundles (not constructors, which is not allowed):
```kotlin
// Sender (ProfileFragment)
val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(user = user)
findNavController().navigate(action)

// Receiver (EditProfileFragment)
private val args: EditProfileFragmentArgs by navArgs()
val user = args.user
```

### `onResume` reload pattern
`HomeFragment` and `ProfileFragment` both call their ViewModel's `loadPosts` / `loadUserPosts` function from `onResume()`. This reads from the local Room cache for a fast, low-latency update whenever the user navigates back to these screens (e.g., after editing a post or creating a new one). A network refresh is only triggered explicitly (swipe-to-refresh on `HomeFragment`, or after a write operation).

---

## 11. Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────┐
│                          UI Layer (View)                              │
│  AuthActivity ──► SignInFragment / SignUpFragment                     │
│  MainActivity ──► HomeFragment / PostFragment / ProfileFragment       │
│                   EditProfileFragment / EditPostFragment              │
└────────────────────────────┬──────────────────────────────────────────┘
                             │  observe LiveData / collect Flow
                             ▼
┌───────────────────────────────────────────────────────────────────────┐
│                       ViewModel Layer                                 │
│  AuthViewModel, HomeViewModel, PostViewModel, ProfileViewModel        │
│  EditProfileViewModel, EditPostViewModel, SignInViewModel, ...        │
└────────────────────────────┬──────────────────────────────────────────┘
                             │  calls methods on
                             ▼
┌───────────────────────────────────────────────────────────────────────┐
│                       Repository (Model singleton)                    │
│  Model.shared                                                         │
│    ├── FirebaseAuthModel      (Firebase Authentication)               │
│    ├── FirebaseModel          (Cloud Firestore – posts & users)       │
│    ├── FirebaseStorageModel   (Firebase Storage – images)             │
│    ├── RoomLocalModel         (offline cache – posts, users, games)   │
│    └── RawgApiClient          (RAWG REST API via Retrofit)            │
└──────────┬────────────────────────────────┬───────────────────────────┘
           │ reads/writes                   │ reads/writes
           ▼                                ▼
┌─────────────────────┐          ┌──────────────────────────┐
│  Room (AppDatabase) │          │  Firebase / RAWG API      │
│  users / posts /    │◄─ sync ──│  (source of truth)        │
│  games tables       │          │                           │
└─────────────────────┘          └──────────────────────────┘
```

---

## 12. Key Dependencies

| Library | Version ref | Purpose |
|---|---|---|
| **Firebase BOM** | `firebase.bom` | Manages all Firebase library versions |
| `firebase-firestore` | – | NoSQL cloud database |
| `firebase-auth` | – | User authentication |
| `firebase-storage` | – | Image/file cloud storage |
| **Jetpack Navigation** | `navigation.fragment.ktx` / `navigation.ui.ktx` | Fragment navigation, Safe Args |
| **Room** | `room.runtime`, `room.ktx`, `room.compiler` | Local SQLite ORM – fully implemented as the offline cache (`AppDatabase`, `PostDao`, `UserDao`, `GameDao`, `RoomLocalModel`) |
| **Lifecycle** | `lifecycle.viewmodel.ktx`, `lifecycle.livedata.ktx`, `lifecycle.runtime.ktx` | ViewModel, LiveData, lifecycleScope |
| **Retrofit 2** | `retrofit`, `retrofit.converter.gson` | Type-safe REST client for RAWG API |
| **OkHttp** | `okhttp`, `okhttp.logging.interceptor` | HTTP client used by Retrofit |
| **Glide** | `glide`, `glide.compiler` | Image loading, caching, and transformation |
| **Material Components** | `material` | `MaterialButton`, `FloatingActionButton`, `BottomNavigationView`, `TextInputEditText`, `CircularProgressIndicator` |
| **SwipeRefreshLayout** | `androidx.swiperefreshlayout` | Pull-to-refresh on `HomeFragment` |
| **Kotlin Parcelize** | plugin `kotlin.parcelize` | `@Parcelize` annotation for `User` and `Post` to pass via Intents / Safe Args |
| **Safe Args** | plugin `navigation.safeargs` | Generates type-safe navigation argument classes |
| **Kotlin Coroutines** | via `lifecycle.runtime.ktx` | `lifecycleScope.launch`, `SharedFlow.collect`, coroutine-based Room DAOs |
