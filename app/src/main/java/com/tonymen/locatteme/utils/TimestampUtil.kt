package com.tonymen.locatteme.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

object TimestampUtil {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    fun parseStringToTimestamp(dateString: String?): Timestamp? {
        return if (dateString.isNullOrEmpty()) {
            null
        } else {
            try {
                val date = dateFormat.parse(dateString)
                date?.let { Timestamp(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun formatTimestampToString(timestamp: Timestamp?): String {
        return timestamp?.let {
            dateFormat.format(it.toDate())
        } ?: ""
    }


}
