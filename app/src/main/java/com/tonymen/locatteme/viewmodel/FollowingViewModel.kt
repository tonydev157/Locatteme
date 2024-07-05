package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class FollowingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    fun getFollowingPosts(userId: String, lastVisible: DocumentSnapshot?): Task<QuerySnapshot> {
        return db.collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .continueWithTask { task ->
                val followedIds = task.result?.documents?.map { it.getString("followedId") ?: "" }
                if (followedIds.isNullOrEmpty()) {
                    return@continueWithTask db.collection("posts").whereEqualTo("autorId", "").get()
                }

                val query = if (lastVisible == null) {
                    db.collection("posts")
                        .whereIn("autorId", followedIds)
                        .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                        .limit(10)
                } else {
                    db.collection("posts")
                        .whereIn("autorId", followedIds)
                        .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                        .startAfter(lastVisible)
                        .limit(10)
                }
                return@continueWithTask query.get()
            }
    }
}
