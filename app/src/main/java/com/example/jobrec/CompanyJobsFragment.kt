package com.example.jobrec
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
class CompanyJobsFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private var companyId: String? = null
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var jobsAdapter: JobsAdapter
    private lateinit var addJobFab: FloatingActionButton
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
        return inflater.inflate(R.layout.fragment_company_jobs, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jobsRecyclerView = view.findViewById(R.id.jobsRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        addJobFab = view.findViewById(R.id.addJobFab)
        setupRecyclerView()
        setupFab()
        loadJobs()
    }
    private fun setupRecyclerView() {
        jobsAdapter = JobsAdapter(
            onJobClick = { job ->
                val intent = Intent(requireContext(), JobDetailsActivity::class.java)
                intent.putExtra("jobId", job.id)
                startActivity(intent)
            }
        )
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
        companyId?.let { id ->
            db.collection("jobs")
                .whereEqualTo("companyId", id)
                .get()
                .addOnSuccessListener { documents ->
                    val jobs = documents.mapNotNull { doc ->
                        try {
                            val job = doc.toObject(Job::class.java)
                            job.id = doc.id
                            job
                        } catch (e: Exception) {
                            Log.e("CompanyJobsFragment", "Error converting job document", e)
                            null
                        }
                    }.sortedByDescending {
                        try {
                            it.postedDate.toDate()
                        } catch (e: Exception) {
                            java.util.Date()
                        }
                    }
                    jobsAdapter.submitList(jobs)
                    emptyView.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading jobs: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}