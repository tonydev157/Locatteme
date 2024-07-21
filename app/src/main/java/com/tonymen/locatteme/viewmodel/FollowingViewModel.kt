package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class FollowingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    suspend fun getFollowingPosts(lastVisible: DocumentSnapshot? = null): QuerySnapshot {
        // Get the list of users that the current user is following
        val followingQuery = db.collection("follows")
            .whereEqualTo("followerId", currentUserId!!)
            .get()
            .await()

        // Extract the IDs of the users being followed
        val followingIds = followingQuery.documents.map { it.getString("followedId") ?: "" } + currentUserId

        // Build the query to get the posts from the followed users, ordered by publication date
        var query = db.collection("posts")
            .whereIn("autorId", followingIds)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .limit(10)

        // If there is a last visible document, start the query after this document
        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        // Execute the query and return the results
        return query.get().await()
    }
}
