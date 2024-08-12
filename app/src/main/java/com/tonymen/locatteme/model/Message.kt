package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val messageType: MessageType = MessageType.TEXT,
    val readBy: List<String> = emptyList()
)
