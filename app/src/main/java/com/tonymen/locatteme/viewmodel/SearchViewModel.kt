package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    fun searchUsers(query: String) {
        val lowercaseQuery = query.toLowerCase()
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.mapNotNull { it.toObject(User::class.java) }
                // Filtrar usuarios localmente sin importar mayúsculas/minúsculas
                val filteredList = userList.filter {
                    it.username.toLowerCase().contains(lowercaseQuery)
                }
                _users.value = filteredList
            }
            .addOnFailureListener {
                _users.value = emptyList()
            }
    }

    fun searchPosts(query: String) {
        val lowercaseQuery = query.toLowerCase().trim()
        val words = lowercaseQuery.split(" ")

        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                // Filtrar posts localmente sin importar mayúsculas/minúsculas
                val filteredList = postList.filter { post ->
                    words.all { word ->
                        post.nombres.toLowerCase().contains(word) ||
                                post.apellidos.toLowerCase().contains(word)
                    }
                }
                _posts.value = filteredList
            }
            .addOnFailureListener {
                _posts.value = emptyList()
            }
    }
}
