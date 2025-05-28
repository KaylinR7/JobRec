// Example implementation for ProfileActivity.kt
// This shows how to integrate the cascading location dropdowns

// Add these imports to ProfileActivity.kt:
import com.example.jobrec.utils.LocationUtils
import com.google.android.material.textfield.MaterialAutoCompleteTextView

// Add these properties to ProfileActivity class:
private lateinit var provinceInput: MaterialAutoCompleteTextView
private lateinit var cityInput: MaterialAutoCompleteTextView
private var selectedProvince: String = ""
private var selectedCity: String = ""

// In onCreate() method, after findViewById calls:
private fun initializeLocationInputs() {
    provinceInput = findViewById(R.id.provinceInput)
    cityInput = findViewById(R.id.cityInput)
    
    // Setup cascading dropdowns
    LocationUtils.setupCascadingLocationSpinners(
        context = this,
        provinceSpinner = provinceInput,
        citySpinner = cityInput,
        selectedProvince = currentUser?.province,
        selectedCity = currentUser?.city
    ) { province, city ->
        selectedProvince = province
        selectedCity = city
        Log.d("ProfileActivity", "Location selected: $city, $province")
    }
}

// In loadUserData() method, add:
private fun loadUserData() {
    // ... existing code ...
    
    // Set location data
    user.province.let { province ->
        if (province.isNotEmpty()) {
            selectedProvince = province
            // The cascading dropdown will handle setting the city
        }
    }
    
    user.city.let { city ->
        if (city.isNotEmpty()) {
            selectedCity = city
        }
    }
    
    // Reinitialize dropdowns with loaded data
    LocationUtils.setupCascadingLocationSpinners(
        context = this,
        provinceSpinner = provinceInput,
        citySpinner = cityInput,
        selectedProvince = selectedProvince,
        selectedCity = selectedCity
    ) { province, city ->
        selectedProvince = province
        selectedCity = city
    }
}

// In saveProfile() method, update the userData map:
private fun saveProfile() {
    val userId = auth.currentUser?.uid
    if (userId != null) {
        // Validate location selection
        if (!LocationUtils.isValidLocationSelection(selectedProvince, selectedCity)) {
            Toast.makeText(this, "Please select both province and city", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userData = hashMapOf(
            "name" to nameInput.text.toString().trim(),
            "surname" to surnameInput.text.toString().trim(),
            "email" to emailInput.text.toString().trim(),
            "phoneNumber" to phoneNumberInput.text.toString().trim(),
            "province" to selectedProvince,
            "city" to selectedCity,  // Changed from "address"
            "summary" to summaryInput.text.toString().trim(),
            // ... rest of the fields
        )
        
        // ... rest of save logic
    }
}

// For PostJobActivity.kt, similar implementation:
class PostJobActivity {
    private lateinit var jobProvinceInput: MaterialAutoCompleteTextView
    private lateinit var jobCityInput: MaterialAutoCompleteTextView
    private var selectedJobProvince: String = ""
    private var selectedJobCity: String = ""
    
    private fun initializeJobLocationInputs() {
        jobProvinceInput = findViewById(R.id.jobProvinceInput)
        jobCityInput = findViewById(R.id.jobCityInput)
        
        LocationUtils.setupCascadingLocationSpinners(
            context = this,
            provinceSpinner = jobProvinceInput,
            citySpinner = jobCityInput
        ) { province, city ->
            selectedJobProvince = province
            selectedJobCity = city
        }
    }
    
    private fun saveJob() {
        if (!LocationUtils.isValidLocationSelection(selectedJobProvince, selectedJobCity)) {
            Toast.makeText(this, "Please select both province and city for the job location", Toast.LENGTH_SHORT).show()
            return
        }
        
        val jobData = hashMapOf(
            "title" to titleInput.text.toString().trim(),
            "province" to selectedJobProvince,
            "city" to selectedJobCity,  // Changed from "location"
            // ... rest of job fields
        )
        
        // ... rest of save logic
    }
}

// For SignupActivity.kt:
class SignupActivity {
    private lateinit var provinceInput: MaterialAutoCompleteTextView
    private lateinit var cityInput: MaterialAutoCompleteTextView
    private var selectedProvince: String = ""
    private var selectedCity: String = ""
    
    private fun initializeLocationInputs() {
        provinceInput = findViewById(R.id.provinceInput)
        cityInput = findViewById(R.id.cityInput)
        
        LocationUtils.setupCascadingLocationSpinners(
            context = this,
            provinceSpinner = provinceInput,
            citySpinner = cityInput
        ) { province, city ->
            selectedProvince = province
            selectedCity = city
        }
    }
    
    private fun registerUser() {
        if (!LocationUtils.isValidLocationSelection(selectedProvince, selectedCity)) {
            Toast.makeText(this, "Please select both province and city", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userData = hashMapOf(
            "name" to etName.text.toString().trim(),
            "email" to etEmail.text.toString().trim(),
            "phoneNumber" to etCellNumber.text.toString().trim(),
            "province" to selectedProvince,
            "city" to selectedCity,  // Changed from "address"
            // ... rest of user fields
        )
        
        // ... rest of registration logic
    }
}

// Migration Notes:
// 1. Update all existing database queries to use "city" instead of "address"
// 2. Add migration logic to convert existing "address" data to "city" where possible
// 3. Update all activities that use location data to use the new cascading dropdowns
// 4. Test the location matching algorithm with the new hierarchical data

// Database Migration Example:
/*
fun migrateAddressToCity() {
    val db = FirebaseFirestore.getInstance()
    
    db.collection("users")
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val address = document.getString("address") ?: ""
                if (address.isNotEmpty() && !document.contains("city")) {
                    // Try to extract city from address
                    val extractedCity = extractCityFromAddress(address)
                    if (extractedCity.isNotEmpty()) {
                        document.reference.update("city", extractedCity)
                    }
                }
            }
        }
}

private fun extractCityFromAddress(address: String): String {
    // Simple extraction logic - can be improved
    val cities = LocationData.getAllCities()
    return cities.find { city ->
        address.contains(city, ignoreCase = true)
    } ?: ""
}
*/
