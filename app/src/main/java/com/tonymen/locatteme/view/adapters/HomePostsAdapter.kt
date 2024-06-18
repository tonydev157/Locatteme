package com.tonymen.locatteme.view.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.utils.TimestampUtil
import com.tonymen.locatteme.view.PostDetailActivity

class HomePostsAdapter(
    private var posts: List<Post>,
    private val context: Context
) : RecyclerView.Adapter<HomePostsAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.postImageView)
        val nombresTextView: TextView = itemView.findViewById(R.id.nombresTextView)
        val apellidosTextView: TextView = itemView.findViewById(R.id.apellidosTextView)
        val edadTextView: TextView = itemView.findViewById(R.id.edadTextView)
        val provinciaTextView: TextView = itemView.findViewById(R.id.provinciaTextView)
        val ciudadTextView: TextView = itemView.findViewById(R.id.ciudadTextView)
        val nacionalidadTextView: TextView = itemView.findViewById(R.id.nacionalidadTextView)
        val estadoTextView: TextView = itemView.findViewById(R.id.estadoTextView)
        val lugarDesaparicionTextView: TextView = itemView.findViewById(R.id.lugarDesaparicionTextView)
        val fechaDesaparicionTextView: TextView = itemView.findViewById(R.id.fechaDesaparicionTextView)
        val caracteristicasTextView: TextView = itemView.findViewById(R.id.caracteristicasTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_home, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        Glide.with(holder.itemView.context)
            .load(post.fotoGrande)
            .centerCrop()
            .into(holder.imageView)

        holder.nombresTextView.text = post.nombres
        holder.apellidosTextView.text = post.apellidos
        holder.edadTextView.text = post.edad.toString()
        holder.provinciaTextView.text = post.provincia
        holder.ciudadTextView.text = post.ciudad
        holder.nacionalidadTextView.text = post.nacionalidad
        holder.estadoTextView.text = post.estado
        holder.lugarDesaparicionTextView.text = post.lugarDesaparicion
        holder.fechaDesaparicionTextView.text = TimestampUtil.formatTimestampToString(post.fechaDesaparicion)
        holder.caracteristicasTextView.text = post.caracteristicas

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra("postId", post.id)
                putExtra("fotoGrande", post.fotoGrande)
                putExtra("nombres", post.nombres)
                putExtra("apellidos", post.apellidos)
                putExtra("edad", post.edad)
                putExtra("provincia", post.provincia)
                putExtra("ciudad", post.ciudad)
                putExtra("nacionalidad", post.nacionalidad)
                putExtra("estado", post.estado)
                putExtra("lugarDesaparicion", post.lugarDesaparicion)
                putExtra("fechaDesaparicion", TimestampUtil.formatTimestampToString(post.fechaDesaparicion))
                putExtra("caracteristicas", post.caracteristicas)
                putExtra("autorId", post.autorId)
                putExtra("fechaPublicacion", TimestampUtil.formatTimestampToString(post.fechaPublicacion))
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
