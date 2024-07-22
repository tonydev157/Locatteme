package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Chat

class ActiveChatsViewModel : ViewModel() {

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    init {
        fetchActiveChats()
    }

    private fun fetchActiveChats() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val activeChats = documents.mapNotNull { it.toObject(Chat::class.java) }
                _chats.value = activeChats
            }
            .addOnFailureListener { exception ->
                // Manejar el error aquí
            }
    }
}
