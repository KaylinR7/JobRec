const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Cloud Function to send FCM notifications
 * This replaces the need for server keys
 */
exports.sendNotification = functions.https.onCall(async (data, context) => {
  // Verify the user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { token, title, body, notificationData } = data;

  // Validate required fields
  if (!token || !title || !body) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required fields: token, title, body');
  }

  try {
    // Create the notification payload
    const message = {
      token: token,
      notification: {
        title: title,
        body: body,
      },
      data: notificationData || {},
      android: {
        notification: {
          icon: 'ic_app_logo',
          color: '#2196F3',
          sound: 'default',
          channelId: determineChannelId(notificationData?.type)
        }
      }
    };

    // Send the notification
    const response = await admin.messaging().send(message);

    console.log('Successfully sent message:', response);

    return {
      success: true,
      messageId: response
    };

  } catch (error) {
    console.error('Error sending message:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send notification', error);
  }
});

/**
 * Cloud Function to send notifications to multiple tokens
 */
exports.sendMulticastNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { tokens, title, body, notificationData } = data;

  if (!tokens || !Array.isArray(tokens) || tokens.length === 0) {
    throw new functions.https.HttpsError('invalid-argument', 'tokens must be a non-empty array');
  }

  try {
    const message = {
      tokens: tokens,
      notification: {
        title: title,
        body: body,
      },
      data: notificationData || {},
      android: {
        notification: {
          icon: 'ic_app_logo',
          color: '#2196F3',
          sound: 'default',
          channelId: determineChannelId(notificationData?.type)
        }
      }
    };

    const response = await admin.messaging().sendMulticast(message);

    console.log('Successfully sent multicast message:', response);

    return {
      success: true,
      successCount: response.successCount,
      failureCount: response.failureCount,
      responses: response.responses
    };

  } catch (error) {
    console.error('Error sending multicast message:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send notifications', error);
  }
});

/**
 * Cloud Function to send notification to a topic
 */
exports.sendTopicNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { topic, title, body, notificationData } = data;

  if (!topic || !title || !body) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required fields: topic, title, body');
  }

  try {
    const message = {
      topic: topic,
      notification: {
        title: title,
        body: body,
      },
      data: notificationData || {},
      android: {
        notification: {
          icon: 'ic_app_logo',
          color: '#2196F3',
          sound: 'default',
          channelId: determineChannelId(notificationData?.type)
        }
      }
    };

    const response = await admin.messaging().send(message);

    console.log('Successfully sent topic message:', response);

    return {
      success: true,
      messageId: response
    };

  } catch (error) {
    console.error('Error sending topic message:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send topic notification', error);
  }
});

/**
 * Helper function to determine notification channel ID
 */
function determineChannelId(notificationType) {
  switch (notificationType) {
    case 'job_application':
      return 'company_notifications';
    case 'application_status':
      return 'application_notifications';
    case 'new_job':
      return 'job_notifications';
    case 'meeting_invitation':
      return 'meeting_notifications';
    default:
      return 'careerworx_default';
  }
}

/**
 * Firestore trigger to send notifications when new messages are created
 */
exports.onNewMessage = functions.firestore
  .document('messages/{messageId}')
  .onCreate(async (snap, context) => {
    const messageData = snap.data();

    try {
      // Get the recipient's FCM token (check both users and companies)
      let recipientDoc = await admin.firestore()
        .collection('users')
        .doc(messageData.receiverId)
        .get();

      if (!recipientDoc.exists) {
        // Try companies collection
        recipientDoc = await admin.firestore()
          .collection('companies')
          .doc(messageData.receiverId)
          .get();
      }

      if (!recipientDoc.exists) {
        console.log('Recipient not found in users or companies');
        return;
      }

      const fcmToken = recipientDoc.data().fcmToken;
      if (!fcmToken) {
        console.log('No FCM token for recipient');
        return;
      }

      // Handle different message types
      let title = 'New Message';
      let body = '';
      let channelId = 'careerworx_default';

      if (messageData.type === 'meeting_invite') {
        title = 'Meeting Invitation';
        body = `${messageData.senderName || 'Someone'} invited you for an interview`;
        channelId = 'meeting_notifications';
      } else {
        // Regular chat message
        title = `Message from ${messageData.senderName || 'Someone'}`;
        body = messageData.content || 'You have a new message';
        channelId = 'careerworx_default';
      }

      // Send notification
      const message = {
        token: fcmToken,
        notification: {
          title: title,
          body: body,
        },
        data: {
          type: messageData.type || 'chat_message',
          messageId: context.params.messageId,
          senderId: messageData.senderId,
          conversationId: messageData.conversationId,
          senderName: messageData.senderName || ''
        },
        android: {
          notification: {
            icon: 'ic_app_logo',
            color: '#2196F3',
            sound: 'default',
            channelId: channelId
          }
        }
      };

      await admin.messaging().send(message);
      console.log(`Message notification sent: ${messageData.type || 'chat_message'}`);

    } catch (error) {
      console.error('Error sending message notification:', error);
    }
  });

/**
 * Firestore trigger to send notifications when new applications are created
 */
