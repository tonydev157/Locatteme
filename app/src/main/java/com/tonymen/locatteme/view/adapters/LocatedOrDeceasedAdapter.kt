package com.tonymen.locatteme.view.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.utils.TimestampUtil
import com.tonymen.locatteme.view.HomeFragments.PostCommentsFragment
import com.tonymen.locatteme.view.HomeFragments.PostDetailFragment
import com.tonymen.locatteme.view.HomeFragments.ProfileFragment
import com.tonymen.locatteme.view.HomeFragments.UserProfileFragment
import org.ocpsoft.prettytime.PrettyTime
import java.util.Date

class LocatedOrDeceasedAdapter(
    private var posts: List<Post> = listOf()
) : RecyclerView.Adapter<LocatedOrDeceasedAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val imageView: ImageView = itemView.findViewById(R.id.postImageView)
        val estadoOverlayTextView: TextView = itemView.findViewById(R.id.estadoOverlayTextView)
        val fechaPublicacionTextView: TextView = itemView.findViewById(R.id.fechaPublicacionTextView)
        val nombresTextView: TextView = itemView.findViewById(R.id.nombresTextView)
        val apellidosTextView: TextView = itemView.findViewById(R.id.apellidosTextView)
        val edadTextView: TextView = itemView.findViewById(R.id.edadTextView)
        val provinciaTextView: TextView = itemView.findViewById(R.id.provinciaTextView)
        val ciudadTextView: TextView = itemView.findViewById(R.id.ciudadTextView)
        val nacionalidadTextView: TextView = itemView.findViewById(R.id.nacionalidadTextView)
        val lugarDesaparicionTextView: TextView = itemView.findViewById(R.id.lugarDesaparicionTextView)
        val fechaDesaparicionTextView: TextView = itemView.findViewById(R.id.fechaDesaparicionTextView)
        val caracteristicasTextView: TextView = itemView.findViewById(R.id.caracteristicasTextView)
        val numerosContactoTextView: TextView = itemView.findViewById(R.id.numerosContactoTextView) // Nueva TextView para números de contacto
        val commentIcon: ImageView = itemView.findViewById(R.id.commentIcon)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_located_or_deceased, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        Glide.with(holder.itemView.context)
            .load(post.fotoGrande)
            .centerCrop()
            .into(holder.imageView)

        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        db.collection("users").document(post.autorId).get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)
            user?.let {
                holder.usernameTextView.text = it.username
                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(it.profileImageUrl)
                        .circleCrop()
                        .into(holder.profileImageView)
                } else {
                    holder.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                }

                val isCurrentUser = currentUserId == user.id

                val clickListener = View.OnClickListener {
                    val fragment = if (isCurrentUser) {
                        ProfileFragment()
                    } else {
                        UserProfileFragment.newInstance(user.id)
                    }
                    val transaction = (holder.itemView.context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                holder.profileImageView.setOnClickListener(clickListener)
                holder.usernameTextView.setOnClickListener(clickListener)
            }
        }

        val prettyTime = PrettyTime()
        holder.fechaPublicacionTextView.text = "Publicado hace: ${prettyTime.format(Date(post.fechaPublicacion.seconds * 1000))}"
        holder.nombresTextView.text = "Nombre: ${post.nombres}"
        holder.apellidosTextView.text = "Apellidos: ${post.apellidos}"
        holder.edadTextView.text = "Edad: ${post.edad}"
        holder.provinciaTextView.text = "Provincia: ${post.provincia}"
        holder.ciudadTextView.text = "Ciudad: ${post.ciudad}"
        holder.nacionalidadTextView.text = "Nacionalidad: ${post.nacionalidad}"
        holder.lugarDesaparicionTextView.text = "Lugar de Desaparición: ${post.lugarDesaparicion}"
        holder.fechaDesaparicionTextView.text = "Fecha de Desaparición: ${TimestampUtil.formatTimestampToString(post.fechaDesaparicion)}"
        holder.caracteristicasTextView.text = "Características: ${post.caracteristicas}"

        // Display numerosContacto
        if (post.numerosContacto.isNotEmpty()) {
            holder.numerosContactoTextView.text = "Contacto: ${post.numerosContacto.joinToString(", ")}"
        } else {
            holder.numerosContactoTextView.text = "No hay números de contacto disponibles"
        }

        if (post.estado == "Localizado") {
            holder.estadoOverlayTextView.text = "Localizado"
            holder.estadoOverlayTextView.setTextColor(holder.itemView.context.getColor(R.color.green))
        } else if (post.estado == "Muerto") {
            holder.estadoOverlayTextView.text = "Muerto"
            holder.estadoOverlayTextView.setTextColor(holder.itemView.context.getColor(R.color.red))
        }

        holder.itemView.setOnClickListener {
            val fragment = PostDetailFragment().apply {
                arguments = Bundle().apply {
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
                    putStringArrayList("numerosContacto", ArrayList(post.numerosContacto)) // Pass numerosContacto to the fragment
                }
            }
            val transaction = (holder.itemView.context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        holder.commentIcon.setOnClickListener {
            val fragment = PostCommentsFragment().apply {
                arguments = Bundle().apply {
                    putString("postId", post.id)
                }
            }
            val transaction = (holder.itemView.context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun submitList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

}
