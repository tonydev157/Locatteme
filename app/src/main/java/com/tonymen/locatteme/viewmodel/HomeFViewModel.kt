package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

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