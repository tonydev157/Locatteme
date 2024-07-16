package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Comment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostCommentsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> get() = _comments

    fun loadComments(postId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("posts").document(postId).collection("comments").get().await()
                val commentsList = snapshot.toObjects(Comment::class.java).sortedBy { it.fechaComentario } // Ordenar por fechaComentario
                _comments.postValue(commentsList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addComment(postId: String, commentText: String, userId: String) {
        val newComment = Comment(userId, commentText, com.google.firebase.Timestamp.now())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("posts").document(postId).collection("comments").add(newComment).await()
                loadComments(postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
