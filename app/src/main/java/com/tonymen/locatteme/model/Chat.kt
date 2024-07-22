package com.tonymen.locatteme.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val unreadMessages: Map<String, Int> = emptyMap() // Mapa de usuarioId a número de mensajes no leídos
)
