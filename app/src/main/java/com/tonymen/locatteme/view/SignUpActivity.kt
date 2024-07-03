package com.tonymen.locatteme.view

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.QuerySnapshot
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

        // Configuración de validación en tiempo real
        setupRealTimeValidation()
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
                                    user.delete().addOnCompleteListener {
                                        Toast.makeText(this, "Error al vincular Google como proveedor: ${linkTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
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
        checkIfUserDataIsUnique(firstName, lastName, username, age, idNumber, email, phone) { isUnique ->
            binding.progressBar.visibility = View.GONE
            if (isUnique) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
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
        }
    }

    private fun validateInput(firstName: String, lastName: String, username: String, age: Int?, idNumber: String, email: String, password: String, confirmPassword: String, phone: String): Boolean {
        var isValid = true

        if (firstName.isEmpty() || !firstName.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
            binding.firstNameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.firstNameEditText.error = "Nombre inválido"
            isValid = false
        } else {
            binding.firstNameEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (lastName.isEmpty() || !lastName.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
            binding.lastNameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.lastNameEditText.error = "Apellido inválido"
            isValid = false
        } else {
            binding.lastNameEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (username.isEmpty() || !username.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*\$"))) {
            binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.usernameEditText.error = "Username inválido"
            isValid = false
        } else {
            binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (age == null || age <= 0 || age > 100) {
            binding.ageEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.ageEditText.error = "Edad inválida"
            isValid = false
        } else {
            binding.ageEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (!isValidIdNumber(idNumber)) {
            binding.idNumberEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.idNumberEditText.error = "Cédula no válida"
            isValid = false
        } else {
            binding.idNumberEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.emailEditText.error = "Correo inválido"
            isValid = false
        } else {
            binding.emailEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (password.isEmpty() || !isValidPassword(password)) {
            binding.passwordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.passwordEditText.error = "Contraseña inválida"
            isValid = false
        } else {
            binding.passwordEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.confirmPasswordEditText.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (phone.length != 10 || !phone.matches(Regex("^09[0-9]{8}\$"))) {
            binding.phoneEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.phoneEditText.error = "Teléfono inválido"
            isValid = false
        } else {
            binding.phoneEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        return isValid
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

    private fun setupRealTimeValidation() {
        // Validación en tiempo real para Nombre
        binding.firstNameEditText.addTextChangedListener(createTextWatcher(binding.firstNameEditText) { text ->
            if (text.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
                binding.firstNameEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.firstNameEditText.error = null
            } else {
                binding.firstNameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.firstNameEditText.error = "Nombre inválido"
            }
        })

        // Validación en tiempo real para Apellido
        binding.lastNameEditText.addTextChangedListener(createTextWatcher(binding.lastNameEditText) { text ->
            if (text.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
                binding.lastNameEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.lastNameEditText.error = null
            } else {
                binding.lastNameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.lastNameEditText.error = "Apellido inválido"
            }
        })

        // Validación en tiempo real para Username
        binding.usernameEditText.addTextChangedListener(createTextWatcher(binding.usernameEditText) { text ->
            if (text.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*\$"))) {
                binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.usernameEditText.error = null
            } else {
                binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.usernameEditText.error = "Username inválido"
            }
        })

        // Validación en tiempo real para Edad
        binding.ageEditText.addTextChangedListener(createTextWatcher(binding.ageEditText) { text ->
            val age = text.toIntOrNull()
            if (age != null && age in 1..100) {
                binding.ageEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.ageEditText.error = null
            } else {
                binding.ageEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.ageEditText.error = "Edad inválida"
            }
        })

        // Validación en tiempo real para Cédula
        binding.idNumberEditText.addTextChangedListener(createTextWatcher(binding.idNumberEditText) { text ->
            if (isValidIdNumber(text)) {
                binding.idNumberEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.idNumberEditText.error = null
            } else {
                binding.idNumberEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.idNumberEditText.error = "Cédula no válida"
            }
        })

        // Validación en tiempo real para Email
        binding.emailEditText.addTextChangedListener(createTextWatcher(binding.emailEditText) { text ->
            if (Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
                binding.emailEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.emailEditText.error = null
            } else {
                binding.emailEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.emailEditText.error = "Correo inválido"
            }
        })

        // Validación en tiempo real para Contraseña
        binding.passwordEditText.addTextChangedListener(createTextWatcher(binding.passwordEditText) { text ->
            if (isValidPassword(text)) {
                binding.passwordEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.passwordEditText.error = null
            } else {
                binding.passwordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.passwordEditText.error = "Contraseña inválida"
            }
        })

        // Validación en tiempo real para Confirmación de Contraseña
        binding.confirmPasswordEditText.addTextChangedListener(createTextWatcher(binding.confirmPasswordEditText) { text ->
            if (text == binding.passwordEditText.text.toString()) {
                binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.confirmPasswordEditText.error = null
            } else {
                binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.confirmPasswordEditText.error = "Las contraseñas no coinciden"
            }
        })

        // Validación en tiempo real para Teléfono
        binding.phoneEditText.addTextChangedListener(createTextWatcher(binding.phoneEditText) { text ->
            if (text.matches(Regex("^09[0-9]{8}\$"))) {
                binding.phoneEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.phoneEditText.error = null
            } else {
                binding.phoneEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.phoneEditText.error = "Teléfono inválido"
            }
        })
    }

    private fun createTextWatcher(editText: EditText, validation: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validation(s.toString())
            }
        }
    }

    private fun checkIfUserDataIsUnique(
        firstName: String, lastName: String, username: String, age: Int,
        idNumber: String, email: String, phone: String,
        callback: (Boolean) -> Unit) {

        val db = Firebase.firestore

        val usernameCheck = db.collection("users").whereEqualTo("username", username).get()
        val idNumberCheck = db.collection("users").whereEqualTo("cedulaIdentidad", idNumber).get()
        val phoneCheck = db.collection("users").whereEqualTo("telefono", phone).get()
        val emailCheck = db.collection("users").whereEqualTo("correoElectronico", email).get()

        Tasks.whenAllSuccess<QuerySnapshot>(usernameCheck, idNumberCheck, phoneCheck, emailCheck)
            .addOnSuccessListener { results ->
                var isUnique = true

                if (!results[0].isEmpty) {
                    binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                    binding.usernameEditText.error = "El nombre de usuario ya está en uso."
                    isUnique = false
                }

                if (!results[1].isEmpty) {
                    binding.idNumberEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                    binding.idNumberEditText.error = "La cédula ya está en uso."
                    isUnique = false
                }

                if (!results[2].isEmpty) {
                    binding.phoneEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                    binding.phoneEditText.error = "El teléfono ya está en uso."
                    isUnique = false
                }

                if (!results[3].isEmpty) {
                    binding.emailEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                    binding.emailEditText.error = "El correo ya está en uso."
                    isUnique = false
                }

                callback(isUnique)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al verificar los datos: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
