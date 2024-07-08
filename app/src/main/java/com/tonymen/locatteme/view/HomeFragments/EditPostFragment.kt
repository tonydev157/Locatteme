package com.tonymen.locatteme.view.HomeFragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentEditPostBinding
import com.tonymen.locatteme.model.EcuadorLocations
import com.tonymen.locatteme.model.Nationalities
import com.tonymen.locatteme.model.Post
import com.tonymen.locatteme.utils.SearchUtils
import com.tonymen.locatteme.viewmodel.CreatePostViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.tonymen.locatteme.view.HomeActivity
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var createPostViewModel: CreatePostViewModel
    private var selectedPhotoUri: Uri? = null
    private lateinit var ecuadorLocations: EcuadorLocations
    private lateinit var nationalities: Nationalities
    private val calendar = Calendar.getInstance()
    private lateinit var post: Post

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        createPostViewModel = ViewModelProvider(this).get(CreatePostViewModel::class.java)
        binding.viewModel = createPostViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val postId = arguments?.getString("postId") ?: return binding.root

        loadEcuadorLocations()
        loadNationalities()
        loadPostData(postId)
        setupListeners()

        return binding.root
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

    private fun loadPostData(postId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts").document(postId).get().addOnSuccessListener { document ->
            post = document.toObject(Post::class.java) ?: return@addOnSuccessListener
            populateFields(post)
        }
    }

    private fun populateFields(post: Post) {
        binding.apply {
            Glide.with(this@EditPostFragment)
                .load(post.fotoGrande)
                .into(photoPreviewImageView)
            nombresEditText.setText(post.nombres)
            apellidosEditText.setText(post.apellidos)
            edadSpinner.setSelection(post.edad - 1)
            provinciaAutoComplete.setText(post.provincia, false)
            ciudadAutoComplete.setText(post.ciudad, false)
            nacionalidadAutoComplete.setText(post.nacionalidad, false)
            lugarDesaparicionEditText.setText(post.lugarDesaparicion)
            fechaDesaparicionEditText.setText(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(post.fechaDesaparicion.toDate()))
            caracteristicasEditText.setText(post.caracteristicas)
            post.numerosContacto.forEachIndexed { index, numero ->
                if (index == 0) {
                    contactoEditText1.setText(numero)
                } else {
                    addContactoEditText().setText(numero)
                }
            }
        }
    }

    private fun setupListeners() {
        setupEdadSpinner()
        setupProvinciaAutoComplete()
        setupCiudadAutoComplete()
        setupNacionalidadAutoComplete()
        setupContactos()

        binding.uploadPhotoButton.setOnClickListener {
            openImagePicker()
        }

        binding.guardarButton.setOnClickListener {
            guardarPost()
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
            if (!hasFocus) validateField(binding.nacionalidadAutoComplete, binding.nacionalidadErrorText, optional = true)
        }
        binding.lugarDesaparicionEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(binding.lugarDesaparicionEditText, binding.lugarDesaparicionErrorText, optional = true)
        }
        binding.fechaDesaparicionEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(binding.fechaDesaparicionEditText, binding.fechaDesaparicionErrorText)
        }
        binding.caracteristicasEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateField(binding.caracteristicasEditText, binding.caracteristicasErrorText, optional = true)
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, provincias)
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
            val selectedCiudad = (binding.ciudadAutoComplete.adapter as ArrayAdapter<String>).getItem(position).toString()
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nationalities.nationalidades)
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

    private fun updateCiudadesAutoComplete(provincia: String) {
        val provinciaObj = ecuadorLocations.provinces.find { it.name == provincia }
        if (provinciaObj != null) {
            val ciudades = provinciaObj.cities
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ciudades)
            binding.ciudadAutoComplete.setAdapter(adapter)
            binding.ciudadAutoComplete.isEnabled = true
        }
    }

    private fun setupContactos() {
        binding.addContactoButton.setOnClickListener {
            addContactoEditText()
        }
    }

    private fun addContactoEditText(): EditText {
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
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    validateContacto(this@apply)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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
        return contactoEditText
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
                    .into(binding.photoPreviewImageView)
                binding.photoPreviewImageView.visibility = View.VISIBLE
                Toast.makeText(context, "Foto seleccionada correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadPostImagesToFirebase(
        imageUri: Uri,
        onSuccess: (String, String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRefSmall = createPostViewModel.getPostImageStorageReference(usuarioId, "${post.id}_small")
        val storageRefLarge = createPostViewModel.getPostImageStorageReference(usuarioId, "${post.id}_large")

        Glide.with(this)
            .asBitmap()
            .load(imageUri)
            .override(184, 284)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    uploadBitmapToStorage(storageRefSmall, resource, { smallImageUrl ->
                        Glide.with(this@EditPostFragment)
                            .asBitmap()
                            .load(imageUri)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    uploadBitmapToStorage(storageRefLarge, resource, { largeImageUrl ->
                                        onSuccess(smallImageUrl, largeImageUrl)
                                    }, { exception ->
                                        onFailure(exception)
                                    })
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })
                    }, { exception ->
                        onFailure(exception)
                    })
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun uploadBitmapToStorage(
        storageRef: StorageReference,
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { exception ->
                    onFailure(exception)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    private fun guardarPost() {
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = binding.nombresEditText.text.toString().trim()
        val apellidos = binding.apellidosEditText.text.toString().trim()
        val edad = binding.edadSpinner.selectedItem.toString().toInt()
        val provincia = binding.provinciaAutoComplete.text.toString().trim()
        val ciudad = binding.ciudadAutoComplete.text.toString().trim()
        val nacionalidad = binding.nacionalidadAutoComplete.text.toString().trim()
        val lugarDesaparicion = binding.lugarDesaparicionEditText.text.toString().trim()
        val caracteristicas = binding.caracteristicasEditText.text.toString().trim()
        val fechaDesaparicion = binding.fechaDesaparicionEditText.text.toString().trim()
        val numerosContacto = getContactos()

        if (nombres.isEmpty() || apellidos.isEmpty() || (selectedPhotoUri == null && post.fotoGrande.isEmpty()) || fechaDesaparicion.isEmpty()) {
            Toast.makeText(context, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            validateField(binding.nombresEditText, binding.nombresErrorText)
            validateField(binding.apellidosEditText, binding.apellidosErrorText)
            validateField(binding.provinciaAutoComplete, binding.provinciaErrorText)
            validateField(binding.ciudadAutoComplete, binding.ciudadErrorText)
            validateField(binding.fechaDesaparicionEditText, binding.fechaDesaparicionErrorText)
            return
        }

        val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val fechaDesaparicionDate = format.parse(fechaDesaparicion)
        val currentDate = Date()

        if (fechaDesaparicionDate != null && fechaDesaparicionDate.after(currentDate)) {
            Toast.makeText(context, "La fecha de desaparición no puede ser en el futuro", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.guardarButton.isEnabled = false

        selectedPhotoUri?.let { uri ->
            uploadPostImagesToFirebase(uri, { smallImageUrl, largeImageUrl ->
                val searchKeywords = SearchUtils.generateSearchKeywords(nombres, apellidos)
                val updatedPost = post.copy(
                    fotoPequena = smallImageUrl,
                    fotoGrande = largeImageUrl,
                    nombres = nombres,
                    apellidos = apellidos,
                    edad = edad,
                    provincia = provincia,
                    ciudad = ciudad,
                    nacionalidad = nacionalidad,
                    lugarDesaparicion = lugarDesaparicion,
                    fechaDesaparicion = Timestamp(fechaDesaparicionDate),
                    caracteristicas = caracteristicas,
                    numerosContacto = numerosContacto,
                    searchKeywords = searchKeywords
                )

                createPostViewModel.updatePost(updatedPost)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        binding.guardarButton.isEnabled = true
                        Toast.makeText(context, "Post actualizado exitosamente", Toast.LENGTH_SHORT).show()
                        (activity as HomeActivity).isPostSaved = true
                        requireActivity().onBackPressed()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        binding.guardarButton.isEnabled = true
                        Toast.makeText(context, "Error al actualizar el post: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }, { exception ->
                binding.progressBar.visibility = View.GONE
                binding.guardarButton.isEnabled = true
                Toast.makeText(context, "Error al subir la imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
            })
        } ?: run {
            val searchKeywords = SearchUtils.generateSearchKeywords(nombres, apellidos)
            val updatedPost = post.copy(
                nombres = nombres,
                apellidos = apellidos,
                edad = edad,
                provincia = provincia,
                ciudad = ciudad,
                nacionalidad = nacionalidad,
                lugarDesaparicion = lugarDesaparicion,
                fechaDesaparicion = Timestamp(fechaDesaparicionDate),
                caracteristicas = caracteristicas,
                numerosContacto = numerosContacto,
                searchKeywords = searchKeywords
            )

            createPostViewModel.updatePost(updatedPost)
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    binding.guardarButton.isEnabled = true
                    Toast.makeText(context, "Post actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    (activity as HomeActivity).isPostSaved = true
                    requireActivity().onBackPressed()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.guardarButton.isEnabled = true
                    Toast.makeText(context, "Error al actualizar el post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun validateField(editText: EditText, errorTextView: TextView, optional: Boolean = false) {
        if (editText.text.toString().trim().isEmpty() && !optional) {
            errorTextView.visibility = View.VISIBLE
            editText.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            errorTextView.visibility = View.GONE
            editText.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun validateField(autoCompleteTextView: AutoCompleteTextView, errorTextView: TextView, optional: Boolean = false) {
        if (autoCompleteTextView.text.toString().trim().isEmpty() && !optional) {
            errorTextView.visibility = View.VISIBLE
            autoCompleteTextView.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            errorTextView.visibility = View.GONE
            autoCompleteTextView.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun validateNombre(editText: EditText) {
        val pattern = Regex("^[A-Z][a-zA-Z]*$")
        if (!pattern.matches(editText.text.toString().trim())) {
            editText.error = "Nombre inválido."
            editText.setBackgroundResource(R.drawable.edit_text_border_red)
        } else {
            editText.setBackgroundResource(R.drawable.edit_text_border_green)
        }
    }

    private fun validateApellido(editText: EditText) {
        val pattern = Regex("^[A-Z][a-zA-Z]*$")
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
            if (contactoLayout is LinearLayout && contactoLayout.childCount > 0 && contactoLayout.getChildAt(0) is EditText) {
                val contactoEditText = contactoLayout.getChildAt(0) as EditText
                val contacto = contactoEditText.text.toString().trim()
                if (contacto.isNotEmpty()) {
                    contactos.add(contacto)
                }
            }
        }
        return contactos
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
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
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.fechaDesaparicionEditText.setText(sdf.format(calendar.time))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val homeActivity = activity as? HomeActivity
        homeActivity?.enableCreatePostButton()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }
}
