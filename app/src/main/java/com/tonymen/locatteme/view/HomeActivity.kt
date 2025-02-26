package com.tonymen.locatteme.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityHomeBinding
import com.tonymen.locatteme.model.JSONHelper
import com.tonymen.locatteme.model.UPC
import com.tonymen.locatteme.view.HomeFragments.*
import com.tonymen.locatteme.viewmodel.HomeViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false
    private val viewModel: HomeViewModel by viewModels()
    private var isCreatePostButtonEnlarged = false
    var isPostSaved = false // Variable to track if the post is saved
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isNavigating = false
    private val navigationHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if the user is authenticated
        val user = auth.currentUser
        if (user == null) {
            navigateToLogin()
        } else if (!user.isEmailVerified) {
            showToast("Por favor, verifica tu correo electrónico.", 2000, R.color.primaryColor)
        }

        val navView: BottomNavigationView = binding.bottomNavigationView

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    handleNavigation(HomeFragment(), "HomeFragment")
                    true
                }
                R.id.navigation_following -> {
                    handleNavigation(FollowingFragment(), "FollowingFragment")
                    true
                }
                R.id.navigation_create_post -> {
                    if (user == null || !user.isEmailVerified) {
                        showToast("Autentica tu correo electrónico", 2000, R.color.primaryColor)
                        false
                    } else {
                        handleCreatePostNavigation()
                        true
                    }
                }
                R.id.navigation_search -> {
                    handleNavigation(SearchFragment(), "SearchFragment")
                    true
                }
                R.id.navigation_profile -> {
                    handleNavigation(ProfileFragment(), "ProfileFragment")
                    true
                }
                else -> false
            }
        }

        // Load the default fragment
        if (savedInstanceState == null) {
            navView.selectedItemId = R.id.navigation_home
        }

        // Add settings icon functionality
        binding.settingsIcon.setOnClickListener { view ->
            showPopupMenu(view)
        }

        // Listener to update the BottomNavigationView selection
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            when (currentFragment) {
                is HomeFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_home
                is FollowingFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_following
                is CreatePostFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_create_post
                is SearchFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_search
                is ProfileFragment -> binding.bottomNavigationView.selectedItemId = R.id.navigation_profile
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkVerificationStatus()
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) {
            showToast("Por favor, verifica tu correo electrónico.", 2000, R.color.primaryColor)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleCreatePostNavigation() {
        if (isNavigating) return
        isNavigating = true
        navigationHandler.postDelayed({ isNavigating = false }, 500)  // 500 ms delay

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is CreatePostFragment && !isPostSaved) {
            showExitWarningDialog {
                removeFragmentFromBackStack("CreatePostFragment")
                loadFragment(CreatePostFragment(), "CreatePostFragment")
                disableCreatePostButton()
            }
        } else {
            loadFragment(CreatePostFragment(), "CreatePostFragment")
            disableCreatePostButton()
        }
    }

    private fun showToast(message: String, duration: Int, color: Int) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        val view = toast.view
        val text = view?.findViewById<TextView>(android.R.id.message)
        text?.setTextColor(ContextCompat.getColor(this, color))
        toast.show()

        Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, duration.toLong())
    }

    private fun handleNavigation(fragment: Fragment, tag: String) {
        if (isNavigating) return
        isNavigating = true
        navigationHandler.postDelayed({ isNavigating = false }, 500)  // 500 ms delay

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is CreatePostFragment && !isPostSaved) {
            showExitWarningDialog {
                removeFragmentFromBackStack("CreatePostFragment")
                loadFragment(fragment, tag)
            }
        } else {
            val fragmentInStack = supportFragmentManager.findFragmentByTag(tag)
            if (fragmentInStack != null) {
                supportFragmentManager.popBackStack(tag, 0)
            } else {
                loadFragment(fragment, tag)
            }
        }
    }

    fun loadFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        if (fragmentManager.findFragmentByTag(tag) == null) {
            transaction.replace(R.id.fragmentContainer, fragment, tag)
            transaction.addToBackStack(tag)
        } else {
            transaction.replace(R.id.fragmentContainer, fragment, tag)
        }

        transaction.commit()
    }

    private fun removeFragmentFromBackStack(tag: String) {
        supportFragmentManager.popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is HomeFragment) {
            currentFragment.refreshContent()
        }
    }

    private fun closeCreatePostFragment() {
        removeFragmentFromBackStack("CreatePostFragment")
        resetCreatePostButton()
        binding.bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (isCreatePostButtonEnlarged) {
            resetCreatePostButton()
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
        } else if (currentFragment is CreatePostFragment && !isPostSaved) {
            showExitWarningDialog {
                isPostSaved = true // Marcar como guardado temporalmente para evitar bucles
                resetCreatePostButton()
                binding.bottomNavigationView.selectedItemId = R.id.navigation_home
                isPostSaved = false // Restaurar el estado
                removeFragmentFromBackStack("CreatePostFragment")
            }
        } else if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
        } else if (currentFragment is HomeFragment) {
            if (backPressedOnce) {
                finishAffinity() // This will close the app
            } else {
                this.backPressedOnce = true
                Toast.makeText(this, "Presiona nuevamente para salir", Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ backPressedOnce = false }, 2000)
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitWarningDialog(onExit: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage("Si sales se borrará el post actual.")
            .setPositiveButton("Salir") { _, _ -> onExit() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun resetCreatePostButton() {
        binding.createPostHint.visibility = View.GONE
        isCreatePostButtonEnlarged = false
    }

    fun enableCreatePostButton() {
        val createPostMenuItem = binding.bottomNavigationView.menu.findItem(R.id.navigation_create_post)
        createPostMenuItem.isEnabled = true
    }

    private fun disableCreatePostButton() {
        val createPostMenuItem = binding.bottomNavigationView.menu.findItem(R.id.navigation_create_post)
        createPostMenuItem.isEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.config_menu, menu)

        val user = auth.currentUser
        if (user != null && user.isEmailVerified) {
            menu?.findItem(R.id.verification_account)?.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_find_upc -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            findNearestUPCs(location.latitude, location.longitude)
                        } else {
                            Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }

            R.id.menu_safety_numbers -> {
                showEmergencyNumbersDialog()
                true
            }
            R.id.menu_logout -> {
                auth.signOut()
                navigateToLogin()
                true
            }
            R.id.verification_account -> {
                sendVerificationEmail()
                true
            }
            R.id.menu_report_error -> {
                Log.d("MenuClick", "Clic en Reportar Error") // 📌 Agrega este Log
                openWebPage("https://forms.gle/fN4A51vviq1jRMMD8")
                true
            }
            R.id.menu_feedback -> {
                Log.d("MenuClick", "Clic en Feedback") // 📌 Agrega este Log
                openWebPage("https://forms.gle/LrWrgEDCGKgeTx7n6")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 👈 Asegura que se abre en un nuevo contexto
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WebIntent", "Error al abrir el enlace: ${e.message}")
            Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.config_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            Log.d("PopupMenu", "Clic en: ${menuItem.title}") // 📌 Agrega este Log

            when (menuItem.itemId) {
                R.id.menu_find_upc -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                findNearestUPCs(location.latitude, location.longitude)
                            } else {
                                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    true
                }

                R.id.menu_safety_numbers -> {
                    showEmergencyNumbersDialog()
                    true
                }
                R.id.menu_report_error -> {
                    Log.d("MenuClick", "Clic en Reportar Error desde Popup") // 📌
                    openWebPage("https://forms.gle/fN4A51vviq1jRMMD8")
                    true
                }
                R.id.menu_feedback -> {
                    Log.d("MenuClick", "Clic en Feedback desde Popup") // 📌
                    openWebPage("https://forms.gle/LrWrgEDCGKgeTx7n6")
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    navigateToLogin()
                    true
                }
                R.id.verification_account -> {
                    sendVerificationEmail()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    private fun showEmergencyNumbersDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_emergency_numbers, null)
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun sendVerificationEmail() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            showToast("Usuario no autenticado.", 2000, R.color.primaryColor)
            return
        }

        val progressBar = ProgressBar(this)
        val dialog = AlertDialog.Builder(this)
            .setView(progressBar)
            .setCancelable(false)
            .create()
        dialog.show()

        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                dialog.dismiss()
                if (task.isSuccessful) {
                    showToast("Correo de verificación enviado. Revisa tu bandeja de entrada.", 2000, R.color.primaryColor)
                } else {
                    showToast("Error al enviar el correo de verificación.", 2000, R.color.primaryColor)
                }
            }
            .addOnFailureListener { exception ->
                dialog.dismiss()
                showToast("Error: ${exception.message}", 2000, R.color.primaryColor)
            }
    }

    /**
     * ✅ **Verifica si el usuario ya validó su correo.**
     * 🔹 **Debe ejecutarse después de iniciar sesión o al abrir la app.**
     */
    private fun checkVerificationStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            return  // Si no hay usuario, no hacer nada
        }

        user.reload() // 🔄 Actualiza el estado del usuario en Firebase
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!user.isEmailVerified) {
                        showToast("⚠️ Tu cuenta aún no está verificada. Revisa tu correo.", 2000, R.color.red)
                    }
                } else {
                    Log.e("FirebaseAuth", "Error al verificar estado de cuenta: ${task.exception?.message}")
                }
            }
    }



    private fun findNearestUPCs(latitude: Double, longitude: Double) {
        val apiKey = "AIzaSyDpOSQmFoWsVsHY0z185gnjV1aXut3gECw" // 🔥 Asegúrate de usar una API Key válida
        val placeType = "police" // 🔍 Tipo de lugar: Estaciones de Policía (UPC)
        val radius = 5000 // 📍 Radio de búsqueda en metros (5 km)

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=$latitude,$longitude" +
                "&radius=$radius" +
                "&type=$placeType" +
                "&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PlacesAPI", "❌ Error en la solicitud: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "🚨 Error al conectar con Google Maps API", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("PlacesAPI", "❌ Error en la respuesta de Google Maps")
                        runOnUiThread {
                            Toast.makeText(this@HomeActivity, "⚠️ No se pudo obtener datos de Google Maps", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val responseData = it.peekBody(Long.MAX_VALUE).string() // 🔥 Usamos peekBody() en lugar de body
                    val json = JSONObject(responseData)
                    val results = json.optJSONArray("results")

                    if (results == null || results.length() == 0) {
                        Log.e("PlacesAPI", "⚠️ No se encontraron UPC cercanas")
                        runOnUiThread {
                            Toast.makeText(this@HomeActivity, "📍 No se encontraron UPC cercanas", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    var nearestPlace: JSONObject? = null
                    var minDistance = Double.MAX_VALUE

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val location = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lon = location.getDouble("lng")

                        val distance = calculateDistance(latitude, longitude, lat, lon)

                        if (distance < minDistance) {
                            minDistance = distance
                            nearestPlace = place
                        }
                    }

                    if (nearestPlace != null) {
                        val name = nearestPlace.getString("name")
                        val location = nearestPlace.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lon = location.getDouble("lng")

                        val placeInfo = "🚔 UPC más cercana: $name\n📍 Lat: $lat, Lon: $lon\n📏 Distancia: ${String.format("%.2f", minDistance)} km"
                        Log.d("PlacesAPI", placeInfo)

                        runOnUiThread {
                            Toast.makeText(this@HomeActivity, placeInfo, Toast.LENGTH_LONG).show()
                            openGoogleMaps(lat, lon, name)
                        }
                    }
                }
            }
        })
    }




    private fun openGoogleMaps(lat: Double, lon: Double, name: String) {
        try {
            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($name)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Log.e("MapsError", "❌ Error al abrir Google Maps: ${e.message}")
            Toast.makeText(this, "⚠️ No se pudo abrir Google Maps", Toast.LENGTH_SHORT).show()
        }
    }






    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                if (location != null) {
                    Log.d("Location", "Current location: (${location.latitude}, ${location.longitude})")
                    val nearestUPC = getNearestUPC(location.latitude, location.longitude)
                    if (nearestUPC != null) {
                        val uri = "geo:${nearestUPC.latitude},${nearestUPC.longitude}?q=${nearestUPC.latitude},${nearestUPC.longitude}(${nearestUPC.name})"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No se encontró ninguna UPC cercana", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                }
            })
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getNearestUPC(lat: Double, lon: Double): UPC? {
        val upcs = JSONHelper.loadUPCs(this)
        var nearestUPC: UPC? = null
        var minDistance = Double.MAX_VALUE

        for ((_, upcList) in upcs) {
            for (upc in upcList) {
                val distance = calculateDistance(lat, lon, upc.latitude, upc.longitude)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestUPC = upc
                }
            }
        }
        return nearestUPC
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // 🌍 Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastKnownLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}
