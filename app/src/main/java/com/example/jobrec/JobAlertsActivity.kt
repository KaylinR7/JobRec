package com.example.jobrec
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jobrec.adapters.JobAlertsAdapter
import com.example.jobrec.models.JobAlert
import com.example.jobrec.models.NotificationPreference
import com.example.jobrec.repositories.JobAlertRepository
import com.example.jobrec.services.NotificationService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
class JobAlertsActivity : AppCompatActivity() {
    private lateinit var jobAlertRepository: JobAlertRepository
    private lateinit var notificationService: NotificationService
    private lateinit var jobAlertsAdapter: JobAlertsAdapter
    private lateinit var emailNotificationsSwitch: SwitchMaterial
    private lateinit var pushNotificationsSwitch: SwitchMaterial
    private lateinit var notificationFrequencyLayout: TextInputLayout
    private lateinit var notificationFrequencyDropdown: AutoCompleteTextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_alerts)
        jobAlertRepository = JobAlertRepository()
        notificationService = NotificationService(this)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initializeViews()
        setupNotificationPreferences()
        setupJobAlertsList()
        setupAddJobAlertButton()
    }
    private fun initializeViews() {
        emailNotificationsSwitch = findViewById(R.id.emailNotificationsSwitch)
        pushNotificationsSwitch = findViewById(R.id.pushNotificationsSwitch)
        notificationFrequencyLayout = findViewById(R.id.notificationFrequencyLayout)
        notificationFrequencyDropdown = findViewById(R.id.notificationFrequencyDropdown)
        val frequencies = arrayOf("Immediate", "Daily", "Weekly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies)
        notificationFrequencyDropdown.setAdapter(adapter)
    }
    private fun setupNotificationPreferences() {
        lifecycleScope.launch {
            try {
                val preference = jobAlertRepository.getNotificationPreference()
                preference?.let {
                    emailNotificationsSwitch.isChecked = it.emailNotifications
                    pushNotificationsSwitch.isChecked = it.pushNotifications
                    notificationFrequencyDropdown.setText(it.notificationFrequency, false)
                }
                emailNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    updateNotificationPreference()
                }
                pushNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    updateNotificationPreference()
                }
                notificationFrequencyDropdown.setOnItemClickListener { _, _, _, _ ->
                    updateNotificationPreference()
                }
            } catch (e: Exception) {
                Toast.makeText(this@JobAlertsActivity, "Error loading preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupJobAlertsList() {
        val recyclerView = findViewById<RecyclerView>(R.id.jobAlertsRecyclerView)
        jobAlertsAdapter = JobAlertsAdapter(
            onDelete = { alert -> deleteJobAlert(alert) },
            onToggle = { alert -> toggleJobAlert(alert) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@JobAlertsActivity)
            adapter = jobAlertsAdapter
        }
        loadJobAlerts()
    }
    private fun setupAddJobAlertButton() {
        findViewById<FloatingActionButton>(R.id.addJobAlertFab).setOnClickListener {
            showAddJobAlertDialog()
        }
    }
    private fun loadJobAlerts() {
        lifecycleScope.launch {
            try {
                val alerts = jobAlertRepository.getUserJobAlerts()
                jobAlertsAdapter.submitList(alerts)
            } catch (e: Exception) {
                Toast.makeText(this@JobAlertsActivity, "Error loading alerts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateNotificationPreference() {
        lifecycleScope.launch {
            try {
                val preference = NotificationPreference(
                    emailNotifications = emailNotificationsSwitch.isChecked,
                    pushNotifications = pushNotificationsSwitch.isChecked,
                    notificationFrequency = notificationFrequencyDropdown.text.toString()
                )
                jobAlertRepository.updateNotificationPreference(preference)
                Toast.makeText(this@JobAlertsActivity, "Preferences updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@JobAlertsActivity, "Error updating preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showAddJobAlertDialog() {
    }
    private fun deleteJobAlert(alert: JobAlert) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Job Alert")
            .setMessage("Are you sure you want to delete this job alert?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        jobAlertRepository.deleteJobAlert(alert.id)
                        loadJobAlerts()
                        Toast.makeText(this@JobAlertsActivity, "Job alert deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@JobAlertsActivity, "Error deleting alert: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun toggleJobAlert(alert: JobAlert) {
        lifecycleScope.launch {
            try {
                val updatedAlert = alert.copy(isActive = !alert.isActive)
                jobAlertRepository.updateJobAlert(updatedAlert)
                loadJobAlerts()
                Toast.makeText(
                    this@JobAlertsActivity,
                    if (updatedAlert.isActive) "Job alert activated" else "Job alert deactivated",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@JobAlertsActivity, "Error updating alert: ${e.message}", Toast.LENGTH_SHORT).show()
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