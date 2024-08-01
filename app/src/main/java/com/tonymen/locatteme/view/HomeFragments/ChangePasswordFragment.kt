package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentChangePasswordBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                lifecycleScope.launch {
                    changePassword(currentPassword, newPassword)
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.showHideCurrentPasswordButton.setOnClickListener {
            togglePasswordVisibility(binding.currentPasswordEditText, binding.showHideCurrentPasswordButton)
            isCurrentPasswordVisible = !isCurrentPasswordVisible
        }

        binding.showHideNewPasswordButton.setOnClickListener {
            togglePasswordVisibility(binding.newPasswordEditText, binding.showHideNewPasswordButton)
            isNewPasswordVisible = !isNewPasswordVisible
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
        if (password.contains(" ")) {
            return false
        }

        val digitPattern = Regex(".*[0-9].*")
        val lowerCasePattern = Regex(".*[a-z].*")
        val upperCasePattern = Regex(".*[A-Z].*")
        val specialCharPattern = Regex(".*[^a-zA-Z0-9].*")

        return password.length in 8..20 &&
                digitPattern.containsMatchIn(password) &&
                lowerCasePattern.containsMatchIn(password) &&
                upperCasePattern.containsMatchIn(password) &&
                specialCharPattern.containsMatchIn(password)
    }

    private suspend fun changePassword(currentPassword: String, newPassword: String) {
        showLoading(true)
        try {
            val user = auth.currentUser
            if (user != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
        } catch (e: Exception) {
            _binding?.apply {
                currentPasswordEditText.setBackgroundResource(R.drawable.edit_text_invalid)
                currentPasswordEditText.error = "Contraseña actual incorrecta"
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            showLoading(false)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        _binding?.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            changePasswordButton.isEnabled = !isLoading
            cancelButton.isEnabled = !isLoading
            currentPasswordEditText.isEnabled = !isLoading
            newPasswordEditText.isEnabled = !isLoading
            confirmPasswordEditText.isEnabled = !isLoading
            showHideCurrentPasswordButton.isEnabled = !isLoading
            showHideNewPasswordButton.isEnabled = !isLoading

            // Deshabilitar toda la vista
            root.isEnabled = !isLoading
        }
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

    private fun togglePasswordVisibility(editText: EditText, toggleButton: View) {
        if (editText.inputType == 144) {
            editText.inputType = 129
            toggleButton.setBackgroundResource(R.drawable.ic_eye_off)
        } else {
            editText.inputType = 144
            toggleButton.setBackgroundResource(R.drawable.ic_eye)
        }
        editText.setSelection(editText.text.length)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
