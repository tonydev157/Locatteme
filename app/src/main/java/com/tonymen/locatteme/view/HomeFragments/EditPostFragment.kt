package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.databinding.FragmentEditPostBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.utils.TimestampUtil

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var postId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        postId = arguments?.getString("postId") ?: ""
        Log.d("EditPostFragment", "Received Post ID: $postId")

        if (postId.isNotEmpty()) {
            loadPostData(postId)
        } else {
            Toast.makeText(requireContext(), "Error al cargar el post", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener {
            savePostData()
        }
    }

    private fun loadPostData(postId: String) {
        Log.d("EditPostFragment", "Loading post data for ID: $postId")
        db.collection("posts").whereEqualTo("id", postId).get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d("EditPostFragment", "No document exists with ID: $postId")
                Toast.makeText(requireContext(), "No existe el post", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            for (document in documents) {
                val post = document.toObject(Post::class.java)
                Log.d("EditPostFragment", "Document snapshot: $document")
                if (post != null) {
                    binding.apply {
                        Glide.with(this@EditPostFragment)
                            .load(post.fotoGrande)
                            .into(photoImageView)

                        nombresEditText.setText(post.nombres)
                        apellidosEditText.setText(post.apellidos)
                        edadEditText.setText(post.edad.toString())
                        provinciaEditText.setText(post.provincia)
                        ciudadEditText.setText(post.ciudad)
                        nacionalidadEditText.setText(post.nacionalidad)
                        estadoEditText.setText(post.estado)
                        lugarDesaparicionEditText.setText(post.lugarDesaparicion)
                        fechaDesaparicionEditText.setText(TimestampUtil.formatTimestampToString(post.fechaDesaparicion))
                        caracteristicasEditText.setText(post.caracteristicas)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al cargar los datos del post", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("EditPostFragment", "Error al cargar el post: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al cargar el post: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePostData() {
        val postUpdates = mapOf(
            "nombres" to binding.nombresEditText.text.toString(),
            "apellidos" to binding.apellidosEditText.text.toString(),
            "edad" to binding.edadEditText.text.toString().toIntOrNull(),
            "provincia" to binding.provinciaEditText.text.toString(),
            "ciudad" to binding.ciudadEditText.text.toString(),
            "nacionalidad" to binding.nacionalidadEditText.text.toString(),
            "estado" to binding.estadoEditText.text.toString(),
            "lugarDesaparicion" to binding.lugarDesaparicionEditText.text.toString(),
            "fechaDesaparicion" to TimestampUtil.parseStringToTimestamp(binding.fechaDesaparicionEditText.text.toString()),
            "caracteristicas" to binding.caracteristicasEditText.text.toString()
        )

        db.collection("posts").document(postId).update(postUpdates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Post actualizado", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error al actualizar el post: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
