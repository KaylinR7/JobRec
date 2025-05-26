package com.example.jobrec
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
class UserApplicationsActivity : AppCompatActivity() {
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var applicationsAdapter: UserApplicationsAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_applications)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        applicationsRecyclerView = findViewById(R.id.applicationsRecyclerView)
        tabLayout = findViewById(R.id.tabLayout)
        applicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        applicationsAdapter = UserApplicationsAdapter { application ->
            showApplicationDetails(application)
        }
        applicationsRecyclerView.adapter = applicationsAdapter
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                loadApplications(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        val statuses = arrayOf("PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWING", "OFFERED", "REJECTED")
        statuses.forEach { status ->
            tabLayout.addTab(tabLayout.newTab().setText(status))
        }
        loadApplications(0)
    }
    private fun loadApplications(statusIndex: Int) {
        val statuses = arrayOf("PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWING", "OFFERED", "REJECTED")
        val status = statuses[statusIndex]
        db.collection("applications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", status)
            .get()
            .addOnSuccessListener { documents ->
                val applications = documents.mapNotNull { document ->
                    try {
                        val app = document.toObject(Application::class.java)
                        app.id = document.id
                        app
                    } catch (e: Exception) {
                        null
                    }
                }
                val sortedApplications = applications.sortedByDescending {
                    it.appliedDate.seconds
                }
                applicationsAdapter.updateApplications(sortedApplications)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showApplicationDetails(application: Application) {
        val dialog = UserApplicationDetailsDialog.newInstance(application)
        dialog.show(supportFragmentManager, "ApplicationDetails")
    }
}