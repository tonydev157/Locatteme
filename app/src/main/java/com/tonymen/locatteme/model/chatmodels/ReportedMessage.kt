package com.tonymen.locatteme.model.chatmodels

import com.google.firebase.Timestamp

data class ReportedMessage(
    val messageId: String,                    // ID del mensaje reportado
    val reportedBy: String,                   // ID del usuario que reporta
    val reason: String,                       // Razón del reporte
    val timestamp: Timestamp = Timestamp.now()// Fecha/hora del reporte
)
