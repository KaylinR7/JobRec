package com.example.jobrec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class CompanyApplicationsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApplicationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val ARG_COMPANY_ID = "company_id"

        fun newInstance(companyId: String): CompanyApplicationsFragment {
            val fragment = CompanyApplicationsFragment()
            val args = Bundle()
            args.putString(ARG_COMPANY_ID, companyId)
            fragment.arguments = args
            return fragment
        }
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

        recyclerView = view.findViewById(R.id.applicationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ApplicationAdapter(emptyList()) { application ->
            // Handle application click
            // TODO: Navigate to application details
        }

        recyclerView.adapter = adapter
        loadApplications()
    }

    private fun loadApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("applications")
            .whereEqualTo("companyId", currentUser.uid)
            .orderBy("appliedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val applications = documents.mapNotNull { document ->
                    try {
                        val appliedDate = document.getDate("appliedDate") ?: Date()
                        JobApplication(
                            id = document.id,
                            jobId = document.getString("jobId") ?: "",
                            jobTitle = document.getString("jobTitle") ?: "",
                            userId = document.getString("userId") ?: "",
                            applicantName = document.getString("applicantName") ?: "",
                            companyId = document.getString("companyId") ?: "",
                            appliedDate = appliedDate,
                            status = document.getString("status") ?: "Pending",
                            cvUrl = document.getString("cvUrl") ?: "",
                            coverLetter = document.getString("coverLetter") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                adapter.updateApplications(applications)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error loading applications: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 