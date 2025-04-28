package com.example.jobrec

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class AdminCompaniesActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminCompaniesAdapter
    private var companyIds: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_companies)

        recyclerView = findViewById(R.id.adminCompaniesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminCompaniesAdapter(emptyList(),
            onViewClick = { company -> viewCompany(company) },
            onEditClick = { company -> editCompany(company) },
            onDeleteClick = { company ->
                val index = adapter.companiesList.indexOfFirst { c -> c.email == company.email }
                if (index != -1) {
                    val companyId = companyIds[index]
                    deleteCompany(companyId)
                }
            }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.addCompanyFab)?.setOnClickListener {
            addCompany()
        }

        loadCompanies()
    }

    private fun loadCompanies() {
        db.collection("companies")
            .get()
            .addOnSuccessListener { result ->
                val companies = mutableListOf<Company>()
                companyIds.clear()
                for (document in result) {
                    val company = document.toObject(Company::class.java)
                    companies.add(company)
                    companyIds.add(document.id)
                }
                adapter.updateCompanies(companies)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading companies: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCompany(companyId: String) {
        db.collection("companies").document(companyId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Company deleted", Toast.LENGTH_SHORT).show()
                loadCompanies()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting company: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addCompany() {
        // TODO: Show dialog or activity to add a new company
        Toast.makeText(this, "Add Company (not implemented)", Toast.LENGTH_SHORT).show()
    }

    private fun viewCompany(company: Company) {
        // TODO: Show dialog or activity to view company details
        Toast.makeText(this, "View Company: ${company.companyName}", Toast.LENGTH_SHORT).show()
    }

    private fun editCompany(company: Company) {
        // TODO: Show dialog or activity to edit company details
        Toast.makeText(this, "Edit Company: ${company.companyName}", Toast.LENGTH_SHORT).show()
    }
} 