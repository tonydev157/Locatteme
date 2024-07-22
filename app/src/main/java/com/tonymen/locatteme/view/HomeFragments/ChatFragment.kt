package com.tonymen.locatteme.view.HomeFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.databinding.FragmentChatBinding
import com.tonymen.locatteme.model.Message
import com.tonymen.locatteme.view.adapters.MessagesAdapter
import com.tonymen.locatteme.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: MessagesAdapter

    private var chatId: String? = null
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        chatId = arguments?.getString("chatId")
        currentUserId = arguments?.getString("currentUserId") ?: auth.currentUser?.uid

        setupRecyclerView()
        setupSendButton()

        chatId?.let {
            Log.d("ChatFragment", "chatId: $it")
            observeMessages(it)
        }
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(emptyList())
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true // Los mensajes más recientes se muestran en la parte inferior
        }
        binding.recyclerViewMessages.adapter = adapter
    }

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotEmpty()) {
                val message = Message(
                    id = "",
                    senderId = currentUserId.orEmpty(),
                    messageText = messageText,
                    timestamp = Timestamp.now()
                )
                sendMessage(message)
                binding.editTextMessage.text.clear()
            } else {
                Toast.makeText(context, "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(message: Message) {
        lifecycleScope.launch {
            try {
                val messagesRef = db.collection("chats").document(chatId.orEmpty()).collection("messages")
                val newMessageRef = messagesRef.document()
                val messageWithId = message.copy(id = newMessageRef.id)
                newMessageRef.set(messageWithId).await()
                Log.d("FirestoreOperation", "Message sent: ${messageWithId.id}")
            } catch (e: Exception) {
                Toast.makeText(context, "Error al enviar el mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirestoreOperation", "Error al enviar el mensaje", e)
            }
        }
    }

    private fun observeMessages(chatId: String) {
        viewModel.loadMessages(chatId)
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.updateMessages(messages)
            binding.recyclerViewMessages.scrollToPosition(messages.size - 1) // Desplaza al último mensaje
            markMessagesAsRead(chatId, messages)
        }
    }

    private fun markMessagesAsRead(chatId: String, messages: List<Message>) {
        messages.forEach { message ->
            if (!message.readBy.contains(currentUserId.orEmpty()) && message.id.isNotEmpty()) {
                Log.d("MarkMessageAsRead", "Marking message ${message.id} as read")
                viewModel.markMessageAsRead(chatId, message.id)
            } else {
                Log.w("MarkMessageAsRead", "Message ID is empty or already read: ${message.id}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(chatId: String, currentUserId: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString("chatId", chatId)
                putString("currentUserId", currentUserId)
            }
        }
    }
}
