# Calendar Feature Troubleshooting Guide

## Issue: Calendar button not showing changes

If you're not seeing the calendar changes when clicking the calendar button, here are the steps to troubleshoot:

### 1. **Check if the app was rebuilt**
The most common issue is that the app needs to be rebuilt after adding new files and features.

**Solution:**
- In Android Studio, go to **Build** → **Clean Project**
- Then go to **Build** → **Rebuild Project**
- Run the app again

### 2. **Check the logs**
I've added logging to help debug the issue. Check the Android Studio Logcat for these messages:

**When clicking calendar button:**
- `HomeActivity: Calendar button clicked`

**When CalendarActivity starts:**
- `CalendarActivity: CalendarActivity onCreate called`
- `CalendarActivity: Layout set successfully`
- `CalendarActivity: Firebase initialized`
- `CalendarActivity: Views initialized`
- `CalendarActivity: Setup completed`

**If you see errors:**
- Look for any error messages in the logs
- Check if there are any missing dependencies or compilation errors

### 3. **Verify the calendar icon is visible**
Check if you can see the calendar icon in the bottom navigation bar:
- The calendar icon should appear between "Applications" and "Profile"
- If the icon is missing, the bottom navigation menu might not have been updated

### 4. **Test the navigation manually**
Try this temporary test:

1. Add this code to any button click in HomeActivity:
```kotlin
startActivity(Intent(this, CalendarActivity::class.java))
```

2. If this works, the issue is with the bottom navigation
3. If this doesn't work, there's a compilation issue

### 5. **Check for compilation errors**
Look for these potential issues:

**Missing imports:**
- CalendarActivity might not be importing correctly
- Check if all adapter classes are found

**Layout issues:**
- Verify all layout files exist
- Check if all view IDs are correct

**Manifest registration:**
- Verify CalendarActivity is registered in AndroidManifest.xml

### 6. **Force refresh the project**
Sometimes Android Studio needs a refresh:

1. **File** → **Invalidate Caches and Restart**
2. Choose **Invalidate and Restart**
3. Wait for the project to reload
4. Rebuild the project

### 7. **Check the build output**
Look at the build output for any errors:
- **Build** → **Make Project**
- Check the **Build** tab at the bottom for any errors
- Fix any compilation errors before testing

### 8. **Verify Firebase setup**
Make sure Firebase is properly configured:
- Check if `google-services.json` is in the `app/` folder
- Verify Firebase dependencies are in `build.gradle`
- Ensure internet permission is granted

### 9. **Test with a simple calendar**
If the full calendar doesn't work, try this minimal test:

Create a simple CalendarTestActivity:
```kotlin
class CalendarTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "Calendar Test - This Works!"
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        
        setContentView(textView)
    }
}
```

Register it in AndroidManifest.xml and test navigation to it.

### 10. **Common Solutions**

**If you see "Class not found" error:**
- Clean and rebuild the project
- Check if CalendarActivity is in the correct package

**If the app crashes:**
- Check the stack trace in Logcat
- Look for missing layout files or view IDs

**If nothing happens when clicking:**
- Check if the bottom navigation listener is set up correctly
- Verify the menu item ID matches

### 11. **Quick Test Steps**

1. **Clean & Rebuild** the project
2. **Run** the app
3. **Click** the calendar icon in bottom navigation
4. **Check Logcat** for the debug messages I added
5. **Report** what you see in the logs

### 12. **If still not working**

Please share:
1. Any error messages from Logcat
2. Whether you see the calendar icon in the bottom navigation
3. What happens when you click the calendar button (nothing, crash, error message)
4. The exact Android Studio version you're using

The calendar feature is fully implemented and should work once the app is properly rebuilt. The most common issue is simply needing to clean and rebuild the project after adding new files.
