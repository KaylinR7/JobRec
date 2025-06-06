package com.example.jobrec
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
class HelpActivity : AppCompatActivity() {
    private lateinit var helpText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        helpText = findViewById(R.id.helpText)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Help"
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        helpText.text = """
            Frequently Asked Questions:
            1. How do I apply for a job?
            - Browse through the available jobs
            - Click on a job to view details
            - Click the "Apply Now" button
            - Fill in the required information
            - Submit your application
            2. How do I update my profile?
            - Go to the Profile section
            - Click on the field you want to update
            - Enter the new information
            - Click Save
            3. How do I search for jobs?
            - Use the search bar at the top
            - Use the filter chips to narrow down results
            - Click on a job to view details
            4. How do I contact support?
            - Go to the Contact section
            - Use the provided contact information
            - Or send us an email directly
            For more help, please contact our support team.
        """.trimIndent()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 