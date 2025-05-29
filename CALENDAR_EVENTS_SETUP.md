# Calendar Events Setup

This document explains how to add sample calendar events scheduled for tomorrow to the DUT CareerHub application.

## Overview

The `setup-calendar-events.js` script creates sample calendar events for tomorrow that will be visible in both the student and company calendar views. This is useful for testing and demonstrating the calendar functionality.

## What Events Are Created

The script creates **11 sample events** for tomorrow:

### Student Events (5 events)
**For user1 (John Doe):**
- 9:00 AM - Software Developer Interview with TechCorp (Online)
- 2:30 PM - Career Counseling Session (In-person)
- 4:00 PM - Portfolio Review Workshop (In-person)

**For user2 (Jane Smith):**
- 10:30 AM - Marketing Specialist Interview with BusinessSolutions (Online)
- 6:00 PM - Networking Event for Business Students (In-person)

### Company Events (6 events)
**For company1 (TechCorp):**
- 9:00 AM - Interview with John Doe for Software Developer (Online)
- 11:00 AM - Team Planning Meeting (In-person)
- 3:00 PM - Campus Recruitment Preparation (In-person)

**For company2 (BusinessSolutions):**
- 10:30 AM - Interview with Jane Smith for Marketing Specialist (Online)
- 1:00 PM - Client Presentation Review (In-person)
- 4:30 PM - University Partnership Meeting (Online)

## Event Types

The events include:
- **Interview Events**: Linked to specific jobs and companies
- **Regular Events**: Career counseling, workshops, meetings
- **Online Meetings**: With meeting links (Google Meet, Zoom, Teams)
- **In-person Meetings**: With location details
- **Various Durations**: From 60 minutes to 3 hours

## Prerequisites

Before running the script, make sure you have:

1. **Node.js installed** on your system
2. **Firebase project set up** with the correct configuration
3. **Sample users and companies** created (run `setup-data.js` first if not done)
4. **Internet connection** to connect to Firebase

### Installing Node.js (if not already installed)

**Option 1: Download from official website**
1. Go to [https://nodejs.org/](https://nodejs.org/)
2. Download the LTS version for your operating system
3. Run the installer and follow the instructions
4. Verify installation by running: `node --version` and `npm --version`

**Option 2: Using package managers**
- **Windows (with Chocolatey)**: `choco install nodejs`
- **Windows (with Winget)**: `winget install OpenJS.NodeJS`
- **macOS (with Homebrew)**: `brew install node`
- **Linux (Ubuntu/Debian)**: `sudo apt install nodejs npm`

## How to Run

1. **Install dependencies**:
   ```bash
   npm install
   ```

   Or if you don't have package.json:
   ```bash
   npm install firebase
   ```

2. **Run the calendar events setup script**:
   ```bash
   npm run setup-calendar
   ```

   Or directly:
   ```bash
   node setup-calendar-events.js
   ```

3. **Alternative: Run all setup scripts**:
   ```bash
   npm run setup-all
   ```

3. **Expected output**:
   ```
   📅 Setting up calendar events for tomorrow...
   📝 Adding 11 calendar events for tomorrow...
   ✅ Added Student event: "Software Developer Interview - TechCorp" for user1 at 9:00 AM
   ✅ Added Student event: "Career Counseling Session" for user1 at 2:30 PM
   ... (more events)

   🎉 All tomorrow's calendar events created successfully!
   📊 Events summary:
      - Total events: 11
      - Student events: 5
      - Company events: 6
      - Interview events: 4
      - Online meetings: 5
      - In-person meetings: 6

   📅 All events are scheduled for: [Tomorrow's Date]
   💡 You can now open the calendar in the app to see tomorrow's events!
   ```

## Verifying the Events

After running the script, you can verify the events were created by:

1. **Opening the Android app**
2. **Logging in as a student** (john.doe@example.com or jane.smith@example.com)
3. **Going to the Calendar tab**
4. **Checking the "Tomorrow's Events" section**
5. **Logging in as a company** (hr@techcorp.com or hr@businesssolutions.com)
6. **Going to the Company Calendar**
7. **Checking the "Tomorrow's Interviews" section**

## Database Structure

Events are stored in the `calendar_events` collection in Firestore with the following structure:

```javascript
{
  userId: "user1" | companyId: "company1",
  title: "Event Title",
  description: "Event Description",
  date: Timestamp (tomorrow's date),
  time: "9:00 AM",
  duration: 90, // minutes
  meetingType: "online" | "in-person",
  location: "Location for in-person meetings",
  meetingLink: "Link for online meetings",
  notes: "Additional notes",
  isInterview: true | false,
  jobId: "job1", // if interview
  companyId: "company1", // if interview
  status: "scheduled",
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

## Troubleshooting

**If you get Firebase connection errors:**
- Check your internet connection
- Verify the Firebase configuration in the script
- Make sure the Firebase project is active

**If events don't appear in the app:**
- Make sure you're logged in as the correct user
- Check that the date calculation is correct
- Verify the events were created in Firestore console

**If you want to run the script multiple times:**
- The script will create duplicate events each time it's run
- You may want to clear the `calendar_events` collection first

## Related Files

- `setup-data.js` - Creates sample users, companies, and jobs
- `CalendarActivity.kt` - Student calendar implementation
- `CompanyCalendarActivity.kt` - Company calendar implementation
- `CalendarEvent.kt` - Event data model
