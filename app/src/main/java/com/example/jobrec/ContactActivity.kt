package com.example.jobrec
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.widget.Toolbar
class ContactActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var contactText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        toolbar = findViewById(R.id.toolbar)
        contactText = findViewById(R.id.contactText)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Contact Us"
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        contactText.text = """
            For any inquiries, please contact us at:
            Email: support@careerworx.com
            Phone: +1 (555) 123-4567
            Address: 123 Job Street, Employment City, EC 12345
            Business Hours:
            Monday - Friday: 9:00 AM - 5:00 PM
            Saturday: 10:00 AM - 2:00 PM
            Sunday: Closed
        """.trimIndent()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}