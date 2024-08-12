package com.tonymen.locatteme.view.adapters

import com.google.firebase.Timestamp
import com.tonymen.locatteme.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

interface MessageClickHandler {
    fun onImageClick(message: Message)
    fun onVideoClick(message: Message)
    fun onAudioClick(message: Message)

    // Método para formatear la marca de tiempo
    fun formatTimestamp(timestamp: Timestamp): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(timestamp.toDate())
    }
}
