package com.tonymen.locatteme.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityHomeBinding
import com.tonymen.locatteme.model.JSONHelper
import com.tonymen.locatteme.model.UPC
import com.tonymen.locatteme.view.HomeFragments.CreatePostFragment
import com.tonymen.locatteme.view.HomeFragments.FollowingFragment
import com.tonymen.locatteme.view.HomeFragments.HomeFragment
import com.tonymen.locatteme.view.HomeFragments.ProfileFragment
import com.tonymen.locatteme.view.HomeFragments.SearchFragment
import com.tonymen.locatteme.viewmodel.HomeViewModel
import kotlin.math.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false
    private val viewModel: HomeViewModel by viewModels()
    private var isCreatePostButtonEnlarged = false
    var isPostSaved = false // Variable to track if the post is saved
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val navView: BottomNavigationView = binding.bottomNavigationView

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    handleNavigation(HomeFragment())
                    true
                }
                R.id.navigation_following -> {
                    handleNavigation(FollowingFragment())
                    true
                }
                R.id.navigation_create_post -> {
                    if (isCreatePostButtonEnlarged) {
                        loadFragment(CreatePostFragment())
                        resetCreatePostButton()
                        disableCreatePostButton()
                    } else {
                        enlargeCreatePostButton()
                    }
                    true
                }
                R.id.navigation_search -> {
                    handleNavigation(SearchFragment())
                    true
                }
                R.id.navigation_profile -> {
                    handleNavigation(ProfileFragment())
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
    }

    private fun handleNavigation(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is CreatePostFragment && !isPostSaved) {
            showExitWarningDialog {
                resetCreatePostButton()
                loadFragment(fragment)
            }
        } else {
            if (isCreatePostButtonEnlarged) {
                resetCreatePostButton()
            }
            loadFragment(fragment)
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is HomeFragment) {
            currentFragment.refreshContent()
        }
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
            }
        } else if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
        } else if (currentFragment is HomeFragment) {
            if (backPressedOnce) {
                finish()
            } else {
                this.backPressedOnce = true
                Toast.makeText(this, "Presiona nuevamente para salir", Toast.LENGTH_SHORT).show()
                android.os.Handler().postDelayed({ backPressedOnce = false }, 2000)
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

    private fun enlargeCreatePostButton() {
        val createPostMenuItem = binding.bottomNavigationView.menu.findItem(R.id.navigation_create_post)
        val scaleAnimation = ScaleAnimation(
            1f, 1.5f, 1f, 1.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = 300
        scaleAnimation.fillAfter = true

        val createPostButtonView = findViewById<View>(createPostMenuItem.itemId)
        createPostButtonView.startAnimation(scaleAnimation)

        createPostMenuItem.isEnabled = true
        binding.createPostHint.visibility = View.VISIBLE
        isCreatePostButtonEnlarged = true
    }

    public fun resetCreatePostButton() {
        val createPostMenuItem = binding.bottomNavigationView.menu.findItem(R.id.navigation_create_post)
        val scaleAnimation = ScaleAnimation(
            1.5f, 1f, 1.5f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = 300
        scaleAnimation.fillAfter = true

        val createPostButtonView = findViewById<View>(createPostMenuItem.itemId)
        createPostButtonView.startAnimation(scaleAnimation)

        createPostMenuItem.isEnabled = true
        binding.createPostHint.visibility = View.GONE
        isCreatePostButtonEnlarged = false
    }

    public fun enableCreatePostButton() {
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
        return true
    }
    //Numeros de emergencia metodo
    private fun showEmergencyNumbersDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_emergency_numbers, null)
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK", null)
        builder.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_find_upc -> {
                findNearestUPC()
                true
            }
            R.id.menu_safety_numbers -> {
                showEmergencyNumbersDialog()
                true
            }
            R.id.menu_danger_zones -> {
                // Acción para "Zonas más peligrosas"
                true
            }
            R.id.menu_logout -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.config_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_find_upc -> {
                    findNearestUPC()
                    true
                }
                R.id.menu_safety_numbers -> {
                    showEmergencyNumbersDialog()
                    true
                }
                R.id.menu_danger_zones -> {
                    // Acción para "Zonas más peligrosas"
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    private fun findNearestUPC() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLastKnownLocation()
        }
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                    if (location != null) {
                        // Log para verificar la ubicación actual
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
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "No se pudo obtener la ubicación: permiso denegado", Toast.LENGTH_SHORT).show()
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
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
