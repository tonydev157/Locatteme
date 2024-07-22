package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class ChatPreview(
    val chatId: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Timestamp = Timestamp.now(),
    val otherUserId: String = "" // ID del otro usuario en la conversación
)
