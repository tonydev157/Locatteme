package com.tonymen.locatteme.view

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.databinding.ActivitySendVerificationEmailBinding

class SendVerificationEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendVerificationEmailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendVerificationEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.sendEmailButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (validateEmail(email)) {
                checkEmailAndSendVerification(email)
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

    private fun checkEmailAndSendVerification(email: String) {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("users")
            .whereEqualTo("correoElectronico", email)
            .get()
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful && !task.result.isEmpty) {
                    sendVerificationEmailToEmail(email)
                } else {
                    Toast.makeText(this, "Correo no registrado. Por favor regístrate primero.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al verificar el correo: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVerificationEmailToEmail(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://www.example.com/verify?uid=${auth.currentUser?.uid}")
            .setHandleCodeInApp(true)
            .setIOSBundleId("com.example.ios")
            .setAndroidPackageName("com.tonymen.locatteme", true, null)
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { sendTask ->
                if (sendTask.isSuccessful) {
                    Toast.makeText(this, "Correo de verificación enviado.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al enviar el correo de verificación: ${sendTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
