# Hierarchical City-Province Location System Implementation

## Overview
This implementation provides a comprehensive hierarchical location system for the CareerWorx job matching platform, replacing the simple address field with intelligent city-province relationships.

## Key Features Implemented

### 1. **LocationData Model**
- Complete mapping of South African provinces to their cities
- 9 provinces with 150+ cities accurately mapped
- Utility methods for validation and lookup
- Helper functions for province-city relationships

### 2. **Enhanced User Model**
- **BREAKING CHANGE**: Replaced `address` field with `city` field
- Users now have both `province` and `city` fields
- Maintains backward compatibility through migration strategies

### 3. **Enhanced Job Model**
- **BREAKING CHANGE**: Renamed `location` field to `city` for consistency
- Jobs now have both `province` and `city` fields
- Clear separation between province and city data

### 4. **Intelligent Location Matching Algorithm**
- **Hierarchical matching** with multiple priority levels:
  - **100%**: Perfect city match or remote work
  - **95%**: City-province hierarchy match (e.g., user: "Johannesburg" ↔ job: "Gauteng")
  - **90%**: Same province, cities not specified
  - **85%**: Same province, different cities
  - **80%**: Partial city name matches
  - **75%**: Partial province matches
  - **50%**: Different provinces

### 5. **Cascading Dropdown UI Components**
- **LocationUtils** utility class for easy integration
- Province selection triggers city dropdown population
- Validation ensures only valid province-city combinations
- Consistent UI across all forms (signup, profile, job posting)

### 6. **Updated Layouts**
- **ProfileActivity**: Address field → City dropdown
- **SignupActivity**: Address field → City dropdown  
- **PostJobActivity**: Single location field → Province + City dropdowns
- Material Design dropdowns with proper styling

## Technical Implementation

### Files Created/Modified:

#### **New Files:**
1. `LocationData.kt` - Province-city mapping and utilities
2. `LocationUtils.kt` - UI helper for cascading dropdowns
3. `LOCATION_IMPLEMENTATION_EXAMPLE.kt` - Integration examples

#### **Modified Files:**
1. `User.kt` - Replaced `address` with `city` field
2. `Job.kt` - Renamed `location` to `city` field
3. `JobMatchingService.kt` - Enhanced location matching algorithm
4. `JobMatch.kt` - Updated MatchCriteria with province/city
5. Layout files - Updated forms to use cascading dropdowns
6. `JobMatchingTest.kt` - Added location matching tests

### Database Schema Changes:
```
User Collection:
- ❌ address: String (removed)
+ ✅ city: String (added)
  ✅ province: String (existing)

Job Collection:
- ❌ location: String (renamed)
+ ✅ city: String (renamed from location)
  ✅ province: String (existing)
```

## Location Matching Examples

### Perfect Matches (100%):
- User: "Johannesburg, Gauteng" ↔ Job: "Johannesburg, Gauteng"
- Any location ↔ "Remote" work

### Excellent Matches (95%):
- User: "Johannesburg, Gauteng" ↔ Job: "-, Gauteng" 
- User: "-, Gauteng" ↔ Job: "Johannesburg, Gauteng"

### Good Matches (85-90%):
- User: "Johannesburg, Gauteng" ↔ Job: "Pretoria, Gauteng"
- User: "-, Gauteng" ↔ Job: "-, Gauteng"

### Fair Matches (50-80%):
- User: "Johannesburg, Gauteng" ↔ Job: "Cape Town, Western Cape"
- Partial city/province name matches

## Integration Guide

### 1. **For Profile Forms:**
```kotlin
LocationUtils.setupCascadingLocationSpinners(
    context = this,
    provinceSpinner = provinceInput,
    citySpinner = cityInput,
    selectedProvince = user.province,
    selectedCity = user.city
) { province, city ->
    selectedProvince = province
    selectedCity = city
}
```

### 2. **For Validation:**
```kotlin
if (!LocationUtils.isValidLocationSelection(selectedProvince, selectedCity)) {
    Toast.makeText(this, "Please select both province and city", Toast.LENGTH_SHORT).show()
    return
}
```

### 3. **For Display:**
```kotlin
val locationText = LocationUtils.getFormattedLocation(user.province, user.city)
// Returns: "Johannesburg, Gauteng" or "Location not specified"
```

## Migration Strategy

### 1. **Database Migration:**
- Add `city` field to existing user documents
- Attempt to extract city from existing `address` field
- Keep `address` field temporarily for fallback

### 2. **Gradual Rollout:**
- Phase 1: Add city field alongside address
- Phase 2: Update UI to use cascading dropdowns
- Phase 3: Migrate data and remove address field
- Phase 4: Update all location references

### 3. **Data Extraction Logic:**
```kotlin
fun extractCityFromAddress(address: String): String {
    val cities = LocationData.getAllCities()
    return cities.find { city ->
        address.contains(city, ignoreCase = true)
    } ?: ""
}
```

## Benefits

### **For Users:**
- ✅ Easier location selection with guided dropdowns
- ✅ More accurate job recommendations based on location
- ✅ Better understanding of job proximity
- ✅ Consistent location data across the platform

### **For Employers:**
- ✅ More precise candidate filtering by location
- ✅ Better local talent discovery
- ✅ Accurate location-based job posting

### **For the Platform:**
- ✅ Improved job matching accuracy
- ✅ Better analytics on location-based trends
- ✅ Reduced data inconsistencies
- ✅ Enhanced search and filtering capabilities

## Testing

Comprehensive test suite includes:
- Perfect location matches (100% score)
- Same province matches (85-90% score)
- City-province hierarchy recognition (95% score)
- Cross-province matching (50% score)
- Remote work handling (100% score)

## Future Enhancements

1. **Distance-based matching** using GPS coordinates
2. **Transport route analysis** for commute feasibility
3. **Regional economic data** integration
4. **Multi-location job support** (hybrid/multiple offices)
5. **Location preference weighting** by user

This hierarchical location system provides a solid foundation for accurate, user-friendly location handling that significantly improves job matching quality and user experience.
