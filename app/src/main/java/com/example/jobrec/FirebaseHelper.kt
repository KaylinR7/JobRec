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
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
        auth = FirebaseAuth.getInstance()
        usersCollection = db.collection("users")
        companiesCollection = db.collection("companies")
    }
    fun addUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
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
    fun addCompany(company: Company, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val companyWithUserId = company.copy(userId = userId, id = company.registrationNumber)
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
    fun checkUser(email: String, password: String, callback: (Boolean, String?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userEmail = email.lowercase()
                Log.d(TAG, "Looking for user with email (lowercase): $userEmail")
                usersCollection.get()
                    .addOnSuccessListener { userDocuments ->
                        val userDoc = userDocuments.find { doc ->
                            doc.getString("email")?.lowercase() == userEmail
                        }
                        if (userDoc != null) {
                            callback(true, "user", null)
                            return@addOnSuccessListener
                        }
                        companiesCollection.get()
                            .addOnSuccessListener { companyDocuments ->
                                val companyDoc = companyDocuments.find { doc ->
                                    doc.getString("email")?.lowercase() == userEmail
                                }
                                if (companyDoc != null) {
                                    callback(true, "company", null)
                                } else {
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
    fun isEmailExists(email: String, callback: (Boolean) -> Unit) {
        val userEmail = email.lowercase()
        Log.d(TAG, "Checking if email exists (lowercase): $userEmail")
        usersCollection.get()
            .addOnSuccessListener { userDocuments ->
                val userDoc = userDocuments.find { doc ->
                    doc.getString("email")?.lowercase() == userEmail
                }
                if (userDoc != null) {
                    callback(true)
                    return@addOnSuccessListener
                }
                companiesCollection.get()
                    .addOnSuccessListener { companyDocuments ->
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
    fun recoverOrCreateUser(user: User, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    usersCollection.document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                callback(true, null) 
                            } else {
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
                addUser(user, password, callback)
            }
    }
    fun recoverOrCreateCompany(company: Company, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(company.email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val companyEmail = company.email.lowercase()
                    Log.d(TAG, "Checking if company exists with email (lowercase): $companyEmail")
                    companiesCollection.get()
                        .addOnSuccessListener { documents ->
                            val companyDoc = documents.find { doc ->
                                doc.getString("email")?.lowercase() == companyEmail
                            }
                            if (companyDoc != null) {
                                callback(true, null) 
                            } else {
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
                addCompany(company, password, callback)
            }
    }
}