package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val de: String = "",
    val mensaje: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
