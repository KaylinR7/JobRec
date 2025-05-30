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
    
    // Only send notifications for certain message types
    if (messageData.type === 'meeting_invite') {
      try {
        // Get the recipient's FCM token
        const recipientDoc = await admin.firestore()
          .collection('users')
          .doc(messageData.receiverId)
          .get();
        
        if (!recipientDoc.exists) {
          console.log('Recipient not found');
          return;
        }
        
        const fcmToken = recipientDoc.data().fcmToken;
        if (!fcmToken) {
          console.log('No FCM token for recipient');
          return;
        }
        
        // Send notification
        const message = {
          token: fcmToken,
          notification: {
            title: 'Meeting Invitation',
            body: `${messageData.senderName || 'Someone'} invited you for an interview`,
          },
          data: {
            type: 'meeting_invitation',
            messageId: context.params.messageId,
            senderId: messageData.senderId,
            conversationId: messageData.conversationId
          },
          android: {
            notification: {
              icon: 'ic_app_logo',
              color: '#2196F3',
              sound: 'default',
              channelId: 'meeting_notifications'
            }
          }
        };
        
        await admin.messaging().send(message);
        console.log('Meeting invitation notification sent');
        
      } catch (error) {
        console.error('Error sending meeting invitation notification:', error);
      }
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
