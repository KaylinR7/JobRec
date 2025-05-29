package com.example.jobrec.utils

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object PhoneNumberFormatter {
    
    /**
     * Formats a South African phone number to +27 XX XXX XXXX format
     */
    fun formatSAPhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            // If starts with 27, format as +27 XX XXX XXXX
            digitsOnly.startsWith("27") && digitsOnly.length == 11 -> {
                val number = digitsOnly.substring(2)
                "+27 ${number.substring(0, 2)} ${number.substring(2, 5)} ${number.substring(5)}"
            }
            // If starts with 0, replace with +27 and format
            digitsOnly.startsWith("0") && digitsOnly.length == 10 -> {
                val number = digitsOnly.substring(1)
                "+27 ${number.substring(0, 2)} ${number.substring(2, 5)} ${number.substring(5)}"
            }
            // If 9 digits, assume missing leading 0
            digitsOnly.length == 9 -> {
                "+27 ${digitsOnly.substring(0, 2)} ${digitsOnly.substring(2, 5)} ${digitsOnly.substring(5)}"
            }
            // Return as-is if doesn't match expected patterns
            else -> phoneNumber
        }
    }
    
    /**
     * Validates if a phone number is a valid South African number
     */
    fun isValidSAPhoneNumber(phoneNumber: String): Boolean {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            // +27 format (11 digits total)
            digitsOnly.startsWith("27") && digitsOnly.length == 11 -> {
                val localNumber = digitsOnly.substring(2)
                isValidLocalNumber(localNumber)
            }
            // 0 format (10 digits total)
            digitsOnly.startsWith("0") && digitsOnly.length == 10 -> {
                val localNumber = digitsOnly.substring(1)
                isValidLocalNumber(localNumber)
            }
            // 9 digits (assuming missing leading 0)
            digitsOnly.length == 9 -> {
                isValidLocalNumber(digitsOnly)
            }
            else -> false
        }
    }
    
    /**
     * Validates the local part of a South African phone number
     */
    private fun isValidLocalNumber(localNumber: String): Boolean {
        if (localNumber.length != 9) return false
        
        // Valid SA mobile prefixes
        val validMobilePrefixes = listOf(
            "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", // Vodacom
            "71", "72", "73", "74", "76", "78", "79", // MTN
            "81", "82", "83", "84", // Cell C
            "85", "86", "87" // Telkom Mobile
        )
        
        // Valid landline area codes (first 2 digits)
        val validLandlinePrefixes = listOf(
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", // Gauteng
            "21", "22", "23", // Western Cape
            "31", "32", "33", "35", "36", "39", // KwaZulu-Natal
            "41", "42", "43", "45", "46", "47", "48", "49", // Eastern Cape
            "51", "53", "56", "57", "58", // Free State
            "54", // Northern Cape
            "12", "13", "14", "15", // Limpopo
            "13", "17", // Mpumalanga
            "14", "18" // North West
        )
        
        val prefix = localNumber.substring(0, 2)
        return validMobilePrefixes.contains(prefix) || validLandlinePrefixes.contains(prefix)
    }
    
    /**
     * Sets up automatic phone number formatting for an input field
     */
    fun setupPhoneNumberFormatting(
        phoneInput: TextInputEditText,
        phoneLayout: TextInputLayout
    ) {
        phoneInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val input = s?.toString() ?: ""
                
                // Only format if user is typing (not empty)
                if (input.isNotEmpty()) {
                    val formatted = formatSAPhoneNumber(input)
                    if (formatted != input) {
                        s?.replace(0, s.length, formatted)
                    }
                    
                    // Validate and show error
                    if (input.length >= 9) { // Only validate when enough digits are entered
                        val isValid = isValidSAPhoneNumber(formatted)
                        phoneLayout.error = if (!isValid) "Please enter a valid South African phone number" else null
                    } else {
                        phoneLayout.error = null
                    }
                }
                
                isFormatting = false
            }
        })
    }
    
    /**
     * Validates phone number and shows error if invalid
     */
    fun validateAndShowError(
        phoneInput: TextInputEditText,
        phoneLayout: TextInputLayout
    ): Boolean {
        val phoneNumber = phoneInput.text?.toString() ?: ""
        
        if (phoneNumber.isEmpty()) {
            phoneLayout.error = "Phone number is required"
            return false
        }
        
        val isValid = isValidSAPhoneNumber(phoneNumber)
        if (!isValid) {
            phoneLayout.error = "Please enter a valid South African phone number"
            return false
        }
        
        phoneLayout.error = null
        return true
    }
}
