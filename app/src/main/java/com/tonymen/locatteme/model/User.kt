package com.tonymen.locatteme.model

data class User(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val username: String = "",
    val edad: Int = 0,
    val cedulaIdentidad: String = "",
    val correoElectronico: String = "",
    val telefono: String = "",
    val profileImageUrl: String = "",
    val seguidores: List<String> = emptyList(),
    val seguidos: List<String> = emptyList(),
    val publicaciones: List<String> = emptyList()
)
