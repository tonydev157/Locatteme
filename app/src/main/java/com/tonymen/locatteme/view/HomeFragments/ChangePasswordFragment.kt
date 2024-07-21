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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        binding.changePasswordButton.setOnClickListener {
            val currentPassword = binding.currentPasswordEditText.text.toString()
            val newPassword = binding.newPasswordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (validateInput(currentPassword, newPassword, confirmPassword)) {
                changePassword(currentPassword, newPassword)
            }
        }

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.showHideCurrentPasswordButton.setOnClickListener {
            if (isCurrentPasswordVisible) {
                binding.currentPasswordEditText.inputType = 129
                binding.showHideCurrentPasswordButton.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.currentPasswordEditText.inputType = 144
                binding.showHideCurrentPasswordButton.setImageResource(R.drawable.ic_eye)
            }
            isCurrentPasswordVisible = !isCurrentPasswordVisible
            binding.currentPasswordEditText.setSelection(binding.currentPasswordEditText.text.length)
        }

        binding.showHideNewPasswordButton.setOnClickListener {
            if (isNewPasswordVisible) {
                binding.newPasswordEditText.inputType = 129
                binding.showHideNewPasswordButton.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.newPasswordEditText.inputType = 144
                binding.showHideNewPasswordButton.setImageResource(R.drawable.ic_eye)
            }
            isNewPasswordVisible = !isNewPasswordVisible
            binding.newPasswordEditText.setSelection(binding.newPasswordEditText.text.length)
        }

        setupRealTimeValidation()
        return binding.root
    }

    private fun validateInput(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        var isValid = true

        if (currentPassword.isEmpty()) {
            binding.currentPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.currentPasswordEditText.error = "Contraseña actual requerida"
            isValid = false
        } else {
            binding.currentPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (newPassword.isEmpty() || !isValidPassword(newPassword)) {
            binding.newPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.newPasswordEditText.error = "Contraseña inválida"
            isValid = false
        } else {
            binding.newPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        if (newPassword != confirmPassword) {
            binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
            binding.confirmPasswordEditText.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
        }

        return isValid
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,16}$"
        return password.matches(passwordRegex.toRegex())
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        showLoading(true)
        val user = auth.currentUser
        if (user != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        showLoading(false)
                        if (updateTask.isSuccessful) {
                            Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            requireActivity().onBackPressed()
                        } else {
                            Toast.makeText(requireContext(), "Error al actualizar la contraseña: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    showLoading(false)
                    binding.currentPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                    binding.currentPasswordEditText.error = "Contraseña actual incorrecta"
                    Toast.makeText(requireContext(), "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.changePasswordButton.isEnabled = !isLoading
        binding.cancelButton.isEnabled = !isLoading
    }

    private fun setupRealTimeValidation() {
        binding.currentPasswordEditText.addTextChangedListener(createTextWatcher(binding.currentPasswordEditText) { text ->
            if (text.isNotEmpty()) {
                binding.currentPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.currentPasswordEditText.error = null
            } else {
                binding.currentPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.currentPasswordEditText.error = "Contraseña actual requerida"
            }
        })

        binding.newPasswordEditText.addTextChangedListener(createTextWatcher(binding.newPasswordEditText) { text ->
            if (isValidPassword(text)) {
                binding.newPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.newPasswordEditText.error = null
            } else {
                binding.newPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.newPasswordEditText.error = "Contraseña inválida"
            }
        })

        binding.confirmPasswordEditText.addTextChangedListener(createTextWatcher(binding.confirmPasswordEditText) { text ->
            if (text == binding.newPasswordEditText.text.toString()) {
                binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_valid)
                binding.confirmPasswordEditText.error = null
            } else {
                binding.confirmPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                binding.confirmPasswordEditText.error = "Las contraseñas no coinciden"
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
