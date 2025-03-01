package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentFollowingBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.view.adapters.FollowingPostsAdapter
import com.tonymen.locatteme.viewmodel.FollowingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FollowingFragment : Fragment() {

    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: FollowingViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: FollowingPostsAdapter
    private val posts = mutableListOf<Post>()
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false
    private var allPostsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this).get(FollowingViewModel::class.java)

        setupRecyclerView()
        setupSwipeRefresh()
        loadUserProfile()
        loadPosts()

        return binding.root
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerView
        postAdapter = FollowingPostsAdapter(posts, requireContext())
        recyclerView.adapter = postAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && !isLoading && !allPostsLoaded) {
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()
                val user = document.toObject(User::class.java)
                withContext(Dispatchers.Main) {
                    user?.let {
                        binding.usernameTextView.text = it.username
                        if (!it.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@FollowingFragment)
                                .load(it.profileImageUrl)
                                .circleCrop()
                                .into(binding.profileImageView)
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Manejo de la cancelación de la corrutina si es necesario
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                    Toast.makeText(context, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadPosts() {
        if (allPostsLoaded) return

        isLoading = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val documents = viewModel.getFollowingPosts(lastVisible)
                withContext(Dispatchers.Main) {
                    if (documents.isEmpty) {
                        allPostsLoaded = true
                        isLoading = false
                        if (posts.isEmpty()) {
                            binding.noPostsTextView.visibility = View.VISIBLE
                        }
                        return@withContext
                    }
                    lastVisible = documents.documents.lastOrNull()
                    val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                    posts.addAll(postList)
                    postAdapter.notifyDataSetChanged()
                    binding.noPostsTextView.visibility = View.GONE
                    isLoading = false
                }
            } catch (e: CancellationException) {
                // Manejo de la cancelación de la corrutina si es necesario
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadMorePosts() {
        loadPosts()
    }

    private fun refreshContent() {
        posts.clear()
        lastVisible = null
        allPostsLoaded = false
        loadPosts()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
