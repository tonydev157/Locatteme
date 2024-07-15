package com.tonymen.locatteme.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User

class Repository {

    private val db = FirebaseFirestore.getInstance()

    fun searchUsers(query: String, callback: (List<User>) -> Unit) {
        // Implementar lógica de búsqueda de usuarios
    }

    fun searchPosts(query: String, callback: (List<Post>) -> Unit) {
        // Implementar lógica de búsqueda de publicaciones
    }

    fun filterPosts(estado: String, fechaDesaparicion: String, fechaPublicacion: String, provincia: String, callback: (List<Post>) -> Unit) {
        val postsRef = db.collection("posts")
        var query = postsRef.whereEqualTo("estado", estado)

        if (fechaDesaparicion.isNotEmpty()) {
            query = query.whereEqualTo("fechaDesaparicion", fechaDesaparicion)
        }

        if (fechaPublicacion.isNotEmpty()) {
            query = query.whereEqualTo("fechaPublicacion", fechaPublicacion)
        }

        if (provincia.isNotEmpty()) {
            query = query.whereEqualTo("provincia", provincia)
        }

        query.get().addOnSuccessListener { documents ->
            val posts = documents.map { it.toObject(Post::class.java) }
            callback(posts)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }
}
