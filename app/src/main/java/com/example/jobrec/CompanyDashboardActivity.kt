package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.viewpager2.widget.ViewPager2

class CompanyDashboardActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_dashboard)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get company ID from intent
        companyId = intent.getStringExtra("companyId") ?: ""
        if (companyId.isEmpty()) {
            Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Company Dashboard"

        // Initialize ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        fab = findViewById(R.id.fab)

        // Set up ViewPager adapter
        setupViewPager()

        // Set up FAB click listener
        fab.setOnClickListener {
            // Only show FAB on Jobs tab
            if (viewPager.currentItem == 1) {
                startActivity(Intent(this, PostJobActivity::class.java).apply {
                    putExtra("companyId", companyId)
                })
            }
        }

        // Update FAB visibility based on current tab
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                fab.visibility = if (position == 1) android.view.View.VISIBLE else android.view.View.GONE
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.company_dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.action_search -> {
                startActivity(Intent(this, CandidateSearchActivity::class.java))
                true
            }
            R.id.action_analytics -> {
                startActivity(Intent(this, EmployerAnalyticsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = CompanyDashboardPagerAdapter(this)
        
        // Connect TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Profile"
                1 -> "Jobs"
                2 -> "Applications"
                else -> null
            }
        }.attach()
    }

    inner class CompanyDashboardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CompanyProfileFragment.newInstance(companyId)
                1 -> CompanyJobsFragment.newInstance(companyId)
                2 -> CompanyApplicationsFragment.newInstance(companyId)
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }
    }
} 