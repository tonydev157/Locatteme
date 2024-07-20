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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentProfileBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.view.AccountOptionsFragment
import com.tonymen.locatteme.view.FollowersNFragment
import com.tonymen.locatteme.view.FollowingNFragment
import com.tonymen.locatteme.view.adapters.UserPostsAdapter
import com.tonymen.locatteme.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var adapter: UserPostsAdapter
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return binding.root
        }

        setupRecyclerView()
        loadUserProfile()

        binding.profileImageView.setOnClickListener {
            showChangeProfilePictureDialog()
        }

        val followersClickListener = View.OnClickListener {
            val fragment = FollowersNFragment.newInstance(userId!!)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        val followingClickListener = View.OnClickListener {
            val fragment = FollowingNFragment.newInstance(userId!!)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.followersCount.setOnClickListener(followersClickListener)
        binding.followingCount.setOnClickListener(followingClickListener)
        binding.followersLabel.setOnClickListener(followersClickListener)
        binding.followingLabel.setOnClickListener(followingClickListener)

        binding.buttonLogout.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AccountOptionsFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = UserPostsAdapter(emptyList(), parentFragmentManager)
        binding.recyclerViewUserPosts.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerViewUserPosts.adapter = adapter
        loadUserPosts()
    }

    private fun loadUserProfile() {
        userId?.let { id ->
            lifecycleScope.launch {
                try {
                    val document = profileViewModel.getUser(id)
                    if (document != null) {
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
                            Glide.with(this@ProfileFragment)
                                .load(profileImageUrl)
                                .circleCrop()
                                .into(binding.profileImageView)
                        }
                    }
                } catch (exception: Exception) {
                    Toast.makeText(context, "Error al cargar el perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserPosts() {
        userId?.let { id ->
            lifecycleScope.launch {
                try {
                    val documents = profileViewModel.getUserPosts(id)
                    val posts = documents.mapNotNull { it.toObject(Post::class.java) }
                    adapter.updatePosts(posts)
                    binding.postsCount.text = posts.size.toString() // Actualizar el contador de publicaciones
                } catch (exception: Exception) {
                    Toast.makeText(context, "Error al cargar las publicaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
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
        userId?.let { id ->
            lifecycleScope.launch {
                try {
                    val storageRef = profileViewModel.getStorageReference(id)
                    storageRef.putFile(imageUri).await()
                    val uri = storageRef.downloadUrl.await()
                    profileViewModel.updateProfileImageUrl(id, uri.toString())
                    Toast.makeText(context, "Imagen de perfil actualizada", Toast.LENGTH_SHORT).show()
                    loadUserProfile()
                } catch (exception: Exception) {
                    Toast.makeText(context, "Error al subir la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
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
