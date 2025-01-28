package com.tonymen.locatteme.model.chatmodels

import com.google.firebase.Timestamp

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT
}

enum class MessageStatus {
    SENT, DELIVERED, SEEN, FAILED
}

data class Reaction(
    val userId: String,    // ID del usuario que reaccionó
    val emoji: String       // Emoji de la reacción
)

data class Message(
    val id: String = "",                                // ID único del mensaje
    val senderId: String = "",                          // ID del remitente
    val messageText: String = "",                       // Texto del mensaje
    val imageUrl: String? = null,                       // URL de Firebase Storage (si es imagen)
    val videoUrl: String? = null,                       // URL de Firebase Storage (si es video)
    val audioUrl: String? = null,                       // URL de Firebase Storage (si es audio)
    val documentUrl: String? = null,                    // URL de Firebase Storage (si es documento)
    val localImagePath: String? = null,                 // Ruta local del archivo de imagen
    val localVideoPath: String? = null,                 // Ruta local del archivo de video
    val localAudioPath: String? = null,                 // Ruta local del archivo de audio
    val localDocumentPath: String? = null,              // Ruta local del documento
    val timestamp: Timestamp = Timestamp.now(),         // Marca de tiempo del mensaje
    val messageType: MessageType = MessageType.TEXT,    // Tipo de mensaje
    val status: MessageStatus = MessageStatus.SENT,     // Estado del mensaje (SENT, DELIVERED, SEEN)
    val readBy: List<String> = emptyList(),             // IDs de usuarios que leyeron el mensaje
    val deliveredTo: List<String> = emptyList(),        // IDs de usuarios que recibieron el mensaje
    val reactions: List<Reaction> = emptyList(),        // Reacciones al mensaje
    val isDeleted: Boolean = false,                     // Si el mensaje fue eliminado
    val deletedFor: List<String> = emptyList(),         // IDs de usuarios para los que fue eliminado
    val repliedMessage: Message? = null,                // Mensaje al que se está respondiendo
    val editedAt: Timestamp? = null,                    // Fecha de última edición (si fue editado)
    val expiresAt: Timestamp? = null,                   // Fecha/hora de expiración del mensaje (si es temporal)
    val forwarded: Boolean = false,                     // Si el mensaje fue reenviado
    val isPinned: Boolean = false,                      // Si el mensaje está fijado
    val mentions: List<String> = emptyList(),           // IDs de usuarios mencionados
    val threadMessages: List<Message> = emptyList()     // Mensajes relacionados en un hilo
)
