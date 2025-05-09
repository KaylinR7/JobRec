package com.example.jobrec.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.Job
import com.example.jobrec.JobDetailsActivity
import com.example.jobrec.JobsAdapter
import com.example.jobrec.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class JobsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var jobsAdapter: JobsAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
    }

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
        recyclerView.layoutManager = LinearLayoutManager(context)

        jobsAdapter = JobsAdapter { job ->
            val intent = Intent(requireContext(), JobDetailsActivity::class.java)
            intent.putExtra("jobId", job.id)
            startActivity(intent)
        }

        recyclerView.adapter = jobsAdapter
        loadJobs()
    }

    private fun loadJobs() {
        db.collection("jobs")
            .orderBy("postedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.mapNotNull { it.toObject(Job::class.java) }
                jobsAdapter.submitList(jobs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}