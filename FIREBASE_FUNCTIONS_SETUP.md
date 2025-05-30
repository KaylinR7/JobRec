# Firebase Functions Setup for Push Notifications

## Why Firebase Functions? 🚀

Since Firebase has deprecated server keys for newer projects, Firebase Functions is the modern, secure way to send push notifications. This approach:

- ✅ **No server keys needed** - Uses Firebase Admin SDK securely
- ✅ **Automatic scaling** - Handles any number of notifications
- ✅ **Built-in security** - User authentication required
- ✅ **Real-time triggers** - Automatically sends notifications when data changes
- ✅ **Free tier available** - 125K invocations per month free

## Quick Setup (5 minutes) ⚡

### Step 1: Install Firebase CLI
```bash
npm install -g firebase-tools
```

### Step 2: Login to Firebase
```bash
firebase login
```

### Step 3: Navigate to Functions Directory
```bash
cd "/Users/nuveshannaicker/Desktop/Kotlin App/firebase-functions"
```

### Step 4: Initialize Firebase (if not already done)
```bash
firebase init functions
```
- Select your existing project: `careerworx-f5bc6`
- Choose JavaScript
- Install dependencies: Yes

### Step 5: Install Dependencies
```bash
cd functions
npm install
```

### Step 6: Deploy Functions
```bash
firebase deploy --only functions
```

## What Gets Deployed 📦

The setup includes these Cloud Functions:

### 1. `sendNotification`
- Sends notification to a specific device token
- Called from your Android app
- Replaces the need for server keys

### 2. `sendMulticastNotification`
- Sends notification to multiple devices at once
- Useful for broadcasting to all company users

### 3. `sendTopicNotification`
- Sends notification to all devices subscribed to a topic
- Good for general announcements

### 4. `onNewMessage` (Automatic Trigger)
- Automatically sends notifications when meeting invitations are created
- Triggers when documents are added to `messages` collection

### 5. `onNewApplication` (Automatic Trigger)
- Automatically sends notifications when job applications are submitted
- Triggers when documents are added to `applications` collection

## Testing the Setup 🧪

### 1. Check Deployment
After deployment, you should see:
```
✔  functions: Finished running predeploy script.
✔  functions[sendNotification(us-central1)]: Successful create operation.
✔  functions[sendMulticastNotification(us-central1)]: Successful create operation.
✔  functions[sendTopicNotification(us-central1)]: Successful create operation.
✔  functions[onNewMessage(us-central1)]: Successful create operation.
✔  functions[onNewApplication(us-central1)]: Successful create operation.
```

### 2. Test from Android App
1. Run your app
2. Go to test notifications
3. Try "FCM Test"
4. Check logs - you should see:
   ```
   D/NotificationManager: Notification sent successfully via Firebase Functions
   ```

### 3. Test Real Notifications
1. Install app on two devices
2. Log in with different accounts
3. Send a meeting invitation
4. The other device should receive the notification!

## How It Works 🔧

### Before (Broken)
```
Android App → Server Key → FCM → Other Device
     ❌ No server key available
```

### After (Working)
```
Android App → Firebase Functions → Firebase Admin SDK → FCM → Other Device
     ✅ Secure, modern approach
```

## Automatic Notifications 🤖

Once deployed, these notifications will be sent automatically:

- **Meeting Invitations**: When someone sends a meeting invite in chat
- **Job Applications**: When a student applies for a job
- **Application Status**: When companies update application status (you can add this trigger)

## Troubleshooting 🔍

### Functions Not Deploying?
```bash
# Check if you're logged in
firebase login --reauth

# Check project
firebase use careerworx-f5bc6

# Try deploying one function at a time
firebase deploy --only functions:sendNotification
```

### App Can't Call Functions?
1. Make sure user is logged in (functions require authentication)
2. Check internet connection
3. Verify project ID matches in both places

### Still Not Working?
Check the logs:
```bash
firebase functions:log
```

## Cost 💰

Firebase Functions pricing:
- **Free tier**: 125K invocations/month, 40K GB-seconds/month
- **Paid tier**: $0.40 per million invocations

For a typical job app, this is essentially free unless you have thousands of users.

## Next Steps 🎯

1. **Deploy the functions** (5 minutes)
2. **Test notifications** between devices
3. **Add more triggers** as needed (new jobs, status updates, etc.)
4. **Monitor usage** in Firebase Console

Once deployed, your notifications will work reliably without any server keys or backend maintenance! 🎉
