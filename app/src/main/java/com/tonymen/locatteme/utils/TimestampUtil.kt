package com.tonymen.locatteme.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.tonymen.locatteme.model.Post

object TimestampUtil {

    fun getPost(document: DocumentSnapshot): Post {
        return document.toObject(Post::class.java) ?: Post()
    }

    fun formatTimestampToString(timestamp: com.google.firebase.Timestamp): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}
