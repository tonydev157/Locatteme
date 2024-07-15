package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import java.text.SimpleDateFormat
import java.util.*

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

    private val _filteredPosts = MutableLiveData<List<Post>>()
    val filteredPosts: LiveData<List<Post>> get() = _filteredPosts

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun searchUsers(query: String) {
        val lowercaseQuery = query.lowercase(Locale.getDefault())
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.mapNotNull { it.toObject(User::class.java) }
                val filteredList = userList.filter {
                    it.username.lowercase(Locale.getDefault()).contains(lowercaseQuery)
                }
                _users.value = filteredList
            }
            .addOnFailureListener {
                _users.value = emptyList()
            }
    }

    fun searchPosts(query: String) {
        val lowercaseQuery = query.lowercase(Locale.getDefault()).trim()
        val words = lowercaseQuery.split(" ")

        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                val filteredList = postList.filter { post ->
                    words.all { word ->
                        post.nombres.lowercase(Locale.getDefault()).contains(word) ||
                                post.apellidos.lowercase(Locale.getDefault()).contains(word)
                    }
                }
                _posts.value = filteredList
            }
            .addOnFailureListener {
                _posts.value = emptyList()
            }
    }

    fun searchFilteredPosts(query: String) {
        val lowercaseQuery = query.lowercase(Locale.getDefault()).trim()
        val words = lowercaseQuery.split(" ")

        _filteredPosts.value?.let { filteredList ->
            val finalList = filteredList.filter { post ->
                words.all { word ->
                    post.nombres.lowercase(Locale.getDefault()).contains(word) ||
                            post.apellidos.lowercase(Locale.getDefault()).contains(word)
                }
            }
            _posts.value = finalList
        }
    }

    fun filterPosts(
        startDisappearanceDate: String?, endDisappearanceDate: String?,
        startPublicationDate: String?, endPublicationDate: String?,
        status: String?, province: String?, city: String?,
        query: String? = null
    ) {
        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                val filteredList = postList.filter { post ->
                    var matches = true

                    // Check status
                    if (!status.isNullOrEmpty()) {
                        matches = matches && post.estado == status
                    }

                    // Check province
                    if (!province.isNullOrEmpty()) {
                        matches = matches && post.provincia == province
                    }

                    // Check city
                    if (!city.isNullOrEmpty()) {
                        matches = matches && post.ciudad == city
                    }

                    // Check disappearance date range
                    if (!startDisappearanceDate.isNullOrEmpty() || !endDisappearanceDate.isNullOrEmpty()) {
                        val start = startDisappearanceDate?.let { dateFormat.parse(it) }
                        val end = endDisappearanceDate?.let { dateFormat.parse(it) }
                        val disappearanceDate = post.fechaDesaparicion?.toDate()

                        if (start != null) {
                            matches = matches && (disappearanceDate?.after(start) ?: false || disappearanceDate == start)
                        }
                        if (end != null) {
                            matches = matches && (disappearanceDate?.before(end) ?: false || disappearanceDate == end)
                        }
                    }

                    // Check publication date range
                    if (!startPublicationDate.isNullOrEmpty() || !endPublicationDate.isNullOrEmpty()) {
                        val start = startPublicationDate?.let { dateFormat.parse(it) }
                        val end = endPublicationDate?.let { dateFormat.parse(it) }
                        val publicationDate = post.fechaPublicacion?.toDate()

                        if (start != null) {
                            matches = matches && (publicationDate?.after(start) ?: false || publicationDate == start)
                        }
                        if (end != null) {
                            matches = matches && (publicationDate?.before(end) ?: false || publicationDate == end)
                        }
                    }

                    // Check query
                    if (!query.isNullOrEmpty()) {
                        val words = query.split(" ")
                        matches = matches && words.all { word ->
                            post.nombres.lowercase(Locale.getDefault()).contains(word) ||
                                    post.apellidos.lowercase(Locale.getDefault()).contains(word)
                        }
                    }

                    matches
                }
                _filteredPosts.value = filteredList
                _posts.value = filteredList
            }
            .addOnFailureListener {
                _posts.value = emptyList()
                _filteredPosts.value = emptyList()
            }
    }

    fun clearFilters() {
        _filteredPosts.value = emptyList()
        _posts.value = emptyList()
        _users.value = emptyList()
    }

    fun clearUsers() {
        _users.value = emptyList()
    }
}
