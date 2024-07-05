package com.tonymen.locatteme.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.tonymen.locatteme.model.User

class FollowersNViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _followers = MutableLiveData<List<User>>()
    val followers: LiveData<List<User>> get() = _followers

    fun getFollowers(userId: String): LiveData<List<User>> {
        val followersLiveData = MutableLiveData<List<User>>()

        db.collection("follows").whereEqualTo("followedId", userId).get()
            .addOnSuccessListener { documents ->
                val followerIds = documents.mapNotNull { it.getString("followerId") }
                Log.d("FollowersNViewModel", "Follower IDs: $followerIds")

                if (followerIds.isNotEmpty()) {
                    db.collection("users").whereIn("id", followerIds).get().addOnSuccessListener { querySnapshot ->
                        val followersList = querySnapshot.toObjects(User::class.java)
                        Log.d("FollowersNViewModel", "Followers List: $followersList")
                        followersLiveData.value = followersList
                    }.addOnFailureListener { exception ->
                        Log.e("FollowersNViewModel", "Error getting followers: ", exception)
                        followersLiveData.value = emptyList()
                    }
                } else {
                    followersLiveData.value = emptyList()
                }
            }.addOnFailureListener { exception ->
                Log.e("FollowersNViewModel", "Error getting follows: ", exception)
                followersLiveData.value = emptyList()
            }

        return followersLiveData
    }
}
