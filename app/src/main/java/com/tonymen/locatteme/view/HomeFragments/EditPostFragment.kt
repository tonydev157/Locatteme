package com.tonymen.locatteme.view.HomeFragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentEditPostBinding
import com.tonymen.locatteme.model.EcuadorLocations
import com.tonymen.locatteme.model.Nationalities
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.utils.TimestampUtil
import com.tonymen.locatteme.viewmodel.CreatePostViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.tonymen.locatteme.utils.dpToPx
import com.bumptech.glide.request.target.Target


class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var createPostViewModel: CreatePostViewModel
    private var selectedPhotoUri: Uri? = null
    private lateinit var ecuadorLocations: EcuadorLocations
    private lateinit var nationalities: Nationalities
    private val calendar = Calendar.getInstance()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var postId: String
    private lateinit var documentId: String // Almacenar el ID del documento

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        createPostViewModel = ViewModelProvider(this).get(CreatePostViewModel::class.java)
        binding.viewModel = createPostViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        loadEcuadorLocations()
        loadNationalities()
        setupListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        postId = arguments?.getString("postId") ?: ""
        Log.d("EditPostFragment", "Post ID: $postId")

        if (postId.isNotEmpty()) {
            loadPostData(postId)
        } else {
            Toast.makeText(requireContext(), "Error al cargar el post", Toast.LENGTH_SHORT).show()
        }

        binding.guardarButton.setOnClickListener {
            showLoading(true)
            savePostData()
        }

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
    private fun showLoading(isLoading: Boolean) {
        if (_binding == null) return // Asegúrate de que el binding no es nulo
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.guardarButton.isEnabled = false
            binding.cancelButton.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.guardarButton.isEnabled = true
            binding.cancelButton.isEnabled = true
        }
    }



    private fun loadPostData(postId: String) {
        Log.d("EditPostFragment", "Loading post data for ID: $postId")
        db.collection("posts").whereEqualTo("id", postId).get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d("EditPostFragment", "No document exists with ID: $postId")
                Toast.makeText(requireContext(), "No existe el post", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            for (document in documents) {
                val post = document.toObject(Post::class.java)
                documentId = document.id // Almacenar el ID del documento
                Log.d("EditPostFragment", "Document snapshot: $document")
                if (post != null) {
                    binding.apply {
                        Glide.with(this@EditPostFragment)
                            .load(post.fotoGrande)
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) // Ajusta el tamaño original de la imagen
                            .fitCenter() // Asegúrate de que Glide use fitCenter
                            .into(photoPreviewImageView)
                        photoPreviewImageView.visibility = View.VISIBLE // Asegurarse de que la imagen es visible

                        nombresEditText.setText(post.nombres)
                        apellidosEditText.setText(post.apellidos)
                        edadSpinner.setSelection(post.edad - 1)
                        provinciaAutoComplete.setText(post.provincia, false)
                        ciudadAutoComplete.setText(post.ciudad, false)
                        nacionalidadAutoComplete.setText(post.nacionalidad, false)
                        lugarDesaparicionEditText.setText(post.lugarDesaparicion)
                        fechaDesaparicionEditText.setText(TimestampUtil.formatTimestampToString(post.fechaDesaparicion))
                        caracteristicasEditText.setText(post.caracteristicas)
                        estadoSpinner.setSelection(getEstadoIndex(post.estado)) // Set the spinner to the correct state

                        // Load contact numbers
                        if (post.numerosContacto.isNotEmpty()) {
                            binding.contactoEditText1.setText(post.numerosContacto[0])
                            post.numerosContacto.drop(1).forEach { numero ->
                                addContactoEditText(numero)
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar los datos del post",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("EditPostFragment", "Error al cargar el post: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Error al cargar el post: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getEstadoIndex(estado: String): Int {
        val estados = listOf("Desaparecido", "Localizado", "Muerto")
        return estados.indexOf(estado)
    }

    private fun savePostData() {
        if (selectedPhotoUri != null) {
            uploadPhotoAndSaveData()
        } else {
            updatePostData(null, null)
        }
    }

    private fun uploadPhotoAndSaveData() {
        val storageRef = storage.reference
        val photoRef = storageRef.child("photos/${UUID.randomUUID()}")
        val uploadTask = photoRef.putFile(selectedPhotoUri!!)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            photoRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                updatePostData(downloadUri.toString(), downloadUri.toString()) // Replace with actual URLs for different sizes
            } else {
                Toast.makeText(requireContext(), "Error al subir la foto", Toast.LENGTH_SHORT).show()
                showLoading(false) // Ocultar el ProgressBar y habilitar los botones en caso de error
            }
        }
    }

    private fun updatePostData(fotoPequena: String?, fotoGrande: String?) {
        val postUpdates = mutableMapOf(
            "nombres" to binding.nombresEditText.text.toString(),
            "apellidos" to binding.apellidosEditText.text.toString(),
            "edad" to binding.edadSpinner.selectedItem.toString().toIntOrNull(),
            "provincia" to binding.provinciaAutoComplete.text.toString(),
            "ciudad" to binding.ciudadAutoComplete.text.toString(),
            "nacionalidad" to binding.nacionalidadAutoComplete.text.toString(),
            "lugarDesaparicion" to binding.lugarDesaparicionEditText.text.toString(),
            "fechaDesaparicion" to TimestampUtil.parseStringToTimestamp(binding.fechaDesaparicionEditText.text.toString()),
            "caracteristicas" to binding.caracteristicasEditText.text.toString(),
            "numerosContacto" to getContactos(),
            "estado" to binding.estadoSpinner.selectedItem.toString()
        )

        if (fotoPequena != null && fotoGrande != null) {
            postUpdates["fotoPequena"] = fotoPequena
            postUpdates["fotoGrande"] = fotoGrande
        }

        val documentReference = db.collection("posts").document(documentId)

        documentReference.update(postUpdates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Post actualizado", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }.addOnFailureListener { e ->
            Toast.makeText(
                requireContext(),
                "Error al actualizar el post: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }.addOnCompleteListener {
            showLoading(false) // Ocultar el ProgressBar y habilitar los botones después de la operación
        }
    }

    private fun loadEcuadorLocations() {
        val inputStream = resources.openRawResource(R.raw.ecuador_locations)
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<EcuadorLocations>() {}.type
        ecuadorLocations = Gson().fromJson(json, type)
    }

    private fun loadNationalities() {
        val inputStream = resources.openRawResource(R.raw.nacionalidades)
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<Nationalities>() {}.type
        nationalities = Gson().fromJson(json, type)
    }

    private fun setupListeners() {
        setupEdadSpinner()
        setupProvinciaAutoComplete()
        setupCiudadAutoComplete()
        setupNacionalidadAutoComplete()
        setupEstadoSpinner()
        setupContactos()

        binding.uploadPhotoButton.setOnClickListener {
            openImagePicker()
        }

        binding.fechaDesaparicionEditText.setOnClickListener {
            showDatePickerDialog()
        }

        binding.nombresEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateNombre(binding.nombresEditText)
        }
        binding.apellidosEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateApellido(binding.apellidosEditText)
        }
        binding.provinciaAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(binding.provinciaAutoComplete, binding.provinciaErrorText)
        }
        binding.ciudadAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(binding.ciudadAutoComplete, binding.ciudadErrorText)
        }
        binding.nacionalidadAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(
                binding.nacionalidadAutoComplete,
                binding.nacionalidadErrorText,
                optional = true
            )
        }
        binding.lugarDesaparicionEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(
                binding.lugarDesaparicionEditText,
                binding.lugarDesaparicionErrorText,
                optional = true
            )
        }
        binding.fechaDesaparicionEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(
                binding.fechaDesaparicionEditText,
                binding.fechaDesaparicionErrorText
            )
        }
        binding.caracteristicasEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(
                binding.caracteristicasEditText,
                binding.caracteristicasErrorText,
                optional = true
            )
        }

        binding.nombresEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateNombre(binding.nombresEditText)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.apellidosEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateApellido(binding.apellidosEditText)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.contactoEditText1.filters = arrayOf(InputFilter.LengthFilter(10))
        binding.contactoEditText1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateContacto(binding.contactoEditText1)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupEdadSpinner() {
        val edades = (1..100).toList().map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, edades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.edadSpinner.adapter = adapter
        binding.edadSpinner.setSelection(0)
    }

    private fun setupProvinciaAutoComplete() {
        val provincias = ecuadorLocations.provinces.map { it.name }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provincias)
        binding.provinciaAutoComplete.setAdapter(adapter)
        binding.provinciaAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedProvincia = adapter.getItem(position).toString()
            binding.provinciaAutoComplete.setText(selectedProvincia, false)
            binding.provinciaAutoComplete.isEnabled = false
            binding.clearProvinciaButton.visibility = View.VISIBLE
            updateCiudadesAutoComplete(selectedProvincia)
        }
        binding.clearProvinciaButton.setOnClickListener {
            binding.provinciaAutoComplete.text.clear()
            binding.provinciaAutoComplete.isEnabled = true
            binding.clearProvinciaButton.visibility = View.GONE
            binding.ciudadAutoComplete.text.clear()
            binding.ciudadAutoComplete.isEnabled = false
        }
    }

    private fun setupCiudadAutoComplete() {
        binding.ciudadAutoComplete.isEnabled = false
        binding.ciudadAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.ciudadAutoComplete.adapter != null) {
                binding.ciudadAutoComplete.showDropDown()
            }
        }
        binding.ciudadAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedCiudad =
                (binding.ciudadAutoComplete.adapter as ArrayAdapter<String>).getItem(position)
                    .toString()
            binding.ciudadAutoComplete.setText(selectedCiudad, false)
            binding.ciudadAutoComplete.isEnabled = false
            binding.clearCiudadButton.visibility = View.VISIBLE
        }
        binding.clearCiudadButton.setOnClickListener {
            binding.ciudadAutoComplete.text.clear()
            binding.ciudadAutoComplete.isEnabled = true
            binding.clearCiudadButton.visibility = View.GONE
        }
    }

    private fun setupNacionalidadAutoComplete() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            nationalities.nationalidades
        )
        binding.nacionalidadAutoComplete.setAdapter(adapter)
        binding.nacionalidadAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedNacionalidad = adapter.getItem(position).toString()
            binding.nacionalidadAutoComplete.setText(selectedNacionalidad, false)
            binding.nacionalidadAutoComplete.isEnabled = false
            binding.clearNacionalidadButton.visibility = View.VISIBLE
        }
        binding.clearNacionalidadButton.setOnClickListener {
            binding.nacionalidadAutoComplete.text.clear()
            binding.nacionalidadAutoComplete.isEnabled = true
            binding.clearNacionalidadButton.visibility = View.GONE
        }
    }

    private fun setupEstadoSpinner() {
        val estados = listOf("Desaparecido", "Localizado", "Muerto")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.estadoSpinner.adapter = adapter
    }

    private fun updateCiudadesAutoComplete(provincia: String) {
        val provinciaObj = ecuadorLocations.provinces.find { it.name == provincia }
        if (provinciaObj != null) {
            val ciudades = provinciaObj.cities
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ciudades
            )
            binding.ciudadAutoComplete.setAdapter(adapter)
            binding.ciudadAutoComplete.isEnabled = true
        }
    }

    private fun setupContactos() {
        binding.addContactoButton.setOnClickListener {
            addContactoEditText()
        }
    }

    private fun addContactoEditText(numero: String = "") {
        val contactoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8.dpToPx(), 0, 0)
            }
        }

        val contactoEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
            hint = "Número de Contacto"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            filters = arrayOf(InputFilter.LengthFilter(10))
            setText(numero) // Set the initial value if provided
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    validateContacto(this@apply)
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        val removeButton = Button(requireContext()).apply {
            text = "-"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8.dpToPx(), 0, 0, 0)
            }
            background = resources.getDrawable(R.drawable.rounded_button_background, null)
            setOnClickListener {
                binding.contactosContainer.removeView(contactoLayout)
            }
        }

        contactoLayout.addView(contactoEditText)
        contactoLayout.addView(removeButton)
        binding.contactosContainer.addView(contactoLayout)
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
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) // Ajusta el tamaño original de la imagen
                    .fitCenter() // Asegúrate de que Glide use fitCenter
                    .into(binding.photoPreviewImageView)
                binding.photoPreviewImageView.visibility = View.VISIBLE
                Toast.makeText(context, "Foto seleccionada correctamente", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun showDatePickerDialog() {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateLabel()
            }

        DatePickerDialog(
            requireContext(), dateSetListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateLabel() {
        val myFormat = "dd/MM/yyyy" // In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.fechaDesaparicionEditText.setText(sdf.format(calendar.time))
    }

    private fun validateField(
        editText: EditText,
        errorTextView: TextView,
        optional: Boolean = false
    ) {
        if (editText.text.toString().trim().isEmpty() && !optional) {
            errorTextView.visibility = View.VISIBLE
            editText.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            errorTextView.visibility = View.GONE
            editText.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun validateField(
        autoCompleteTextView: AutoCompleteTextView,
        errorTextView: TextView,
        optional: Boolean = false
    ) {
        if (autoCompleteTextView.text.toString().trim().isEmpty() && !optional) {
            errorTextView.visibility = View.VISIBLE
            autoCompleteTextView.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            errorTextView.visibility = View.GONE
            autoCompleteTextView.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun validateNombre(editText: EditText) {
        val pattern = Regex("^[A-Z][a-zA-ZñÑ]*$")
        if (!pattern.matches(editText.text.toString().trim())) {
            editText.error = "Nombre inválido."
            editText.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            editText.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun validateApellido(editText: EditText) {
        val pattern = Regex("^[A-Z][a-zA-ZñÑ]*$")
        if (!pattern.matches(editText.text.toString().trim())) {
            editText.error = "Apellido inválido."
            editText.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            editText.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }


    private fun validateContacto(editText: EditText) {
        val pattern = Regex("^09[0-9]{8}$")
        if (!pattern.matches(editText.text.toString().trim())) {
            editText.error = "Número de contacto inválido."
            editText.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            editText.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun getContactos(): List<String> {
        val contactos = mutableListOf<String>()
        for (i in 0 until binding.contactosContainer.childCount) {
            val contactoLayout = binding.contactosContainer.getChildAt(i)
            if (contactoLayout is LinearLayout && contactoLayout.childCount > 0 && contactoLayout.getChildAt(
                    0
                ) is EditText
            ) {
                val contactoEditText = contactoLayout.getChildAt(0) as EditText
                val contacto = contactoEditText.text.toString().trim()
                if (contacto.isNotEmpty()) {
                    contactos.add(contacto)
                }
            }
        }
        return contactos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }
}
