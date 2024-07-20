package com.tonymen.locatteme.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.databinding.FragmentDeleteAccountBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DeleteAccountFragment : Fragment() {

    private var _binding: FragmentDeleteAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeleteAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.deleteButton.setOnClickListener {
            val password = binding.passwordEditText.text.toString()
            if (password.isNotEmpty()) {
                deleteAccount(password)
            } else {
                Toast.makeText(requireContext(), "Ingrese la contraseña", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser
        if (user != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)

            user.reauthenticate(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val userId = user.uid
                            db.collection("users").document(userId).delete().await()
                            user.delete().await()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Cuenta eliminada con éxito", Toast.LENGTH_SHORT).show()
                                // Redirige al usuario a la pantalla de inicio de sesión o cierra la sesión
                                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                startActivity(intent)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Log.e("DeleteAccountFragment", "Error eliminando la cuenta", e)
                                Toast.makeText(requireContext(), "Error al eliminar la cuenta: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Autenticación fallida: contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
