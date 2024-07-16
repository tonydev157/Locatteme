package com.tonymen.locatteme.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemCommentBinding
import com.tonymen.locatteme.model.Comment
import com.tonymen.locatteme.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var commentsList = listOf<Comment>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = commentsList.size

    fun submitList(comments: List<Comment>) {
        commentsList = comments.reversed() // Revertir la lista para mostrar los más recientes abajo
        notifyDataSetChanged()
    }

    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        private val prettyTime = PrettyTime(Locale.getDefault())

        fun bind(comment: Comment) {
            // Llama al método para obtener los datos del usuario basado en userId
            CoroutineScope(Dispatchers.IO).launch {
                val user = getUserFromId(comment.userId)
                withContext(Dispatchers.Main) {
                    user?.let {
                        Glide.with(binding.profileImageView.context)
                            .load(it.profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(binding.profileImageView)

                        binding.usernameTextView.text = it.username
                    } ?: run {
                        // Manejar caso cuando el usuario no se encuentra
                        Glide.with(binding.profileImageView.context)
                            .load(R.drawable.ic_profile_placeholder)
                            .into(binding.profileImageView)
                    }
                    binding.commentTextView.text = comment.comentario
                    binding.commentDateTextView.text = "Publicado hace: ${prettyTime.format(Date(comment.fechaComentario.seconds * 1000))}"
                }
            }
        }

        private suspend fun getUserFromId(userId: String): User? {
            return try {
                val document = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
                document.toObject(User::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
