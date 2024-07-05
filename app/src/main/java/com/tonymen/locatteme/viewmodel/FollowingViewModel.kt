package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class FollowingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    fun getFollowingPosts(lastVisible: DocumentSnapshot? = null): Task<QuerySnapshot> {
        val followingQuery = db.collection("follows")
            .whereEqualTo("followerId", currentUserId!!)

        return followingQuery.get().continueWithTask { task ->
            val followingIds = task.result?.documents?.map { it.getString("followedId") ?: "" } ?: emptyList()

            val query = db.collection("posts")
                .whereIn("autorId", followingIds + currentUserId)
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                .let { if (lastVisible != null) it.startAfter(lastVisible) else it }
                .limit(10)

            query.get()
        }
    }
}
