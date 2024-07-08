package com.tonymen.locatteme.view.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.utils.TimestampUtil
import com.tonymen.locatteme.view.HomeFragments.PostDetailFragment

class UserPostsAdapter(
    private var posts: List<Post>,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<UserPostsAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.postImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        Glide.with(holder.itemView.context)
            .load(post.fotoPequena)
            .centerCrop()
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
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

            fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
