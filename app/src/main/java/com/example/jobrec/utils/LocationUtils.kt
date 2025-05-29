package com.example.jobrec.utils

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import com.example.jobrec.models.LocationData

object LocationUtils {

    /**
     * Sets up cascading province and city spinners
     * @param context The context
     * @param provinceSpinner The province spinner
     * @param citySpinner The city spinner
     * @param selectedProvince Initially selected province (optional)
     * @param selectedCity Initially selected city (optional)
     * @param onLocationSelected Callback when both province and city are selected
     */
    fun setupCascadingLocationSpinners(
        context: Context,
        provinceSpinner: Spinner,
        citySpinner: Spinner,
        selectedProvince: String? = null,
        selectedCity: String? = null,
        onLocationSelected: ((province: String, city: String) -> Unit)? = null
    ) {
        // Setup province spinner
        val provinceAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            mutableListOf("Select Province").apply { addAll(LocationData.provinces) }
        )
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        provinceSpinner.adapter = provinceAdapter

        // Setup city spinner (initially empty)
        val cityAdapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            mutableListOf("Select City")
        )
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter

        // Set initial selections if provided
        selectedProvince?.let { province ->
            val provinceIndex = LocationData.provinces.indexOf(province)
            if (provinceIndex >= 0) {
                provinceSpinner.setSelection(provinceIndex + 1) // +1 for "Select Province" item
                updateCitySpinner(context, citySpinner, province, selectedCity)
            }
        }

        // Province selection listener
        provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) { // Skip "Select Province" item
                    val selectedProvinceName = LocationData.provinces[position - 1]
                    updateCitySpinner(context, citySpinner, selectedProvinceName, null)
                } else {
                    // Clear city spinner when no province is selected
                    clearCitySpinner(context, citySpinner)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                clearCitySpinner(context, citySpinner)
            }
        }

        // City selection listener
        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0 && provinceSpinner.selectedItemPosition > 0) { // Skip "Select City" item
                    val selectedProvinceName = LocationData.provinces[provinceSpinner.selectedItemPosition - 1]
                    val cities = LocationData.getCitiesForProvince(selectedProvinceName)
                    if (position - 1 < cities.size) {
                        val selectedCityName = cities[position - 1]
                        onLocationSelected?.invoke(selectedProvinceName, selectedCityName)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Sets up cascading province and city AutoCompleteTextViews
     * @param context The context
     * @param provinceSpinner The province AutoCompleteTextView
     * @param citySpinner The city AutoCompleteTextView
     * @param selectedProvince Initially selected province (optional)
     * @param selectedCity Initially selected city (optional)
     * @param onLocationSelected Callback when both province and city are selected
     */
    fun setupCascadingLocationSpinners(
        context: Context,
        provinceSpinner: AutoCompleteTextView,
        citySpinner: AutoCompleteTextView,
        selectedProvince: String? = null,
        selectedCity: String? = null,
        onLocationSelected: ((province: String, city: String) -> Unit)? = null
    ) {
        Log.d("LocationUtils", "Setting up cascading dropdowns")

        // Setup province dropdown
        val provinceAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            LocationData.provinces.toMutableList()
        )
        provinceSpinner.setAdapter(provinceAdapter)
        provinceSpinner.threshold = 1

        // Setup city dropdown (initially disabled)
        citySpinner.isEnabled = false
        citySpinner.setText("Select province first", false)

        // Province selection listener
        provinceSpinner.setOnItemClickListener { parent, view, position, id ->
            val selectedProvinceName = parent.getItemAtPosition(position) as String
            Log.d("LocationUtils", "Province clicked: $selectedProvinceName")

            // Get cities for selected province
            val cities = LocationData.getCitiesForProvince(selectedProvinceName)
            Log.d("LocationUtils", "Found ${cities.size} cities for $selectedProvinceName")

            if (cities.isNotEmpty()) {
                // Create new adapter for cities
                val cityAdapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    cities.toMutableList()
                )

                // Update city dropdown
                citySpinner.setAdapter(cityAdapter)
                citySpinner.isEnabled = true
                citySpinner.setText("", false)
                citySpinner.threshold = 1

                // Set click listener for city dropdown
                citySpinner.setOnClickListener {
                    Log.d("LocationUtils", "City dropdown clicked")
                    citySpinner.showDropDown()
                }

                // City selection listener
                citySpinner.setOnItemClickListener { cityParent, cityView, cityPosition, cityId ->
                    val selectedCityName = cityParent.getItemAtPosition(cityPosition) as String
                    Log.d("LocationUtils", "City selected: $selectedCityName")
                    onLocationSelected?.invoke(selectedProvinceName, selectedCityName)
                }

                Log.d("LocationUtils", "City dropdown setup complete")
            } else {
                Log.w("LocationUtils", "No cities found for province: $selectedProvinceName")
                citySpinner.isEnabled = false
                citySpinner.setText("No cities available", false)
            }

            // Trigger callback with province and empty city
            onLocationSelected?.invoke(selectedProvinceName, "")
        }

        // Make province dropdown show on click
        provinceSpinner.setOnClickListener {
            Log.d("LocationUtils", "Province dropdown clicked")
            provinceSpinner.showDropDown()
        }

        // Set initial selections if provided
        selectedProvince?.let { province ->
            if (LocationData.provinces.contains(province)) {
                provinceSpinner.setText(province, false)
                // Trigger the same logic as clicking
                val cities = LocationData.getCitiesForProvince(province)
                if (cities.isNotEmpty()) {
                    val cityAdapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        cities.toMutableList()
                    )
                    citySpinner.setAdapter(cityAdapter)
                    citySpinner.isEnabled = true
                    citySpinner.threshold = 1

                    selectedCity?.let { city ->
                        if (cities.contains(city)) {
                            citySpinner.setText(city, false)
                        }
                    }
                }
            }
        }

        Log.d("LocationUtils", "Cascading dropdowns setup complete")
    }



    /**
     * Updates the city spinner based on selected province
     */
    private fun updateCitySpinner(
        context: Context,
        citySpinner: Spinner,
        province: String,
        selectedCity: String?
    ) {
        val cities = LocationData.getCitiesForProvince(province)
        val cityAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            mutableListOf("Select City").apply { addAll(cities) }
        )
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter

        // Set selected city if provided
        selectedCity?.let { city ->
            val cityIndex = cities.indexOf(city)
            if (cityIndex >= 0) {
                citySpinner.setSelection(cityIndex + 1) // +1 for "Select City" item
            }
        }
    }

    /**
     * Clears the city spinner
     */
    private fun clearCitySpinner(context: Context, citySpinner: Spinner) {
        val cityAdapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            listOf("Select City")
        )
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter
    }

    /**
     * Gets the selected province from spinner
     */
    fun getSelectedProvince(provinceSpinner: Spinner): String? {
        return if (provinceSpinner.selectedItemPosition > 0) {
            LocationData.provinces[provinceSpinner.selectedItemPosition - 1]
        } else null
    }

    /**
     * Gets the selected city from spinner
     */
    fun getSelectedCity(citySpinner: Spinner, province: String?): String? {
        return if (citySpinner.selectedItemPosition > 0 && province != null) {
            val cities = LocationData.getCitiesForProvince(province)
            if (citySpinner.selectedItemPosition - 1 < cities.size) {
                cities[citySpinner.selectedItemPosition - 1]
            } else null
        } else null
    }

    /**
     * Validates that the selected province and city combination is valid
     */
    fun isValidLocationSelection(province: String?, city: String?): Boolean {
        return when {
            province.isNullOrEmpty() -> false
            city.isNullOrEmpty() -> false
            else -> LocationData.isValidProvinceCity(province, city)
        }
    }

    /**
     * Gets a formatted location string for display
     */
    fun getFormattedLocation(province: String?, city: String?): String {
        return when {
            !province.isNullOrEmpty() && !city.isNullOrEmpty() -> "$city, $province"
            !city.isNullOrEmpty() -> city
            !province.isNullOrEmpty() -> province
            else -> "Location not specified"
        }
    }

    /**
     * Test method to verify LocationData is working
     */
    fun testLocationData(): String {
        val testResults = mutableListOf<String>()

        testResults.add("Total provinces: ${LocationData.provinces.size}")
        testResults.add("Provinces: ${LocationData.provinces}")

        LocationData.provinces.forEach { province ->
            val cities = LocationData.getCitiesForProvince(province)
            testResults.add("$province: ${cities.size} cities")
            if (cities.isNotEmpty()) {
                testResults.add("  First 3 cities: ${cities.take(3)}")
            }
        }

        return testResults.joinToString("\n")
    }
}
