package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val unreadMessages: Map<String, Int> = emptyMap(), // Mapa de usuarioId a número de mensajes no leídos
    val lastMessageText: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val lastMessageSentByCurrentUser: Boolean = false,
    val isLastMessageRead: Boolean = true
)
