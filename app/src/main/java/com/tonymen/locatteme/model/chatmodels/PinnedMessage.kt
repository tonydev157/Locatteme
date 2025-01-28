package com.tonymen.locatteme.model.chatmodels

import com.google.firebase.Timestamp

data class PinnedMessage(
    val chatId: String,                       // ID del chat
    val messageId: String,                    // ID del mensaje fijado
    val pinnedBy: String,                     // ID del usuario que fijó el mensaje
    val pinnedAt: Timestamp = Timestamp.now() // Fecha/hora de fijación
)
