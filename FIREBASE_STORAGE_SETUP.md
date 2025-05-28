# Firebase Storage Setup Guide

## Current Issue
Your Firebase project region doesn't support free Storage buckets, so you need to create a paid Cloud Storage bucket.

## Option 1: Set Up Paid Storage (Recommended)

### Step 1: Create Storage Bucket
1. Go to [Firebase Console - Storage](https://console.firebase.google.com/project/careerworx-f5bc6/storage)
2. Click **"Create or import a Cloud Storage bucket"**
3. Choose the same region as your Firestore database
4. Accept the pricing (very affordable - usually a few cents for typical usage)

### Step 2: Deploy Security Rules
After setting up the bucket, run:
```bash
cd "/Users/nuveshannaicker/Desktop/Kotlin App"
firebase deploy --only storage --project careerworx-f5bc6
```

### Step 3: Test Profile Picture Upload
- Open your app
- Go to Profile
- Try uploading a profile picture
- Should work with Firebase Storage

## Option 2: Use Current Fallback System

### What I've Implemented
The app now automatically falls back to storing images in Firestore as base64 when Storage isn't available.

### How It Works
1. **First Try**: Upload to Firebase Storage
2. **If Storage Fails**: Automatically convert image to base64 and store in Firestore
3. **Loading**: App checks for both Storage URL and base64 data

### Limitations of Fallback
- Images stored as base64 in Firestore count against document size limits
- Slightly slower loading compared to Storage URLs
- Less efficient for large images

## Pricing Information

### Firebase Storage Costs (Very Affordable)
- **Storage**: $0.026 per GB per month
- **Downloads**: $0.12 per GB
- **Uploads**: $0.12 per GB

### Example Costs
- 1000 profile pictures (100KB each) = ~0.1GB = **$0.003/month**
- 10,000 image downloads = ~1GB = **$0.12**

## Recommendation
Set up the paid Storage bucket - it's extremely affordable and provides better performance than the Firestore fallback.

## Current Status
✅ App works with Firestore fallback (no setup needed)
⏳ Firebase Storage setup pending (recommended for better performance)
