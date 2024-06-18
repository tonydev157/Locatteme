package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Admin(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
