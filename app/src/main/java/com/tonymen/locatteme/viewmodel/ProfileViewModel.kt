package com.tonymen.locatteme.viewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val isUpdatingProfileImage = MutableLiveData<Boolean>()

    suspend fun getUser(userId: String) = db.collection("users").document(userId).get().await()

    suspend fun updateProfileImageUrl(userId: String, profileImageUrl: String) {
        db.collection("users").document(userId).update("profileImageUrl", profileImageUrl).await()
    }

    fun getStorageReference(userId: String) = storage.reference.child("profileImages/$userId.jpg")

    suspend fun getUserPosts(userId: String) = db.collection("posts")
        .whereEqualTo("autorId", userId)
        .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
        .get()
        .await()

    suspend fun updateProfileImage(userId: String, imageUri:    Uri, onComplete: () -> Unit, onError: (String) -> Unit) {
        isUpdatingProfileImage.value = true
        try {
            val storageRef = getStorageReference(userId)
            storageRef.putFile(imageUri).await()
            val uri = storageRef.downloadUrl.await()
            updateProfileImageUrl(userId, uri.toString())
            onComplete()
        } catch (exception: Exception) {
            onError(exception.message ?: "Error al subir la imagen")
        } finally {
            isUpdatingProfileImage.value = false
        }
    }
}

