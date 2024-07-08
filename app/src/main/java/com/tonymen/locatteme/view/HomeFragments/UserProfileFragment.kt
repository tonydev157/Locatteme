package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentUserProfileBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.Follow
import com.tonymen.locatteme.view.FollowersNFragment
import com.tonymen.locatteme.view.FollowingNFragment
import com.tonymen.locatteme.view.adapters.UserPostsAdapter
import com.tonymen.locatteme.viewmodel.ProfileViewModel

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

        profileViewModel.getUser(userId).addOnSuccessListener { document ->
            if (_binding != null && document != null) {
                Log.d("UserProfileFragment", "Document data: ${document.data}")

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

                if (userId != currentUserId) {
                    binding.followButton.visibility = View.VISIBLE
                } else {
                    binding.followButton.visibility = View.GONE
                }
            } else {
                Log.e("UserProfileFragment", "Document is null or binding is null")
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
    }

    private fun checkIfFollowing() {
        val db = FirebaseFirestore.getInstance()
        val followsRef = db.collection("follows")

        followsRef.whereEqualTo("followerId", currentUserId)
            .whereEqualTo("followedId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    binding.followButton.text = "Siguiendo"
                } else {
                    binding.followButton.text = "Seguir"
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, "Error al verificar si sigue: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleFollowButtonClick() {
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
            binding.followButton.text = "Siguiendo"
            updateFollowersCount(1)
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Error al seguir: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unfollowUser() {
        val db = FirebaseFirestore.getInstance()
        val followsRef = db.collection("follows")
        val userRef = db.collection("users").document(currentUserId)
        val followedUserRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val followQuery = followsRef.whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followedId", userId)
                .get()
            followQuery.addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    followsRef.document(document.id).delete()
                }
            }

            transaction.update(userRef, "seguidos", FieldValue.arrayRemove(userId))
            transaction.update(followedUserRef, "seguidores", FieldValue.arrayRemove(currentUserId))
        }.addOnSuccessListener {
            binding.followButton.text = "Seguir"
            updateFollowersCount(-1)
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Error al dejar de seguir: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFollowersCount(delta: Int) {
        val currentCount = binding.followersCount.text.toString().toInt()
        binding.followersCount.text = (currentCount + delta).toString()
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