exports.onNewApplication = functions.firestore
  .document('applications/{applicationId}')
  .onCreate(async (snap, context) => {
    const applicationData = snap.data();

    try {
      // Get the company's FCM token
      const companyDoc = await admin.firestore()
        .collection('companies')
        .doc(applicationData.companyId)
        .get();

      if (!companyDoc.exists) {
        console.log('Company not found');
        return;
      }

      const fcmToken = companyDoc.data().fcmToken;
      if (!fcmToken) {
        console.log('No FCM token for company');
        return;
      }

      // Get applicant name
      const userDoc = await admin.firestore()
        .collection('users')
        .doc(applicationData.userId)
        .get();

      const applicantName = userDoc.exists ?
        `${userDoc.data().name} ${userDoc.data().surname}` :
        'Someone';

      // Send notification
      const message = {
        token: fcmToken,
        notification: {
          title: 'New Job Application',
          body: `${applicantName} applied for ${applicationData.jobTitle}`,
        },
        data: {
          type: 'job_application',
          applicationId: context.params.applicationId,
          jobTitle: applicationData.jobTitle,
          applicantName: applicantName
        },
        android: {
          notification: {
            icon: 'ic_app_logo',
            color: '#2196F3',
            sound: 'default',
            channelId: 'company_notifications'
          }
        }
      };

      await admin.messaging().send(message);
      console.log('Job application notification sent');

    } catch (error) {
      console.error('Error sending job application notification:', error);
    }
  });

/**
 * Firestore trigger to send notifications when new jobs are posted
 */
exports.onNewJob = functions.firestore
  .document('jobs/{jobId}')
  .onCreate(async (snap, context) => {
    const jobData = snap.data();

    try {
      // Get company information
      const companyDoc = await admin.firestore()
        .collection('companies')
        .doc(jobData.companyId)
        .get();

      const companyName = companyDoc.exists ? companyDoc.data().name : 'A company';

      // Get all students with matching interests/skills
      const studentsSnapshot = await admin.firestore()
        .collection('users')
        .where('role', '==', 'student')
        .get();

      const notifications = [];

      studentsSnapshot.forEach(studentDoc => {
        const studentData = studentDoc.data();
        const fcmToken = studentData.fcmToken;

        if (!fcmToken) return;

        // Check if student's interests match job field
        const studentInterests = studentData.interests || [];
        const jobField = jobData.field || jobData.category || '';

        const isMatch = studentInterests.some(interest =>
          interest.toLowerCase().includes(jobField.toLowerCase()) ||
          jobField.toLowerCase().includes(interest.toLowerCase())
        );

        if (isMatch || !jobField) { // Send to all if no specific field
          const message = {
            token: fcmToken,
            notification: {
              title: 'New Job Opportunity',
              body: `${companyName} posted: ${jobData.title} in ${jobData.location || 'your area'}`,
            },
            data: {
              type: 'new_job',
              jobId: context.params.jobId,
              jobTitle: jobData.title,
              companyName: companyName,
              location: jobData.location || '',
              field: jobField
            },
            android: {
              notification: {
                icon: 'ic_app_logo',
                color: '#2196F3',
                sound: 'default',
                channelId: 'job_notifications'
              }
            }
          };

          notifications.push(admin.messaging().send(message));
        }
      });

      if (notifications.length > 0) {
        await Promise.all(notifications);
        console.log(`New job notifications sent to ${notifications.length} students`);
      }

    } catch (error) {
      console.error('Error sending new job notifications:', error);
    }
  });

/**
 * Firestore trigger to send notifications when application status changes
 */
exports.onApplicationStatusUpdate = functions.firestore
  .document('applications/{applicationId}')
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // Check if status changed
    if (beforeData.status === afterData.status) {
      return;
    }

    try {
      // Get student's FCM token
      const userDoc = await admin.firestore()
        .collection('users')
        .doc(afterData.userId)
        .get();

      if (!userDoc.exists) {
        console.log('Student not found');
        return;
      }

      const fcmToken = userDoc.data().fcmToken;
      if (!fcmToken) {
        console.log('No FCM token for student');
        return;
      }

      // Get company name
      const companyDoc = await admin.firestore()
        .collection('companies')
        .doc(afterData.companyId)
        .get();

      const companyName = companyDoc.exists ? companyDoc.data().name : 'Company';

      // Determine notification message based on status
      let title = 'Application Update';
      let body = '';

      switch (afterData.status.toLowerCase()) {
        case 'accepted':
        case 'approved':
          title = '🎉 Application Accepted!';
          body = `Congratulations! ${companyName} accepted your application for ${afterData.jobTitle}`;
          break;
        case 'rejected':
        case 'declined':
          title = 'Application Update';
          body = `${companyName} has updated your application status for ${afterData.jobTitle}`;
          break;
        case 'viewed':
        case 'under_review':
          title = 'Application Viewed';
          body = `${companyName} has viewed your application for ${afterData.jobTitle}`;
          break;
        case 'interview':
        case 'interview_scheduled':
          title = '📅 Interview Scheduled';
          body = `${companyName} wants to interview you for ${afterData.jobTitle}`;
          break;
        default:
          title = 'Application Update';
          body = `${companyName} updated your application for ${afterData.jobTitle}`;
      }

      // Send notification
      const message = {
        token: fcmToken,
        notification: {
          title: title,
          body: body,
        },
        data: {
          type: 'application_status',
          applicationId: context.params.applicationId,
          status: afterData.status,
          jobTitle: afterData.jobTitle,
          companyName: companyName
        },
        android: {
          notification: {
            icon: 'ic_app_logo',
            color: '#2196F3',
            sound: 'default',
            channelId: 'application_notifications'
          }
        }
      };

      await admin.messaging().send(message);
      console.log(`Application status notification sent: ${afterData.status}`);

    } catch (error) {
      console.error('Error sending application status notification:', error);
    }
  });
