package com.example.jobrec

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
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

class AdminUsersActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUsersAdapter
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyStateView: LinearLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var pagination: AdminPagination

    private var userIds: MutableList<String> = mutableListOf()
    private var allUsers: MutableList<User> = mutableListOf()
    private var filteredUsers: MutableList<User> = mutableListOf()
    private val TAG = "AdminUsersActivity"

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }

        // Set toolbar title
        findViewById<TextView>(R.id.adminToolbarTitle).text = "Manage Users"

        // Initialize views
        recyclerView = findViewById(R.id.adminUsersRecyclerView)
        progressIndicator = findViewById(R.id.progressIndicator)
        emptyStateView = findViewById(R.id.emptyStateView)
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminUsersAdapter(
            emptyList(),
            onViewClick = { user -> viewUser(user) },
            onEditClick = { user -> editUser(user) },
            onDeleteClick = { user ->
                val index = adapter.usersList.indexOfFirst { u -> u.email == user.email }
                if (index != -1) {
                    val userId = userIds[index]
                    deleteUser(userId)
                }
            }
        )
        recyclerView.adapter = adapter

        // Setup pagination
        pagination = AdminPagination(
            findViewById(R.id.pagination_layout),
            pageSize = 5
        ) { page ->
            updateUsersList()
        }

        // Setup search
        setupSearch()

        // Setup FAB
        findViewById<FloatingActionButton>(R.id.addUserFab).setOnClickListener {
            addUser()
        }

        // Check for search query from intent
        intent.getStringExtra("SEARCH_QUERY")?.let { query ->
            if (query.isNotEmpty()) {
                searchEditText.setText(query)
                searchUsers(query)
            } else {
                loadUsers()
            }
        } ?: loadUsers() // Always load users if no search query
    }

    private fun setupSearch() {
        // Setup search text change listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // We'll handle search on button click
            }
        })

        // Setup search button click listener
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isEmpty()) {
                loadUsers()
            } else {
                searchUsers(query)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadUsers() {
        Log.d(TAG, "Loading users from Firestore")
        showLoading(true)
        searchQuery = ""

        // Clear existing data
        allUsers.clear()
        filteredUsers.clear()
        userIds.clear()

        // Try both "users" and "Users" collections to ensure we find all users
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Found ${result.size()} users in 'users' collection")

                for (document in result) {
                    try {
                        val user = document.toObject(User::class.java)
                        Log.d(TAG, "Loaded user: ${user.email}, name: ${user.name}")
                        allUsers.add(user)
                        userIds.add(document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to User: ${e.message}")
                    }
                }

                // If we found users, update the UI
                if (allUsers.isNotEmpty()) {
                    Log.d(TAG, "Updating UI with ${allUsers.size} users")
                    filteredUsers.addAll(allUsers)
                    updateUsersList()
                    showLoading(false)
                } else {
                    // If no users found in "users", try "Users" (capital U) as fallback
                    Log.d(TAG, "No users found in 'users' collection, trying 'Users' collection")
                    loadUsersFromCapitalCollection()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading users", exception)
                Toast.makeText(this, "Error loading users: ${exception.message}", Toast.LENGTH_SHORT).show()

                // Try "Users" (capital U) as fallback
                loadUsersFromCapitalCollection()
            }
    }

    private fun loadUsersFromCapitalCollection() {
        db.collection("Users")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Found ${result.size()} users in 'Users' collection")

                for (document in result) {
                    try {
                        val user = document.toObject(User::class.java)
                        Log.d(TAG, "Loaded user from 'Users': ${user.email}, name: ${user.name}")
                        allUsers.add(user)
                        userIds.add(document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to User: ${e.message}")
                    }
                }

                filteredUsers.addAll(allUsers)
                updateUsersList()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading from 'Users' collection", exception)
                Toast.makeText(this, "Error loading users: ${exception.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
                showEmptyState(true)
            }
    }

    private fun searchUsers(query: String) {
        Log.d(TAG, "Searching users with query: $query")
        showLoading(true)
        searchQuery = query.lowercase()

        // Filter users based on search query
        filteredUsers.clear()
        filteredUsers.addAll(allUsers.filter { user ->
            user.name.lowercase().contains(searchQuery) ||
            user.surname.lowercase().contains(searchQuery) ||
            user.email.lowercase().contains(searchQuery)
        })

        // Reset pagination to first page
        pagination.resetToFirstPage()

        // Update UI
        updateUsersList()
        showLoading(false)
    }

    private fun updateUsersList() {
        val pageItems = pagination.getPageItems(filteredUsers)

        // Update adapter with current page items
        adapter.updateUsers(pageItems)

        // Update pagination info
        pagination.updateItemCount(filteredUsers.size)

        // Show empty state if no users found
        showEmptyState(filteredUsers.isEmpty())
    }

    private fun showLoading(show: Boolean) {
        progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun addUser() {
        val dialog = AdminEditUserDialog.newInstance(User())
        dialog.onUserUpdated = {
            loadUsers()
        }
        dialog.show(supportFragmentManager, "AdminEditUserDialog")
    }

    private fun viewUser(user: User) {
        // Find the user ID
        val index = allUsers.indexOfFirst { u -> u.email == user.email }
        if (index != -1) {
            val userId = userIds[index]

            // Show user details in read-only mode
            val dialog = AdminEditUserDialog.newInstance(user, userId)
            dialog.show(supportFragmentManager, "AdminViewUserDialog")
        }
    }

    private fun editUser(user: User) {
        // Find the user ID
        val index = allUsers.indexOfFirst { u -> u.email == user.email }
        if (index != -1) {
            val userId = userIds[index]

            // Show edit dialog
            val dialog = AdminEditUserDialog.newInstance(user, userId)
            dialog.onUserUpdated = {
                loadUsers()
            }
            dialog.onUserDeleted = {
                loadUsers()
            }
            dialog.show(supportFragmentManager, "AdminEditUserDialog")
        }
    }

    private fun deleteUser(userId: String) {
        Log.d(TAG, "Attempting to delete user with ID: $userId")

        // Show confirmation dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteUser(userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteUser(userId: String) {
        showLoading(true)

        // Try to delete from both collections to ensure we delete the user
        db.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "User deleted successfully from 'users' collection")
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user from 'users'", e)

                // Try deleting from "Users" (capital U) as fallback
                db.collection("Users").document(userId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "User deleted successfully from 'Users' collection")
                        Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(TAG, "Error deleting user from 'Users'", e2)
                        Toast.makeText(this, "Error deleting user: ${e2.message}", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                    }
            }
    }
}