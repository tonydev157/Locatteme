package com.tonymen.locatteme.view.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.view.PostDetailActivity

class UserPostsAdapter(
    private var posts: List<Post>,
    private val context: android.content.Context
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
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra("fotoGrande", post.fotoGrande)
                putExtra("nombres", post.nombres)
                putExtra("apellidos", post.apellidos)
                putExtra("edad", post.edad)
                putExtra("provincia", post.provincia)
                putExtra("ciudad", post.ciudad)
                putExtra("nacionalidad", post.nacionalidad)
                putExtra("estado", post.estado)
                putExtra("lugarDesaparicion", post.lugarDesaparicion)
                putExtra("fechaDesaparicion", post.fechaDesaparicion.seconds)
                putExtra("caracteristicas", post.caracteristicas)
                putExtra("autorId", post.autorId)
                putExtra("fechaPublicacion", post.fechaPublicacion.seconds)
            }
            context.startActivity(intent)
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
