package com.tonymen.locatteme.model

import com.google.firebase.Timestamp
import java.io.Serializable

import com.tonymen.locatteme.model.LocationData
import com.tonymen.locatteme.model.Comment

data class PetPost(
    var id: String = "",
    val nombreMascota: String = "", // Nombre de la mascota
    val tipoMascota: String = "", // Ej: Perro, Gato, etc.
    val raza: String = "", // Raza de la mascota
    val edad: Int = 0, // Edad de la mascota
    val genero: String = "", // Ej: Macho, Hembra
    val color: String = "", // Color principal de la mascota
    val tamanio: String = "", // Tamaño de la mascota: Pequeño, Mediano, Grande
    val fotoPequena: String = "", // URL para la versión pequeña de la imagen de la mascota
    val fotoGrande: String = "", // URL para la versión grande de la imagen de la mascota
    val lugarDesaparicion: String = "", // Descripción del lugar de desaparición
    val lugarDesaparicionMaps: LocationData = LocationData(), // Ubicación en el mapa
    val fechaDesaparicion: Timestamp = Timestamp.now(), // Fecha y hora de la desaparición
    val caracteristicas: String = "", // Características distintivas (ej: collar, cicatrices)
    val estado: String = "Desaparecido", // Estado de la mascota (ej: Desaparecido, Encontrado)
    val recompensa: String = "", // Detalles sobre la recompensa ofrecida
    val fechaPublicacion: Timestamp = Timestamp.now(), // Fecha de publicación
    val autorId: String = "", // ID del usuario que publicó
    val comentarios: List<Comment> = emptyList(), // Comentarios en la publicación
    val numerosContacto: List<String> = emptyList(), // Números de contacto
    val searchKeywords: List<String> = emptyList(), // Palabras clave para búsqueda
    val numeroActualizaciones: Int = 0 // Contador de actualizaciones del post
) : Serializable
