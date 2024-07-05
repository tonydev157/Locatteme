package com.tonymen.locatteme.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tonymen.locatteme.databinding.FragmentFollowersNBinding
import com.tonymen.locatteme.view.adapters.FollowersNAdapter
import com.tonymen.locatteme.viewmodel.FollowersNViewModel

class FollowersNFragment : Fragment() {

    private var _binding: FragmentFollowersNBinding? = null
    private val binding get() = _binding!!
    private lateinit var followersNViewModel: FollowersNViewModel
    private lateinit var adapter: FollowersNAdapter
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowersNBinding.inflate(inflater, container, false)
        followersNViewModel = ViewModelProvider(this).get(FollowersNViewModel::class.java)
        userId = arguments?.getString("userId") ?: ""

        setupRecyclerView()
        loadFollowers()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = FollowersNAdapter(emptyList(), requireContext())
        binding.recyclerViewFollowers.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewFollowers.adapter = adapter
    }

    private fun loadFollowers() {
        followersNViewModel.getFollowers(userId).observe(viewLifecycleOwner, Observer { followers ->
            followers.forEach { follower ->
                Log.d("FollowersNFragment", "Follower received: ${follower.username}, ${follower.nombre} ${follower.apellido}")
            }
            adapter.updateFollowers(followers)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: String): FollowersNFragment {
            val fragment = FollowersNFragment()
            val args = Bundle()
            args.putString("userId", userId)
            fragment.arguments = args
            return fragment
        }
    }
}
