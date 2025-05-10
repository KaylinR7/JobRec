package com.example.jobrec

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminUsersActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUsersAdapter
    private var userIds: MutableList<String> = mutableListOf()
    private val TAG = "AdminUsersActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Manage Users"
        }

        recyclerView = findViewById(R.id.adminUsersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminUsersAdapter(emptyList()) { user ->
            val index = adapter.usersList.indexOfFirst { u -> u.email == user.email }
            if (index != -1) {
                val userId = userIds[index]
                deleteUser(userId)
            }
        }
        recyclerView.adapter = adapter

        loadUsers()
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

        // Try both "users" and "Users" collections to ensure we find all users
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "Found ${result.size()} users in 'users' collection")

                val users = mutableListOf<User>()
                userIds.clear()

                for (document in result) {
                    try {
                        val user = document.toObject(User::class.java)
                        Log.d(TAG, "Loaded user: ${user.email}, name: ${user.name}")
                        users.add(user)
                        userIds.add(document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to User: ${e.message}")
                    }
                }

                // If we found users, update the adapter
                if (users.isNotEmpty()) {
                    Log.d(TAG, "Updating adapter with ${users.size} users")
                    adapter.updateUsers(users)
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

                val users = mutableListOf<User>()
                userIds.clear()

                for (document in result) {
                    try {
                        val user = document.toObject(User::class.java)
                        Log.d(TAG, "Loaded user from 'Users': ${user.email}, name: ${user.name}")
                        users.add(user)
                        userIds.add(document.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to User: ${e.message}")
                    }
                }

                adapter.updateUsers(users)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading from 'Users' collection", exception)
                Toast.makeText(this, "Error loading users: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUser(userId: String) {
        Log.d(TAG, "Attempting to delete user with ID: $userId")

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
                    }
            }
    }
}