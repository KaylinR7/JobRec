package com.example.jobrec.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.R
import com.example.jobrec.Job
import com.example.jobrec.JobAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

class JobsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noJobsText: TextView
    private lateinit var adapter: JobAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_jobs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.jobsRecyclerView)
        noJobsText = view.findViewById(R.id.noJobsText)

        setupRecyclerView()
        loadJobs()
    }

    private fun setupRecyclerView() {
        adapter = JobAdapter(emptyList()) { job ->
            // TODO: Handle job click
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@JobsFragment.adapter
        }
    }

    private fun loadJobs() {
        db.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { document ->
                    try {
                        Job(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            companyId = document.getString("companyId") ?: "",
                            companyName = document.getString("companyName") ?: "",
                            location = document.getString("location") ?: "",
                            salary = document.getString("salary") ?: "",
                            type = document.getString("type") ?: "",
                            description = document.getString("description") ?: "",
                            requirements = document.getString("requirements") ?: "",
                            postedDate = document.getTimestamp("postedDate") ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (jobs.isEmpty()) {
                    noJobsText.visibility = View.VISIBLE
                    noJobsText.text = "No jobs available"
                } else {
                    noJobsText.visibility = View.GONE
                }

                adapter.updateJobs(jobs)
            }
            .addOnFailureListener { e ->
                noJobsText.visibility = View.VISIBLE
                noJobsText.text = "Error loading jobs: ${e.message}"
            }
    }
} 