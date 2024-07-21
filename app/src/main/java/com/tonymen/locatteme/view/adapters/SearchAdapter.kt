package com.tonymen.locatteme.view.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemSearchPostBinding
import com.tonymen.locatteme.databinding.ItemSearchUserBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.utils.TimestampUtil
import com.tonymen.locatteme.view.HomeFragments.PostDetailFragment
import com.tonymen.locatteme.view.HomeFragments.ProfileFragment
import com.tonymen.locatteme.view.HomeFragments.UserProfileFragment

class SearchAdapter(
    private var users: List<User> = listOf(),
    private var posts: List<Post> = listOf(),
    private val currentUserId: String // Pasar el ID del usuario autenticado
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

    inner class UserViewHolder(private val binding: ItemSearchUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.username.text = user.username
            binding.fullName.text = "${user.nombre} ${user.apellido}"
            val profileImageUrl = user.profileImageUrl
            if (!profileImageUrl.isNullOrEmpty()) {
                Glide.with(binding.profileImage.context).load(profileImageUrl).circleCrop().into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }

            itemView.setOnClickListener {
                val context = it.context
                if (context is FragmentActivity) {
                    val fragment = if (user.id == currentUserId) {
                        ProfileFragment()
                    } else {
                        UserProfileFragment().apply {
                            arguments = Bundle().apply {
                                putString("userId", user.id)
                            }
                        }
                    }
                    context.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    inner class PostViewHolder(private val binding: ItemSearchPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.postTitle.text = "${post.nombres} ${post.apellidos}"
            binding.postDetails.text = post.caracteristicas
            Glide.with(binding.postImage.context).load(post.fotoPequena).into(binding.postImage)

            itemView.setOnClickListener {
                val context = it.context
                if (context is FragmentActivity) {
                    val fragment = PostDetailFragment()
                    val bundle = Bundle().apply {
                        putString("postId", post.id)
                        putString("fotoGrande", post.fotoGrande)
                        putString("nombres", post.nombres)
                        putString("apellidos", post.apellidos)
                        putInt("edad", post.edad)
                        putString("provincia", post.provincia)
                        putString("ciudad", post.ciudad)
                        putString("nacionalidad", post.nacionalidad)
                        putString("estado", post.estado)
                        putString("lugarDesaparicion", post.lugarDesaparicion)
                        putString("fechaDesaparicion", TimestampUtil.formatTimestampToString(post.fechaDesaparicion))
                        putString("caracteristicas", post.caracteristicas)
                        putString("autorId", post.autorId)
                        putString("fechaPublicacion", TimestampUtil.formatTimestampToString(post.fechaPublicacion))
                    }
                    fragment.arguments = bundle

                    context.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }
}
