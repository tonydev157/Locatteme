package com.tonymen.locatteme.model.chatmodels

enum class MediaType {
    IMAGE, VIDEO, AUDIO, DOCUMENT
}

data class MediaDownload(
    val fileUrl: String,                      // URL del archivo en Firebase Storage
    val localPath: String,                    // Ruta local del archivo descargado
    val fileType: MediaType                   // Tipo de archivo (Imagen, Video, Audio, Documento)
)
