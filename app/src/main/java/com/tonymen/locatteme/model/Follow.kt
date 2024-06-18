package com.tonymen.locatteme.model

import com.google.firebase.Timestamp

data class Follow(
    val id: String = "",
    val followerId: String = "",
    val followedId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
