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
import com.tonymen.locatteme.model.Chat
import com.tonymen.locatteme.model.Message
import com.tonymen.locatteme.view.adapters.MessagesAdapter
import com.tonymen.locatteme.viewmodel.ChatViewModel
import kotlinx.coroutines.*
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
    private var otherUserId: String? = null

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

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
        otherUserId = arguments?.getString("otherUserId")

        setupRecyclerView()
        setupSendButton()

        chatId?.let {
            coroutineScope.launch {
                checkAndCreateChat(it) // Verificar y crear el chat si no existe
                observeMessages(it)
            }
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
                coroutineScope.launch {
                    sendMessage(message)
                }
                binding.editTextMessage.text.clear()
            } else {
                Toast.makeText(context, "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun sendMessage(message: Message) {
        try {
            withContext(Dispatchers.IO) {
                val chatRef = db.collection("chats").document(chatId.orEmpty())

                // Verificar si el chat existe
                val chatDoc = chatRef.get().await()

                if (!chatDoc.exists()) {
                    // Asegurarse de que ambos IDs de usuario estén en la lista de participantes
                    val participants = listOf(currentUserId.orEmpty(), otherUserId.orEmpty())
                    val chat = Chat(
                        id = chatId.orEmpty(),
                        participants = participants,
                        lastMessageText = message.messageText,
                        lastMessageTimestamp = message.timestamp,
                        lastMessageSentByCurrentUser = true,
                        isLastMessageRead = false
                    )
                    chatRef.set(chat).await()
                    Log.d("FirestoreOperation", "Chat creado: ${chat.id}")
                }

                // Ahora envía el mensaje
                val messagesRef = chatRef.collection("messages")
                val newMessageRef = messagesRef.document()
                val messageWithId = message.copy(id = newMessageRef.id)
                newMessageRef.set(messageWithId).await()

                // Actualizar el documento del chat con el último mensaje enviado
                chatRef.update(
                    mapOf(
                        "lastMessageText" to message.messageText,
                        "lastMessageTimestamp" to message.timestamp,
                        "lastMessageSentByCurrentUser" to (message.senderId == currentUserId),
                        "isLastMessageRead" to false
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    Log.d("FirestoreOperation", "Mensaje enviado: ${messageWithId.id}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (isActive) {
                    Toast.makeText(context, "Error al enviar el mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("FirestoreOperation", "Error al enviar el mensaje", e)
            }
        }
    }

    private suspend fun sendMessageAndUpdate(message: Message) {
        try {
            withContext(Dispatchers.IO) {
                val chatRef = db.collection("chats").document(chatId.orEmpty())

                // Verificar si el chat existe
                val chatDoc = chatRef.get().await()

                if (!chatDoc.exists()) {
                    // Asegurarse de que ambos IDs de usuario estén en la lista de participantes
                    val participants = listOf(currentUserId.orEmpty(), otherUserId.orEmpty())
                    val chat = Chat(
                        id = chatId.orEmpty(),
                        participants = participants,
                        lastMessageText = message.messageText,
                        lastMessageTimestamp = message.timestamp,
                        lastMessageSentByCurrentUser = true,
                        isLastMessageRead = false
                    )
                    chatRef.set(chat).await()
                    Log.d("FirestoreOperation", "Chat creado: ${chat.id}")
                }

                // Ahora envía el mensaje
                val messagesRef = chatRef.collection("messages")
                val newMessageRef = messagesRef.document()
                val messageWithId = message.copy(id = newMessageRef.id)
                newMessageRef.set(messageWithId).await()

                // Actualizar el documento del chat con el último mensaje enviado
                chatRef.update(
                    mapOf(
                        "lastMessageText" to message.messageText,
                        "lastMessageTimestamp" to message.timestamp,
                        "lastMessageSentByCurrentUser" to (message.senderId == currentUserId),
                        "isLastMessageRead" to false
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    Log.d("FirestoreOperation", "Mensaje enviado y chat actualizado: ${messageWithId.id}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (isActive) {
                    Toast.makeText(context, "Error al enviar el mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("FirestoreOperation", "Error al enviar el mensaje", e)
            }
        }
    }

    private suspend fun observeMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            viewModel.loadMessages(chatId)
            withContext(Dispatchers.Main) {
                viewModel.messages.observe(viewLifecycleOwner) { messages ->
                    adapter.updateMessages(messages)
                    binding.recyclerViewMessages.scrollToPosition(messages.size - 1) // Desplaza al último mensaje
                    coroutineScope.launch {
                        markMessagesAsRead(chatId, messages)
                    }
                }
            }
        }
    }

    private suspend fun markMessagesAsRead(chatId: String, messages: List<Message>) {
        withContext(Dispatchers.IO) {
            messages.forEach { message ->
                if (!message.readBy.contains(currentUserId.orEmpty()) && message.id.isNotEmpty()) {
                    Log.d("MarkMessageAsRead", "Marking message ${message.id} as read")
                    viewModel.markMessageAsRead(chatId, message.id)
                } else {
                    Log.w("MarkMessageAsRead", "Message ID is empty or already read: ${message.id}")
                }
            }
        }
    }

    private suspend fun checkAndCreateChat(chatId: String) {
        try {
            withContext(Dispatchers.IO) {
                val chatDoc = db.collection("chats").document(chatId).get().await()
                if (!chatDoc.exists()) {
                    val participants = listOf(currentUserId.orEmpty(), otherUserId.orEmpty()) // Asegurarse de incluir ambos participantes
                    val chat = Chat(
                        id = chatId,
                        participants = participants,
                        lastMessageText = "",
                        lastMessageTimestamp = Timestamp.now(),
                        lastMessageSentByCurrentUser = false,
                        isLastMessageRead = true
                    )
                    db.collection("chats").document(chatId).set(chat).await()
                    Log.d("FirestoreOperation", "Chat created: $chatId")
                } else {
                    Log.d("FirestoreOperation", "Chat already exists: $chatId")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (isActive) {
                    Toast.makeText(context, "Error al crear el chat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("FirestoreOperation", "Error creating chat", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        job.cancel() // Cancelar el job para evitar fugas de memoria
    }

    companion object {
        fun newInstance(chatId: String, currentUserId: String, otherUserId: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString("chatId", chatId)
                putString("currentUserId", currentUserId)
                putString("otherUserId", otherUserId)
            }
        }
    }
}
