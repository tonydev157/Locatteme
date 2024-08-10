package com.tonymen.locatteme.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Post(
    var id: String = "",
    val fotoPequena: String = "",
    val fotoGrande: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val edad: Int = 0,
    val genero: String = "",
    val provincia: String = "",
    val ciudad: String = "",
    val nacionalidad: String = "",
    val estado: String = "Desaparecido",
    val lugarDesaparicion: String = "",
    val lugarDesaparicionMaps: LocationData = LocationData(),
    val fechaDesaparicion: Timestamp = Timestamp.now(),
    val caracteristicas: String = "",
    val fechaPublicacion: Timestamp = Timestamp.now(),
    val numeroActualizaciones: Int = 0, // Campo para contar las actualizaciones
    val autorId: String = "",
    val comentarios: List<Comment> = emptyList(),
    val numerosContacto: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList()
) : Serializable

data class LocationData(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccionCompleta: String = ""
) : Serializable

data class Comment(
    val userId: String = "",
    val comentario: String = "",
    val fechaComentario: Timestamp = Timestamp.now()
) : Serializable
