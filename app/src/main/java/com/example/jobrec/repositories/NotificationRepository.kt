package com.example.jobrec.repositories

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling notifications
 */
class NotificationRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_READ = "read"
        const val FIELD_TITLE = "title"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TYPE = "type"
        const val FIELD_JOB_ID = "jobId"

        // Notification types
        const val TYPE_NEW_JOB = "new_job"
        const val TYPE_APPLICATION_STATUS = "application_status"
        const val TYPE_NEW_MESSAGE = "new_message"
    }



    /**
     * Get notifications for the current user with pagination
     * Using a simpler query that doesn't require a composite index
     */
    suspend fun getNotifications(pageSize: Int, lastDocument: DocumentSnapshot? = null): Pair<List<Notification>, DocumentSnapshot?> {
        try {
            val currentUser = auth.currentUser ?: return Pair(emptyList(), null)

            // Get all notifications for the user without ordering
            // This avoids the need for a composite index
            val querySnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()


            // Check if we have any documents
            if (querySnapshot.isEmpty) {
                return Pair(emptyList(), null)
            }

            // Process documents
            val documents = querySnapshot.documents

            // Sort in memory instead of in the query
            // Handle potential null timestamps safely
            val sortedDocs = documents.sortedWith(compareByDescending<DocumentSnapshot> { doc ->
                doc.getTimestamp(FIELD_TIMESTAMP)?.seconds ?: 0L
            }.thenByDescending {
                it.id // Secondary sort by document ID for stability
            })

            // Apply pagination in memory
            val startIndex = if (lastDocument != null) {
                // Find the index of the last document
                val lastIndex = sortedDocs.indexOfFirst { it.id == lastDocument.id }
                if (lastIndex != -1) lastIndex + 1 else 0
            } else {
                0
            }

            // Get the page of documents
            val endIndex = minOf(startIndex + pageSize, sortedDocs.size)
            val pageDocs = if (startIndex < sortedDocs.size) {
                sortedDocs.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            // Get the last visible document for pagination
            val lastVisible = pageDocs.lastOrNull()

            // Parse notifications with better error handling for different timestamp formats
            val notifications = pageDocs.mapNotNull { doc ->
                try {
                    // Get timestamp safely, handling different possible formats
                    val timestamp = try {
                        // Try to get as Timestamp object first
                        val ts = doc.getTimestamp(FIELD_TIMESTAMP)
                        if (ts != null) {
                            ts
                        } else {
                            // Try to get as Date
                            val date = doc.getDate(FIELD_TIMESTAMP)
                            if (date != null) {
                                com.google.firebase.Timestamp(date.time / 1000, 0)
                            } else {
                                // Try to get as Long (seconds since epoch)
                                val seconds = doc.getLong(FIELD_TIMESTAMP)
                                if (seconds != null) {
                                    com.google.firebase.Timestamp(seconds, 0)
                                } else {
                                    // Try to get as Map with seconds and nanoseconds
                                    val map = doc.get(FIELD_TIMESTAMP) as? Map<String, Any>
                                    if (map != null && map.containsKey("seconds")) {
                                        val secs = (map["seconds"] as? Number)?.toLong() ?: 0
                                        val nanos = (map["nanoseconds"] as? Number)?.toInt() ?: 0
                                        com.google.firebase.Timestamp(secs, nanos)
                                    } else {
                                        // Try to get as String and parse
                                        val str = doc.getString(FIELD_TIMESTAMP)
                                        if (str != null) {
                                            try {
                                                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                                val parsedDate = dateFormat.parse(str)
                                                if (parsedDate != null) {
                                                    com.google.firebase.Timestamp(parsedDate)
                                                } else {
                                                    com.google.firebase.Timestamp.now()
                                                }
                                            } catch (e: Exception) {
                                                com.google.firebase.Timestamp.now()
                                            }
                                        } else {
                                            // Default to current time if all else fails
                                            com.google.firebase.Timestamp.now()
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error parsing timestamp for doc ${doc.id}: ${e.message}")
                        // Print the actual value to help diagnose
                        val rawValue = doc.get(FIELD_TIMESTAMP)
                        println("Raw timestamp value: $rawValue (${rawValue?.javaClass?.name})")
                        com.google.firebase.Timestamp.now()
                    }

                    // Create notification object
                    Notification(
                        id = doc.id,
                        title = doc.getString(FIELD_TITLE) ?: "",
                        message = doc.getString(FIELD_MESSAGE) ?: "",
                        type = doc.getString(FIELD_TYPE) ?: "",
                        jobId = doc.getString(FIELD_JOB_ID),
                        timestamp = timestamp,
                        read = try { doc.getBoolean(FIELD_READ) ?: false } catch (e: Exception) { false }
                    )
                } catch (e: Exception) {
                    println("Error creating notification object for doc ${doc.id}: ${e.message}")
                    null
                }
            }

            // Deduplicate notifications by content (title, message, jobId)
            val uniqueNotifications = notifications
                .groupBy { Triple(it.title, it.message, it.jobId) }
                .map { (_, group) -> group.maxByOrNull { it.timestamp.seconds } ?: group.first() }
                .sortedByDescending { it.timestamp.seconds }
            return Pair(uniqueNotifications, lastVisible)

        } catch (e: Exception) {
            // If the main approach fails, return an empty list
            return Pair(emptyList(), null)
        }
    }



    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update(FIELD_READ, true)
            .await()
    }

    /**
     * Save a new notification
     */
    suspend fun saveNotification(title: String, message: String, type: String, jobId: String? = null) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        val notification = hashMapOf(
            FIELD_USER_ID to currentUser.uid,
            FIELD_TITLE to title,
            FIELD_MESSAGE to message,
            FIELD_TYPE to type,
            FIELD_JOB_ID to jobId,
            FIELD_TIMESTAMP to com.google.firebase.Timestamp.now(),
            FIELD_READ to false
        )

        db.collection(COLLECTION_NOTIFICATIONS)
            .add(notification)
            .await()
    }

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .await()
    }

    /**
     * Delete all notifications for the current user
     */
    suspend fun deleteAllNotifications() {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        val notificationsSnapshot = db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo(FIELD_USER_ID, currentUser.uid)
            .get()
            .await()

        // Delete each notification
        for (doc in notificationsSnapshot.documents) {
            db.collection(COLLECTION_NOTIFICATIONS)
                .document(doc.id)
                .delete()
                .await()
        }
    }

    /**
     * Get unread notification count
     * Using a simpler approach to avoid composite index requirements
     */
    suspend fun getUnreadCount(): Int {
        val currentUser = auth.currentUser ?: return 0

        try {
            // Get all notifications for the user
            val notificationsSnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()

            // Count unread notifications in memory
            return notificationsSnapshot.documents.count { doc ->
                try {
                    doc.getBoolean(FIELD_READ) == false
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Fix notifications with invalid timestamps
     * This method will update all notifications for the current user with a valid Timestamp
     */
    suspend fun fixNotificationTimestamps() {
        val currentUser = auth.currentUser ?: return


        try {
            // Get all notifications for the user
            val querySnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()


            // Update each notification with a valid timestamp
            var fixedCount = 0
            for (doc in querySnapshot.documents) {
                try {
                    // Check if timestamp is valid
                    val isValidTimestamp = try {
                        doc.getTimestamp(FIELD_TIMESTAMP) != null
                    } catch (e: Exception) {
                        false
                    }

                    // If timestamp is not valid, update it
                    if (!isValidTimestamp) {

                        // Update with current timestamp
                        db.collection(COLLECTION_NOTIFICATIONS)
                            .document(doc.id)
                            .update(FIELD_TIMESTAMP, com.google.firebase.Timestamp.now())
                            .await()

                        fixedCount++
                    }
                } catch (e: Exception) {
                }
            }

        } catch (e: Exception) {
        }
    }

    /**
     * Clean up duplicate notifications in the database
     * This method will delete duplicate notifications based on content
     */
    suspend fun cleanupDuplicateNotifications() {
        val currentUser = auth.currentUser ?: return


        try {
            // Get all notifications for the user
            val querySnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()


            // Group notifications by content
            val notificationsByContent = mutableMapOf<String, MutableList<DocumentSnapshot>>()

            for (doc in querySnapshot.documents) {
                val title = doc.getString(FIELD_TITLE) ?: ""
                val message = doc.getString(FIELD_MESSAGE) ?: ""
                val jobId = doc.getString(FIELD_JOB_ID) ?: ""

                // Create a key based on content
                val contentKey = "$title|$message|$jobId"

                if (!notificationsByContent.containsKey(contentKey)) {
                    notificationsByContent[contentKey] = mutableListOf()
                }

                notificationsByContent[contentKey]?.add(doc)
            }

            // Find duplicates and delete all but the newest one
            var deletedCount = 0

            for ((contentKey, docs) in notificationsByContent) {
                if (docs.size > 1) {

                    // Sort by timestamp (newest first)
                    val sortedDocs = docs.sortedByDescending {
                        try {
                            it.getTimestamp(FIELD_TIMESTAMP)?.seconds ?: 0
                        } catch (e: Exception) {
                            0
                        }
                    }

                    // Keep the newest one, delete the rest
                    for (i in 1 until sortedDocs.size) {
                        try {
                            db.collection(COLLECTION_NOTIFICATIONS)
                                .document(sortedDocs[i].id)
                                .delete()
                                .await()

                            deletedCount++
                        } catch (e: Exception) {
                        }
                    }
                }
            }

        } catch (e: Exception) {
        }
    }



    /**
     * Data class for notifications
     */
    data class Notification(
        val id: String,
        val title: String,
        val message: String,
        val type: String,
        val jobId: String? = null,
        val timestamp: com.google.firebase.Timestamp,
        val read: Boolean = false
    )
}
