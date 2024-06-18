package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val userId: String = "",
    val tipo: String = "",
    val mensaje: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val leido: Boolean = false
)
