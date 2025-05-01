package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val emailInput = findViewById<EditText>(R.id.adminEmailInput)
        val passwordInput = findViewById<EditText>(R.id.adminPasswordInput)
        val loginButton = findViewById<Button>(R.id.adminLoginButton)
        val backToLoginLink = findViewById<TextView>(R.id.backToLoginLink)

        // Apply animations
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Animate elements
        findViewById<View>(R.id.adminEmailInput).startAnimation(slideUp)
        findViewById<View>(R.id.adminPasswordInput).startAnimation(slideUp)
        findViewById<View>(R.id.adminLoginButton).startAnimation(slideUp)
        findViewById<View>(R.id.backToLoginLink).startAnimation(fadeIn)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            // Simple hardcoded admin credentials
            if (email == "admin@jobrec.com" && password == "admin123") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
                finish()
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show()
            }
        }

        backToLoginLink.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_up)
        }
    }
} 