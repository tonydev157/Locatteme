    package com.tonymen.locatteme.viewmodel

    import androidx.lifecycle.ViewModel
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.Query
    import com.google.firebase.storage.FirebaseStorage

    class ProfileViewModel : ViewModel() {
        private val db = FirebaseFirestore.getInstance()
        private val storage = FirebaseStorage.getInstance()

        fun getUser(userId: String) = db.collection("users").document(userId).get()

        fun updateProfileImageUrl(userId: String, profileImageUrl: String) =
            db.collection("users").document(userId).update("profileImageUrl", profileImageUrl)

        fun getStorageReference(userId: String) = storage.reference.child("profileImages/$userId.jpg")

        // Adjusted to descending order to show the most recent posts first
        fun getUserPosts(userId: String) = db.collection("posts")
            .whereEqualTo("autorId", userId)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .get()
    }
