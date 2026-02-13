package com.example.squadapp.entities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.R
import com.example.squadapp.utils.TimeUtils
import com.google.android.material.button.MaterialButton

class PostAdapter(private val posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val userProfileImage: ImageView = itemView.findViewById(R.id.user_profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name_text)
        val discordTag: TextView = itemView.findViewById(R.id.discord_tag_text)
        val postTime: TextView = itemView.findViewById(R.id.post_time)
        val postText: TextView = itemView.findViewById(R.id.post_text)
        val copyDiscordBtn: MaterialButton = itemView.findViewById(R.id.copy_discord_btn)

        fun bind(post: Post) {
            postImage.setImageResource(post.image)
            userProfileImage.setImageResource(post.user.profileImage)
            userName.text = post.user.username
            discordTag.text = post.user.discordTag
            postTime.text = TimeUtils.getTimeAgoString(post.creationTime)
            postText.text = post.description

            // Copy Discord tag on button click
            copyDiscordBtn.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Discord Tag", post.user.discordTag)
                clipboard.setPrimaryClip(clip)
                // Optional: Show a toast message
                Toast.makeText(itemView.context, "Discord tag copied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size
}

