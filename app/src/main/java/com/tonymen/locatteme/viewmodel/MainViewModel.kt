package com.tonymen.locatteme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.model.Post

class MainViewModel : ViewModel() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun addUser(user: User) {
        db.collection("users").document(user.id).set(user)
            .addOnSuccessListener { Log.d("Firestore", "User added successfully!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding user", e) }
    }

    fun addPost(post: Post) {
        db.collection("posts").document(post.id).set(post)
            .addOnSuccessListener { Log.d("Firestore", "Post added successfully!") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding post", e) }
    }

    fun getUser(userId: String) = db.collection("users").document(userId).get()

    fun updateProfileImageUrl(userId: String, profileImageUrl: String) =
        db.collection("users").document(userId).update("profileImageUrl", profileImageUrl)
}
