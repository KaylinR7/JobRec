package com.example.jobrec
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.utils.AdminPagination
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
class AdminCompaniesActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminCompaniesAdapter
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyStateView: LinearLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var pagination: AdminPagination
    private var companyIds: MutableList<String> = mutableListOf()
    private var allCompanies: MutableList<Company> = mutableListOf()
    private var filteredCompanies: MutableList<Company> = mutableListOf()
    private val TAG = "AdminCompaniesActivity"
    private var searchQuery: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_companies)
        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
        findViewById<TextView>(R.id.adminToolbarTitle).text = "Manage Companies"
        recyclerView = findViewById(R.id.adminCompaniesRecyclerView)
        progressIndicator = findViewById(R.id.progressIndicator)
        emptyStateView = findViewById(R.id.emptyStateView)
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminCompaniesAdapter(
            emptyList(),
            onViewClick = { company -> viewCompany(company) },
            onEditClick = { company -> editCompany(company) },
            onDeleteClick = { company ->
                val index = allCompanies.indexOfFirst { c -> c.email == company.email }
                if (index != -1) {
                    val companyId = companyIds[index]
                    deleteCompany(companyId)
                }
            }
        )
        recyclerView.adapter = adapter
        pagination = AdminPagination(
            findViewById(R.id.pagination_layout),
            pageSize = 5
        ) { page ->
            updateCompaniesList()
        }
        setupSearch()
        findViewById<FloatingActionButton>(R.id.addCompanyFab).setOnClickListener {
            addCompany()
        }
        intent.getStringExtra("SEARCH_QUERY")?.let { query ->
            if (query.isNotEmpty()) {
                searchEditText.setText(query)
                searchCompanies(query)
            } else {
                loadCompanies()
            }
        } ?: loadCompanies()
    }
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
            }
        })
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isEmpty()) {
                loadCompanies()
            } else {
                searchCompanies(query)
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun loadCompanies() {
        Log.d(TAG, "Loading companies from Firestore")
        showLoading(true)
        searchQuery = ""
        allCompanies.clear()
        filteredCompanies.clear()
        companyIds.clear()
        db.collection("companies")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Found ${result.size()} companies")
                for (document in result) {
                    try {
                        val company = document.toObject(Company::class.java)
                        Log.d(TAG, "Loaded company: ${company.companyName}")
                        allCompanies.add(company)
                        companyIds.add(document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Company: ${e.message}")
                    }
                }
                filteredCompanies.addAll(allCompanies)
                updateCompaniesList()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading companies", exception)
                Toast.makeText(this, "Error loading companies: ${exception.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
                showEmptyState(true)
            }
    }
    private fun searchCompanies(query: String) {
        Log.d(TAG, "Searching companies with query: $query")
        showLoading(true)
        searchQuery = query.lowercase()
        filteredCompanies.clear()
        filteredCompanies.addAll(allCompanies.filter { company ->
            company.companyName.lowercase().contains(searchQuery) ||
            company.email.lowercase().contains(searchQuery) ||
            company.industry?.lowercase()?.contains(searchQuery) ?: false
        })
        pagination.resetToFirstPage()
        updateCompaniesList()
        showLoading(false)
    }
    private fun updateCompaniesList() {
        val pageItems = pagination.getPageItems(filteredCompanies)
        adapter.updateCompanies(pageItems)
        pagination.updateItemCount(filteredCompanies.size)
        showEmptyState(filteredCompanies.isEmpty())
    }
    private fun showLoading(show: Boolean) {
        progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    private fun showEmptyState(show: Boolean) {
        emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    private fun deleteCompany(companyId: String) {
        Log.d(TAG, "Attempting to delete company with ID: $companyId")
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Company")
            .setMessage("Are you sure you want to delete this company? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteCompany(companyId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun performDeleteCompany(companyId: String) {
        showLoading(true)
        db.collection("companies").document(companyId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Company deleted successfully")
                Toast.makeText(this, "Company deleted", Toast.LENGTH_SHORT).show()
                loadCompanies()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting company", e)
                Toast.makeText(this, "Error deleting company: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    private fun addCompany() {
        val dialog = AdminEditCompanyDialog.newInstance(Company())
        dialog.onCompanyUpdated = {
            loadCompanies()
        }
        dialog.show(supportFragmentManager, "AdminEditCompanyDialog")
    }
    private fun viewCompany(company: Company) {
        val index = allCompanies.indexOfFirst { c -> c.email == company.email }
        if (index != -1) {
            val companyId = companyIds[index]
            val dialog = AdminEditCompanyDialog.newInstance(company, companyId)
            dialog.show(supportFragmentManager, "AdminViewCompanyDialog")
        }
    }
    private fun editCompany(company: Company) {
        val index = allCompanies.indexOfFirst { c -> c.email == company.email }
        if (index != -1) {
            val companyId = companyIds[index]
            val dialog = AdminEditCompanyDialog.newInstance(company, companyId)
            dialog.onCompanyUpdated = {
                loadCompanies()
            }
            dialog.onCompanyDeleted = {
                loadCompanies()
            }
            dialog.show(supportFragmentManager, "AdminEditCompanyDialog")
        }
    }
}