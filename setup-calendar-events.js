const { initializeApp } = require('firebase/app');
const { getFirestore, collection, doc, setDoc, addDoc, Timestamp } = require('firebase/firestore');

// Your Firebase config from google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyCtx9phNT-1PGHQvdXF2B-nM1i8mjfLsrE",
  authDomain: "careerworx-f5bc6.firebaseapp.com",
  projectId: "careerworx-f5bc6",
  storageBucket: "careerworx-f5bc6.firebasestorage.app",
  messagingSenderId: "820823115311",
  appId: "1:820823115311:android:7bc9899be0640c17cee8da"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function setupTomorrowCalendarEvents() {
  console.log('📅 Setting up calendar events for tomorrow...');

  try {
    // Calculate tomorrow's date
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(0, 0, 0, 0); // Start of tomorrow

    // Helper function to create a timestamp for tomorrow at a specific time
    function createTomorrowTimestamp(hour, minute = 0) {
      const date = new Date(tomorrow);
      date.setHours(hour, minute, 0, 0);
      return Timestamp.fromDate(date);
    }

    // Sample calendar events for tomorrow
    const tomorrowEvents = [
      // Student events (user1 - John Doe) - These are already accepted invitations
      {
        userId: 'user1',
        title: 'Software Developer Interview - TechCorp',
        description: 'Technical interview for Software Developer position at TechCorp',
        date: createTomorrowTimestamp(9, 0), // 9:00 AM
        time: '9:00 AM',
        duration: 90,
        meetingType: 'online',
        meetingLink: 'https://meet.google.com/abc-defg-hij',
        notes: 'Prepare for coding questions and system design',
        isInterview: true,
        jobId: 'job1',
        companyId: 'company1',
        status: 'scheduled',
        invitationMessageId: 'msg_accepted_1'
      },
      {
        userId: 'user1',
        title: 'Career Counseling Session',
        description: 'One-on-one career guidance session',
        date: createTomorrowTimestamp(14, 30), // 2:30 PM
        time: '2:30 PM',
        duration: 60,
        meetingType: 'in-person',
        location: 'DUT Career Center, Room 201',
        notes: 'Discuss career goals and job search strategies',
        isInterview: false,
        status: 'scheduled'
      },
      {
        userId: 'user1',
        title: 'Portfolio Review Workshop',
        description: 'Group workshop to review and improve portfolios',
        date: createTomorrowTimestamp(16, 0), // 4:00 PM
        time: '4:00 PM',
        duration: 120,
        meetingType: 'in-person',
        location: 'DUT Computer Lab 3',
        notes: 'Bring laptop and current portfolio materials',
        isInterview: false,
        status: 'scheduled'
      },

      // Student events (user2 - Jane Smith)
      {
        userId: 'user2',
        title: 'Marketing Specialist Interview - BusinessSolutions',
        description: 'Interview for Marketing Specialist position',
        date: createTomorrowTimestamp(10, 30), // 10:30 AM
        time: '10:30 AM',
        duration: 60,
        meetingType: 'online',
        meetingLink: 'https://zoom.us/j/123456789',
        notes: 'Prepare marketing campaign examples and portfolio',
        isInterview: true,
        jobId: 'job2',
        companyId: 'company2',
        status: 'scheduled'
      },
      {
        userId: 'user2',
        title: 'Networking Event - Business Students',
        description: 'Monthly networking event for business students',
        date: createTomorrowTimestamp(18, 0), // 6:00 PM
        time: '6:00 PM',
        duration: 180,
        meetingType: 'in-person',
        location: 'DUT Business School Auditorium',
        notes: 'Bring business cards and dress professionally',
        isInterview: false,
        status: 'scheduled'
      },

      // Company events (company1 - TechCorp)
      {
        companyId: 'company1',
        title: 'Interview - John Doe (Software Developer)',
        description: 'Technical interview with John Doe for Software Developer position',
        date: createTomorrowTimestamp(9, 0), // 9:00 AM
        time: '9:00 AM',
        duration: 90,
        meetingType: 'online',
        meetingLink: 'https://meet.google.com/abc-defg-hij',
        notes: 'Technical interview - focus on React, Node.js, and system design',
        isInterview: true,
        jobId: 'job1',
        status: 'scheduled'
      },
      {
        companyId: 'company1',
        title: 'Team Planning Meeting',
        description: 'Weekly team planning and sprint review',
        date: createTomorrowTimestamp(11, 0), // 11:00 AM
        time: '11:00 AM',
        duration: 60,
        meetingType: 'in-person',
        location: 'TechCorp Office, Conference Room A',
        notes: 'Review sprint progress and plan next iteration',
        isInterview: false,
        status: 'scheduled'
      },
      {
        companyId: 'company1',
        title: 'Campus Recruitment Preparation',
        description: 'Prepare for upcoming campus recruitment drive',
        date: createTomorrowTimestamp(15, 0), // 3:00 PM
        time: '3:00 PM',
        duration: 90,
        meetingType: 'in-person',
        location: 'TechCorp Office, HR Department',
        notes: 'Finalize job descriptions and interview questions',
        isInterview: false,
        status: 'scheduled'
      },

      // Company events (company2 - BusinessSolutions)
      {
        companyId: 'company2',
        title: 'Interview - Jane Smith (Marketing Specialist)',
        description: 'Interview with Jane Smith for Marketing Specialist position',
        date: createTomorrowTimestamp(10, 30), // 10:30 AM
        time: '10:30 AM',
        duration: 60,
        meetingType: 'online',
        meetingLink: 'https://zoom.us/j/123456789',
        notes: 'Assess marketing skills and cultural fit',
        isInterview: true,
        jobId: 'job2',
        status: 'scheduled'
      },
      {
        companyId: 'company2',
        title: 'Client Presentation Review',
        description: 'Review presentation for major client meeting',
        date: createTomorrowTimestamp(13, 0), // 1:00 PM
        time: '1:00 PM',
        duration: 75,
        meetingType: 'in-person',
        location: 'BusinessSolutions Office, Boardroom',
        notes: 'Final review before client presentation on Friday',
        isInterview: false,
        status: 'scheduled'
      },
      {
        companyId: 'company2',
        title: 'University Partnership Meeting',
        description: 'Discuss partnership opportunities with DUT',
        date: createTomorrowTimestamp(16, 30), // 4:30 PM
        time: '4:30 PM',
        duration: 90,
        meetingType: 'online',
        meetingLink: 'https://teams.microsoft.com/l/meetup-join/xyz',
        notes: 'Explore internship and graduate recruitment programs',
        isInterview: false,
        status: 'scheduled'
      }
    ];

    // Sample pending meeting invitations (messages)
    const pendingInvitations = [
      {
        id: 'msg_pending_1',
        conversationId: 'conv_1',
        senderId: 'company1',
        senderName: 'TechCorp HR',
        receiverId: 'user2',
        content: 'Meeting invitation',
        type: 'meeting_invite',
        interviewDetails: {
          date: createTomorrowTimestamp(11, 0), // 11:00 AM
          time: '11:00 AM',
          duration: 60,
          type: 'online',
          meetingLink: 'https://meet.google.com/pending-interview-1',
          status: 'pending',
          jobTitle: 'Frontend Developer',
          companyName: 'TechCorp',
          jobId: 'job3',
          companyId: 'company1'
        },
        isRead: false,
        createdAt: Timestamp.now()
      },
      {
        id: 'msg_pending_2',
        conversationId: 'conv_2',
        senderId: 'company2',
        senderName: 'BusinessSolutions HR',
        receiverId: 'user1',
        content: 'Meeting invitation',
        type: 'meeting_invite',
        interviewDetails: {
          date: createTomorrowTimestamp(15, 30), // 3:30 PM
          time: '3:30 PM',
          duration: 45,
          type: 'in-person',
          location: 'BusinessSolutions Office, Conference Room B',
          status: 'pending',
          jobTitle: 'Business Analyst',
          companyName: 'BusinessSolutions',
          jobId: 'job4',
          companyId: 'company2'
        },
        isRead: false,
        createdAt: Timestamp.now()
      }
    ];

    // Add all events to Firestore
    console.log(`📝 Adding ${tomorrowEvents.length} calendar events for tomorrow...`);

    for (const event of tomorrowEvents) {
      // Add timestamps for created and updated
      const eventWithTimestamps = {
        ...event,
        createdAt: Timestamp.now(),
        updatedAt: Timestamp.now()
      };

      await addDoc(collection(db, 'calendar_events'), eventWithTimestamps);

      const eventType = event.userId ? 'Student' : 'Company';
      const eventOwner = event.userId || event.companyId;
      console.log(`✅ Added ${eventType} event: "${event.title}" for ${eventOwner} at ${event.time}`);
    }

    // Add pending meeting invitations to messages collection
    console.log(`\n📨 Adding ${pendingInvitations.length} pending meeting invitations...`);

    for (const invitation of pendingInvitations) {
      await addDoc(collection(db, 'messages'), invitation);
      console.log(`✅ Added pending invitation: "${invitation.interviewDetails.jobTitle}" for ${invitation.receiverId} at ${invitation.interviewDetails.time}`);
    }

    console.log('\n🎉 All tomorrow\'s calendar events created successfully!');
    console.log('📊 Events summary:');
    console.log(`   - Total events: ${tomorrowEvents.length}`);
    console.log(`   - Student events: ${tomorrowEvents.filter(e => e.userId).length}`);
    console.log(`   - Company events: ${tomorrowEvents.filter(e => e.companyId).length}`);
    console.log(`   - Interview events: ${tomorrowEvents.filter(e => e.isInterview).length}`);
    console.log(`   - Online meetings: ${tomorrowEvents.filter(e => e.meetingType === 'online').length}`);
    console.log(`   - In-person meetings: ${tomorrowEvents.filter(e => e.meetingType === 'in-person').length}`);
    console.log(`   - Pending invitations: ${pendingInvitations.length}`);

    const tomorrowDate = tomorrow.toLocaleDateString();
    console.log(`\n📅 All events are scheduled for: ${tomorrowDate}`);
    console.log('\n💡 You can now open the calendar in the app to see tomorrow\'s events!');
    console.log('📱 Students will see pending invitations on their dashboard that they can accept or decline.');
    console.log('🔔 Once accepted, the events will appear in their calendar automatically.');

  } catch (error) {
    console.error('❌ Error setting up calendar events:', error);
  }
}

setupTomorrowCalendarEvents();
