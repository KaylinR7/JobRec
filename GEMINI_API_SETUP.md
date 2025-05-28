# Gemini API Setup Guide

## Step 1: Get Your API Key

1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Sign in with your Google account
3. Click "Get API Key" in the left sidebar
4. Create a new API key
5. Copy the API key

## Step 2: Add Your API Key to the App

You need to replace `"YOUR_GEMINI_API_KEY_HERE"` with your actual API key in these files:

### File 1: `app/src/main/java/com/example/jobrec/services/GeminiJobMatchingService.kt`
- Line 13: Replace `"YOUR_GEMINI_API_KEY_HERE"` with your API key

### File 2: `app/src/main/java/com/example/jobrec/chatbot/ChatbotRepository.kt`
- Line 19: Replace `"YOUR_GEMINI_API_KEY_HERE"` with your API key

## Example:
```kotlin
// Before:
private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"

// After:
private const val API_KEY = "AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

## Step 3: Sync and Build

1. Sync your project in Android Studio
2. Clean and rebuild the project
3. Test the app

## Features Now Using Gemini:

✅ **Job Matching**: AI-powered job recommendations with percentage scores
✅ **Chatbot**: Intelligent responses for user questions
✅ **Free Tier**: 15 requests per minute, 1500 per day
✅ **No Credit Card**: Required for free tier

## Free Tier Limits:
- **Rate Limit**: 15 requests per minute
- **Daily Limit**: 1500 requests per day
- **Monthly Limit**: No monthly limit on free tier

## Troubleshooting:

### If you get API errors:
1. Check that your API key is correct
2. Make sure you haven't exceeded rate limits
3. Verify your Google account has access to Gemini API

### If job matching isn't working:
- The app will fall back to simple keyword matching if Gemini fails
- Check the logs for any error messages

### Rate Limit Handling:
- The app automatically falls back to local matching if API limits are hit
- Consider upgrading to paid tier if you need higher limits

## Next Steps:
- Test job matching by viewing recommended jobs
- Test the chatbot by asking questions
- Monitor your API usage in Google AI Studio
