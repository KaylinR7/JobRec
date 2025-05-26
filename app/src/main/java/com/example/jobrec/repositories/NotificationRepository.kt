package com.example.jobrec.repositories
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
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
        const val TYPE_NEW_JOB = "new_job"
        const val TYPE_APPLICATION_STATUS = "application_status"
        const val TYPE_NEW_MESSAGE = "new_message"
    }
    suspend fun getNotifications(pageSize: Int, lastDocument: DocumentSnapshot? = null): Pair<List<Notification>, DocumentSnapshot?> {
        try {
            val currentUser = auth.currentUser ?: return Pair(emptyList(), null)
            val querySnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()
            if (querySnapshot.isEmpty) {
                return Pair(emptyList(), null)
            }
            val documents = querySnapshot.documents
            val sortedDocs = documents.sortedWith(compareByDescending<DocumentSnapshot> { doc ->
                doc.getTimestamp(FIELD_TIMESTAMP)?.seconds ?: 0L
            }.thenByDescending {
                it.id 
            })
            val startIndex = if (lastDocument != null) {
                val lastIndex = sortedDocs.indexOfFirst { it.id == lastDocument.id }
                if (lastIndex != -1) lastIndex + 1 else 0
            } else {
                0
            }
            val endIndex = minOf(startIndex + pageSize, sortedDocs.size)
            val pageDocs = if (startIndex < sortedDocs.size) {
                sortedDocs.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            val lastVisible = pageDocs.lastOrNull()
            val notifications = pageDocs.mapNotNull { doc ->
                try {
                    val timestamp = try {
                        val ts = doc.getTimestamp(FIELD_TIMESTAMP)
                        if (ts != null) {
                            ts
                        } else {
                            val date = doc.getDate(FIELD_TIMESTAMP)
                            if (date != null) {
                                com.google.firebase.Timestamp(date.time / 1000, 0)
                            } else {
                                val seconds = doc.getLong(FIELD_TIMESTAMP)
                                if (seconds != null) {
                                    com.google.firebase.Timestamp(seconds, 0)
                                } else {
                                    val map = doc.get(FIELD_TIMESTAMP) as? Map<String, Any>
                                    if (map != null && map.containsKey("seconds")) {
                                        val secs = (map["seconds"] as? Number)?.toLong() ?: 0
                                        val nanos = (map["nanoseconds"] as? Number)?.toInt() ?: 0
                                        com.google.firebase.Timestamp(secs, nanos)
                                    } else {
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
                                            com.google.firebase.Timestamp.now()
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error parsing timestamp for doc ${doc.id}: ${e.message}")
                        val rawValue = doc.get(FIELD_TIMESTAMP)
                        println("Raw timestamp value: $rawValue (${rawValue?.javaClass?.name})")
                        com.google.firebase.Timestamp.now()
                    }
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
            val uniqueNotifications = notifications
                .groupBy { Triple(it.title, it.message, it.jobId) }
                .map { (_, group) -> group.maxByOrNull { it.timestamp.seconds } ?: group.first() }
                .sortedByDescending { it.timestamp.seconds }
            return Pair(uniqueNotifications, lastVisible)
        } catch (e: Exception) {
            return Pair(emptyList(), null)
        }
    }
    suspend fun markAsRead(notificationId: String) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update(FIELD_READ, true)
            .await()
    }
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
    suspend fun deleteNotification(notificationId: String) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .await()
    }
    suspend fun deleteAllNotifications() {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val notificationsSnapshot = db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo(FIELD_USER_ID, currentUser.uid)
            .get()
            .await()
        for (doc in notificationsSnapshot.documents) {
            db.collection(COLLECTION_NOTIFICATIONS)
                .document(doc.id)
                .delete()
                .await()
        }
    }
    suspend fun getUnreadCount(): Int {
        val currentUser = auth.currentUser ?: return 0
        try {
            val notificationsSnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()
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
    suspend fun fixNotificationTimestamps() {
        val currentUser = auth.currentUser ?: return
        try {
            val querySnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()
            var fixedCount = 0
            for (doc in querySnapshot.documents) {
                try {
                    val isValidTimestamp = try {
                        doc.getTimestamp(FIELD_TIMESTAMP) != null
                    } catch (e: Exception) {
                        false
                    }
                    if (!isValidTimestamp) {
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
    suspend fun cleanupDuplicateNotifications() {
        val currentUser = auth.currentUser ?: return
        try {
            val querySnapshot = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo(FIELD_USER_ID, currentUser.uid)
                .get()
                .await()
            val notificationsByContent = mutableMapOf<String, MutableList<DocumentSnapshot>>()
            for (doc in querySnapshot.documents) {
                val title = doc.getString(FIELD_TITLE) ?: ""
                val message = doc.getString(FIELD_MESSAGE) ?: ""
                val jobId = doc.getString(FIELD_JOB_ID) ?: ""
                val contentKey = "$title|$message|$jobId"
                if (!notificationsByContent.containsKey(contentKey)) {
                    notificationsByContent[contentKey] = mutableListOf()
                }
                notificationsByContent[contentKey]?.add(doc)
            }
            var deletedCount = 0
            for ((contentKey, docs) in notificationsByContent) {
                if (docs.size > 1) {
                    val sortedDocs = docs.sortedByDescending {
                        try {
                            it.getTimestamp(FIELD_TIMESTAMP)?.seconds ?: 0
                        } catch (e: Exception) {
                            0
                        }
                    }
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
