package com.tonymen.locatteme.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentEditProfileBinding
import com.tonymen.locatteme.model.User

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentUsername: String
    private lateinit var currentCedula: String
    private lateinit var currentTelefono: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        binding.saveButton.setOnClickListener {
            val nombre = binding.nombreEditText.text.toString()
            val apellido = binding.apellidoEditText.text.toString()
            val username = binding.usernameEditText.text.toString()
            val edad = binding.edadEditText.text.toString().toIntOrNull()
            val cedula = binding.cedulaEditText.text.toString()
            val telefono = binding.telefonoEditText.text.toString()

            if (validateInput(nombre, apellido, username, edad, cedula, telefono)) {
                checkIfUserDataIsUnique(username, cedula, telefono) { isUnique ->
                    if (isUnique) {
                        updateUserProfile(nombre, apellido, username, edad!!, cedula, telefono)
                    }
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        setupRealTimeValidation()
        loadUserData()

        return binding.root
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)
            if (user != null) {
                currentUsername = user.username
                currentCedula = user.cedulaIdentidad
                currentTelefono = user.telefono

                binding.nombreEditText.setText(user.nombre)
                binding.apellidoEditText.setText(user.apellido)
                binding.usernameEditText.setText(user.username)
                binding.edadEditText.setText(user.edad.toString())
                binding.cedulaEditText.setText(user.cedulaIdentidad)
                binding.telefonoEditText.setText(user.telefono)
            }
        }
    }

    private fun updateUserProfile(nombre: String, apellido: String, username: String, edad: Int, cedula: String, telefono: String) {
        val userId = auth.currentUser?.uid ?: return

        val userUpdates = mapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "username" to username,
            "edad" to edad,
            "cedulaIdentidad" to cedula,
            "telefono" to telefono
        )

        db.collection("users").document(userId).update(userUpdates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error al actualizar el perfil: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(nombre: String, apellido: String, username: String, edad: Int?, cedula: String, telefono: String): Boolean {
        var isValid = true

        if (nombre.isEmpty() || !nombre.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
            binding.nombreEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.nombreEditText.error = "Nombre inválido"
            isValid = false
        } else {
            binding.nombreEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (apellido.isEmpty() || !apellido.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
            binding.apellidoEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.apellidoEditText.error = "Apellido inválido"
            isValid = false
        } else {
            binding.apellidoEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (username.isEmpty() || !username.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*\$"))) {
            binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.usernameEditText.error = "Username inválido"
            isValid = false
        } else {
            binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (edad == null || edad <= 0 || edad > 100) {
            binding.edadEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.edadEditText.error = "Edad inválida"
            isValid = false
        } else {
            binding.edadEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (!isValidIdNumber(cedula)) {
            binding.cedulaEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.cedulaEditText.error = "Cédula no válida"
            isValid = false
        } else {
            binding.cedulaEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (telefono.length != 10 || !telefono.matches(Regex("^09[0-9]{8}\$"))) {
            binding.telefonoEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.telefonoEditText.error = "Teléfono inválido"
            isValid = false
        } else {
            binding.telefonoEditText.setBackgroundResource(R.drawable.edit_text_valid)
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

    private fun setupRealTimeValidation() {
        binding.nombreEditText.addTextChangedListener(createTextWatcher(binding.nombreEditText) { text ->
            if (text.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
                binding.nombreEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.nombreEditText.error = null
            } else {
                binding.nombreEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.nombreEditText.error = "Nombre inválido"
            }
        })

        binding.apellidoEditText.addTextChangedListener(createTextWatcher(binding.apellidoEditText) { text ->
            if (text.matches(Regex("^[A-Z][a-zA-Z]*\$"))) {
                binding.apellidoEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.apellidoEditText.error = null
            } else {
                binding.apellidoEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.apellidoEditText.error = "Apellido inválido"
            }
        })

        binding.usernameEditText.addTextChangedListener(createTextWatcher(binding.usernameEditText) { text ->
            if (text.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*\$"))) {
                binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.usernameEditText.error = null
            } else {
                binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.usernameEditText.error = "Username inválido"
            }
        })

        binding.edadEditText.addTextChangedListener(createTextWatcher(binding.edadEditText) { text ->
            val edad = text.toIntOrNull()
            if (edad != null && edad in 1..100) {
                binding.edadEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.edadEditText.error = null
            } else {
                binding.edadEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.edadEditText.error = "Edad inválida"
            }
        })

        binding.cedulaEditText.addTextChangedListener(createTextWatcher(binding.cedulaEditText) { text ->
            if (isValidIdNumber(text)) {
                binding.cedulaEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.cedulaEditText.error = null
            } else {
                binding.cedulaEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.cedulaEditText.error = "Cédula no válida"
            }
        })

        binding.telefonoEditText.addTextChangedListener(createTextWatcher(binding.telefonoEditText) { text ->
            if (text.matches(Regex("^09[0-9]{8}\$"))) {
                binding.telefonoEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.telefonoEditText.error = null
            } else {
                binding.telefonoEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.telefonoEditText.error = "Teléfono inválido"
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

    private fun checkIfUserDataIsUnique(username: String, cedula: String, telefono: String, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val usernameCheck = db.collection("users").whereEqualTo("username", username).get()
        val cedulaCheck = db.collection("users").whereEqualTo("cedulaIdentidad", cedula).get()
        val telefonoCheck = db.collection("users").whereEqualTo("telefono", telefono).get()

        usernameCheck.addOnCompleteListener { usernameTask ->
            cedulaCheck.addOnCompleteListener { cedulaTask ->
                telefonoCheck.addOnCompleteListener { telefonoTask ->
                    val isUsernameUnique = usernameTask.result?.isEmpty == true || currentUsername == username
                    val isCedulaUnique = cedulaTask.result?.isEmpty == true || currentCedula == cedula
                    val isTelefonoUnique = telefonoTask.result?.isEmpty == true || currentTelefono == telefono

                    if (!isUsernameUnique) {
                        binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                        binding.usernameEditText.error = "El nombre de usuario ya está en uso."
                    } else {
                        binding.usernameEditText.setBackgroundResource(R.drawable.edit_text_valid)
                        binding.usernameEditText.error = null
                    }

                    if (!isCedulaUnique) {
                        binding.cedulaEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                        binding.cedulaEditText.error = "La cédula ya está en uso."
                    } else {
                        binding.cedulaEditText.setBackgroundResource(R.drawable.edit_text_valid)
                        binding.cedulaEditText.error = null
                    }

                    if (!isTelefonoUnique) {
                        binding.telefonoEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                        binding.telefonoEditText.error = "El teléfono ya está en uso."
                    } else {
                        binding.telefonoEditText.setBackgroundResource(R.drawable.edit_text_valid)
                        binding.telefonoEditText.error = null
                    }

                    callback(isUsernameUnique && isCedulaUnique && isTelefonoUnique)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
