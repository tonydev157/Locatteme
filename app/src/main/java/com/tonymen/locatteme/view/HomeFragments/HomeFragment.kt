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
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentHomeBinding
import com.tonymen.locatteme.view.HomeFragments.LocatedOrDeceasedFragment
import com.tonymen.locatteme.view.HomeFragments.UserProfileFragment
import com.tonymen.locatteme.view.Homefragments.ActiveChatsFragment
import com.tonymen.locatteme.view.adapters.HomePostsAdapter
import com.tonymen.locatteme.viewmodel.HomeFViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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
        viewModel.loadUserProfile()
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.usernameTextView.text = it.username
                if (!it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(it.profileImageUrl)
                        .circleCrop()
                        .into(binding.profileImageView)
                } else {
                    binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        }

        lifecycleScope.launch {
            try {
                viewModel.posts.collectLatest { pagingData ->
                    postAdapter.submitData(pagingData)
                }
            } catch (e: CancellationException) {
                // La coroutine fue cancelada, no necesitamos hacer nada
            } catch (e: Exception) {
                // Mostrar el error al usuario o registrar el error para depuración
                Toast.makeText(requireContext(), "Error al cargar publicaciones: ${e.message}", Toast.LENGTH_LONG).show()
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

        binding.headerLayout.findViewById<View>(R.id.ic_chat).setOnClickListener {
            val fragment = ActiveChatsFragment()
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
