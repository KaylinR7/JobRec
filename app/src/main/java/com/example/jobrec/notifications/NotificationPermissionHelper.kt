package com.example.jobrec.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jobrec.R

/**
 * Helper class for managing notification permissions
 * Handles Android 13+ notification permission requirements
 */
class NotificationPermissionHelper(private val activity: Activity) {
    
    companion object {
        private const val TAG = "NotificationPermissionHelper"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val NOTIFICATION_SETTINGS_REQUEST_CODE = 1002
        
        private const val PREF_NAME = "notification_permissions"
        private const val PREF_PERMISSION_DENIED_COUNT = "permission_denied_count"
        private const val PREF_DONT_ASK_AGAIN = "dont_ask_again"
    }
    
    private val sharedPreferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Request notification permission with user-friendly explanation
     */
    fun requestNotificationPermission(
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }
        
        when {
            isNotificationPermissionGranted() -> {
                Log.d(TAG, "Notification permission already granted")
                onGranted()
            }
            
            shouldShowRationale() -> {
                showPermissionRationaleDialog(onGranted, onDenied)
            }
            
            shouldShowSettingsDialog() -> {
                showSettingsDialog()
            }
            
            else -> {
                requestPermissionDirectly()
            }
        }
    }
    
    /**
     * Show rationale dialog explaining why notification permission is needed
     */
    private fun showPermissionRationaleDialog(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle("Enable Notifications")
            .setMessage("CareerWorx needs notification permission to keep you updated about:\n\n" +
                    "• New job opportunities that match your profile\n" +
                    "• Application status updates\n" +
                    "• Interview invitations\n" +
                    "• Important messages from employers\n\n" +
                    "You can always change this in settings later.")
            .setIcon(R.drawable.ic_app_logo)
            .setPositiveButton("Allow") { _, _ ->
                requestPermissionDirectly()
            }
            .setNegativeButton("Not Now") { _, _ ->
                incrementDeniedCount()
                onDenied()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show dialog to redirect user to app settings
     */
    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Enable Notifications in Settings")
            .setMessage("To receive important updates about jobs and applications, please enable notifications in your device settings.\n\n" +
                    "Go to: Settings > Apps > CareerWorx > Notifications")
            .setIcon(R.drawable.ic_app_logo)
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // User chose not to enable notifications
            }
            .show()
    }
    
    /**
     * Request permission directly from system
     */
    private fun requestPermissionDirectly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted")
                resetDeniedCount()
                onGranted()
            } else {
                Log.d(TAG, "Notification permission denied")
                incrementDeniedCount()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )) {
                        // User selected "Don't ask again"
                        markDontAskAgain()
                    }
                }
                onDenied()
            }
        }
    }
    
    /**
     * Open app settings for manual permission enabling
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivityForResult(intent, NOTIFICATION_SETTINGS_REQUEST_CODE)
    }
    
    /**
     * Check if we should show permission rationale
     */
    private fun shouldShowRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
    
    /**
     * Check if we should show settings dialog instead of requesting permission
     */
    private fun shouldShowSettingsDialog(): Boolean {
        val deniedCount = sharedPreferences.getInt(PREF_PERMISSION_DENIED_COUNT, 0)
        val dontAskAgain = sharedPreferences.getBoolean(PREF_DONT_ASK_AGAIN, false)
        return deniedCount >= 2 || dontAskAgain
    }
    
    /**
     * Increment the count of times permission was denied
     */
    private fun incrementDeniedCount() {
        val currentCount = sharedPreferences.getInt(PREF_PERMISSION_DENIED_COUNT, 0)
        sharedPreferences.edit()
            .putInt(PREF_PERMISSION_DENIED_COUNT, currentCount + 1)
            .apply()
    }
    
    /**
     * Reset denied count when permission is granted
     */
    private fun resetDeniedCount() {
        sharedPreferences.edit()
            .putInt(PREF_PERMISSION_DENIED_COUNT, 0)
            .putBoolean(PREF_DONT_ASK_AGAIN, false)
            .apply()
    }
    
    /**
     * Mark that user selected "Don't ask again"
     */
    private fun markDontAskAgain() {
        sharedPreferences.edit()
            .putBoolean(PREF_DONT_ASK_AGAIN, true)
            .apply()
    }
    
    /**
     * Show a subtle reminder about enabling notifications
     */
    fun showSubtleReminder() {
        if (!isNotificationPermissionGranted()) {
            AlertDialog.Builder(activity)
                .setTitle("Stay Updated")
                .setMessage("Enable notifications to never miss important job opportunities and application updates.")
                .setPositiveButton("Enable") { _, _ ->
                    requestNotificationPermission()
                }
                .setNegativeButton("Later") { _, _ -> }
                .show()
        }
    }
    
    /**
     * Check notification settings and show appropriate dialog
     */
    fun checkAndRequestPermission(showReminderIfDenied: Boolean = true) {
        when {
            isNotificationPermissionGranted() -> {
                Log.d(TAG, "Notifications are enabled")
                // Initialize notification manager
                NotificationManager.getInstance().initialize(activity)
            }
            
            showReminderIfDenied -> {
                requestNotificationPermission(
                    onGranted = {
                        NotificationManager.getInstance().initialize(activity)
                    },
                    onDenied = {
                        Log.d(TAG, "User denied notification permission")
                    }
                )
            }
            
            else -> {
                Log.d(TAG, "Notification permission not granted, but not showing reminder")
            }
        }
    }
}
