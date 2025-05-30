package com.example.jobrec.utils

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

object CompanyRegistrationFormatter {
    
    /**
     * Formats a South African company registration number to YYYY/NNNNNN/NN format
     */
    fun formatRegistrationNumber(regNumber: String): String {
        // Remove all non-alphanumeric characters except forward slashes
        val cleaned = regNumber.replace(Regex("[^0-9/]"), "")
        
        // If already properly formatted, return as-is
        if (isValidFormat(cleaned)) {
            return cleaned
        }
        
        // Remove existing slashes for reformatting
        val digitsOnly = cleaned.replace("/", "")
        
        return when {
            // If we have enough digits, format properly
            digitsOnly.length >= 10 -> {
                val year = digitsOnly.substring(0, 4)
                val sequence = digitsOnly.substring(4, 10)
                val suffix = if (digitsOnly.length >= 12) digitsOnly.substring(10, 12) else "07"
                "$year/$sequence/$suffix"
            }
            // If we have at least 4 digits, start formatting
            digitsOnly.length >= 4 -> {
                val year = digitsOnly.substring(0, 4)
                val remaining = digitsOnly.substring(4)
                when {
                    remaining.length >= 6 -> {
                        val sequence = remaining.substring(0, 6)
                        val suffix = if (remaining.length >= 8) remaining.substring(6, 8) else ""
                        if (suffix.isNotEmpty()) "$year/$sequence/$suffix" else "$year/$sequence"
                    }
                    remaining.isNotEmpty() -> "$year/$remaining"
                    else -> year
                }
            }
            else -> digitsOnly
        }
    }
    
    /**
     * Validates if a registration number follows the correct format
     */
    private fun isValidFormat(regNumber: String): Boolean {
        val pattern = Regex("^\\d{4}/\\d{6}/\\d{2}$")
        return pattern.matches(regNumber)
    }
    
    /**
     * Validates if a company registration number is valid
     */
    fun isValidRegistrationNumber(regNumber: String): Boolean {
        val cleaned = regNumber.replace(Regex("[^0-9/]"), "")
        
        if (!isValidFormat(cleaned)) return false
        
        val parts = cleaned.split("/")
        if (parts.size != 3) return false
        
        val year = parts[0].toIntOrNull() ?: return false
        val sequence = parts[1]
        val suffix = parts[2]
        
        // Validate year (should be reasonable - between 1900 and current year + 1)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year < 1900 || year > currentYear + 1) return false
        
        // Validate sequence (6 digits)
        if (sequence.length != 6 || sequence.toIntOrNull() == null) return false
        
        // Validate suffix (common suffixes in SA)
        val validSuffixes = listOf("07", "23", "08", "06", "21", "22", "10", "11")
        if (!validSuffixes.contains(suffix)) return false
        
        return true
    }
    
    /**
     * Sets up automatic registration number formatting
     */
    fun setupRegistrationNumberFormatting(
        regInput: TextInputEditText,
        regLayout: TextInputLayout
    ) {
        regInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val input = s?.toString() ?: ""
                
                if (input.isNotEmpty()) {
                    val formatted = formatRegistrationNumber(input)
                    if (formatted != input) {
                        s?.replace(0, s.length, formatted)
                    }
                    
                    // Validate if complete enough
                    if (input.replace(Regex("[^0-9]"), "").length >= 10) {
                        val isValid = isValidRegistrationNumber(formatted)
                        regLayout.error = if (!isValid) {
                            "Please enter a valid SA company registration number (YYYY/NNNNNN/NN)"
                        } else null
                    } else {
                        regLayout.error = null
                    }
                }
                
                isFormatting = false
            }
        })
    }
    
    /**
     * Validates registration number and shows error if invalid
     */
    fun validateAndShowError(
        regInput: TextInputEditText,
        regLayout: TextInputLayout
    ): Boolean {
        val regNumber = regInput.text?.toString() ?: ""
        
        if (regNumber.isEmpty()) {
            regLayout.error = "Company registration number is required"
            return false
        }
        
        val isValid = isValidRegistrationNumber(regNumber)
        if (!isValid) {
            regLayout.error = "Please enter a valid SA company registration number (YYYY/NNNNNN/NN)"
            return false
        }
        
        regLayout.error = null
        return true
    }
    
    /**
     * Generates example registration numbers for help text
     */
    fun getExampleRegistrationNumbers(): List<String> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return listOf(
            "$currentYear/123456/07",
            "${currentYear - 1}/654321/23",
            "${currentYear - 2}/111222/08"
        )
    }
}
