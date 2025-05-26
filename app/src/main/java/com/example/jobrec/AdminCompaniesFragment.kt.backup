package com.example.jobrec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class AdminCompaniesFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var companiesAdapter: CompaniesAdapter
    private val companies = mutableListOf<Company>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_companies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.companiesRecyclerView)
        val addButton = view.findViewById<MaterialButton>(R.id.addCompanyButton)

        companiesAdapter = CompaniesAdapter(companies) { company, action ->
            when (action) {
                "edit" -> showEditCompanyDialog(company)
                "delete" -> deleteCompany(company)
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = companiesAdapter
        }

        addButton.setOnClickListener {
            showEditCompanyDialog(Company())
        }

        loadCompanies()
    }

    private fun loadCompanies() {
        db.collection("companies")
            .get()
            .addOnSuccessListener { documents ->
                companies.clear()
                for (document in documents) {
                    val company = document.toObject(Company::class.java).copy(id = document.id)
                    companies.add(company)
                }
                companiesAdapter.notifyDataSetChanged()
            }
    }

    private fun showEditCompanyDialog(company: Company) {
        // TODO: Implement company edit dialog
    }

    private fun deleteCompany(company: Company) {
        db.collection("companies").document(company.id)
            .delete()
            .addOnSuccessListener {
                loadCompanies()
            }
    }
}

class CompaniesAdapter(
    private val companies: List<Company>,
    private val onCompanyAction: (Company, String) -> Unit
) : RecyclerView.Adapter<CompaniesAdapter.CompanyViewHolder>() {

    class CompanyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView = view.findViewById<android.widget.TextView>(R.id.companyNameTextView)
        val industryTextView = view.findViewById<android.widget.TextView>(R.id.industryTextView)
        val locationTextView = view.findViewById<android.widget.TextView>(R.id.locationTextView)
        val editButton = view.findViewById<MaterialButton>(R.id.editButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_company, parent, false)
        return CompanyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        val company = companies[position]
        holder.nameTextView.text = company.companyName
        holder.industryTextView.text = company.industry
        holder.locationTextView.text = company.location

        holder.editButton.setOnClickListener {
            onCompanyAction(company, "edit")
        }

        holder.deleteButton.setOnClickListener {
            onCompanyAction(company, "delete")
        }
    }

    override fun getItemCount() = companies.size
} 