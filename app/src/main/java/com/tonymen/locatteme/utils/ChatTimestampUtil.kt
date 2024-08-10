package com.tonymen.locatteme.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

object ChatTimestampUtil {
    private val hourFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    fun formatChatTimestamp(timestamp: Timestamp): String {
        val messageDate = timestamp.toDate()
        val now = Calendar.getInstance().time
        val diff = now.time - messageDate.time

        val oneDayInMillis = 24 * 60 * 60 * 1000

        return when {
            diff < oneDayInMillis -> hourFormat.format(messageDate) // Menos de 24h
            diff < 2 * oneDayInMillis -> "Ayer" // Entre 24 y 48 horas
            else -> dateFormat.format(messageDate) // Más de 48 horas
        }
    }
}
