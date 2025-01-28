package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tonymen.locatteme.model.chatmodels.Message
import com.tonymen.locatteme.model.chatmodels.MessageStatus
import com.tonymen.locatteme.model.chatmodels.MessageType
import kotlinx.coroutines.flow.*
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var lastVisibleDocument: DocumentSnapshot? = null // Último documento visible
    private var isLoadingOlder = false
    private var isLoadingRealTime = false

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage

    // Cargar los últimos mensajes iniciales
    fun loadInitialMessages(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(20)
            .get()
            .addOnSuccessListener { snapshot ->
                val loadedMessages = snapshot.toObjects(Message::class.java)
                _messages.value = loadedMessages
                lastVisibleDocument = snapshot.documents.firstOrNull() // Guardar el primer documento
            }
    }

    // Cargar mensajes más antiguos
    fun loadOlderMessages(chatId: String) {
        if (isLoadingOlder || lastVisibleDocument == null) return
        isLoadingOlder = true

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .endBefore(lastVisibleDocument) // Usar el documento visible
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                val olderMessages = snapshot.toObjects(Message::class.java)
                if (olderMessages.isNotEmpty()) {
                    lastVisibleDocument = snapshot.documents.firstOrNull() // Actualizar el último documento visible
                    _messages.value = olderMessages + _messages.value // Añadir arriba
                }
                isLoadingOlder = false
            }
    }

    // Escucha en tiempo real mensajes nuevos
    fun listenToMessages(chatId: String) {
        if (isLoadingRealTime) return
        isLoadingRealTime = true

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val newMessages = snapshot.toObjects(Message::class.java)
                _messages.value = newMessages // Mantener el orden
            }
    }

    // Enviar un mensaje
    fun sendMessage(chatId: String, senderId: String) {
        if (_inputMessage.value.isBlank()) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            messageText = _inputMessage.value,
            messageType = MessageType.TEXT,
            timestamp = com.google.firebase.Timestamp.now(),
            status = MessageStatus.SENT
        )

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.id)
            .set(message)
            .addOnSuccessListener {
                _inputMessage.value = "" // Limpiar campo
            }
    }

    fun onInputMessageChanged(newValue: String) {
        _inputMessage.value = newValue
    }
}
