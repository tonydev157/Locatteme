package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.tonymen.locatteme.model.User
import com.google.firebase.firestore.FirebaseFirestore

class SignUpViewModel : ViewModel() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun addUser(user: User) {
        db.collection("users").document(user.id).set(user)
            .addOnSuccessListener { /* manejo de éxito */ }
            .addOnFailureListener { /* manejo de error */ }
    }
}
