package com.example.jobrec

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView
    private lateinit var appNameText: TextView
    private lateinit var taglineText: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("JobRecPrefs", Context.MODE_PRIVATE)

        // Initialize views
        logoImage = findViewById(R.id.logoImage)
        appNameText = findViewById(R.id.appNameText)
        taglineText = findViewById(R.id.taglineText)

        // Apply animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        logoImage.startAnimation(fadeIn)
        appNameText.startAnimation(slideUp)
        taglineText.startAnimation(slideUp)

        // Delay for splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndNavigate()
        }, 2000) // 2 seconds delay
    }

    private fun checkUserAndNavigate() {
        // Check if user is already logged in
        val currentUser = auth.currentUser

        // Check for saved user type in SharedPreferences
        val savedUserType = sharedPreferences.getString("user_type", null)
        val savedUserId = sharedPreferences.getString("user_id", null)

        Log.d(TAG, "Saved user type: $savedUserType, Saved user ID: $savedUserId")

        if (currentUser != null) {
            Log.d(TAG, "User is logged in, checking user type")

            // If we have a saved user type, use it for faster startup
            if (savedUserType != null && savedUserId != null) {
                Log.d(TAG, "Using saved user type: $savedUserType")
                when (savedUserType) {
                    "company" -> {
                        val intent = Intent(this, CompanyDashboardActivity::class.java)
                        intent.putExtra("companyId", savedUserId)
                        startActivity(intent)
                        finish()
                    }
                    "admin" -> {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                        finish()
                    }
                    "student" -> {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    else -> {
                        // If saved type is invalid, check from server
                        checkUserTypeAndRedirect(currentUser.email ?: "", currentUser.uid)
                    }
                }
            } else {
                // No saved user type, check from server
                checkUserTypeAndRedirect(currentUser.email ?: "", currentUser.uid)
            }
        } else {
            Log.d(TAG, "No user logged in, going to login screen")
            // User is not logged in, go to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun navigateToStudentView() {
        // Navigate to student view (HomeActivity)
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("isDefaultStudent", true)
        startActivity(intent)
        finish()
    }

    private fun checkUserTypeAndRedirect(email: String, userId: String) {
        // Check user type in Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: ""
                    Log.d(TAG, "User role: $role")

                    // Save user type and ID to SharedPreferences for faster startup next time
                    val editor = sharedPreferences.edit()

                    when (role) {
                        "company" -> {
                            // Company user
                            editor.putString("user_type", "company")
                            editor.putString("user_id", userId)
                            editor.apply()

                            val intent = Intent(this, CompanyDashboardActivity::class.java)
                            intent.putExtra("companyId", userId)
                            startActivity(intent)
                        }
                        "admin" -> {
                            // Admin user
                            editor.putString("user_type", "admin")
                            editor.putString("user_id", userId)
                            editor.apply()

                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        }
                        else -> {
                            // Regular user (student)
                            editor.putString("user_type", "student")
                            editor.putString("user_id", userId)
                            editor.apply()

                            startActivity(Intent(this, HomeActivity::class.java))
                        }
                    }
                } else {
                    // Check if this is a company account
                    checkCompanyAccount(email, userId)
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user type", e)
                // Fallback to email check if there's an error
                fallbackEmailCheck(email)
                finish()
            }
    }

    private fun checkCompanyAccount(email: String, userId: String) {
        // Check if this is a company account by querying the companies collection (case-insensitive)
        val userEmail = email.lowercase()
        Log.d(TAG, "Looking for company with email (lowercase): $userEmail")

        // Get all companies and filter by email case-insensitively
        db.collection("companies")
            .get()
            .addOnSuccessListener { documents ->
                // Find company with matching email (case-insensitive)
                val companyDoc = documents.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (companyDoc != null) {
                    // It's a company
                    Log.d(TAG, "User is a company, redirecting to CompanyDashboardActivity")
                    val registrationNumber = companyDoc.getString("registrationNumber")

                    if (registrationNumber != null) {
                        // Use registration number as company ID for consistency
                        Log.d(TAG, "Found company with registration number: $registrationNumber")

                        // Save user type and ID to SharedPreferences
                        val editor = sharedPreferences.edit()
                        editor.putString("user_type", "company")
                        editor.putString("user_id", registrationNumber)
                        editor.apply()

                        val intent = Intent(this, CompanyDashboardActivity::class.java)
                        intent.putExtra("companyId", registrationNumber)
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "Registration number not found in company document")
                        fallbackEmailCheck(email)
                    }
                } else {
                    // Fallback to email check if not found in companies collection
                    Log.d(TAG, "User not found in companies collection, falling back to email check")
                    fallbackEmailCheck(email)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking company account", e)
                fallbackEmailCheck(email)
            }
    }

    private fun fallbackEmailCheck(email: String) {
        // Save user type based on email pattern
        val editor = sharedPreferences.edit()

        if (email.endsWith("@company.com")) {
            // Company user
            editor.putString("user_type", "company")
            editor.putString("user_id", auth.currentUser?.uid ?: "")
            editor.apply()

            startActivity(Intent(this, CompanyDashboardActivity::class.java))
        } else if (email == "admin@jobrec.com") {
            // Admin user
            editor.putString("user_type", "admin")
            editor.putString("user_id", auth.currentUser?.uid ?: "")
            editor.apply()

            startActivity(Intent(this, AdminDashboardActivity::class.java))
        } else {
            // Regular user (student)
            editor.putString("user_type", "student")
            editor.putString("user_id", auth.currentUser?.uid ?: "")
            editor.apply()

            startActivity(Intent(this, HomeActivity::class.java))
        }
    }
}
