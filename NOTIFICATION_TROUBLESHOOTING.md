# Notification Troubleshooting Guide

## Issue Description

**Problem**: Test notifications work when using the test button, but real notifications between devices don't show up.

**Root Cause**: The notification system has two different implementations:
1. **Test notifications** use `LocalNotificationService` which creates local notifications directly on the device
2. **Real notifications** use `NotificationManager` which attempts to send FCM (Firebase Cloud Messaging) notifications between devices, but the actual FCM sending implementation was incomplete

## Technical Analysis

### What Works ✅
- **Local test notifications** via `LocalNotificationService.showTestNotification()`
- **FCM token generation and storage** in Firestore
- **Notification channels and permissions** are properly configured
- **Firebase messaging service** is registered and can receive notifications

### What Doesn't Work ❌
- **Device-to-device notifications** because `NotificationManager.sendNotificationViaHTTPv1()` was just a placeholder
- **FCM HTTP API calls** were not actually being made
- **Server-side authentication** for FCM is missing

## Solution Implemented

### 1. Fixed NotificationManager
- Updated `sendNotificationViaHTTPv1()` to actually call the FCM service
- Added proper error handling and logging
- Connected to the `FCMHttpV1Service` for actual sending

### 2. Enhanced FCMHttpV1Service
- Implemented proper FCM Legacy API calls
- Added comprehensive logging for debugging
- Created proper FCM payload structure
- Added server key configuration (needs to be set)

### 3. Added Debugging
- Detailed logging throughout the notification flow
- Clear error messages when server key is missing
- Payload logging for troubleshooting

## How to Complete the Fix

### Step 1: Get FCM Server Key
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `careerworx-f5bc6`
3. Go to **Project Settings** (gear icon)
4. Click on **Cloud Messaging** tab
5. Copy the **Server key** (legacy)

### Step 2: Configure Server Key
Replace the placeholder in `FCMHttpV1Service.kt`:
```kotlin
private const val FCM_SERVER_KEY = "YOUR_ACTUAL_SERVER_KEY_HERE"
```

**⚠️ Security Warning**: Server keys should not be stored in client code in production. This is a temporary solution for testing.

### Step 3: Test Real Notifications
1. Install the app on two different devices
2. Log in with different accounts (one company, one student)
3. Try sending a meeting invitation or job application
4. Check the logs for FCM sending attempts

## Current Status

### What's Fixed ✅
- Notification flow is now connected end-to-end
- FCM service actually attempts to send notifications
- Comprehensive logging for debugging
- Proper error handling

### What Still Needs Configuration ⚙️
- FCM Server Key needs to be added
- Backend server implementation (recommended for production)

### Testing Without Server Key
Even without the server key, you can now:
- See detailed logs of notification attempts
- Verify FCM tokens are being generated and stored
- Confirm the notification flow is working
- Test the payload structure

## Production Recommendations

### 1. Backend Server Implementation
For production, implement a backend server with:
- Firebase Admin SDK
- Proper authentication
- Server-side FCM sending
- API endpoints for notification requests

### 2. Security Best Practices
- Never store server keys in client code
- Use Firebase Admin SDK on backend
- Implement proper user authentication
- Add rate limiting for notifications

### 3. Enhanced Features
- Notification delivery receipts
- Retry mechanisms for failed sends
- Notification analytics
- User notification preferences

## Debugging Commands

### Check FCM Tokens
```bash
# In Android Studio Logcat, filter by:
Tag: NotificationManager
Tag: FCMHttpV1Service
Tag: CareerWorxFirebaseMessagingService
```

### Test Notification Flow
1. Use the test button (works locally)
2. Send a meeting invitation (tests real FCM)
3. Apply for a job (tests company notifications)
4. Check logs for detailed flow information

## Files Modified

1. `app/src/main/java/com/example/jobrec/notifications/NotificationManager.kt`
   - Fixed `sendNotificationViaHTTPv1()` method
   - Added proper FCM service integration

2. `app/src/main/java/com/example/jobrec/notifications/FCMHttpV1Service.kt`
   - Implemented actual FCM Legacy API calls
   - Added comprehensive logging and error handling
   - Added server key configuration

3. `NOTIFICATION_TROUBLESHOOTING.md` (this file)
   - Complete troubleshooting guide
   - Setup instructions
   - Production recommendations

## Next Steps

1. **Immediate**: Add the FCM server key to test real notifications
2. **Short-term**: Implement proper backend server for production
3. **Long-term**: Add advanced notification features and analytics
