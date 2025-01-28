package com.tonymen.locatteme.view.HomeFragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.adapter.ActiveChatsAdapter
import com.tonymen.locatteme.databinding.FragmentActiveChatsBinding
import com.tonymen.locatteme.view.ui.ChatActivity
import com.tonymen.locatteme.viewmodel.ActiveChatsViewModel

class ActiveChatsFragment : Fragment() {

    private var _binding: FragmentActiveChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ActiveChatsViewModel
    private lateinit var chatAdapter: ActiveChatsAdapter
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        viewModel = ViewModelProvider(this).get(ActiveChatsViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshChats() // Refresca los chats cuando el fragmento vuelve a ser visible
    }

    private fun setupRecyclerView() {
        chatAdapter = ActiveChatsAdapter(emptyList()) { chat ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("chatId", chat.id) // ID del chat seleccionado
                putExtra("currentUserId", currentUserId) // ID del usuario actual
                putExtra("otherUserId", chat.participants.first { it != currentUserId }) // ID del otro usuario
            }
            startActivity(intent) // Inicia la actividad del chat
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = chatAdapter
    }


    private fun observeViewModel() {
        viewModel.chats.observe(viewLifecycleOwner, Observer { chats ->
            chatAdapter.updateData(chats)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
