package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val timestamp: Timestamp = Timestamp.now(), // Hora de envío del mensaje
    val readTimestamp: Timestamp? = null, // Hora en que se leyó el mensaje, null si no se ha leído
    val readBy: List<String> = emptyList() // Lista de IDs de usuarios que han leído el mensaje
)
