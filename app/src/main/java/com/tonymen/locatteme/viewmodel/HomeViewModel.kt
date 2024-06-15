package com.tonymen.locatteme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Post

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    fun addPost(post: Post) {
        db.collection("posts").document(post.id).set(post)
            .addOnSuccessListener { Log.d("Firestore", "Post added successfully!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding post", e) }
    }
}
