package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.adapter.ChatDisplay
import com.tonymen.locatteme.model.chatmodels.Chat
import com.tonymen.locatteme.model.chatmodels.Message
import com.tonymen.locatteme.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActiveChatsViewModel : ViewModel() {

    private val _chats = MutableLiveData<List<ChatDisplay>>()
    val chats: LiveData<List<ChatDisplay>> get() = _chats

    init {
        loadChatsInRealTime()
    }

    // Método para refrescar los chats cuando sea necesario
    fun refreshChats() {
        loadChatsInRealTime()
    }

    private fun loadChatsInRealTime() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Manejar el error
                    return@addSnapshotListener
                }

                val chatList = mutableListOf<ChatDisplay>()

                viewModelScope.launch {
                    snapshot?.documents?.forEach { document ->
                        val chat = document.toObject(Chat::class.java)
                        val otherUserId = chat?.participants?.firstOrNull { it != currentUserId }

                        if (chat != null && otherUserId != null) {
                            try {
                                val userDoc = db.collection("users").document(otherUserId).get().await()
                                val user = userDoc.toObject(User::class.java)

                                if (user != null) {
                                    // Obtener los mensajes del chat
                                    val messages = db.collection("chats")
                                        .document(chat.id)
                                        .collection("messages")
                                        .get()
                                        .await()
                                        .toObjects(Message::class.java)

                                    // Calcular la cantidad de mensajes no leídos
                                    val unreadCount = messages.count { !it.readBy.contains(currentUserId) }

                                    val chatDisplay = ChatDisplay(
                                        chat = chat,
                                        userName = "${user.nombre} ${user.apellido}",
                                        userProfileImageUrl = user.profileImageUrl,
                                        unreadCount = unreadCount  // Añadir el contador de mensajes no leídos
                                    )
                                    chatList.add(chatDisplay)
                                }
                            } catch (ex: Exception) {
                                // Manejar el error
                            }
                        }
                    }

                    // Ordenar los chats por la fecha del último mensaje (del más reciente al más antiguo)
                    chatList.sortByDescending { it.chat.lastMessageTimestamp }
                    _chats.postValue(chatList)
                }
            }
    }
}
