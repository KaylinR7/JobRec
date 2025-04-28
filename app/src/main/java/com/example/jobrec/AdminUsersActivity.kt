package com.example.jobrec

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminUsersActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUsersAdapter
    private var userIds: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

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

    private fun loadUsers() {
        db.collection("Users")
            .get()
            .addOnSuccessListener { result ->
                val users = mutableListOf<User>()
                userIds.clear()
                for (document in result) {
                    val user = document.toObject(User::class.java)
                    users.add(user)
                    userIds.add(document.id)
                }
                adapter.updateUsers(users)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading users: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUser(userId: String) {
        db.collection("Users").document(userId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 