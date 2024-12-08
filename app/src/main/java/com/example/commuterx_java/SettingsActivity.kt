package com.example.commuterx_java

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_settings

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish() // This will close the SettingsActivity
                    overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right)
                    true
                }
                R.id.navigation_settings -> {
                    true
                }
                else -> false
            }
        }
    }

    // This ensures the animation also works when using the back button
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right)
    }
}