package com.tonymen.locatteme.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.databinding.ItemSearchPostBinding
import com.tonymen.locatteme.databinding.ItemSearchUserBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User

class SearchAdapter(
    private var users: List<User> = listOf(),
    private var posts: List<Post> = listOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val USER_VIEW_TYPE = 1
    private val POST_VIEW_TYPE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == USER_VIEW_TYPE) {
            val binding = ItemSearchUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserViewHolder(binding)
        } else {
            val binding = ItemSearchPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            PostViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == USER_VIEW_TYPE) {
            (holder as UserViewHolder).bind(users[position])
        } else {
            (holder as PostViewHolder).bind(posts[position - users.size])
        }
    }

    override fun getItemCount(): Int {
        return users.size + posts.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < users.size) USER_VIEW_TYPE else POST_VIEW_TYPE
    }

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    class UserViewHolder(private val binding: ItemSearchUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.username.text = user.username
            binding.fullName.text = "${user.nombre} ${user.apellido}"
            Glide.with(binding.profileImage.context).load(user.profileImageUrl).circleCrop().into(binding.profileImage)
        }
    }

    class PostViewHolder(private val binding: ItemSearchPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.postTitle.text = "${post.nombres} ${post.apellidos}"
            binding.postDetails.text = post.caracteristicas
            Glide.with(binding.postImage.context).load(post.fotoPequena).into(binding.postImage)
        }
    }
}
