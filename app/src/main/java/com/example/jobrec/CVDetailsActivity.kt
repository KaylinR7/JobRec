package com.example.jobrec
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class CVDetailsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var educationAdapter: EducationAdapter
    private lateinit var name: String
    private lateinit var surname: String
    private lateinit var email: String
    private lateinit var phoneNumber: String
    private lateinit var address: String
    private lateinit var idNumber: String
    private lateinit var password: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cv_details)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "CV Details"
        }
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
        name = intent.getStringExtra("name") ?: ""
        surname = intent.getStringExtra("surname") ?: ""
        email = intent.getStringExtra("email") ?: ""
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        address = intent.getStringExtra("address") ?: ""
        idNumber = intent.getStringExtra("idNumber") ?: ""
        password = intent.getStringExtra("password") ?: ""
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val recyclerView = findViewById<RecyclerView>(R.id.educationRecyclerView)
        educationAdapter = EducationAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = educationAdapter
        findViewById<Button>(R.id.addEducationButton).setOnClickListener {
            educationAdapter.addNewEducation()
        }
        findViewById<Button>(R.id.submitButton).setOnClickListener {
            saveCVDetails()
        }
    }
    private fun saveCVDetails() {
        val userId = auth.currentUser?.uid ?: return
        val educations = educationAdapter.getEducationList()
        val cvData = hashMapOf(
            "educations" to educations
        )
        firestore.collection("users").document(userId)
            .collection("cv")
            .document("details")
            .set(cvData)
            .addOnSuccessListener {
                Toast.makeText(this, "CV details saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving CV details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}