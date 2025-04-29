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
import android.util.Log

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

        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            // Get company ID from intent
            companyId = intent.getStringExtra("companyId") ?: run {
                Toast.makeText(this, "Error: Company ID not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Initialize views
            viewPager = findViewById(R.id.viewPager)
            tabLayout = findViewById(R.id.tabLayout)
            fab = findViewById(R.id.fab)

            // Setup toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            // Setup ViewPager and TabLayout
            setupViewPager()

            // Setup FAB
            fab.setOnClickListener {
                val intent = Intent(this, PostJobActivity::class.java)
                intent.putExtra("companyId", companyId)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("CompanyDashboardActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error initializing activity: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupViewPager() {
        try {
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
        } catch (e: Exception) {
            Log.e("CompanyDashboardActivity", "Error in setupViewPager", e)
            Toast.makeText(this, "Error setting up tabs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.company_dashboard_menu, menu)
            return true
        } catch (e: Exception) {
            Log.e("CompanyDashboardActivity", "Error creating options menu", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
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
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e("CompanyDashboardActivity", "Error in onOptionsItemSelected", e)
            Toast.makeText(this, "Error handling menu action: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    inner class CompanyDashboardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return try {
                when (position) {
                    0 -> CompanyProfileFragment.newInstance(companyId)
                    1 -> CompanyJobsFragment.newInstance(companyId)
                    2 -> CompanyApplicationsFragment.newInstance(companyId)
                    else -> throw IllegalArgumentException("Invalid position $position")
                }
            } catch (e: Exception) {
                Log.e("CompanyDashboardActivity", "Error creating fragment", e)
                Toast.makeText(this@CompanyDashboardActivity, "Error loading content: ${e.message}", Toast.LENGTH_SHORT).show()
                CompanyProfileFragment.newInstance(companyId) // Fallback to profile fragment
            }
        }
    }
} 