package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val readBy: List<String> = emptyList()
)
