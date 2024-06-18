package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedId: String = "",
    val tipo: String = "",
    val detalle: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val estado: String = ""
)
