package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Group(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val miembros: List<String> = emptyList(),
    val adminId: String = "",
    val fechaCreacion: Timestamp = Timestamp.now()
)
