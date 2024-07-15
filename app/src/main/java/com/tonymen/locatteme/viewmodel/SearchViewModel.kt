package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.utils.TimestampUtil
import java.util.*

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> get() = _posts

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

    fun filterPosts(startDate: String?, endDate: String?, status: String?, province: String?, city: String?) {
        val start = startDate?.let { TimestampUtil.parseStringToTimestamp(it) }
        val end = endDate?.let { TimestampUtil.parseStringToTimestamp(it) }

        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                val filteredList = postList.filter { post ->
                    val isWithinDateRange = (start == null || post.fechaPublicacion?.toDate()?.after(start.toDate()) == true) &&
                            (end == null || post.fechaPublicacion?.toDate()?.before(end.toDate()) == true)
                    val matchesStatus = status.isNullOrEmpty() || post.estado == status
                    val matchesProvince = province.isNullOrEmpty() || post.provincia == province
                    val matchesCity = city.isNullOrEmpty() || post.ciudad == city

                    isWithinDateRange && matchesStatus && matchesProvince && matchesCity
                }
                _posts.value = filteredList
            }
            .addOnFailureListener {
                _posts.value = emptyList()
            }
    }

    fun getAllPosts() {
        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val postList = documents.mapNotNull { it.toObject(Post::class.java) }
                _posts.value = postList
            }
            .addOnFailureListener {
                _posts.value = emptyList()
            }
    }
}
