package com.tonymen.locatteme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.tonymen.locatteme.model.User

class FollowingNViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _following = MutableLiveData<List<User>>()
    val following: LiveData<List<User>> get() = _following

    fun getFollowing(userId: String): LiveData<List<User>> {
        db.collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val userIds = documents.map { it.getString("followedId") ?: "" }
                if (userIds.isNotEmpty()) {
                    db.collection("users")
                        .whereIn("id", userIds)
                        .get()
                        .addOnSuccessListener { userDocs ->
                            val userList = userDocs.toObjects<User>()
                            _following.value = userList
                        }
                } else {
                    _following.value = emptyList()
                }
            }
            .addOnFailureListener {
                _following.value = emptyList()
            }
        return following
    }
}
