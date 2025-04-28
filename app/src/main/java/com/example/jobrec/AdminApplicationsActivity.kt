package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AdminApplicationsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminApplicationsAdapter
    private lateinit var searchEditText: EditText
    private lateinit var progressIndicator: CircularProgressIndicator
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_applications)

        recyclerView = findViewById(R.id.recyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        progressIndicator = findViewById(R.id.progressIndicator)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminApplicationsAdapter(emptyList()) { application ->
            showApplicationDetails(application)
        }
        recyclerView.adapter = adapter

        setupSearch()
        loadApplications()
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadApplications()
                } else {
                    searchApplications(query)
                }
            }
        })
    }

    private fun loadApplications() {
        progressIndicator.visibility = View.VISIBLE
        db.collection("applications")
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val applications = documents.mapNotNull { document ->
                    try {
                        document.toObject(JobApplication::class.java).copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                adapter.updateApplications(applications)
                progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                // Handle error
                progressIndicator.visibility = View.GONE
            }
    }

    private fun searchApplications(query: String) {
        progressIndicator.visibility = View.VISIBLE
        db.collection("applications")
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val applications = documents.mapNotNull { document ->
                    try {
                        val application = document.toObject(JobApplication::class.java).copy(id = document.id)
                        if (application.jobTitle.contains(query, ignoreCase = true) ||
                            application.applicantName.contains(query, ignoreCase = true) ||
                            application.status.contains(query, ignoreCase = true)) {
                            application
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                adapter.updateApplications(applications)
                progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                // Handle error
                progressIndicator.visibility = View.GONE
            }
    }

    private fun showApplicationDetails(application: JobApplication) {
        val dialog = AdminApplicationDetailsDialog.newInstance(application)
        dialog.show(supportFragmentManager, "ApplicationDetails")
    }

    fun updateApplications(newApplications: List<JobApplication>) {
        adapter.updateApplications(newApplications)
    }
} 