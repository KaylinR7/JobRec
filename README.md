# JobRec App

A job recruitment app that connects job seekers with employers.

## API Keys Setup

This project uses external APIs that require authentication. To set up the necessary API keys:

1. **Hugging Face API Token**:
   - Create an account on [Hugging Face](https://huggingface.co/) if you don't have one
   - Generate an API token from your Hugging Face account settings
   - Add the token to your `local.properties` file:
     ```
     huggingface.token=YOUR_ACTUAL_TOKEN
     ```
   - Replace `YOUR_ACTUAL_TOKEN` with your actual Hugging Face API token
   - **IMPORTANT**: Never commit your `local.properties` file to version control

## Security Best Practices

- API keys and tokens should never be hardcoded in the source code
- Always use the `local.properties` file or environment variables to store sensitive information
- The `local.properties` file is automatically excluded from git by the `.gitignore` file
- If you accidentally commit sensitive information, revoke and regenerate your tokens immediately

## Building the Project

The app will automatically use the API token from your `local.properties` file during the build process.

## Features

- Job search with filters
- Application tracking
- Saved jobs
- Messaging with employers
- Profile management
- CV/resume uploads
- Notifications for job matches
- Company job posting
- Candidate search
- Application review
- Chatbot assistance
