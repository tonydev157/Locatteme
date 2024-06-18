package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.tasks.Task

class HomeFViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    fun getPosts(lastVisible: DocumentSnapshot?): Task<QuerySnapshot> {
        val query = if (lastVisible == null) {
            db.collection("posts")
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                .limit(10)
        } else {
            db.collection("posts")
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(10)
        }
        return query.get()
    }
}
