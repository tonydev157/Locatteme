package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tonymen.locatteme.databinding.FragmentFollowingNBinding
import com.tonymen.locatteme.view.adapters.FollowingNAdapter
import com.tonymen.locatteme.viewmodel.FollowingNViewModel

class FollowingNFragment : Fragment() {

    private var _binding: FragmentFollowingNBinding? = null
    private val binding get() = _binding!!
    private lateinit var followingNViewModel: FollowingNViewModel
    private lateinit var adapter: FollowingNAdapter
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowingNBinding.inflate(inflater, container, false)
        followingNViewModel = ViewModelProvider(this).get(FollowingNViewModel::class.java)
        userId = arguments?.getString("userId") ?: ""

        setupRecyclerView()
        loadFollowing()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = FollowingNAdapter(emptyList(), requireContext())
        binding.recyclerViewFollowing.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewFollowing.adapter = adapter
    }

    private fun loadFollowing() {
        followingNViewModel.getFollowing(userId).observe(viewLifecycleOwner, Observer { following ->
            adapter.updateFollowing(following)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: String): FollowingNFragment {
            val fragment = FollowingNFragment()
            val args = Bundle()
            args.putString("userId", userId)
            fragment.arguments = args
            return fragment
        }
    }
}
