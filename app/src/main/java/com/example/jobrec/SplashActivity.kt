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
import com.example.jobrec.notifications.NotificationPermissionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class SplashActivity : AppCompatActivity() {
    private lateinit var logoImage: ImageView
    private lateinit var appNameText: TextView
    private lateinit var taglineText: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper
    private val TAG = "SplashActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("JobRecPrefs", Context.MODE_PRIVATE)
        notificationPermissionHelper = NotificationPermissionHelper(this)
        logoImage = findViewById(R.id.logoImage)
        appNameText = findViewById(R.id.appNameText)
        taglineText = findViewById(R.id.taglineText)
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        logoImage.startAnimation(fadeIn)
        appNameText.startAnimation(slideUp)
        taglineText.startAnimation(slideUp)
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndNavigate()
        }, 2000)

        // Check notification permissions after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            notificationPermissionHelper.checkAndRequestPermission(showReminderIfDenied = false)
        }, 3000)
    }
    private fun checkUserAndNavigate() {
        val currentUser = auth.currentUser
        val savedUserType = sharedPreferences.getString("user_type", null)
        val savedUserId = sharedPreferences.getString("user_id", null)
        val overrideToStudent = sharedPreferences.getBoolean("override_to_student", false)
        Log.d(TAG, "Saved user type: $savedUserType, Saved user ID: $savedUserId, Override to student: $overrideToStudent")
        if (currentUser != null) {
            Log.d(TAG, "User is logged in, checking user type")
            if (overrideToStudent) {
                Log.d(TAG, "Overriding to student view")
                navigateToStudentView()
                return
            }
            if (savedUserType != null && savedUserId != null) {
                Log.d(TAG, "Using saved user type: $savedUserType")
                when (savedUserType) {
                    "company" -> {
                        val intent = Intent(this, CompanyDashboardActivityNew::class.java)
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
                        checkUserTypeAndRedirect(currentUser.email ?: "", currentUser.uid)
                    }
                }
            } else {
                checkUserTypeAndRedirect(currentUser.email ?: "", currentUser.uid)
            }
        } else {
            Log.d(TAG, "No user logged in, going to login screen")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    private fun navigateToStudentView() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("isDefaultStudent", true)
        startActivity(intent)
        finish()
    }
    private fun checkUserTypeAndRedirect(email: String, userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: ""
                    Log.d(TAG, "User role: $role")
                    val editor = sharedPreferences.edit()
                    when (role) {
                        "company" -> {
                            editor.putString("user_type", "company")
                            editor.putString("user_id", userId)
                            editor.apply()
                            val intent = Intent(this, CompanyDashboardActivityNew::class.java)
                            intent.putExtra("companyId", userId)
                            startActivity(intent)
                        }
                        "admin" -> {
                            editor.putString("user_type", "admin")
                            editor.putString("user_id", userId)
                            editor.apply()
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        }
                        else -> {
                            editor.putString("user_type", "student")
                            editor.putString("user_id", userId)
                            editor.apply()
                            startActivity(Intent(this, HomeActivity::class.java))
                        }
                    }
                } else {
                    checkCompanyAccount(email, userId)
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user type", e)
                fallbackEmailCheck(email)
                finish()
            }
    }
    private fun checkCompanyAccount(email: String, userId: String) {
        val userEmail = email.lowercase()
        Log.d(TAG, "Looking for company with email (lowercase): $userEmail")
        db.collection("companies")
            .get()
            .addOnSuccessListener { documents ->
                val companyDoc = documents.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }
                if (companyDoc != null) {
                    Log.d(TAG, "User is a company, redirecting to CompanyDashboardActivity")
                    val registrationNumber = companyDoc.getString("registrationNumber")
                    if (registrationNumber != null) {
                        Log.d(TAG, "Found company with registration number: $registrationNumber")
                        val editor = sharedPreferences.edit()
                        editor.putString("user_type", "company")
                        editor.putString("user_id", registrationNumber)
                        editor.apply()
                        val intent = Intent(this, CompanyDashboardActivityNew::class.java)
                        intent.putExtra("companyId", registrationNumber)
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "Registration number not found in company document")
                        fallbackEmailCheck(email)
                    }
                } else {
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
        val editor = sharedPreferences.edit()
        if (email.endsWith("@company.com")) {
            editor.putString("user_type", "company")
            editor.putString("user_id", auth.currentUser?.uid ?: "")
            editor.apply()
            startActivity(Intent(this, CompanyDashboardActivityNew::class.java))
        } else if (email == "admin@dutcareerhub.com") {
            editor.putString("user_type", "admin")
            editor.putString("user_id", auth.currentUser?.uid ?: "")
            editor.apply()
            startActivity(Intent(this, AdminDashboardActivity::class.java))
        } else {
            editor.putString("user_type", "student")
            editor.putString("user_id", auth.currentUser?.uid ?: "")
            editor.apply()
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        notificationPermissionHelper.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                Log.d(TAG, "Notification permission granted in SplashActivity")
            },
            onDenied = {
                Log.d(TAG, "Notification permission denied in SplashActivity")
            }
        )
    }
}
