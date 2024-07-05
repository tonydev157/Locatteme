package com.tonymen.locatteme.view.adapters

import android.content.Context
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
import com.tonymen.locatteme.view.HomeFragments.ProfileFragment
import com.tonymen.locatteme.view.HomeFragments.UserProfileFragment
import org.ocpsoft.prettytime.PrettyTime
import java.util.Date

class HomePostsAdapter(
    private var posts: List<Post>,
    private val context: Context
) : RecyclerView.Adapter<HomePostsAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val imageView: ImageView = itemView.findViewById(R.id.postImageView)
        val fechaPublicacionTextView: TextView = itemView.findViewById(R.id.fechaPublicacionTextView)
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

        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        db.collection("users").document(post.autorId).get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)
            user?.let {
                holder.usernameTextView.text = it.username
                Glide.with(holder.itemView.context)
                    .load(it.profileImageUrl)
                    .circleCrop()
                    .into(holder.profileImageView)

                val isCurrentUser = currentUserId == user.id

                // Set the click listener on the profile image and username to navigate to the user's profile
                val clickListener = View.OnClickListener {
                    val fragment = if (isCurrentUser) {
                        ProfileFragment()
                    } else {
                        UserProfileFragment.newInstance(user.id)
                    }
                    val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
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
        holder.estadoTextView.text = "Estado: ${post.estado}"
        holder.lugarDesaparicionTextView.text = "Lugar de Desaparición: ${post.lugarDesaparicion}"
        holder.fechaDesaparicionTextView.text = "Fecha de Desaparición: ${TimestampUtil.formatTimestampToString(post.fechaDesaparicion)}"
        holder.caracteristicasTextView.text = "Características: ${post.caracteristicas}"
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
