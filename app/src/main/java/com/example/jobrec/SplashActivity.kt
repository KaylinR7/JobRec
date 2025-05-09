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

        // Check if we should override to student view
        val overrideToStudent = sharedPreferences.getBoolean("override_to_student", false)

        if (currentUser != null) {
            Log.d(TAG, "User is logged in, checking user type")

            if (overrideToStudent) {
                Log.d(TAG, "Override to student view is enabled, redirecting to HomeActivity")
                // Navigate to student view with default student
                navigateToStudentView()
            } else {
                // User is logged in, check user type and redirect normally
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

                    when (role) {
                        "company" -> {
                            // Company user - check if we should override to student view
                            val intent = Intent(this, CompanyDashboardActivity::class.java)
                            intent.putExtra("companyId", userId)
                            startActivity(intent)
                        }
                        "admin" -> {
                            // Admin user
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        }
                        else -> {
                            // Regular user
                            startActivity(Intent(this, HomeActivity::class.java))
                        }
                    }
                } else {
                    // Fallback to email check if document doesn't exist
                    Log.d(TAG, "User document not found, falling back to email check")
                    fallbackEmailCheck(email)
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

    private fun fallbackEmailCheck(email: String) {
        // Check if we should override to student view
        val overrideToStudent = sharedPreferences.getBoolean("override_to_student", false)

        if (overrideToStudent) {
            navigateToStudentView()
            return
        }

        if (email.endsWith("@company.com")) {
            // Company user
            startActivity(Intent(this, CompanyDashboardActivity::class.java))
        } else if (email == "admin@jobrec.com") {
            // Admin user
            startActivity(Intent(this, AdminDashboardActivity::class.java))
        } else {
            // Regular user
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }
}
