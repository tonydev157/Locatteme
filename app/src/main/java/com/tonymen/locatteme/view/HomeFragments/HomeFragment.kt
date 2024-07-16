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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentHomeBinding
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.view.adapters.HomePostsAdapter
import com.tonymen.locatteme.viewmodel.HomeFViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: HomeFViewModel
    private lateinit var postAdapter: HomePostsAdapter

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
        observeViewModel()
        setupIconClickListener()

        return binding.root
    }

    private fun setupRecyclerView() {
        postAdapter = HomePostsAdapter(requireContext())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
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

                // Set click listeners to navigate to the profile fragment
                val clickListener = View.OnClickListener {
                    val fragment = ProfileFragment()
                    val transaction = parentFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragmentContainer, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }

                binding.profileImageView.setOnClickListener(clickListener)
                binding.usernameTextView.setOnClickListener(clickListener)
            }
        }.addOnFailureListener {
            // En caso de error, mostrar imagen predeterminada
            binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            Toast.makeText(context, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.posts.collectLatest { pagingData ->
                postAdapter.submitData(pagingData)
            }
        }
    }

    fun refreshContent() {
        postAdapter.refresh()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun setupIconClickListener() {
        binding.headerLayout.findViewById<View>(R.id.ic_located_or_deceased).setOnClickListener {
            val fragment = LocatedOrDeceasedFragment()
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
