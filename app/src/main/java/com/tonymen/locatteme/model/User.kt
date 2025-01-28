package com.tonymen.locatteme.model

data class User(
    val id: String = "",                      // ID único del usuario
    val nombre: String = "",                  // Nombre del usuario
    val apellido: String = "",                // Apellido del usuario
    val username: String = "",                // Nombre de usuario
    val edad: Int = 0,                        // Edad
    val cedulaIdentidad: String = "",         // Cédula de identidad
    val correoElectronico: String = "",       // Correo electrónico
    val telefono: String = "",                // Número de teléfono
    val profileImageUrl: String = "",         // URL de la imagen de perfil
    val seguidores: List<String> = emptyList(), // IDs de seguidores
    val seguidos: List<String> = emptyList(),   // IDs de seguidos
    val publicaciones: List<String> = emptyList() // IDs de publicaciones
)
