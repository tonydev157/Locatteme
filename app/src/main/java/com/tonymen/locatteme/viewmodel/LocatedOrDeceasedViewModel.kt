package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tonymen.locatteme.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocatedOrDeceasedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    fun loadPosts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("posts")
                    .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val postsList = snapshot.toObjects(Post::class.java)
                    .filter { it.estado != "Desaparecido" }
                _posts.postValue(postsList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
