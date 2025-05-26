package com.example.jobrec
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var usersCountText: TextView
    private lateinit var companiesCountText: TextView
    private lateinit var jobsCountText: TextView
    private lateinit var applicationsCountText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = ""
        }
        findViewById<TextView>(R.id.adminToolbarTitle).text = "Admin Dashboard"
        usersCountText = findViewById(R.id.usersCount)
        companiesCountText = findViewById(R.id.companiesCount)
        jobsCountText = findViewById(R.id.jobsCount)
        applicationsCountText = findViewById(R.id.applicationsCount)
        findViewById<CardView>(R.id.usersCard).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<CardView>(R.id.companiesCard).setOnClickListener {
            startActivity(Intent(this, AdminCompaniesActivity::class.java))
        }
        findViewById<CardView>(R.id.jobsCard).setOnClickListener {
            startActivity(Intent(this, AdminJobsActivity::class.java))
        }
        findViewById<CardView>(R.id.applicationsCard).setOnClickListener {
            startActivity(Intent(this, AdminApplicationsActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnManageUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnManageCompanies).setOnClickListener {
            startActivity(Intent(this, AdminCompaniesActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnManageJobs).setOnClickListener {
            startActivity(Intent(this, AdminJobsActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnManageApplications).setOnClickListener {
            startActivity(Intent(this, AdminApplicationsActivity::class.java))
        }
        loadDashboardData()
    }
    private fun loadDashboardData() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                usersCountText.text = documents.size().toString()
            }
        db.collection("companies")
            .get()
            .addOnSuccessListener { documents ->
                companiesCountText.text = documents.size().toString()
            }
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                jobsCountText.text = documents.size().toString()
            }
        db.collection("applications")
            .get()
            .addOnSuccessListener { documents ->
                applicationsCountText.text = documents.size().toString()
            }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_as_student -> {
                val sharedPreferences = getSharedPreferences("JobRecPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("override_to_student", true).apply()
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            R.id.action_view_as_company -> {
                true
            }
            R.id.action_logout -> {
                val sharedPreferences = getSharedPreferences("JobRecPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("override_to_student", false)
                    .remove("user_type")
                    .remove("user_id")
                    .apply()
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    override fun onResume() {
        super.onResume()
        loadDashboardData() 
    }
}