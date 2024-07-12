package com.tonymen.locatteme.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Post(
    var id: String = "",
    val fotoPequena: String = "", // URL para la versión pequeña de la imagen
    val fotoGrande: String = "", // URL para la versión grande de la imagen
    val nombres: String = "",
    val apellidos: String = "",
    val edad: Int = 0,
    val provincia: String = "",
    val ciudad: String = "",
    val nacionalidad: String = "",
    val estado: String = "Desaparecido",
    val lugarDesaparicion: String = "",
    val fechaDesaparicion: Timestamp = Timestamp.now(),
    val caracteristicas: String = "",
    val fechaPublicacion: Timestamp = Timestamp.now(),
    val autorId: String = "",
    val comentarios: List<Comment> = emptyList(),
    val numerosContacto: List<String> = emptyList(),
    val searchKeywords: List<String> = emptyList() // Inicia vacío, se llenará en CreatePostFragment

) : Serializable

data class Comment(
    val userId: String = "",
    val comentario: String = "",
    val fechaComentario: Timestamp = Timestamp.now()
) : Serializable
