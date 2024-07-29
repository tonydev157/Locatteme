package com.tonymen.locatteme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Post
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    fun addPost(post: Post) {
        viewModelScope.launch {
            try {
                db.collection("posts").document(post.id).set(post)
                    .addOnSuccessListener { Log.d("Firestore", "Post added successfully!") }
                    .addOnFailureListener { e -> Log.w("Firestore", "Error adding post", e) }
            } catch (e: Exception) {
                Log.e("Firestore", "Error adding post: ${e.message}", e)
            }
        }
    }
}
