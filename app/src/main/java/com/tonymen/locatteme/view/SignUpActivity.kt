package com.tonymen.locatteme.view

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivitySignUpBinding
import com.tonymen.locatteme.viewmodel.SignUpViewModel
import com.tonymen.locatteme.model.User

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isGoogleSignUp = false
    private var googleIdToken: String? = null
    private var isPasswordVisible = false
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInButton.setOnClickListener {
            isGoogleSignUp = true
            signInWithGoogle()
        }

        binding.registerButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val username = binding.usernameEditText.text.toString()
            val age = binding.ageEditText.text.toString().toIntOrNull()
            val idNumber = binding.idNumberEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()

            if (validateInput(firstName, lastName, username, age, idNumber, email, password, confirmPassword, phone)) {
                if (isGoogleSignUp) {
                    // Registro con cuenta de Google
                    googleIdToken?.let {
                        registerUserWithGoogle(firstName, lastName, username, age!!, idNumber, email, password, phone, it)
                    } ?: Toast.makeText(this, "Error de autenticación con Google.", Toast.LENGTH_SHORT).show()
                } else {
                    if (email.endsWith("@gmail.com")) {
                        // Verificar si la cuenta de Google ya existe
                        silentGoogleSignIn(email, firstName, lastName, username, age!!, idNumber, password, phone)
                    } else {
                        // Registrar como cuenta normal
                        registerUser(firstName, lastName, username, age!!, idNumber, email, password, phone)
                    }
                }
            }
        }

        binding.showHidePasswordButton.setOnClickListener {
            if (isPasswordVisible) {
                binding.passwordEditText.inputType = 129
                binding.showHidePasswordButton.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.passwordEditText.inputType = 144
                binding.showHidePasswordButton.setImageResource(R.drawable.ic_eye)
            }
            isPasswordVisible = !isPasswordVisible
            binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
        }
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
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    googleIdToken = account.idToken
                    populateGoogleUserFields(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateGoogleUserFields(account: GoogleSignInAccount) {
        val firstName = account.givenName ?: ""
        val lastName = account.familyName ?: ""
        val email = account.email ?: ""

        binding.firstNameEditText.setText(firstName)
        binding.lastNameEditText.setText(lastName)
        binding.emailEditText.setText(email)
        binding.emailEditText.isEnabled = false
    }

    private fun silentGoogleSignIn(email: String, firstName: String, lastName: String, username: String, age: Int, idNumber: String, password: String, phone: String) {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && account.email == email) {
            googleIdToken = account.idToken
            checkGoogleAccountExistence(firstName, lastName, username, age, idNumber, email, password, phone)
        } else {
            googleSignInClient.silentSignIn().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val account = task.result
                    googleIdToken = account?.idToken
                    checkGoogleAccountExistence(firstName, lastName, username, age, idNumber, email, password, phone)
                } else {
                    registerUser(firstName, lastName, username, age, idNumber, email, password, phone)
                }
            }
        }
    }

    private fun checkGoogleAccountExistence(firstName: String, lastName: String, username: String, age: Int, idNumber: String, email: String, password: String, phone: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods.isNullOrEmpty()) {
                        // Si no hay métodos de inicio de sesión, significa que no hay cuenta existente.
                        googleIdToken?.let {
                            registerUserWithGoogle(firstName, lastName, username, age, idNumber, email, password, phone, it)
                        } ?: Toast.makeText(this, "Error de autenticación con Google.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Si hay métodos de inicio de sesión, significa que la cuenta ya existe.
                        Toast.makeText(this, "Este correo ya está registrado. Inicia sesión.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error al verificar la cuenta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUserWithGoogle(firstName: String, lastName: String, username: String, age: Int, idNumber: String, email: String, password: String, phone: String, idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        user.linkWithCredential(credential)
                            .addOnCompleteListener { linkTask ->
                                if (linkTask.isSuccessful) {
                                    val newUser = User(user.uid, firstName, lastName, username, age, idNumber, email, phone)
                                    viewModel.addUser(newUser)
                                    sendEmailVerification()
                                } else {
                                    Toast.makeText(this, "Error al vincular Google como proveedor: ${linkTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Este correo ya está registrado. Inicia sesión.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al registrarse. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registro exitoso. Por favor, verifica tu correo electrónico.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al enviar el correo de verificación.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(firstName: String, lastName: String, username: String, age: Int, idNumber: String, email: String, password: String, phone: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = User(auth.currentUser!!.uid, firstName, lastName, username, age, idNumber, email, phone)
                    viewModel.addUser(user)
                    sendEmailVerification()
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Este correo ya está registrado. Inicia sesión.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al registrarse. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun validateInput(firstName: String, lastName: String, username: String, age: Int?, idNumber: String, email: String, password: String, confirmPassword: String, phone: String): Boolean {
        if (firstName.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu nombre.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (lastName.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu apellido.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu nombre de usuario.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (age == null || age <= 0) {
            Toast.makeText(this, "Por favor, ingresa una edad válida.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isValidIdNumber(idNumber)) {
            Toast.makeText(this, "Por favor, ingresa una cédula de identidad válida.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, ingresa un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty() || !isValidPassword(password)) {
            Toast.makeText(this, "La contraseña debe tener entre 8 y 16 caracteres, solo letras y números.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, "Por favor, ingresa un número de teléfono válido.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun isValidIdNumber(idNumber: String): Boolean {
        if (idNumber.length != 10) {
            return false
        }

        val coefVal = arrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)
        val verifier = idNumber[9].toString().toInt()
        var sum = 0

        for (i in 0..8) {
            var digit = idNumber[i].toString().toInt() * coefVal[i]
            if (digit >= 10) {
                digit -= 9
            }
            sum += digit
        }

        val modulo = sum % 10
        val result = if (modulo == 0) 0 else 10 - modulo

        return result == verifier
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,16}$"
        return password.matches(passwordRegex.toRegex())
    }

    override fun onStop() {
        super.onStop()
        clearFormFields()
    }

    private fun clearFormFields() {
        binding.firstNameEditText.setText("")
        binding.lastNameEditText.setText("")
        binding.usernameEditText.setText("")
        binding.ageEditText.setText("")
        binding.idNumberEditText.setText("")
        binding.emailEditText.setText("")
        binding.emailEditText.isEnabled = true
        binding.passwordEditText.setText("")
        binding.confirmPasswordEditText.setText("")
        binding.phoneEditText.setText("")
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
