package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val emailInput = findViewById<EditText>(R.id.adminEmailInput)
        val passwordInput = findViewById<EditText>(R.id.adminPasswordInput)
        val loginButton = findViewById<Button>(R.id.adminLoginButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            // Simple hardcoded admin credentials
            if (email == "admin@jobrec.com" && password == "admin123") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 