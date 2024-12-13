package com.example.commuterx_java

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupBottomNavigation()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<LinearLayout>(R.id.notifications_container).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        }

        findViewById<LinearLayout>(R.id.language_container).setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        }

        findViewById<LinearLayout>(R.id.help_center_container).setOnClickListener {
            startActivity(Intent(this, HelpCenterActivity::class.java))
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        }

        findViewById<LinearLayout>(R.id.share_feedback_container).setOnClickListener {
            startActivity(Intent(this, ShareFeedbackActivity::class.java))
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_settings

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
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

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right)
    }
}