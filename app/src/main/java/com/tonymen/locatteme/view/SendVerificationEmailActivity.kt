package com.tonymen.locatteme.view

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.databinding.ActivitySendVerificationEmailBinding

class SendVerificationEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendVerificationEmailBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendVerificationEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.sendEmailButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            if (validateEmail(email)) {
                sendVerificationEmail(email)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, ingresa un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun sendVerificationEmail(email: String) {
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (signInMethods?.isNotEmpty() == true) {
                    val user = auth.currentUser
                    if (user != null && user.email == email) {
                        user.sendEmailVerification().addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(this, "Correo de verificación enviado.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Error al enviar el correo de verificación.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Correo no registrado. Por favor regístrate primero.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Correo no registrado. Por favor regístrate primero.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error al verificar el correo.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
