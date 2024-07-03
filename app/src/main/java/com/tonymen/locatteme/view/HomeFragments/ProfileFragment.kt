package com.tonymen.locatteme.view.HomeFragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentProfileBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.view.adapters.UserPostsAdapter
import com.tonymen.locatteme.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var adapter: UserPostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        setupRecyclerView()
        loadUserProfile()

        binding.profileImageView.setOnClickListener {
            showChangeProfilePictureDialog()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = UserPostsAdapter(emptyList(), requireContext())
        binding.recyclerViewUserPosts.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerViewUserPosts.adapter = adapter
        loadUserPosts()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        profileViewModel.getUser(userId).addOnSuccessListener { document ->
            if (_binding != null && document != null) {
                val nombre = document.getString("nombre") ?: ""
                val apellido = document.getString("apellido") ?: ""
                val username = document.getString("username") ?: ""
                val profileImageUrl = document.getString("profileImageUrl") ?: ""
                val seguidores = document.get("seguidores") as? List<*>
                val seguidos = document.get("seguidos") as? List<*>

                binding.profileTextView.text = "$nombre $apellido"
                binding.fullNameTextView.text = "@$username"
                binding.followersCount.text = (seguidores?.size ?: 0).toString()
                binding.followingCount.text = (seguidos?.size ?: 0).toString()

                if (profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(profileImageUrl)
                        .circleCrop()
                        .into(binding.profileImageView)
                }
            }
        }.addOnFailureListener { exception ->
            if (_binding != null) {
                Toast.makeText(context, "Error al cargar el perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        profileViewModel.getUserPosts(userId).addOnSuccessListener { documents ->
            val posts = documents.mapNotNull { it.toObject(Post::class.java) }
            adapter.updatePosts(posts)
            binding.postsCount.text = posts.size.toString() // Actualizar el contador de publicaciones
        }.addOnFailureListener { exception ->
            if (_binding != null) {
                Toast.makeText(context, "Error al cargar las publicaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangeProfilePictureDialog() {
        val options = arrayOf("Cambiar foto de perfil")
        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openImagePicker()
            }
        }
        builder.show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            selectedImageUri?.let {
                uploadImageToFirebase(it)
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = profileViewModel.getStorageReference(userId)

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    profileViewModel.updateProfileImageUrl(userId, uri.toString())
                        .addOnSuccessListener {
                            if (_binding != null) {
                                Toast.makeText(context, "Imagen de perfil actualizada", Toast.LENGTH_SHORT).show()
                                loadUserProfile()
                            }
                        }
                        .addOnFailureListener { exception ->
                            if (_binding != null) {
                                Toast.makeText(context, "Error al actualizar la imagen de perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                if (_binding != null) {
                    Toast.makeText(context, "Error al subir la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }
}
