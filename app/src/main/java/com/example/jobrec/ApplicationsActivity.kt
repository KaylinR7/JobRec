package com.example.jobrec

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class ApplicationsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var applicationsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applications)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: run {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Applications"

        // Initialize RecyclerView
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        applicationsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load applications
        loadApplications()
    }

    private fun loadApplications() {
        db.collection("applications")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                val applications = documents.map { doc ->
                    Application(
                        id = doc.id,
                        jobTitle = doc.getString("jobTitle") ?: "",
                        applicantName = doc.getString("applicantName") ?: "",
                        applicantEmail = doc.getString("applicantEmail") ?: "",
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getTimestamp("timestamp")?.toDate() ?: java.util.Date()
                    )
                }
                // TODO: Set up adapter and display applications
            }
            .addOnFailureListener { e ->
                Log.e("ApplicationsActivity", "Error loading applications", e)
                Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Application(
        val id: String,
        val jobTitle: String,
        val applicantName: String,
        val applicantEmail: String,
        val status: String,
        val timestamp: java.util.Date
    )
} 