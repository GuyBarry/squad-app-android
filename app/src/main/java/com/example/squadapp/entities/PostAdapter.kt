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

            Model.shared.searchGameById(post.gameId) { game ->
                if (game != null) {
                    gameName.text = game.name

                    if (game.rating > 0) {
                        gameRating.text = String.format(Locale.US, itemView.context.getString(R.string.game_rating_format), game.rating)
                    } else {
                        gameRating.text = ""
                    }

                    val platforms = game.platforms
                    if (platforms.isNotEmpty()) {
                        gamePlatforms.text = platforms.joinToString(", ")
                    } else {
                        gamePlatforms.text = itemView.context.getString(R.string.no_platform_information)
                    }
                } else {
                    gameName.text = itemView.context.getString(R.string.unknown_game)
                    gameRating.text = ""
                    gamePlatforms.text = itemView.context.getString(R.string.no_platform_information)
                }
            }

            copyDiscordBtn.setOnClickListener {
                val clipboard =
                    itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Discord Tag", post.user.discordTag)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(itemView.context, itemView.context.getString(R.string.discord_tag_copied), Toast.LENGTH_SHORT).show()
            }

            // Profile mode shows edit/delete; Home mode shows copy-discord
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

