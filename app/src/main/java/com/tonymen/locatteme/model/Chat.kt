package com.tonymen.locatteme.model

data class Chat(
    val id: String = "",
    val usuarios: List<String> = emptyList(),
    val mensajes: List<String> = emptyList()
)
