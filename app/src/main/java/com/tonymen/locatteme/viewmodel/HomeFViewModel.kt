package com.tonymen.locatteme.viewmodel

import PostDataSource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFViewModel : ViewModel() {

    val posts: Flow<PagingData<Post>> = Pager(PagingConfig(pageSize = 10)) {
        PostDataSource()
    }.flow.cachedIn(viewModelScope)

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    fun loadUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val db = FirebaseFirestore.getInstance()
            val document = db.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            user?.let {
                _user.postValue(it)
            }
        }
    }
}
