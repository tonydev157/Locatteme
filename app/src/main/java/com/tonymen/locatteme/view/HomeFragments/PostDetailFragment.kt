package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentPostDetailBinding
import com.tonymen.locatteme.utils.TimestampUtil
import android.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var postId: String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        postId = arguments?.getString("postId")
        Log.d("PostDetailFragment", "Post ID in onViewCreated: $postId")

        val cleanedPostId = postId?.trim()
        Log.d("PostDetailFragment", "Cleaned Post ID: $cleanedPostId")

        if (cleanedPostId.isNullOrEmpty()) {
            Log.e("PostDetailFragment", "Post ID is null or empty after cleaning!")
            return
        }

        val fotoGrande = arguments?.getString("fotoGrande")
        val nombres = arguments?.getString("nombres")
        val apellidos = arguments?.getString("apellidos")
        val edad = arguments?.getInt("edad", 0)
        val provincia = arguments?.getString("provincia")
        val ciudad = arguments?.getString("ciudad")
        val nacionalidad = arguments?.getString("nacionalidad")
        val estado = arguments?.getString("estado")
        val lugarDesaparicion = arguments?.getString("lugarDesaparicion")
        val fechaDesaparicionStr = arguments?.getString("fechaDesaparicion")
        val caracteristicas = arguments?.getString("caracteristicas")
        val autorId = arguments?.getString("autorId")
        val fechaPublicacionStr = arguments?.getString("fechaPublicacion")
        val numerosContacto = arguments?.getStringArrayList("numerosContacto") ?: arrayListOf()

        Log.d("PostDetailFragment", "Números de contacto: $numerosContacto")

        val fechaDesaparicion = TimestampUtil.parseStringToTimestamp(fechaDesaparicionStr)
        val fechaPublicacion = TimestampUtil.parseStringToTimestamp(fechaPublicacionStr)

        db.collection("users").document(autorId!!).get().addOnSuccessListener { document ->
            val autorNombre = document.getString("nombre") ?: "Desconocido"
            binding.publicadoPorTextView.text = "Publicado por $autorNombre el ${TimestampUtil.formatTimestampToString(fechaPublicacion)}"
        }

        binding.apply {
            Glide.with(this@PostDetailFragment)
                .load(fotoGrande)
                .into(photoImageView)

            nombreTextView.text = nombres
            apellidosTextView.text = apellidos
            edadTextView.text = edad.toString()
            provinciaTextView.text = provincia
            ciudadTextView.text = ciudad
            nacionalidadTextView.text = nacionalidad
            estadoTextView.text = estado
            lugarDesaparicionTextView.text = lugarDesaparicion
            fechaDesaparicionTextView.text = TimestampUtil.formatTimestampToString(fechaDesaparicion)
            caracteristicasTextView.text = caracteristicas

            // Aquí configuramos el TextView de numeros de contacto
            numerosContactoTextView.text = numerosContacto.joinToString(", ") { it }
        }

        if (autorId == auth.currentUser?.uid) {
            binding.editButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.VISIBLE
        } else {
            binding.editButton.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
        }

        binding.editButton.setOnClickListener {
            openEditPostFragment()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.commentsButton.setOnClickListener {
            openCommentsFragment()
        }
    }


    private fun openEditPostFragment() {
        if (postId.isNullOrEmpty()) {
            Log.e("PostDetailFragment", "Post ID is null or empty!")
        } else {
            Log.d("PostDetailFragment", "Passing Post ID: $postId to EditPostFragment")
            val fragment = EditPostFragment().apply {
                arguments = Bundle().apply {
                    putString("postId", postId)
                }
            }
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, fragment)
                addToBackStack(null)
            }
        }
    }

    private fun openCommentsFragment() {
        val fragment = PostCommentsFragment().apply {
            arguments = Bundle().apply {
                putString("postId", postId)
            }
        }
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment)
            addToBackStack(null)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Está seguro de eliminar esta publicación?")
            .setPositiveButton("Confirmar") { _, _ ->
                deletePost()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletePost() {
        if (postId.isNullOrEmpty()) {
            Log.e("PostDetailFragment", "Post ID is null or empty!")
            return
        }

        val cleanedPostId = postId!!.trim()
        Log.d("PostDetailFragment", "Attempting to delete post with ID: $cleanedPostId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("PostDetailFragment", "Fetching document with ID: $cleanedPostId")
                val querySnapshot = db.collection("posts")
                    .whereEqualTo("id", cleanedPostId)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val documentRef = querySnapshot.documents[0].reference
                    Log.d("PostDetailFragment", "Document exists, attempting to delete")
                    documentRef.delete().await()
                    withContext(Dispatchers.Main) {
                        Log.d("PostDetailFragment", "Post deleted successfully")
                        Toast.makeText(requireContext(), "Publicación eliminada con éxito", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("PostDetailFragment", "Document does not exist!")
                        Toast.makeText(requireContext(), "El documento no existe.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PostDetailFragment", "Error deleting post", e)
                    Toast.makeText(requireContext(), "Error al eliminar la publicación: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
