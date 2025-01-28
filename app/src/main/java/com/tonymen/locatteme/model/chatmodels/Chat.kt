package com.tonymen.locatteme.model.chatmodels

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",                     // ID único del chat
    val participants: List<String> = emptyList(), // IDs de los participantes
    val unreadMessages: Map<String, Int> = emptyMap(), // Mensajes no leídos por usuario
    val lastMessageText: String = "",        // Último mensaje
    val lastMessageTimestamp: Timestamp = Timestamp.now(), // Marca de tiempo del último mensaje
    val lastMessageSentByCurrentUser: Boolean = false, // Si el último mensaje fue enviado por el usuario actual
    val isLastMessageRead: Boolean = true,   // Si el último mensaje fue leído
    val isGroupChat: Boolean = false,        // Si es un chat grupal
    val groupName: String? = null,           // Nombre del grupo (si es grupal)
    val groupImageUrl: String? = null,       // Imagen del grupo (si es grupal)
    val groupAdmins: List<String> = emptyList() // IDs de los administradores del grupo
)
