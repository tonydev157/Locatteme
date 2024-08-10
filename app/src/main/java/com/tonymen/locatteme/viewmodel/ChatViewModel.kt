package com.tonymen.locatteme.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Tasks
import com.tonymen.locatteme.model.Chat
import com.tonymen.locatteme.model.Message
import com.google.firebase.Timestamp

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    // Función para crear un nuevo chat
    fun createChat(participantIds: List<String>, initialMessage: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val usersRef = db.collection("users")
        val participantChecks = participantIds.map { userId ->
            usersRef.document(userId).get()
        }

        // Ejecutar todas las comprobaciones en paralelo
        Tasks.whenAllSuccess<DocumentSnapshot>(participantChecks)
            .addOnSuccessListener { snapshots ->
                val allUsersExist = snapshots.all { it.exists() }

                if (allUsersExist) {
                    // Todos los usuarios existen, procedemos a crear el chat
                    val chatId = db.collection("chats").document().id

                    val chat = Chat(
                        id = chatId,
                        participants = participantIds,
                        unreadMessages = participantIds.associateWith { 0 },  // Inicializar con 0 mensajes no leídos para cada participante
                        lastMessageText = initialMessage,
                        lastMessageTimestamp = Timestamp.now(),
                        lastMessageSentByCurrentUser = true,
                        isLastMessageRead = false
                    )

                    db.collection("chats").document(chatId).set(chat)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Chat document successfully created!")
                            onSuccess(chatId)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error creating chat document", e)
                            onFailure(e.message ?: "Error desconocido")
                        }
                } else {
                    // Alguno de los usuarios no existe
                    onFailure("Uno o más participantes no existen en el sistema.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking participants existence", e)
                onFailure(e.message ?: "Error desconocido")
            }
    }

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

        messageRef.update("readBy", com.google.firebase.firestore.FieldValue.arrayUnion(FirebaseAuth.getInstance().currentUser?.uid))
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Mensaje marcado como leído")
            }
            .addOnFailureListener { e ->
                Log.w("ChatViewModel", "Error al marcar el mensaje como leído", e)
            }
    }

    fun getChatIdForParticipants(participantIds: List<String>, onSuccess: (String?) -> Unit) {
        db.collection("chats")
            .whereArrayContains("participants", participantIds.first())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val chat = querySnapshot.documents.firstOrNull { doc ->
                    val participants = doc.get("participants") as? List<String>
                    participants?.containsAll(participantIds) == true && participantIds.containsAll(participants)
                }
                onSuccess(chat?.id)
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al buscar chat por participantes", e)
                onSuccess(null)
            }
    }
}
