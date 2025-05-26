package com.example.jobrec
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
class CompanySignupFragment : Fragment() {
    private lateinit var companyNameInput: TextInputEditText
    private lateinit var registrationNumberInput: TextInputEditText
    private lateinit var industryInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var registerButton: MaterialButton
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_company_signup, container, false)
        companyNameInput = view.findViewById(R.id.companyNameInput)
        registrationNumberInput = view.findViewById(R.id.registrationNumberInput)
        industryInput = view.findViewById(R.id.industryInput)
        emailInput = view.findViewById(R.id.emailInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        registerButton = view.findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            registerCompany()
        }
        return view
    }
    private fun registerCompany() {
        val companyName = companyNameInput.text.toString().trim()
        val registrationNumber = registrationNumberInput.text.toString().trim()
        val industry = industryInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (companyName.isEmpty() || registrationNumber.isEmpty() || industry.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        val company = Company(
            id = registrationNumber,
            companyName = companyName,
            registrationNumber = registrationNumber,
            industry = industry,
            companySize = "",
            location = "",
            website = "",
            description = "",
            contactPersonName = "",
            contactPersonEmail = "",
            contactPersonPhone = "",
            email = email.lowercase()
        )
        FirebaseHelper.getInstance().addCompany(company, password) { success, error ->
            if (success) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Registration successful! Please login to continue.", Toast.LENGTH_LONG).show()
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
