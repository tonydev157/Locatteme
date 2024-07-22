package com.tonymen.locatteme.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.tonymen.locatteme.model.Message

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    fun loadMessages(chatId: String) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error al cargar los mensajes", e)
                    return@addSnapshotListener
                }

                val messagesList = snapshot?.toObjects(Message::class.java) ?: emptyList()
                _messages.value = messagesList
            }
    }

    fun markMessageAsRead(chatId: String, messageId: String) {
        val messageRef = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(messageId)

        Log.d("FirestoreOperation", "Marking message as read: $messageId in chat: $chatId")

        messageRef.update("readBy", FieldValue.arrayUnion(FirebaseAuth.getInstance().currentUser?.uid))
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Mensaje marcado como leído")
            }
            .addOnFailureListener { e ->
                Log.w("ChatViewModel", "Error al marcar el mensaje como leído", e)
            }
    }
}
