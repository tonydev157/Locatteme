package com.tonymen.locatteme.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityLoginBinding
import com.tonymen.locatteme.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Verifica si el usuario ya está autenticado y redirige a HomeActivity si es así
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            navigateToHome()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (validateInput(email, password)) {
                signInUser(email, password)
            }
        }

        binding.createAccountButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.resendVerificationTextView.setOnClickListener {
            binding.resendVerificationProgressBar.visibility = View.VISIBLE
            binding.resendVerificationTextView.isEnabled = false
            resendVerificationEmail()
        }

        // Manejador para la visibilidad de la contraseña
        binding.passwordEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.passwordEditText.right - binding.passwordEditText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.sendVerificationEmailButton.setOnClickListener {
            val intent = Intent(this, SendVerificationEmailActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEditText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val positiveButton = dialogView.findViewById<Button>(R.id.positiveButton)
        val negativeButton = dialogView.findViewById<Button>(R.id.negativeButton)

        val builder = AlertDialog.Builder(this, R.style.TransparentDialog)
        builder.setView(dialogView)

        // Crear un TextView personalizado para el título
        val title = TextView(this)
        title.text = "Restablecer contraseña"
        title.setTextColor(ContextCompat.getColor(this, R.color.primaryColor))
        title.textSize = 20f
        title.setPadding(20, 20, 20, 20)
        title.gravity = Gravity.CENTER
        title.setBackgroundResource(R.drawable.background_gradient) // Aplica el gradiente al título

        builder.setCustomTitle(title)

        val dialog = builder.create()
        dialog.setOnShowListener {
            positiveButton.setOnClickListener {
                val email = emailEditText.text.toString()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    progressBar.visibility = View.VISIBLE
                    positiveButton.isEnabled = false
                    negativeButton.isEnabled = false
                    sendPasswordResetEmail(email, progressBar, dialog)
                } else {
                    showToast("Por favor, ingresa un correo electrónico válido.", 2000)
                }
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun sendPasswordResetEmail(email: String, progressBar: ProgressBar, dialog: AlertDialog) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    showToast("Correo de restablecimiento enviado a $email", 2000, R.color.primaryColor)
                    dialog.dismiss()
                } else {
                    showToast("Error al enviar el correo de restablecimiento.", 2000, R.color.primaryColor)
                }
                dialog.findViewById<Button>(R.id.positiveButton)?.isEnabled = true
                dialog.findViewById<Button>(R.id.negativeButton)?.isEnabled = true
            }
    }

    private fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    binding.resendVerificationProgressBar.visibility = View.GONE
                    binding.resendVerificationTextView.isEnabled = true
                    if (task.isSuccessful) {
                        showToast("Correo de verificación enviado.", 2000, R.color.primaryColor)
                    } else {
                        showToast("Error al enviar el correo de verificación.", 2000, R.color.primaryColor)
                    }
                }
                .addOnFailureListener { exception ->
                    binding.resendVerificationProgressBar.visibility = View.GONE
                    binding.resendVerificationTextView.isEnabled = true
                    showToast("Error: ${exception.message}", 2000, R.color.primaryColor)
                }
        } else {
            binding.resendVerificationProgressBar.visibility = View.GONE
            binding.resendVerificationTextView.isEnabled = true
            showToast("Error: No user is currently signed in.", 2000, R.color.primaryColor)
        }
    }

    private fun showToast(message: String, duration: Int, color: Int) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        val view = toast.view
        val text = view?.findViewById<TextView>(android.R.id.message)
        text?.setTextColor(ContextCompat.getColor(this, color))
        toast.show()

        Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, duration.toLong())
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                checkIfUserExists(account)
            } catch (e: ApiException) {
                showToast("Google sign-in failed: ${e.message}", Toast.LENGTH_LONG + 1000)
            }
        }
    }

    private fun checkIfUserExists(account: GoogleSignInAccount) {
        val email = account.email ?: return
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods?.isNotEmpty() == true) {
                        firebaseAuthWithGoogle(account)
                    } else {
                        showToast("Esta cuenta de Google no está registrada. Por favor, regístrate primero.", Toast.LENGTH_LONG + 1000)
                    }
                } else {
                    showToast("Error al verificar la cuenta de Google.", Toast.LENGTH_LONG + 1000)
                }
            }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.reload()?.addOnCompleteListener {
                        user?.let {
                            // Permitir iniciar sesión sin verificar la cuenta
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    showToast("Firebase authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG + 1000)
                }
            }
    }

    private fun showResendVerificationOption() {
        binding.resendVerificationTextView.visibility = View.VISIBLE
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Por favor, ingresa un correo electrónico válido.", Toast.LENGTH_LONG)
            return false
        }
        if (password.isEmpty()) {
            showToast("Por favor, ingresa una contraseña.", Toast.LENGTH_LONG)
            return false
        }
        return true
    }

    private fun signInUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        showToast("Por favor, verifica tu correo electrónico.", Toast.LENGTH_LONG)
                    }
                } else {
                    showToast("Error al iniciar sesión. Inténtalo de nuevo.", Toast.LENGTH_LONG)
                }
            }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.passwordEditText.inputType = 129 // Tipo de entrada para contraseña oculta
            binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0)
        } else {
            binding.passwordEditText.inputType = 144 // Tipo de entrada para contraseña visible
            binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
        }
        isPasswordVisible = !isPasswordVisible
        // Verifica que el texto no sea nulo antes de acceder a su longitud
        binding.passwordEditText.text?.let {
            binding.passwordEditText.setSelection(it.length)
        }
    }

    private fun showToast(message: String, duration: Int) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()

        Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, duration.toLong())
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
