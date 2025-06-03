const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

// Initialize Firebase Admin SDK
admin.initializeApp();

// Email configuration
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: functions.config().email?.user || 'your-email@gmail.com',
    pass: functions.config().email?.password || 'your-app-password'
  }
});

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

    console.log('New message created:', {
      messageId: context.params.messageId,
      senderId: messageData.senderId,
      receiverId: messageData.receiverId,
      type: messageData.type,
      conversationId: messageData.conversationId
    });

    try {
      // Skip notifications for certain message types or if no receiverId
      if (!messageData.receiverId || messageData.receiverId === messageData.senderId) {
        console.log('Skipping notification - no valid receiver or sender is receiver');
        return;
      }

      // Get the recipient's FCM token (check both users and companies)
      let recipientDoc = await admin.firestore()
        .collection('users')
        .doc(messageData.receiverId)
        .get();

      let recipientType = 'student';

      if (!recipientDoc.exists) {
        // Try companies collection
        recipientDoc = await admin.firestore()
          .collection('companies')
          .doc(messageData.receiverId)
          .get();
        recipientType = 'company';
      }

      if (!recipientDoc.exists) {
        console.log(`Recipient not found in users or companies: ${messageData.receiverId}`);
        return;
      }

      const recipientData = recipientDoc.data();
      const fcmToken = recipientData.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token for recipient: ${messageData.receiverId}`);
        return;
      }

      // Get sender information for better notification content
      let senderDoc = await admin.firestore()
        .collection('users')
        .doc(messageData.senderId)
        .get();

      let senderName = messageData.senderName || 'Someone';
      let senderType = 'student';

      if (!senderDoc.exists) {
        // Try companies collection
        senderDoc = await admin.firestore()
          .collection('companies')
          .doc(messageData.senderId)
          .get();
        senderType = 'company';
      }

      if (senderDoc.exists) {
        const senderData = senderDoc.data();
        if (senderType === 'company') {
          senderName = senderData.companyName || senderData.name || 'Company';
        } else {
          senderName = `${senderData.name || ''} ${senderData.surname || ''}`.trim() || 'Student';
        }
      }

      // Handle different message types
      let title = 'New Message';
      let body = '';
      let channelId = 'careerworx_default';

      if (messageData.type === 'meeting_invite') {
        title = 'Meeting Invitation';
        body = `${senderName} invited you for an interview`;
        channelId = 'meeting_notifications';
      } else {
        // Regular chat message
        title = `Message from ${senderName}`;
        body = messageData.content || 'You have a new message';

        // Use appropriate channel based on recipient type
        channelId = recipientType === 'company' ? 'company_notifications' : 'careerworx_default';
      }

      console.log(`Sending notification to ${recipientType}: ${messageData.receiverId}`);
      console.log(`From ${senderType}: ${messageData.senderId} (${senderName})`);

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
          receiverId: messageData.receiverId,
          conversationId: messageData.conversationId,
          senderName: senderName,
          senderType: senderType,
          recipientType: recipientType
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
      console.log(`Message notification sent successfully: ${messageData.type || 'chat_message'}`);
      console.log(`Notification sent to ${recipientType} ${messageData.receiverId} from ${senderType} ${senderName}`);

    } catch (error) {
      console.error('Error sending message notification:', error);
      console.error('Message data:', messageData);
    }
  });

/**
 * Firestore trigger to send notifications when new applications are created
 */
exports.onNewApplication = functions.firestore
  .document('applications/{applicationId}')
  .onCreate(async (snap, context) => {
    const applicationData = snap.data();

    console.log('New application created:', {
      applicationId: context.params.applicationId,
      companyId: applicationData.companyId,
      userId: applicationData.userId,
      jobTitle: applicationData.jobTitle,
      applicantName: applicationData.applicantName
    });

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

      // Get applicant name - try from application data first, then from user doc
      let applicantName = applicationData.applicantName || 'Someone';

      if (!applicationData.applicantName && applicationData.userId) {
        const userDoc = await admin.firestore()
          .collection('users')
          .doc(applicationData.userId)
          .get();

        if (userDoc.exists) {
          const userData = userDoc.data();
          applicantName = `${userData.name} ${userData.surname}`;
        }
      }

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
      console.log('Job application notification sent successfully');

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

    console.log('New job created:', {
      jobId: context.params.jobId,
      title: jobData.title,
      companyId: jobData.companyId,
      companyName: jobData.companyName,
      city: jobData.city,
      jobField: jobData.jobField
    });

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
        const jobField = jobData.jobField || jobData.field || jobData.category || jobData.specialization || '';

        const isMatch = studentInterests.some(interest =>
          interest.toLowerCase().includes(jobField.toLowerCase()) ||
          jobField.toLowerCase().includes(interest.toLowerCase())
        ) || jobField === '' || studentInterests.length === 0; // Send to all if no specific matching

        if (isMatch) { // Send to matching students or all if no field specified
          const message = {
            token: fcmToken,
            notification: {
              title: 'New Job Opportunity',
              body: `${companyName} posted: ${jobData.title} in ${jobData.city || jobData.location || 'your area'}`,
            },
            data: {
              type: 'new_job',
              jobId: context.params.jobId,
              jobTitle: jobData.title,
              companyName: companyName,
              location: jobData.city || jobData.location || '',
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

    console.log('Application status update:', {
      applicationId: context.params.applicationId,
      oldStatus: beforeData.status,
      newStatus: afterData.status,
      userId: afterData.userId,
      jobTitle: afterData.jobTitle,
      companyId: afterData.companyId
    });

    // Check if status changed
    if (beforeData.status === afterData.status) {
      console.log('Status unchanged, skipping notification');
      return;
    }

    try {
      // Get student's FCM token - check userId field
      const userId = afterData.userId;
      if (!userId) {
        console.log('No userId in application data');
        return;
      }

      const userDoc = await admin.firestore()
        .collection('users')
        .doc(userId)
        .get();

      if (!userDoc.exists) {
        console.log(`Student not found with userId: ${userId}`);
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

/**
 * Cloud Function to send email verification code
 */
exports.sendVerificationEmail = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { email, verificationCode, userType, name } = data;

  if (!email || !verificationCode) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required fields: email, verificationCode');
  }

  try {
    const mailOptions = {
      from: functions.config().email?.user || 'noreply@dutcareerhub.com',
      to: email,
      subject: 'DUTCareerHub - Email Verification',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <div style="text-align: center; margin-bottom: 30px;">
            <h1 style="color: #2196F3; margin: 0;">DUTCareerHub</h1>
            <p style="color: #666; margin: 5px 0;">Your Career Journey Starts Here</p>
          </div>

          <div style="background: #f8f9fa; padding: 30px; border-radius: 10px; margin-bottom: 20px;">
            <h2 style="color: #333; margin-top: 0;">Welcome${name ? ` ${name}` : ''}!</h2>
            <p style="color: #666; line-height: 1.6;">
              Thank you for registering with DUTCareerHub. To complete your ${userType || 'account'} registration,
              please verify your email address using the verification code below:
            </p>

            <div style="text-align: center; margin: 30px 0;">
              <div style="background: #2196F3; color: white; padding: 15px 30px; border-radius: 5px; display: inline-block; font-size: 24px; font-weight: bold; letter-spacing: 3px;">
                ${verificationCode}
              </div>
            </div>

            <p style="color: #666; line-height: 1.6;">
              Enter this code in the app to verify your email address. This code will expire in 10 minutes.
            </p>
          </div>

          <div style="text-align: center; color: #999; font-size: 14px;">
            <p>If you didn't create an account with DUTCareerHub, please ignore this email.</p>
            <p>© 2024 DUTCareerHub. All rights reserved.</p>
          </div>
        </div>
      `
    };

    await transporter.sendMail(mailOptions);
    console.log('Verification email sent successfully to:', email);

    return {
      success: true,
      message: 'Verification email sent successfully'
    };

  } catch (error) {
    console.error('Error sending verification email:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send verification email', error);
  }
});

