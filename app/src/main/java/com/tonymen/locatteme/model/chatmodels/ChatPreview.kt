package com.tonymen.locatteme.model.chatmodels

import com.google.firebase.Timestamp

data class ChatPreview(
    val chatId: String = "",                  // ID único del chat
    val lastMessage: String = "",             // Último mensaje
    val lastTimestamp: Timestamp = Timestamp.now(), // Marca de tiempo del último mensaje
    val otherUserId: String = "",             // ID del otro usuario (si es chat individual)
    val otherUserProfileImage: String? = null,// Imagen de perfil del otro usuario
    val isLastMessageRead: Boolean = true,    // Si el último mensaje fue leído
    val isMuted: Boolean = false              // Si el chat está silenciado
)
