package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentUserProfileBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.Follow
import com.tonymen.locatteme.view.adapters.UserPostsAdapter
import com.tonymen.locatteme.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var adapter: UserPostsAdapter
    private lateinit var userId: String
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        userId = arguments?.getString("userId") ?: ""
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        setupRecyclerView()
        loadUserProfile()
        loadUserPosts()
        setupClickListeners()
        checkIfFollowing()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = UserPostsAdapter(emptyList(), parentFragmentManager)
        binding.recyclerViewUserPosts.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerViewUserPosts.adapter = adapter
    }

    private fun loadUserProfile() {
        if (userId.isEmpty()) return

        lifecycleScope.launch {
            try {
                val document = profileViewModel.getUser(userId)
                if (document != null && isAdded) {
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
                        Glide.with(this@UserProfileFragment)
                            .load(profileImageUrl)
                            .circleCrop()
                            .into(binding.profileImageView)
                    }

                    binding.followButton.visibility = if (userId != currentUserId) View.VISIBLE else View.GONE
                } else {
                    showToast("Documento de perfil no encontrado")
                }
            } catch (e: CancellationException) {
                // Manejar la cancelación de la coroutine
            } catch (exception: Exception) {
                showToast("Error al cargar el perfil: ${exception.message}")
            }
        }
    }

    private fun loadUserPosts() {
        if (userId.isEmpty()) return

        lifecycleScope.launch {
            try {
                val documents = profileViewModel.getUserPosts(userId)
                if (isAdded) {
                    val posts = documents.mapNotNull { it.toObject(Post::class.java) }
                    adapter.updatePosts(posts)
                    binding.postsCount.text = posts.size.toString() // Actualizar el contador de publicaciones
                }
            } catch (e: CancellationException) {
                // Manejar la cancelación de la coroutine
            } catch (exception: Exception) {
                showToast("Error al cargar las publicaciones: ${exception.message}")
            }
        }
    }

    private fun setupClickListeners() {
        binding.followersLayout.setOnClickListener {
            val followersNFragment = FollowersNFragment.newInstance(userId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, followersNFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.followingLayout.setOnClickListener {
            val followingFragment = FollowingNFragment.newInstance(userId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, followingFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.followButton.setOnClickListener {
            handleFollowButtonClick()
        }

        binding.messageButton.setOnClickListener {
            openChatFragment()
        }
    }

    private fun openChatFragment() {
        val chatId = generateChatId(currentUserId, userId)
        val chatFragment = ChatFragment.newInstance(chatId, currentUserId, userId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, chatFragment)
            .addToBackStack(null)
            .commit()
    }


    private fun checkIfFollowing() {
        val db = FirebaseFirestore.getInstance()
        val followsRef = db.collection("follows")

        followsRef.whereEqualTo("followerId", currentUserId)
            .whereEqualTo("followedId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (isAdded) {
                    binding.followButton.text = if (querySnapshot.isEmpty) "Seguir" else "Siguiendo"
                }
            }.addOnFailureListener { exception ->
                if (isAdded) {
                    showToast("Error al verificar si sigue: ${exception.message}")
                }
            }
    }

    private fun handleFollowButtonClick() {
        showProgressBar(true) // Mostrar el ProgressBar cuando se hace clic en el botón
        if (binding.followButton.text == "Seguir") {
            followUser()
        } else {
            unfollowUser()
        }
    }

    private fun followUser() {
        val db = FirebaseFirestore.getInstance()
        val followsRef = db.collection("follows")
        val userRef = db.collection("users").document(currentUserId)
        val followedUserRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val follow = Follow(
                followerId = currentUserId,
                followedId = userId
            )
            followsRef.add(follow)

            transaction.update(userRef, "seguidos", FieldValue.arrayUnion(userId))
            transaction.update(followedUserRef, "seguidores", FieldValue.arrayUnion(currentUserId))
        }.addOnSuccessListener {
            if (isAdded) {
                binding.followButton.text = "Siguiendo"
                updateFollowersCount(1)
                showProgressBar(false) // Ocultar el ProgressBar después de completar la acción
            }
        }.addOnFailureListener { exception ->
            if (isAdded) {
                showToast("Error al seguir: ${exception.message}")
                showProgressBar(false) // Ocultar el ProgressBar en caso de error
            }
        }
    }

    private fun unfollowUser() {
        val db = FirebaseFirestore.getInstance()
        val followsRef = db.collection("follows")
        val userRef = db.collection("users").document(currentUserId)
        val followedUserRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            followsRef.whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followedId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        followsRef.document(document.id).delete()
                    }
                }

            transaction.update(userRef, "seguidos", FieldValue.arrayRemove(userId))
            transaction.update(followedUserRef, "seguidores", FieldValue.arrayRemove(currentUserId))
        }.addOnSuccessListener {
            if (isAdded) {
                binding.followButton.text = "Seguir"
                updateFollowersCount(-1)
                showProgressBar(false) // Ocultar el ProgressBar después de completar la acción
            }
        }.addOnFailureListener { exception ->
            if (isAdded) {
                showToast("Error al dejar de seguir: ${exception.message}")
                showProgressBar(false) // Ocultar el ProgressBar en caso de error
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        if (isAdded) {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            setInteractionEnabled(!show)
        }
    }

    private fun setInteractionEnabled(enabled: Boolean) {
        binding.followButton.isEnabled = enabled
        binding.messageButton.isEnabled = enabled
        binding.followersLayout.isEnabled = enabled
        binding.followingLayout.isEnabled = enabled
    }

    private fun updateFollowersCount(delta: Int) {
        if (isAdded) {
            val currentCount = binding.followersCount.text.toString().toInt()
            binding.followersCount.text = (currentCount + delta).toString()
        }
    }

    private fun generateChatId(userId1: String, userId2: String): String {
        val ids = listOf(userId1, userId2).sorted()
        return "${ids[0]}_${ids[1]}"
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: String) = UserProfileFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
    }
}
