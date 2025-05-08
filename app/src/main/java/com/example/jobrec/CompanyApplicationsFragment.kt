package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CompanyApplicationsFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private var companyId: String? = null
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var applicationsAdapter: ApplicationAdapter
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private val applications = mutableListOf<ApplicationsActivity.Application>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        companyId = FirebaseAuth.getInstance().currentUser?.uid
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_company_applications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        applicationsRecyclerView = view.findViewById(R.id.applicationsRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        bottomNavigation = view.findViewById(R.id.bottomNavigation)
        toolbar = view.findViewById(R.id.toolbar)
        
        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        loadApplications()
    }

    private fun setupToolbar() {
        (requireActivity() as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
                title = "Applications"
            }
        }

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        applicationsAdapter = ApplicationAdapter(applications) { application ->
            showApplicationDetails(application)
        }
        
        applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = applicationsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val status = when (item.itemId) {
                R.id.navigation_pending -> "pending"
                R.id.navigation_reviewed -> "reviewed"
                R.id.navigation_accepted -> "accepted"
                R.id.navigation_rejected -> "rejected"
                else -> null
            }
            loadApplications(status)
            true
        }
    }

    private fun showApplicationDetails(application: ApplicationsActivity.Application) {
        val intent = Intent(requireContext(), CompanyApplicationDetailsActivity::class.java).apply {
            putExtra("applicationId", application.id)
        }
        startActivity(intent)
    }

    private fun loadApplications(status: String? = null) {
        companyId?.let { id ->
            var query = db.collection("applications")
                .whereEqualTo("companyId", id)

            if (status != null) {
                query = query.whereEqualTo("status", status)
            }

            query.get()
                .addOnSuccessListener { documents ->
                    applications.clear()
                    val newApplications = documents.map { document ->
                        ApplicationsActivity.Application(
                            id = document.id,
                            jobTitle = document.getString("jobTitle") ?: "",
                            applicantName = document.getString("applicantName") ?: "",
                            applicantEmail = document.getString("applicantEmail") ?: "",
                            status = document.getString("status") ?: "pending",
                            timestamp = document.getTimestamp("timestamp")?.toDate() ?: java.util.Date()
                        )
                    }.sortedByDescending { it.timestamp }
                    
                    applications.addAll(newApplications)
                    applicationsAdapter.notifyDataSetChanged()
                    
                    // Update empty state
                    emptyView.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
                    applicationsRecyclerView.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
} 