# CareerWorx Notification Setup Guide

## Overview
This guide will help you set up fast, reliable out-of-app notifications for your CareerWorx application. The notifications include:

1. **New job postings** - Students get notified when new jobs are posted
2. **Application status changes** - Students get notified when employers accept/reject their applications
3. **CV reviews** - Students get notified when employers review their CVs
4. **Profile views** - Students get notified when employers view their profiles
5. **New messages** - Both sides get notified of new chat messages

## Step 1: Get Your FCM Server Key

### Option A: Using Firebase Console (Recommended)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (`careerworx-f5bc6` or `job-rec-8006d`)
3. Click on the gear icon (Settings) → Project Settings
4. Go to the "Cloud Messaging" tab
5. Under "Project credentials", find "Server key"
6. Copy the server key (starts with `AAAA...`)

### Option B: Using Google Cloud Console
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Navigate to APIs & Services → Credentials
4. Find your "Firebase Cloud Messaging API" key
5. Copy the API key

## Step 2: Update the FCM Server Key

Replace the placeholder in `app/src/main/java/com/example/jobrec/services/NotificationManager.kt`:

```kotlin
// Replace this line:
private val FCM_SERVER_KEY = "AAAA_YOUR_ACTUAL_FCM_SERVER_KEY_HERE"

// With your actual key:
private val FCM_SERVER_KEY = "AAAA1234567890abcdef..." // Your actual key
```

## Step 3: Verify Firebase Project Configuration

Make sure your `app/google-services.json` file matches your Firebase project:

1. Download the latest `google-services.json` from Firebase Console
2. Replace the existing file in your `app/` directory
3. Ensure the `project_id` matches your intended project

## Step 4: Test Notifications

### Testing New Job Notifications
1. Post a new job from the company side
2. Students subscribed to relevant topics should receive notifications

### Testing Application Status Notifications
1. Company accepts/rejects an application
2. The candidate should receive a notification

### Testing Profile View Notifications
1. Company views a candidate's profile from search
2. The candidate should receive a notification

### Testing CV Review Notifications
1. Company clicks "Review CV" on an application
2. The candidate should receive a notification

### Testing Message Notifications
1. Send a message in chat
2. The recipient should receive a notification

## Step 5: Optimize for Speed

### Current Implementation Benefits:
- **Direct FCM calls** - No server-side delays
- **Immediate triggers** - Notifications sent as soon as actions occur
- **Multiple notification channels** - Different priorities for different types
- **Efficient token management** - Tokens stored in Firestore for fast lookup

### For Even Faster Notifications (Optional):
Consider implementing Firebase Cloud Functions for server-side triggers:

1. Create `functions/` directory in your project
2. Set up Cloud Functions to listen for Firestore changes
3. Move notification logic to server-side for guaranteed delivery

## Troubleshooting

### Notifications Not Received
1. Check if FCM server key is correct
2. Verify device has internet connection
3. Check if app has notification permissions
4. Ensure FCM tokens are being saved to Firestore

### Slow Notifications
1. Verify FCM server key is valid
2. Check network connectivity
3. Monitor Firestore read/write performance

### Testing on Device
1. Install app on physical device (emulator may have issues)
2. Ensure app is not in battery optimization mode
3. Check notification settings in device settings

## Security Notes

⚠️ **Important**: Never commit your FCM server key to version control!

Consider using:
- Environment variables
- Build configuration files
- Secure key management services

## Notification Channels

The app creates these notification channels:
- **Job Notifications** - High priority, with sound and vibration
- **Message Notifications** - High priority, with sound and vibration  
- **Application Updates** - High priority, with sound and vibration
- **Profile Views** - Default priority, with sound but no vibration

Users can customize these in their device notification settings.
