package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentHomeBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.view.adapters.HomePostsAdapter
import com.tonymen.locatteme.viewmodel.HomeFViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: HomeFViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: HomePostsAdapter
    private val posts = mutableListOf<Post>()
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this).get(HomeFViewModel::class.java)

        setupRecyclerView()
        setupSwipeRefresh()
        loadUserProfile()
        loadPosts()

        return binding.root
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerView
        postAdapter = HomePostsAdapter(posts, requireContext())
        recyclerView.adapter = postAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    loadMorePosts()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshContent()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)
            user?.let {
                binding.usernameTextView.text = it.username
                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(it.profileImageUrl)
                        .circleCrop()
                        .into(binding.profileImageView)
                } else {
                    // Si no hay imagen en la base de datos, mostrar imagen predeterminada
                    binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        }.addOnFailureListener {
            // En caso de error, mostrar imagen predeterminada
            binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            Toast.makeText(context, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadPosts() {
        isLoading = true
        viewModel.getPosts(lastVisible).addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                isLoading = false
                return@addOnSuccessListener
            }
            lastVisible = documents.documents[documents.size() - 1]
            val postList = documents.mapNotNull { it.toObject(Post::class.java) }
            posts.addAll(postList)
            postAdapter.notifyDataSetChanged()
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
            Toast.makeText(context, "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMorePosts() {
        loadPosts()
    }

    fun refreshContent() {
        posts.clear()
        lastVisible = null
        loadPosts()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
