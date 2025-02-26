package com.tonymen.locatteme.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class PostReport(
    val id: String = "",                // ID único del reporte
    val postId: String = "",            // ID del post reportado
    val reportedBy: String = "",        // ID del usuario que hace el reporte
    val reason: ReportReason = ReportReason.OTHER,  // Razón del reporte
    val status: ReportStatus = ReportStatus.PENDING, // Estado del reporte
    val reportDate: Timestamp = Timestamp.now(), // Fecha del reporte
    val adminComment: String = ""       // Comentario del administrador (opcional)
) : Serializable

// Enum para clasificar razones del reporte
enum class ReportReason {
    FALSE_INFORMATION, // Información falsa
    INAPPROPRIATE_CONTENT, // Contenido inapropiado
    SPAM, // Spam o abuso
    OTHER // Otra razón
}

// Enum para el estado del reporte
enum class ReportStatus {
    PENDING, // Aún no revisado
    REVIEWED, // Ya revisado
    REJECTED // Rechazado por falta de pruebas
}
