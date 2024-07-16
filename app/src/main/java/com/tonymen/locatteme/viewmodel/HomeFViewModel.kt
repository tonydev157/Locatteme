package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tonymen.locatteme.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    private var lastVisible: DocumentSnapshot? = null

    fun loadPosts(lastVisible: DocumentSnapshot?, onComplete: (DocumentSnapshot?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val query = if (lastVisible == null) {
                    db.collection("posts")
                        .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                } else {
                    db.collection("posts")
                        .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                        .startAfter(lastVisible)
                }
                val snapshot = query.get().await()
                val postList = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                val filteredPosts = postList.filter { it.estado == "Desaparecido" }
                if (filteredPosts.isNotEmpty()) {
                    this@HomeFViewModel.lastVisible = snapshot.documents[snapshot.size() - 1]
                    _posts.postValue(filteredPosts)
                    onComplete(snapshot.documents[snapshot.size() - 1])
                } else {
                    onComplete(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }
}
