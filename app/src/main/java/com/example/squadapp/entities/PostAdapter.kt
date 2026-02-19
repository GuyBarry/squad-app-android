package com.example.squadapp.entities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.squadapp.R
import com.example.squadapp.models.Model
import com.example.squadapp.utils.TimeUtils
import com.google.android.material.button.MaterialButton
import java.util.Locale

class PostAdapter(
    private val posts: List<Post>,
    private val onDeletePost: ((Post) -> Unit)? = null,
    private val onEditPost: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val postImageLoader: ProgressBar = itemView.findViewById(R.id.post_image_loader)
        val userProfileImage: ImageView = itemView.findViewById(R.id.user_profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name_text)
        val discordTag: TextView = itemView.findViewById(R.id.discord_tag_text)
        val postTime: TextView = itemView.findViewById(R.id.post_time)
        val postText: TextView = itemView.findViewById(R.id.post_text)
        val copyDiscordBtn: MaterialButton = itemView.findViewById(R.id.copy_discord_btn)
        val editPostBtn: MaterialButton = itemView.findViewById(R.id.edit_post_btn)
        val deletePostBtn: MaterialButton = itemView.findViewById(R.id.delete_post_btn)
        val gameName: TextView = itemView.findViewById(R.id.game_name_text)
        val gameRating: TextView = itemView.findViewById(R.id.game_rating_text)
        val gamePlatforms: TextView = itemView.findViewById(R.id.game_platforms_text)

        fun bind(post: Post, onDeletePost: ((Post) -> Unit)?, onEditPost: ((Post) -> Unit)?) {
            // Load post image from URL using Glide
            postImageLoader.visibility = View.VISIBLE
            Glide.with(itemView.context)
                .load(post.image)
                .placeholder(R.drawable.post_image_placeholder_2)
                .error(R.drawable.post_image_placeholder_2)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        postImageLoader.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        postImageLoader.visibility = View.GONE
                        return false
                    }
                })
                .into(postImage)

            // Load user profile image from URL using Glide
            Glide.with(itemView.context)
                .load(post.user.profileImage)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .circleCrop()
                .into(userProfileImage)

            userName.text = post.user.username
            discordTag.text = post.user.discordTag
            postTime.text = TimeUtils.getTimeAgoString(post.creationTime)
            postText.text = post.description

            // Fetch game data from RAWG API
            Model.shared.searchGameById(post.gameId) { rawgGame ->
                if (rawgGame != null) {
                    // Set game name
                    gameName.text = rawgGame.name

                    // Set game rating
                    if (rawgGame.rating != null && rawgGame.rating > 0) {
                        gameRating.text = "★ ${String.format(Locale.US, "%.1f", rawgGame.rating)}"
                    } else {
                        gameRating.text = ""
                    }

                    // Set platforms
                    val platforms = rawgGame.platforms?.mapNotNull { it.platform?.name } ?: emptyList()
                    if (platforms.isNotEmpty()) {
                        gamePlatforms.text = platforms.joinToString(", ")
                    } else {
                        gamePlatforms.text = "No platform information"
                    }
                } else {
                    // Fallback if game data couldn't be fetched
                    gameName.text = "Unknown Game"
                    gameRating.text = ""
                    gamePlatforms.text = "No platform information"
                }
            }

            // Copy Discord tag on button click
            copyDiscordBtn.setOnClickListener {
                val clipboard =
                    itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Discord Tag", post.user.discordTag)
                clipboard.setPrimaryClip(clip)
                // Optional: Show a toast message
                Toast.makeText(itemView.context, "Discord tag copied!", Toast.LENGTH_SHORT).show()
            }

            // Profile mode: show edit + delete, hide copy discord
            // Home mode: show copy discord, hide edit + delete
            if (onDeletePost != null) {
                copyDiscordBtn.visibility = View.GONE
                editPostBtn.visibility = View.VISIBLE
                deletePostBtn.visibility = View.VISIBLE

                editPostBtn.setOnClickListener {
                    onEditPost?.invoke(post)
                }
                deletePostBtn.setOnClickListener {
                    onDeletePost.invoke(post)
                }
            } else {
                copyDiscordBtn.visibility = View.VISIBLE
                editPostBtn.visibility = View.GONE
                deletePostBtn.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position], onDeletePost, onEditPost)
    }

    override fun getItemCount(): Int = posts.size
}

