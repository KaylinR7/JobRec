package com.example.jobrec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class StudentSignupFragment : Fragment() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var surnameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var registerButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_signup, container, false)

        // Initialize views
        nameInput = view.findViewById(R.id.etName)
        surnameInput = view.findViewById(R.id.etSurname)
        emailInput = view.findViewById(R.id.etEmail)
        passwordInput = view.findViewById(R.id.etPassword)
        registerButton = view.findViewById(R.id.btnRegister)

        // Set up register button click listener
        registerButton.setOnClickListener {
            registerStudent()
        }

        return view
    }

    private fun registerStudent() {
        val name = nameInput.text.toString().trim()
        val surname = surnameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a basic user object with required fields
        val user = User(
            name = name,
            surname = surname,
            email = email.lowercase(),
            phoneNumber = "",
            province = "",
            address = "",
            summary = "",
            linkedin = "",
            github = "",
            portfolio = "",
            skills = listOf(),
            yearsOfExperience = "",
            certificate = "",
            expectedSalary = "",
            field = "",
            subField = ""
        )

        // Register the user
        FirebaseHelper.getInstance().addUser(user, password) { success, error ->
            if (success) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                }
            } else {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Registration failed: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
