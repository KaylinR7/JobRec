# DUT Career Hub

A comprehensive career management platform for Durban University of Technology students and companies.

## Features

- **Student Portal**: Job search, applications, profile management, calendar
- **Company Portal**: Job posting, candidate management, interview scheduling
- **Calendar System**: Event scheduling, interview management, reminders
- **Messaging**: Real-time communication between students and companies
- **Admin Panel**: User management, system administration

## Quick Setup

### 1. Install Node.js (Required for setup scripts)

**Windows:**
- Download from [https://nodejs.org/](https://nodejs.org/)
- Or use: `winget install OpenJS.NodeJS`

**Verify installation:**
```bash
node --version
npm --version
```

### 2. Setup Firebase Data

```bash
# Install dependencies
npm install

# Setup users, companies, and jobs
npm run setup-data

# Setup calendar events for tomorrow
npm run setup-calendar

# Or setup everything at once
npm run setup-all
```

**Windows users can also use:**
```bash
setup-calendar-events.bat
```

### 3. Test Accounts

After running the setup scripts, you can use these test accounts:

**Students:**
- john.doe@example.com
- jane.smith@example.com

**Companies:**
- hr@techcorp.com
- hr@businesssolutions.com

**Admin:**
- admin@careerworx.com

## Calendar Events

The setup script creates sample events for tomorrow including:
- Job interviews (online and in-person)
- Career counseling sessions
- Networking events
- Company meetings
- Workshop sessions

## Documentation

- [Calendar Events Setup](CALENDAR_EVENTS_SETUP.md) - Detailed calendar setup guide
- [Calendar Firestore Setup](CALENDAR_FIRESTORE_SETUP.md) - Database configuration
- [Calendar Troubleshooting](CALENDAR_TROUBLESHOOTING.md) - Common issues and solutions
- [Firebase Storage Setup](FIREBASE_STORAGE_SETUP.md) - File storage configuration

## Development

This is an Android application built with:
- **Kotlin** - Primary programming language
- **Firebase** - Backend services (Firestore, Authentication, Storage)
- **Material Design** - UI components
- **Navigation Component** - App navigation

## Project Structure

```
app/
├── src/main/java/com/example/jobrec/
│   ├── CalendarActivity.kt          # Student calendar
│   ├── CompanyCalendarActivity.kt   # Company calendar
│   ├── models/CalendarEvent.kt      # Event data model
│   └── ...
├── setup-calendar-events.js        # Calendar setup script
├── setup-data.js                   # Basic data setup
└── package.json                    # Node.js dependencies
```