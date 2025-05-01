package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.ActivityAdminApplicationsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminApplicationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminApplicationsBinding
    private lateinit var adapter: AdminApplicationsAdapter
    private val applications = mutableListOf<Application>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        loadApplications()
    }

    private fun setupRecyclerView() {
        adapter = AdminApplicationsAdapter(applications) { application ->
            showApplicationDetails(application)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminApplicationsActivity)
            adapter = this@AdminApplicationsActivity.adapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
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
        binding.progressIndicator.visibility = View.VISIBLE
        db.collection("applications")
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                applications.clear()
                for (document in documents) {
                    val application = document.toObject(Application::class.java)
                    application.id = document.id
                    applications.add(application)
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressIndicator.visibility = View.GONE
            }
    }

    private fun searchApplications(query: String) {
        binding.progressIndicator.visibility = View.VISIBLE
        db.collection("applications")
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                applications.clear()
                for (document in documents) {
                    val application = document.toObject(Application::class.java)
                    application.id = document.id
                    if (application.jobTitle.contains(query, ignoreCase = true) ||
                        application.applicantName.contains(query, ignoreCase = true) ||
                        application.status.contains(query, ignoreCase = true)) {
                        applications.add(application)
                    }
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressIndicator.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error searching applications: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressIndicator.visibility = View.GONE
            }
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showApplicationDetails(application: Application) {
        val dialog = AdminApplicationDetailsDialog.newInstance(application)
        dialog.show(supportFragmentManager, "ApplicationDetails")
    }
} 