package com.tonymen.locatteme.model.chatmodels

import com.google.firebase.Timestamp

data class UserActive(
    val userId: String = "",                  // ID único del usuario
    val isOnline: Boolean = false,            // Si está en línea
    val lastActive: Timestamp? = null         // Última vez que estuvo activo
)
