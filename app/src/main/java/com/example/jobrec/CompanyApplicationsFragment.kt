package com.example.jobrec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jobrec.databinding.FragmentCompanyApplicationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CompanyApplicationsFragment : Fragment() {
    private var _binding: FragmentCompanyApplicationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ApplicationAdapter
    private val applications = mutableListOf<Application>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var companyId: String = ""

    companion object {
        private const val ARG_COMPANY_ID = "company_id"

        fun newInstance(companyId: String): CompanyApplicationsFragment {
            return CompanyApplicationsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COMPANY_ID, companyId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        companyId = arguments?.getString(ARG_COMPANY_ID) ?: ""
        if (companyId.isEmpty()) {
            Toast.makeText(context, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompanyApplicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadApplications()
    }

    private fun setupRecyclerView() {
        adapter = ApplicationAdapter(applications) { application ->
            // Handle application click
            showApplicationDetails(application)
        }
        binding.applicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CompanyApplicationsFragment.adapter
        }
    }

    private fun loadApplications() {
        if (companyId.isEmpty()) {
            Toast.makeText(context, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("applications")
            .whereEqualTo("companyId", companyId)
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
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE
        binding.applicationsRecyclerView.visibility = if (applications.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showApplicationDetails(application: Application) {
        val dialog = ApplicationDetailsDialog.newInstance(application)
        dialog.show(childFragmentManager, "ApplicationDetails")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 