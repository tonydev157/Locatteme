package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Story(
    val id: String = "",
    val userId: String = "",
    val mediaUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val expira: Timestamp = Timestamp.now()
)
