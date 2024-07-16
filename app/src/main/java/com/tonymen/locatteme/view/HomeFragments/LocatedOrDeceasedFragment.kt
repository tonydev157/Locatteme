package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.databinding.FragmentLocatedOrDeceasedBinding
import com.tonymen.locatteme.view.adapters.LocatedOrDeceasedAdapter
import com.tonymen.locatteme.viewmodel.LocatedOrDeceasedViewModel

class LocatedOrDeceasedFragment : Fragment() {

    private var _binding: FragmentLocatedOrDeceasedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LocatedOrDeceasedViewModel
    private lateinit var adapter: LocatedOrDeceasedAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocatedOrDeceasedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(LocatedOrDeceasedViewModel::class.java)
        auth = FirebaseAuth.getInstance()

        adapter = LocatedOrDeceasedAdapter()
        binding.recyclerViewPostsLoD.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPostsLoD.adapter = adapter

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner, { posts ->
            adapter.submitList(posts)
        })

        viewModel.loadPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