/**
 * Cloud Function to send password reset code
 */
exports.sendPasswordResetEmail = functions.https.onCall(async (data, context) => {
  const { email, resetCode, userType, name } = data;

  if (!email || !resetCode) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required fields: email, resetCode');
  }

  try {
    const mailOptions = {
      from: functions.config().email?.user || 'noreply@dutcareerhub.com',
      to: email,
      subject: 'DUTCareerHub - Password Reset',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <div style="text-align: center; margin-bottom: 30px;">
            <h1 style="color: #2196F3; margin: 0;">DUTCareerHub</h1>
            <p style="color: #666; margin: 5px 0;">Your Career Journey Starts Here</p>
          </div>

          <div style="background: #f8f9fa; padding: 30px; border-radius: 10px; margin-bottom: 20px;">
            <h2 style="color: #333; margin-top: 0;">Password Reset Request</h2>
            <p style="color: #666; line-height: 1.6;">
              Hello${name ? ` ${name}` : ''},
            </p>
            <p style="color: #666; line-height: 1.6;">
              We received a request to reset your password for your DUTCareerHub ${userType || 'account'}.
              Use the verification code below to reset your password:
            </p>

            <div style="text-align: center; margin: 30px 0;">
              <div style="background: #ff5722; color: white; padding: 15px 30px; border-radius: 5px; display: inline-block; font-size: 24px; font-weight: bold; letter-spacing: 3px;">
                ${resetCode}
              </div>
            </div>

            <p style="color: #666; line-height: 1.6;">
              Enter this code in the app to reset your password. This code will expire in 15 minutes.
            </p>

            <p style="color: #d32f2f; line-height: 1.6; font-weight: bold;">
              If you didn't request a password reset, please ignore this email and your password will remain unchanged.
            </p>
          </div>

          <div style="text-align: center; color: #999; font-size: 14px;">
            <p>For security reasons, this link will expire in 15 minutes.</p>
            <p>© 2024 DUTCareerHub. All rights reserved.</p>
          </div>
        </div>
      `
    };

    await transporter.sendMail(mailOptions);
    console.log('Password reset email sent successfully to:', email);

    return {
      success: true,
      message: 'Password reset email sent successfully'
    };

  } catch (error) {
    console.error('Error sending password reset email:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send password reset email', error);
  }
});

/**
 * Cloud Function to reset password with verification code
 * This function uses Firebase Admin SDK to directly update the user's password
 */
exports.resetPasswordWithCode = functions.https.onCall(async (data, context) => {
  const { email, newPassword, resetCode } = data;

  // Validate required fields
  if (!email || !newPassword || !resetCode) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required fields: email, newPassword, resetCode');
  }

  try {
    // Get user by email
    const userRecord = await admin.auth().getUserByEmail(email);

    // Verify the reset code by checking Firestore
    const usersRef = admin.firestore().collection('users');
    const companiesRef = admin.firestore().collection('companies');

    // Check users collection first
    const userQuery = await usersRef.where('email', '==', email.toLowerCase()).get();
    let isValidCode = false;
    let docToUpdate = null;
    let collectionRef = null;

    if (!userQuery.empty) {
      const userDoc = userQuery.docs[0];
      const userData = userDoc.data();
      const storedCode = userData.passwordResetCode;
      const expiryTime = userData.passwordResetExpiry;
      const currentTime = Date.now();

      if (storedCode === resetCode && currentTime < expiryTime) {
        isValidCode = true;
        docToUpdate = userDoc;
        collectionRef = usersRef;
      }
    } else {
      // Check companies collection
      const companyQuery = await companiesRef.where('email', '==', email.toLowerCase()).get();
      if (!companyQuery.empty) {
        const companyDoc = companyQuery.docs[0];
        const companyData = companyDoc.data();
        const storedCode = companyData.passwordResetCode;
        const expiryTime = companyData.passwordResetExpiry;
        const currentTime = Date.now();

        if (storedCode === resetCode && currentTime < expiryTime) {
          isValidCode = true;
          docToUpdate = companyDoc;
          collectionRef = companiesRef;
        }
      }
    }

    if (!isValidCode) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid or expired reset code');
    }

    // Update the user's password using Firebase Admin SDK
    await admin.auth().updateUser(userRecord.uid, {
      password: newPassword
    });

    // Clear the reset codes from Firestore
    await collectionRef.doc(docToUpdate.id).update({
      passwordResetCode: '',
      passwordResetExpiry: 0
    });

    console.log('Password reset successfully for user:', email);

    return {
      success: true,
      message: 'Password reset successfully'
    };

  } catch (error) {
    console.error('Error resetting password:', error);
    throw new functions.https.HttpsError('internal', 'Failed to reset password', error);
  }
});

/**
 * Cloud Function to delete a user from Firebase Authentication by email
 * This is needed for admin operations since client-side code cannot delete other users
 */
exports.deleteUserByEmail = functions.https.onCall(async (data, context) => {
    try {
        // Verify that the request is coming from an authenticated admin
        // For now, we'll allow any authenticated user to call this
        // In production, you should add proper admin role verification
        if (!context.auth) {
            throw new functions.https.HttpsError(
                'unauthenticated',
                'The function must be called while authenticated.'
            );
        }

        const { email } = data;

        if (!email) {
            throw new functions.https.HttpsError(
                'invalid-argument',
                'Email is required.'
            );
        }

        console.log(`Attempting to delete user with email: ${email}`);

        try {
            // Get user by email
            const userRecord = await admin.auth().getUserByEmail(email);
            console.log(`Found user with UID: ${userRecord.uid}`);

            // Delete the user
            await admin.auth().deleteUser(userRecord.uid);
            console.log(`Successfully deleted user with email: ${email} and UID: ${userRecord.uid}`);

            return {
                success: true,
                message: `User with email ${email} deleted successfully`,
                deletedUid: userRecord.uid
            };

        } catch (authError) {
            if (authError.code === 'auth/user-not-found') {
                console.log(`User with email ${email} not found in Firebase Auth`);
                return {
                    success: true,
                    message: `User with email ${email} was not found in Firebase Auth (may have been already deleted)`,
                    deletedUid: null
                };
            } else {
                console.error(`Error deleting user from Firebase Auth:`, authError);
                throw new functions.https.HttpsError(
                    'internal',
                    `Failed to delete user from Firebase Auth: ${authError.message}`
                );
            }
        }

    } catch (error) {
        console.error('Error in deleteUserByEmail function:', error);

        if (error instanceof functions.https.HttpsError) {
            throw error;
        }

        throw new functions.https.HttpsError(
            'internal',
            `An unexpected error occurred: ${error.message}`
        );
    }
});
