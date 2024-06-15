package com.tonymen.locatteme.view

import android.content.Intent
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ActivityHomeBinding
import com.tonymen.locatteme.view.HomeFragments.CreatePostFragment
import com.tonymen.locatteme.view.HomeFragments.FollowingFragment
import com.tonymen.locatteme.view.HomeFragments.HomeFragment
import com.tonymen.locatteme.view.HomeFragments.ProfileFragment
import com.tonymen.locatteme.view.HomeFragments.SearchFragment
import com.tonymen.locatteme.viewmodel.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false
    private val viewModel: HomeViewModel by viewModels()
    private var isCreatePostButtonEnlarged = false
    var isPostSaved = false // Variable to track if the post is saved

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
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
        } else if (currentFragment is HomeFragment) {
            if (backPressedOnce) {
                super.onBackPressed()
                return
            }

            this.backPressedOnce = true
            Toast.makeText(this, "Presiona nuevamente para salir", Toast.LENGTH_SHORT).show()

            android.os.Handler().postDelayed({ backPressedOnce = false }, 2000)
        } else {
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_find_upc -> {
                // Acción para "Encuentra UPC más cercana"
                true
            }
            R.id.menu_safety_numbers -> {
                // Acción para "Números de seguridad"
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
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.config_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_find_upc -> {
                    // Acción para "Encuentra UPC más cercana"
                    true
                }
                R.id.menu_safety_numbers -> {
                    // Acción para "Números de seguridad"
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
}
