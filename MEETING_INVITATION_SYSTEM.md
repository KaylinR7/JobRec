# Meeting Invitation System

## Overview

The DUT CareerHub now implements a comprehensive meeting invitation system where **students must accept meeting invitations before events are added to their calendar**. This ensures students have control over their schedule and can make informed decisions about interview appointments.

## How It Works

### 1. Company Sends Meeting Invitation
- Company schedules a meeting through the chat interface
- A **pending calendar event** is created for the company (marked as "Pending Response")
- A **meeting invitation message** is sent to the student
- **No calendar event is created for the student yet**

### 2. Student Receives Invitation
- Students see pending invitations on their **dashboard home screen**
- Invitations show all meeting details: date, time, duration, type, location/link
- Students can **Accept** or **Decline** each invitation

### 3. Student Accepts Invitation
- Calendar event is **automatically created** for the student
- Company's pending event is **updated to confirmed**
- Both parties now have the meeting in their calendars

### 4. Student Declines Invitation
- Company's pending event is **marked as declined/cancelled**
- No calendar event is created for the student
- Company is notified of the decline

## Key Features

### ✅ **Student Control**
- Students must explicitly accept invitations
- No surprise meetings appear in their calendar
- Clear visibility of all pending invitations

### ✅ **Real-time Updates**
- Company calendar reflects invitation status
- Pending events are visually distinct
- Status updates happen immediately

### ✅ **Complete Information**
- All meeting details shown before acceptance
- Meeting type (online/in-person) clearly indicated
- Location or meeting links provided

### ✅ **Visual Indicators**
- Pending events have different colors and opacity
- Clear status labels ("Pending Response", "Declined")
- Easy-to-use Accept/Decline buttons

## Database Structure

### Calendar Events
```javascript
{
  userId: "student_id" | companyId: "company_id",
  title: "Meeting Title",
  status: "pending" | "scheduled" | "cancelled",
  invitationMessageId: "message_id", // Links to invitation
  // ... other fields
}
```

### Meeting Invitation Messages
```javascript
{
  id: "message_id",
  type: "meeting_invite",
  senderId: "company_id",
  receiverId: "student_id",
  interviewDetails: {
    date: Timestamp,
    time: "2:00 PM",
    duration: 60,
    type: "online" | "in-person",
    location: "Office address",
    meetingLink: "https://meet.google.com/...",
    status: "pending" | "accepted" | "rejected"
  }
}
```

## User Interface

### Student Dashboard
- **Pending Meeting Invitations** section appears when invitations exist
- Shows invitation cards with all meeting details
- **Accept** (green) and **Decline** (red) buttons
- Section hides when no pending invitations

### Company Calendar
- Pending events show as "Interview - Job Title (Pending Response)"
- Different visual styling (reduced opacity)
- Status updates when student responds

### Student Calendar
- Only shows accepted meetings
- No pending events appear
- Clean, confirmed schedule only

## Benefits

### For Students
- **Full control** over their schedule
- **No surprise meetings** in calendar
- **Complete information** before committing
- **Easy acceptance/decline** process

### For Companies
- **Clear invitation status** tracking
- **Professional invitation** process
- **Reduced no-shows** (explicit acceptance)
- **Better communication** with candidates

### For System
- **Consistent data** (no orphaned events)
- **Clear audit trail** of invitations
- **Reduced conflicts** in scheduling
- **Better user experience**

## Testing the System

### Setup Test Data
```bash
# Run the calendar setup script
npm run setup-calendar
```

This creates:
- **11 confirmed calendar events** (already accepted)
- **2 pending meeting invitations** for students to test

### Test Scenarios

1. **Log in as student** (john.doe@example.com or jane.smith@example.com)
2. **Check dashboard** for "Pending Meeting Invitations" section
3. **Accept an invitation** and verify it appears in calendar
4. **Decline an invitation** and verify it disappears
5. **Log in as company** to see status updates

### Expected Behavior
- ✅ Pending invitations appear on student dashboard
- ✅ Accept creates calendar event for student
- ✅ Company calendar shows status changes
- ✅ Decline removes invitation without creating event
- ✅ Visual indicators work correctly

## Technical Implementation

### Key Files Modified
- `CalendarEvent.kt` - Added invitation status support
- `ChatActivity.kt` - Updated meeting scheduling flow
- `HomeActivity.kt` - Added pending invitations display
- `PendingInvitationAdapter.kt` - New adapter for invitations
- `ConversationRepository.kt` - Updated invitation handling

### New Components
- `PendingInvitationAdapter` - Displays invitation cards
- `item_pending_invitation.xml` - Invitation card layout
- Invitation acceptance/decline logic
- Calendar event creation on acceptance

## Future Enhancements

- **Email notifications** for invitations
- **Reminder notifications** for pending invitations
- **Bulk accept/decline** for multiple invitations
- **Reschedule requests** from students
- **Calendar integration** with external calendars
