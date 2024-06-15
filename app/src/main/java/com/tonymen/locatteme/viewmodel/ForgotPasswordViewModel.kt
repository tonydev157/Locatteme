package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordViewModel : ViewModel() {
    fun sendPasswordResetEmail(auth: FirebaseAuth, email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Manejo de éxito
                } else {
                    // Manejo de error
                }
            }
    }
}
