package com.example.jobrec

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class FirebaseHelper private constructor() {
    private val db: FirebaseFirestore
    private val auth: FirebaseAuth
    private val usersCollection: com.google.firebase.firestore.CollectionReference
    private val companiesCollection: com.google.firebase.firestore.CollectionReference

    companion object {
        @Volatile
        private var INSTANCE: FirebaseHelper? = null
        private const val TAG = "FirebaseHelper"

        fun getInstance(): FirebaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseHelper().also { INSTANCE = it }
            }
        }
    }

    init {
        // Initialize Firestore with settings before any other operations
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
        auth = FirebaseAuth.getInstance()
        usersCollection = db.collection("users")
        companiesCollection = db.collection("companies")
    }

    // Add a new user to the database
    fun addUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        // First create the user in Firebase Auth
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                // Then add the user data to Firestore using the Firebase Auth UID
                val userId = authResult.user?.uid
                if (userId != null) {
                    val userWithId = user.copy(id = userId)

                    usersCollection.document(userId)
                        .set(userWithId)
                        .addOnSuccessListener {
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding user to Firestore", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user in Firebase Auth", e)
                callback(false, e.message)
            }
    }

    // Add a new company to the database
    fun addCompany(company: Company, password: String, callback: (Boolean, String?) -> Unit) {
        // First create the user in Firebase Auth
        auth.createUserWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                // Then add the company data to Firestore
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Update company with userId and ensure id matches registrationNumber
                    val companyWithUserId = company.copy(userId = userId, id = company.registrationNumber)

                    // Use registrationNumber as document ID for consistency
                    companiesCollection.document(company.registrationNumber)
                        .set(companyWithUserId)
                        .addOnSuccessListener {
                            Log.d(TAG, "Company added successfully with ID: ${company.registrationNumber}")
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error adding company to Firestore", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating company in Firebase Auth", e)
                callback(false, e.message)
            }
    }

    // Check if a user exists with the given email and password
    fun checkUser(email: String, password: String, callback: (Boolean, String?, String?) -> Unit) {
        // Try to sign in with Firebase Auth first
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userEmail = email.lowercase()
                Log.d(TAG, "Looking for user with email (lowercase): $userEmail")

                // Get all users and filter by email case-insensitively
                usersCollection.get()
                    .addOnSuccessListener { userDocuments ->
                        // Find user with matching email (case-insensitive)
                        val userDoc = userDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }

                        if (userDoc != null) {
                            // User found in users collection
                            callback(true, "user", null)
                            return@addOnSuccessListener
                        }

                        // Get all companies and filter by email case-insensitively
                        companiesCollection.get()
                            .addOnSuccessListener { companyDocuments ->
                                // Find company with matching email (case-insensitive)
                                val companyDoc = companyDocuments.find { doc ->
                                    doc.getString("email")?.lowercase() == userEmail
                                }

                                if (companyDoc != null) {
                                    // User found in companies collection
                                    callback(true, "company", null)
                                } else {
                                    // User authenticated but not found in either collection
                                    // This is a recoverable situation
                                    callback(true, "unknown", null)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking companies collection", e)
                                callback(false, null, e.message)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking users collection", e)
                        callback(false, null, e.message)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error signing in", e)
                callback(false, null, e.message)
            }
    }

    // Check if an email already exists in any collection (case-insensitive)
    fun isEmailExists(email: String, callback: (Boolean) -> Unit) {
        val userEmail = email.lowercase()
        Log.d(TAG, "Checking if email exists (lowercase): $userEmail")

        // Get all users and filter by email case-insensitively
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                // Find user with matching email (case-insensitive)
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }

                if (userDoc != null) {
                    callback(true)
                    return@addOnSuccessListener
                }

                // Get all companies and filter by email case-insensitively
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
                        // Find company with matching email (case-insensitive)
                        val companyDoc = companyDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }
                        callback(companyDoc != null)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking companies collection", e)
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking users collection", e)
                callback(false)
            }
    }

    // Check if an ID number already exists
    fun isIdNumberExists(idNumber: String, callback: (Boolean) -> Unit) {
        usersCollection.document(idNumber)
            .get()
            .addOnSuccessListener { document ->
                callback(document.exists())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking ID number", e)
                callback(false)
            }
    }

    /**
     * Handle the case where a user has an account in Firebase Auth but not in Firestore.
     * This can happen if registration was interrupted or if there was a database inconsistency.
     *
     * @param user The user object to save to Firestore
     * @param password The user's password (used for authentication)
     * @param callback Callback with success/error information
     */
    fun recoverOrCreateUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        // First try to sign in with the existing account
        auth.signInWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                // Get the user ID from the authenticated user
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Check if this user already has a Firestore record
                    usersCollection.document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                callback(true, null) // User already exists in both Auth and Firestore
                            } else {
                                // Create a new Firestore record for this user
                                val userWithId = user.copy(id = userId)

                                usersCollection.document(userId)
                                    .set(userWithId)
                                    .addOnSuccessListener {
                                        callback(true, null)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to create Firestore record for existing Auth user", e)
                                        callback(false, e.message)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking for existing Firestore record", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after sign in")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sign in to existing account", e)

                // If sign-in fails, try to create a new user
                addUser(user, password, callback)
            }
    }

    /**
     * Handle the case where a company has an account in Firebase Auth but not in Firestore.
     * This can happen if registration was interrupted or if there was a database inconsistency.
     *
     * @param company The company object to save to Firestore
     * @param password The company's password (used for authentication)
     * @param callback Callback with success/error information
     */
    fun recoverOrCreateCompany(company: Company, password: String, callback: (Boolean, String?) -> Unit) {
        // First try to sign in with the existing account
        auth.signInWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                // Get the user ID from the authenticated user
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Check if this company already has a Firestore record (case-insensitive)
                    val companyEmail = company.email.lowercase()
                    Log.d(TAG, "Checking if company exists with email (lowercase): $companyEmail")

                    // Get all companies and filter by email case-insensitively
                    companiesCollection.get()
                        .addOnSuccessListener { documents ->
                            // Find company with matching email (case-insensitive)
                            val companyDoc = documents.find { doc ->
                                doc.getString("email")?.lowercase() == companyEmail
                            }

                            if (companyDoc != null) {
                                callback(true, null) // Company already exists in both Auth and Firestore
                            } else {
                                // Create a new Firestore record for this company
                                val companyWithUserId = company.copy(userId = userId, id = company.registrationNumber)

                                companiesCollection.document(company.registrationNumber)
                                    .set(companyWithUserId)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Company recovered successfully with ID: ${company.registrationNumber}")
                                        callback(true, null)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to create Firestore record for existing Auth company", e)
                                        callback(false, e.message)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking for existing Firestore record", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e(TAG, "Failed to get user ID after sign in")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sign in to existing account", e)

                // If sign-in fails, try to create a new company
                addCompany(company, password, callback)
            }
    }
}