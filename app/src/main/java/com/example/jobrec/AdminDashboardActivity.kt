package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Admin Dashboard"
        }

        findViewById<Button>(R.id.btnManageUsers).setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        findViewById<Button>(R.id.btnManageCompanies).setOnClickListener {
            startActivity(Intent(this, AdminCompaniesActivity::class.java))
        }
        findViewById<Button>(R.id.btnManageJobs).setOnClickListener {
            startActivity(Intent(this, AdminJobsActivity::class.java))
        }
        findViewById<Button>(R.id.btnManageApplications).setOnClickListener {
            startActivity(Intent(this, AdminApplicationsActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 