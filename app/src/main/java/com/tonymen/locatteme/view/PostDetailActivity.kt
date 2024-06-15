package com.tonymen.locatteme.view

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityPostDetailBinding
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.model.User
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private var selectedPhotoUri: Uri? = null
    private var postId: String? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

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
        val fechaDesaparicion = intent.getStringExtra("fechaDesaparicion")
        val caracteristicas = intent.getStringExtra("caracteristicas")
        val autorId = intent.getStringExtra("autorId")
        val fechaPublicacion = intent.getStringExtra("fechaPublicacion")

        // Recuperar el nombre del autor en lugar del autorId
        db.collection("users").document(autorId!!).get().addOnSuccessListener { document ->
            val autorNombre = document.getString("nombre") ?: "Desconocido"
            binding.publicadoPorTextView.text = "Publicado por $autorNombre el $fechaPublicacion"
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
            fechaDesaparicionTextView.text = fechaDesaparicion
            caracteristicasTextView.text = caracteristicas
        }

        binding.editButton.setOnClickListener {
            showEditDialog(
                fotoGrande, nombres, apellidos, edad, provincia, ciudad, nacionalidad,
                estado, lugarDesaparicion, fechaDesaparicion, caracteristicas
            )
        }
    }

    private fun showEditDialog(
        fotoGrande: String?,
        nombres: String?,
        apellidos: String?,
        edad: Int,
        provincia: String?,
        ciudad: String?,
        nacionalidad: String?,
        estado: String?,
        lugarDesaparicion: String?,
        fechaDesaparicion: String?,
        caracteristicas: String?
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_post, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Editar Post")
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogView.findViewById<EditText>(R.id.editTextNombre).text.toString()
                val apellido = dialogView.findViewById<EditText>(R.id.editTextApellido).text.toString()
                val edad = dialogView.findViewById<EditText>(R.id.editTextEdad).text.toString().toInt()
                val provincia = dialogView.findViewById<EditText>(R.id.editTextProvincia).text.toString()
                val ciudad = dialogView.findViewById<EditText>(R.id.editTextCiudad).text.toString()
                val nacionalidad = dialogView.findViewById<EditText>(R.id.editTextNacionalidad).text.toString()
                val estado = dialogView.findViewById<Spinner>(R.id.spinnerEstado).selectedItem.toString()
                val lugarDesaparicion = dialogView.findViewById<EditText>(R.id.editTextLugarDesaparicion).text.toString()
                val fechaDesaparicion = dialogView.findViewById<EditText>(R.id.editTextFechaDesaparicion).text.toString()
                val caracteristicas = dialogView.findViewById<EditText>(R.id.editTextCaracteristicas).text.toString()

                val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val fechaDesaparicionDate = format.parse(fechaDesaparicion)

                val post = Post(
                    id = postId!!,
                    fotoPequena = intent.getStringExtra("fotoPequena")!!,
                    fotoGrande = intent.getStringExtra("fotoGrande")!!,
                    nombres = nombre,
                    apellidos = apellido,
                    edad = edad,
                    provincia = provincia,
                    ciudad = ciudad,
                    nacionalidad = nacionalidad,
                    estado = estado,
                    lugarDesaparicion = lugarDesaparicion,
                    fechaDesaparicion = Timestamp(fechaDesaparicionDate),
                    caracteristicas = caracteristicas,
                    fechaPublicacion = Timestamp.now(),
                    autorId = FirebaseAuth.getInstance().currentUser!!.uid
                )

                db.collection("posts").document(postId!!)
                    .set(post)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post actualizado", Toast.LENGTH_SHORT).show()
                        finish() // Cerrar la actividad después de guardar los cambios
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar el post", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        // Cargar la imagen actual en el diálogo de edición
        Glide.with(this)
            .load(fotoGrande)
            .into(dialogView.findViewById<ImageView>(R.id.editImageView))

        // Configurar el DatePicker para fecha de desaparición
        dialogView.findViewById<EditText>(R.id.editTextFechaDesaparicion).setOnClickListener {
            showDatePickerDialog(it as EditText)
        }

        dialogView.findViewById<Button>(R.id.changePhotoButton).setOnClickListener {
            openImagePicker()
        }

        // Poner los valores actuales en los campos de edición
        dialogView.findViewById<EditText>(R.id.editTextNombre).setText(nombres)
        dialogView.findViewById<EditText>(R.id.editTextApellido).setText(apellidos)
        dialogView.findViewById<EditText>(R.id.editTextEdad).setText(edad.toString())
        dialogView.findViewById<EditText>(R.id.editTextProvincia).setText(provincia)
        dialogView.findViewById<EditText>(R.id.editTextCiudad).setText(ciudad)
        dialogView.findViewById<EditText>(R.id.editTextNacionalidad).setText(nacionalidad)
        dialogView.findViewById<EditText>(R.id.editTextLugarDesaparicion).setText(lugarDesaparicion)
        dialogView.findViewById<EditText>(R.id.editTextFechaDesaparicion).setText(fechaDesaparicion)
        dialogView.findViewById<EditText>(R.id.editTextCaracteristicas).setText(caracteristicas)

        dialog.show()
    }

    private fun showDatePickerDialog(editText: EditText) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val myFormat = "dd/MM/yyyy"
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            editText.setText(sdf.format(calendar.time))
        }

        DatePickerDialog(
            this@PostDetailActivity, dateSetListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            selectedPhotoUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .override(600, 900)
                    .into(binding.photoImageView)
                Toast.makeText(this, "Foto seleccionada correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }
}
