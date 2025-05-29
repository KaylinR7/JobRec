# Calendar Feature Firestore Setup

## Required Firestore Indexes

If you encounter query errors related to indexes, you may need to create the following Firestore indexes:

### 1. Calendar Events Collection Index

**Collection:** `calendar_events`

**Fields:**
- `userId` (Ascending)
- `date` (Ascending)

### 2. Alternative Index (if needed)

**Collection:** `calendar_events`

**Fields:**
- `userId` (Ascending)
- `date` (Ascending)
- `time` (Ascending)

## How to Create Indexes

### Option 1: Automatic Creation
1. Use the calendar feature in the app
2. If you get an index error, Firebase will provide a direct link to create the required index
3. Click the link and Firebase will automatically create the index

### Option 2: Manual Creation
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to Firestore Database
4. Click on "Indexes" tab
5. Click "Create Index"
6. Set up the index with the fields listed above

### Option 3: Using Firebase CLI
Add the following to your `firestore.indexes.json` file:

```json
{
  "indexes": [
    {
      "collectionGroup": "calendar_events",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "userId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "date",
          "order": "ASCENDING"
        }
      ]
    }
  ]
}
```

Then run:
```bash
firebase deploy --only firestore:indexes
```

## Firestore Security Rules

Add these rules to your `firestore.rules` file:

```javascript
// Calendar events rules
match /calendar_events/{eventId} {
  allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
  allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
}
```

## Collection Structure

The `calendar_events` collection documents have the following structure:

```javascript
{
  id: "auto-generated",
  userId: "user-uid",
  title: "Event Title",
  description: "Event Description",
  date: Timestamp,
  time: "2:00 PM",
  duration: 60, // minutes
  meetingType: "online" | "in-person",
  location: "Location for in-person meetings",
  meetingLink: "Link for online meetings",
  notes: "Additional notes",
  isInterview: false,
  jobId: "job-id-if-interview",
  companyId: "company-id-if-interview",
  status: "scheduled",
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

## Troubleshooting

### Common Issues:

1. **Index Error**: Follow the automatic index creation link provided by Firebase
2. **Permission Denied**: Check Firestore security rules
3. **Network Error**: Check internet connection and Firebase project configuration

### Query Optimization:

The calendar feature uses client-side filtering to avoid complex Firestore queries that require multiple indexes. This approach:
- Reduces index requirements
- Improves query reliability
- Maintains good performance for typical calendar usage
