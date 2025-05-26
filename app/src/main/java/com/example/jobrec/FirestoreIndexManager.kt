package com.example.jobrec
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
class FirestoreIndexManager {
    companion object {
        private const val TAG = "FirestoreIndexManager"
        fun createIndexes() {
            val db = FirebaseFirestore.getInstance()
            val jobsIndexes = listOf(
                hashMapOf(
                    "collectionGroup" to "jobs",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "companyId",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "postedDate",
                            "order" to "DESCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "jobs",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "postedDate",
                            "order" to "DESCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "jobs",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "title",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "location",
                            "order" to "ASCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "jobs",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "type",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "postedDate",
                            "order" to "DESCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "jobs",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "status",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "postedDate",
                            "order" to "DESCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "__name__",
                            "order" to "DESCENDING"
                        )
                    )
                )
            )
            val applicationsIndexes = listOf(
                hashMapOf(
                    "collectionGroup" to "applications",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "userId",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "appliedDate",
                            "order" to "DESCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "applications",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "companyId",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "appliedDate",
                            "order" to "DESCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "applications",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "status",
                            "order" to "ASCENDING"
                        ),
                        hashMapOf(
                            "fieldPath" to "appliedDate",
                            "order" to "DESCENDING"
                        )
                    )
                )
            )
            val usersIndexes = listOf(
                hashMapOf(
                    "collectionGroup" to "Users",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "email",
                            "order" to "ASCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "Users",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "userType",
                            "order" to "ASCENDING"
                        )
                    )
                )
            )
            val companiesIndexes = listOf(
                hashMapOf(
                    "collectionGroup" to "Companies",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "name",
                            "order" to "ASCENDING"
                        )
                    )
                ),
                hashMapOf(
                    "collectionGroup" to "Companies",
                    "queryScope" to "COLLECTION",
                    "fields" to listOf(
                        hashMapOf(
                            "fieldPath" to "location",
                            "order" to "ASCENDING"
                        )
                    )
                )
            )
            try {
                jobsIndexes.forEachIndexed { index, jobsIndex ->
                    db.collection("indexes")
                        .document("jobs_index_$index")
                        .set(jobsIndex, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully created jobs index $index")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating jobs index $index", e)
                        }
                }
                applicationsIndexes.forEachIndexed { index, applicationsIndex ->
                    db.collection("indexes")
                        .document("applications_index_$index")
                        .set(applicationsIndex, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully created applications index $index")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating applications index $index", e)
                        }
                }
                usersIndexes.forEachIndexed { index, usersIndex ->
                    db.collection("indexes")
                        .document("users_index_$index")
                        .set(usersIndex, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully created users index $index")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating users index $index", e)
                        }
                }
                companiesIndexes.forEachIndexed { index, companiesIndex ->
                    db.collection("indexes")
                        .document("companies_index_$index")
                        .set(companiesIndex, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully created companies index $index")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating companies index $index", e)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating indexes", e)
            }
        }
    }
}