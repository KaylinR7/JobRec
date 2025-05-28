#!/bin/bash

# Deploy Firebase Storage Rules
echo "🚀 Deploying Firebase Storage Rules..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI is not installed. Please install it first:"
    echo "npm install -g firebase-tools"
    exit 1
fi

# Check if user is logged in
if ! firebase projects:list &> /dev/null; then
    echo "❌ Not logged in to Firebase. Please login first:"
    echo "firebase login"
    exit 1
fi

# Deploy storage rules
echo "📋 Deploying storage rules to careerworx-f5bc6..."
firebase deploy --only storage --project careerworx-f5bc6

if [ $? -eq 0 ]; then
    echo "✅ Storage rules deployed successfully!"
    echo ""
    echo "📝 Updated rules include:"
    echo "  - Profile images: /profile_images/{userId}"
    echo "  - Company logos: /company_logos/{companyId}"
    echo "  - CV uploads: /cvs/{cvFileName}"
    echo ""
    echo "🔒 Security features:"
    echo "  - Authentication required for all operations"
    echo "  - User can only upload their own profile image"
    echo "  - File size limits: 2MB for images, 5MB for PDFs"
    echo "  - Content type validation"
else
    echo "❌ Failed to deploy storage rules"
    exit 1
fi
