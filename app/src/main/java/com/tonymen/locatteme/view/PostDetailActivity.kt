package com.tonymen.locatteme.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityPostDetailBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.utils.TimestampUtil
import com.tonymen.locatteme.view.HomeFragments.EditPostFragment
import java.text.SimpleDateFormat
import java.util.*

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private var selectedPhotoUri: Uri? = null
    private var postId: String? = null
    private val calendar = Calendar.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference
        auth = FirebaseAuth.getInstance()

        postId = intent.getStringExtra("postId")

        val fotoGrande = intent.getStringExtra("fotoGrande")
        val nombres = intent.getStringExtra("nombres")
        val apellidos = intent.getStringExtra("apellidos")
        val edad = intent.getIntExtra("edad", 0)
        val provincia = intent.getStringExtra("provincia")
        val ciudad = intent.getStringExtra("ciudad")
        val nacionalidad = intent.getStringExtra("nacionalidad")
        val estado = intent.getStringExtra("estado")
        val lugarDesaparicion = intent.getStringExtra("lugarDesaparicion")
        val fechaDesaparicionStr = intent.getStringExtra("fechaDesaparicion")
        val caracteristicas = intent.getStringExtra("caracteristicas")
        val autorId = intent.getStringExtra("autorId")
        val fechaPublicacionStr = intent.getStringExtra("fechaPublicacion")

        val fechaDesaparicion = TimestampUtil.parseStringToTimestamp(fechaDesaparicionStr)
        val fechaPublicacion = TimestampUtil.parseStringToTimestamp(fechaPublicacionStr)

        db.collection("users").document(autorId!!).get().addOnSuccessListener { document ->
            val autorNombre = document.getString("nombre") ?: "Desconocido"
            binding.publicadoPorTextView.text = "Publicado por $autorNombre el ${TimestampUtil.formatTimestampToString(fechaPublicacion)}"
        }

        binding.apply {
            Glide.with(this@PostDetailActivity)
                .load(fotoGrande)
                .into(photoImageView)

            nombreTextView.text = nombres
            apellidosTextView.text = apellidos
            edadTextView.text = edad.toString()
            provinciaTextView.text = provincia
            ciudadTextView.text = ciudad
            nacionalidadTextView.text = nacionalidad
            estadoTextView.text = estado
            lugarDesaparicionTextView.text = lugarDesaparicion
            fechaDesaparicionTextView.text = TimestampUtil.formatTimestampToString(fechaDesaparicion)
            caracteristicasTextView.text = caracteristicas
        }

        if (autorId == auth.currentUser?.uid) {
            binding.editButton.visibility = View.VISIBLE
        } else {
            binding.editButton.visibility = View.GONE
        }

        binding.editButton.setOnClickListener {
            openEditPostFragment()
        }
    }

    private fun openEditPostFragment() {
        val fragment = EditPostFragment().apply {
            arguments = Bundle().apply {
                putString("postId", postId)
            }
        }
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
            addToBackStack(null)
        }
    }
}
