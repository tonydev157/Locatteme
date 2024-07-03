package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentUserProfileBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.view.adapters.UserPostsAdapter
import com.tonymen.locatteme.viewmodel.ProfileViewModel

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var adapter: UserPostsAdapter
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        userId = arguments?.getString("userId") ?: ""

        setupRecyclerView()
        loadUserProfile()
        loadUserPosts()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = UserPostsAdapter(emptyList(), requireContext())
        binding.recyclerViewUserPosts.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerViewUserPosts.adapter = adapter
    }

    private fun loadUserProfile() {
        if (userId.isEmpty()) return

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
        if (userId.isEmpty()) return

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: String): UserProfileFragment {
            val fragment = UserProfileFragment()
            val args = Bundle()
            args.putString("userId", userId)
            fragment.arguments = args
            return fragment
        }
    }
}
