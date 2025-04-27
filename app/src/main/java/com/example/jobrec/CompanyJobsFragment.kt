package com.example.jobrec

import android.content.Intent
import android.os.Bundle
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
        db.collection("jobs")
            .whereEqualTo("companyId", companyId)
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    noJobsText.visibility = View.VISIBLE
                    noJobsText.text = "Error loading jobs: ${e.message}"
                    return@addSnapshotListener
                }

                val jobs = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Job::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                if (jobs.isEmpty()) {
                    noJobsText.visibility = View.VISIBLE
                    noJobsText.text = "No jobs posted yet"
                } else {
                    noJobsText.visibility = View.GONE
                }

                jobsAdapter.updateJobs(jobs)
            }
    }
} 