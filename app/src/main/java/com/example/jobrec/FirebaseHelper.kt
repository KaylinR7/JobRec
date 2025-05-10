package com.example.jobrec

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class FirebaseHelper private constructor() {
    private val db: FirebaseFirestore
    private val auth: FirebaseAuth
    private val usersCollection: com.google.firebase.firestore.CollectionReference

    companion object {
        @Volatile
        private var INSTANCE: FirebaseHelper? = null

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
        usersCollection = db.collection("users") // Changed to lowercase to match LoginActivity
        Log.d("FirebaseHelper", "Initialized with users collection")
    }

    // Add a new user to the database
    fun addUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        Log.d("FirebaseHelper", "Adding user: ${user.email}")
        Log.d("FirebaseHelper", "User details: name=${user.name}, province=${user.province}, field=${user.field}")

        // First create the user in Firebase Auth
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                Log.d("FirebaseHelper", "Firebase Auth account created successfully")

                // Then add the user data to Firestore using the Firebase Auth UID
                val userId = authResult.user?.uid
                if (userId != null) {
                    Log.d("FirebaseHelper", "Got user ID: $userId")
                    val userWithId = user.copy(id = userId)

                    // Log the complete user object
                    Log.d("FirebaseHelper", "Saving user to Firestore: $userWithId")

                    usersCollection.document(userId)
                        .set(userWithId)
                        .addOnSuccessListener {
                            Log.d("FirebaseHelper", "User added successfully to Firestore: ${user.email}")
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseHelper", "Error adding user to Firestore", e)
                            callback(false, e.message)
                        }
                } else {
                    Log.e("FirebaseHelper", "Failed to get user ID after auth")
                    callback(false, "Failed to get user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error creating user in Firebase Auth", e)
                callback(false, e.message)
            }
    }

    // Check if a user exists with the given email and password
    fun checkUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        // First check if the user exists in Firestore
        usersCollection.whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    callback(false, "User not found")
                    return@addOnSuccessListener
                }

                // If user exists in Firestore, try to sign in with Firebase Auth
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseHelper", "Error signing in", e)
                        callback(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error checking user", e)
                callback(false, e.message)
            }
    }

    // Check if an email already exists
    fun isEmailExists(email: String, callback: (Boolean) -> Unit) {
        usersCollection.whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error checking email", e)
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
                Log.e("FirebaseHelper", "Error checking ID number", e)
                callback(false)
            }
    }
}