package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class CompanyJobsFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var noJobsText: TextView
    private lateinit var jobsAdapter: JobAdapter
    private lateinit var addJobFab: FloatingActionButton

    companion object {
        private const val ARG_COMPANY_ID = "company_id"

        fun newInstance(companyId: String): CompanyJobsFragment {
            val fragment = CompanyJobsFragment()
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
        return inflater.inflate(R.layout.fragment_company_jobs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = FirebaseFirestore.getInstance()
        companyId = arguments?.getString(ARG_COMPANY_ID) ?: return
        
        jobsRecyclerView = view.findViewById(R.id.jobsRecyclerView)
        noJobsText = view.findViewById(R.id.noJobsText)
        addJobFab = view.findViewById(R.id.addJobFab)
        
        setupRecyclerView()
        setupFab()
        loadJobs()
    }

    private fun setupRecyclerView() {
        jobsAdapter = JobAdapter(emptyList()) { job ->
            val intent = Intent(requireContext(), JobDetailsActivity::class.java)
            intent.putExtra("jobId", job.id)
            startActivity(intent)
        }
        
        jobsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = jobsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        addJobFab.setOnClickListener {
            val intent = Intent(requireContext(), PostJobActivity::class.java)
            intent.putExtra("companyId", companyId)
            startActivity(intent)
        }
    }

    private fun loadJobs() {
        Log.d("CompanyJobsFragment", "Starting to load jobs for company: $companyId")
        
        // Use a simpler query first
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("CompanyJobsFragment", "Query succeeded with ${documents.size()} documents")
                val jobs = documents.documents.mapNotNull { document ->
                    try {
                        val job = document.toObject(Job::class.java)?.copy(id = document.id)
                        Log.d("CompanyJobsFragment", "Successfully parsed job: ${job?.title}")
                        job
                    } catch (e: Exception) {
                        Log.e("CompanyJobsFragment", "Error parsing job document", e)
                        null
                    }
                }
                
                // Sort jobs in memory by postedDate
                val sortedJobs = jobs.sortedByDescending { it.postedDate }
                Log.d("CompanyJobsFragment", "Processed ${sortedJobs.size} jobs")
                
                if (sortedJobs.isEmpty()) {
                    noJobsText.visibility = View.VISIBLE
                    noJobsText.text = "No jobs posted yet"
                } else {
                    noJobsText.visibility = View.GONE
                }
                
                jobsAdapter.updateJobs(sortedJobs)
            }
            .addOnFailureListener { error ->
                Log.e("CompanyJobsFragment", "Query failed", error)
                handleError(error)
            }
    }

    private fun handleError(error: Exception) {
        noJobsText.visibility = View.VISIBLE
        noJobsText.text = "Error loading jobs: ${error.message}"
        Log.e("CompanyJobsFragment", "Error loading jobs", error)
    }
} 