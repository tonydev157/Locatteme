package com.tonymen.locatteme.view.Homefragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentActiveChatsBinding
import com.tonymen.locatteme.view.HomeFragments.ChatFragment
import com.tonymen.locatteme.view.adapters.ActiveChatsAdapter
import com.tonymen.locatteme.viewmodel.ActiveChatsViewModel

class ActiveChatsFragment : Fragment() {

    private var _binding: FragmentActiveChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ActiveChatsViewModel
    private lateinit var adapter: ActiveChatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveChatsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ActiveChatsViewModel::class.java)

        setupRecyclerView()
        observeViewModel()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ActiveChatsAdapter(requireContext()) { chatId, userId ->
            openChatFragment(chatId, userId)
        }
        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapter
        }
    }

    private fun observeViewModel() {
        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            adapter.submitList(chats)
        }
    }

    private fun openChatFragment(chatId: String, userId: String) {
        val fragment = ChatFragment.newInstance(chatId, userId)
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
