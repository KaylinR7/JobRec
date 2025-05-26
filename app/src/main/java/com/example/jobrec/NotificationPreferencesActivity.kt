package com.example.jobrec
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jobrec.databinding.ActivityNotificationPreferencesBinding
import com.example.jobrec.models.FieldCategories
import com.example.jobrec.services.NotificationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class NotificationPreferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationPreferencesBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationManager: NotificationManager
    private val TAG = "NotificationPrefs"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Notification Preferences"
        }
        binding.toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        notificationManager = NotificationManager()
        loadUserPreferences()
        binding.saveButton.setOnClickListener {
            saveUserPreferences()
        }
    }
    private fun loadUserPreferences() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to manage notification preferences", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val userId = currentUser.uid
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val preferences = userDoc.get("notificationPreferences") as? Map<String, Boolean> ?: mapOf()
                    binding.jobNotificationsSwitch.isChecked = preferences["allJobs"] ?: true
                    binding.messageNotificationsSwitch.isChecked = preferences["messages"] ?: true
                    binding.jobFieldNotificationsSwitch.isChecked = preferences["jobFieldNotifications"] ?: true
                    val jobField = userDoc.getString("field") ?: ""
                    val jobSpecialization = userDoc.getString("subField") ?: ""
                    binding.jobFieldText.text = if (jobField.isNotEmpty()) jobField else "Not set"
                    binding.jobSpecializationText.text = if (jobSpecialization.isNotEmpty()) jobSpecialization else "Not set"
                    binding.studentPreferencesLayout.visibility = android.view.View.VISIBLE
                    binding.companyPreferencesLayout.visibility = android.view.View.GONE
                } else {
                    val companyQuery = db.collection("companies")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    if (!companyQuery.isEmpty) {
                        val companyDoc = companyQuery.documents[0]
                        val preferences = companyDoc.get("notificationPreferences") as? Map<String, Boolean> ?: mapOf()
                        binding.messageNotificationsSwitch.isChecked = preferences["messages"] ?: true
                        binding.applicationNotificationsSwitch.isChecked = preferences["applications"] ?: true
                        binding.studentPreferencesLayout.visibility = android.view.View.GONE
                        binding.companyPreferencesLayout.visibility = android.view.View.VISIBLE
                    } else {
                        Toast.makeText(this@NotificationPreferencesActivity, "User profile not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notification preferences", e)
                Toast.makeText(this@NotificationPreferencesActivity, "Error loading preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    private fun saveUserPreferences() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val jobField = userDoc.getString("field") ?: ""
                    val jobSpecialization = userDoc.getString("subField") ?: ""
                    val preferences = mapOf(
                        "allJobs" to binding.jobNotificationsSwitch.isChecked,
                        "messages" to binding.messageNotificationsSwitch.isChecked,
                        "jobFieldNotifications" to binding.jobFieldNotificationsSwitch.isChecked
                    )
                    db.collection("users").document(userId)
                        .update("notificationPreferences", preferences)
                        .await()
                    if (binding.jobNotificationsSwitch.isChecked) {
                        notificationManager.subscribeToTopic(NotificationManager.TOPIC_ALL_JOBS)
                    } else {
                        notificationManager.unsubscribeFromTopic(NotificationManager.TOPIC_ALL_JOBS)
                    }
                    if (binding.jobFieldNotificationsSwitch.isChecked && jobField.isNotEmpty()) {
                        val fieldTopic = NotificationManager.TOPIC_JOB_CATEGORY_PREFIX + jobField.lowercase().replace(" ", "_")
                        notificationManager.subscribeToTopic(fieldTopic)
                        if (jobSpecialization.isNotEmpty()) {
                            val specializationTopic = NotificationManager.TOPIC_JOB_SPECIALIZATION_PREFIX + jobSpecialization.lowercase().replace(" ", "_")
                            notificationManager.subscribeToTopic(specializationTopic)
                        }
                    } else if (!binding.jobFieldNotificationsSwitch.isChecked && jobField.isNotEmpty()) {
                        val fieldTopic = NotificationManager.TOPIC_JOB_CATEGORY_PREFIX + jobField.lowercase().replace(" ", "_")
                        notificationManager.unsubscribeFromTopic(fieldTopic)
                        if (jobSpecialization.isNotEmpty()) {
                            val specializationTopic = NotificationManager.TOPIC_JOB_SPECIALIZATION_PREFIX + jobSpecialization.lowercase().replace(" ", "_")
                            notificationManager.unsubscribeFromTopic(specializationTopic)
                        }
                    }
                    Toast.makeText(this@NotificationPreferencesActivity, "Notification preferences saved", Toast.LENGTH_SHORT).show()
                } else {
                    val companyQuery = db.collection("companies")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    if (!companyQuery.isEmpty) {
                        val companyDoc = companyQuery.documents[0]
                        val preferences = mapOf(
                            "messages" to binding.messageNotificationsSwitch.isChecked,
                            "applications" to binding.applicationNotificationsSwitch.isChecked
                        )
                        companyDoc.reference.update("notificationPreferences", preferences)
                            .await()
                        Toast.makeText(this@NotificationPreferencesActivity, "Notification preferences saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@NotificationPreferencesActivity, "User profile not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification preferences", e)
                Toast.makeText(this@NotificationPreferencesActivity, "Error saving preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.saveButton.isEnabled = true
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
}
