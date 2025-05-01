package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var signupLink: TextView
    private lateinit var companySignupLink: TextView
    private lateinit var adminLoginLink: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signupLink = findViewById(R.id.signupLink)
        companySignupLink = findViewById(R.id.companySignupLink)
        adminLoginLink = findViewById(R.id.adminLoginLink)

        // Apply animations
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Animate elements with delay
        findViewById<View>(R.id.emailInput).startAnimation(slideUp)
        findViewById<View>(R.id.passwordInput).startAnimation(slideUp)
        findViewById<View>(R.id.loginButton).startAnimation(slideUp)
        findViewById<View>(R.id.signupLink).startAnimation(fadeIn)
        findViewById<View>(R.id.companySignupLink).startAnimation(fadeIn)
        findViewById<View>(R.id.adminLoginLink).startAnimation(fadeIn)

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserTypeAndRedirect(currentUser.email ?: "")
        }

        loginButton.setOnClickListener {
            loginUser()
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
        }

        companySignupLink.setOnClickListener {
            startActivity(Intent(this, CompanySignupActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
        }

        adminLoginLink.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
        }
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (validateInput(email, password)) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginActivity", "Login successful, checking user type")
                        checkUserTypeAndRedirect(email)
                    } else {
                        Log.e("LoginActivity", "Login failed", task.exception)
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun checkUserTypeAndRedirect(email: String) {
        Log.d("LoginActivity", "Checking user type for email: $email")
        
        // First check if it's a company
        db.collection("companies")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("LoginActivity", "Company query result size: ${documents.size()}")
                
                if (!documents.isEmpty) {
                    // It's a company
                    Log.d("LoginActivity", "User is a company, redirecting to CompanyDashboardActivity")
                    val companyId = documents.documents[0].id
                    val intent = Intent(this, CompanyDashboardActivity::class.java)
                    intent.putExtra("companyId", companyId)
                    startActivity(intent)
                    finish()
                } else {
                    // Check if it's a regular user
                    Log.d("LoginActivity", "Not a company, checking if it's a regular user")
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { userDocuments ->
                            Log.d("LoginActivity", "User query result size: ${userDocuments.size()}")
                            
                            if (!userDocuments.isEmpty) {
                                // It's a regular user
                                Log.d("LoginActivity", "User is a job seeker, redirecting to HomeActivity")
                                val userId = auth.currentUser?.uid
                                Log.d("LoginActivity", "User ID: $userId")
                                val intent = Intent(this, HomeActivity::class.java)
                                intent.putExtra("userId", userId)
                                startActivity(intent)
                                finish()
                            } else {
                                // User not found in either collection
                                Log.e("LoginActivity", "User not found in any collection")
                                Toast.makeText(this, "User not found. Please try again.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("LoginActivity", "Error checking user collection", e)
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error checking company collection", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        return true
    }
} 