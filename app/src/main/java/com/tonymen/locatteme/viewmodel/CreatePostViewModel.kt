package com.tonymen.locatteme.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.tonymen.locatteme.R
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.EcuadorLocations
import com.tonymen.locatteme.model.Nationalities
import java.io.InputStreamReader

class CreatePostViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    lateinit var provinces: List<EcuadorLocations>
    lateinit var nationalities: List<String>

    fun loadEcuadorLocations(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.ecuador_locations)
        val reader = InputStreamReader(inputStream)
        provinces = Gson().fromJson(reader, Array<EcuadorLocations>::class.java).toList()
        reader.close()
    }

    fun loadNationalities(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.nacionalidades)
        val reader = InputStreamReader(inputStream)
        val nationalityList = Gson().fromJson(reader, Nationalities::class.java)
        nationalities = nationalityList.nationalidades
        reader.close()
    }

    fun addPost(post: Post) = db.collection("posts").add(post)

    fun getPostImageStorageReference(userId: String, postId: String) =
        FirebaseStorage.getInstance().reference.child("postImages/$userId/$postId.jpg")

    fun setLoadingState(isLoading: Boolean) {
        _loading.value = isLoading
    }
}
