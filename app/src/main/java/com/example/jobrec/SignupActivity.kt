package com.example.jobrec

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.TextView

class SignupActivity : AppCompatActivity() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneNumberInput: TextInputEditText
    private lateinit var addressInput: TextInputEditText
    private lateinit var idNumberInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var nextButton: Button
    private val firebaseHelper = FirebaseHelper.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        nameInput = findViewById(R.id.nameInput)
        surnameInput = findViewById(R.id.surnameInput)
        emailInput = findViewById(R.id.emailInput)
        phoneNumberInput = findViewById(R.id.phoneInput)
        addressInput = findViewById(R.id.addressInput)
        idNumberInput = findViewById(R.id.idNumberInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        nextButton = findViewById(R.id.nextButton)

        nextButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val surname = surnameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phoneNumber = phoneNumberInput.text.toString().trim()
            val address = addressInput.text.toString().trim()
            val idNumber = idNumberInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInput(name, surname, email, phoneNumber, address, idNumber, password, confirmPassword)) {
                // Check if email already exists
                firebaseHelper.isEmailExists(email) { emailExists ->
                    if (emailExists) {
                        runOnUiThread {
                            emailInput.error = "Email already exists"
                        }
                        return@isEmailExists
                    }

                    // Check if ID number already exists
                    firebaseHelper.isIdNumberExists(idNumber) { idExists ->
                        if (idExists) {
                            runOnUiThread {
                                idNumberInput.error = "ID Number already exists"
                            }
                            return@isIdNumberExists
                        }

                        // If validation passes and email/ID don't exist, proceed to CV details
                        runOnUiThread {
                            // Create intent to CVDetailsActivity
                            val intent = Intent(this, CVDetailsActivity::class.java)
                            
                            // Pass user data to CVDetailsActivity
                            intent.putExtra("name", name)
                            intent.putExtra("surname", surname)
                            intent.putExtra("email", email)
                            intent.putExtra("phoneNumber", phoneNumber)
                            intent.putExtra("address", address)
                            intent.putExtra("idNumber", idNumber)
                            intent.putExtra("password", password)
                            
                            // Start CVDetailsActivity
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    private fun validateInput(
        name: String,
        surname: String,
        email: String,
        phoneNumber: String,
        address: String,
        idNumber: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            nameInput.error = "Name is required"
            isValid = false
        }

        if (surname.isEmpty()) {
            surnameInput.error = "Surname is required"
            isValid = false
        }

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            isValid = false
        }

        if (phoneNumber.isEmpty()) {
            phoneNumberInput.error = "Phone number is required"
            isValid = false
        }

        if (address.isEmpty()) {
            addressInput.error = "Address is required"
            isValid = false
        }

        if (idNumber.isEmpty()) {
            idNumberInput.error = "ID number is required"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Confirm password is required"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordInput.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }
} 