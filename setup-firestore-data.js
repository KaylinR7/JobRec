const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const auth = admin.auth();

async function setupFirestoreData() {
  console.log('Setting up Firestore collections and sample data...');

  try {
    // Default password for all test accounts
    const defaultPassword = 'password123';

    // Create sample users with Firebase Auth
    const usersData = [
      {
        id: 'user1',
        name: 'John',
        surname: 'Doe',
        email: 'john.doe@example.com',
        role: 'user',
        education: 'Bachelor of Computer Science',
        experience: '2-3 years',
        location: 'Cape Town',
        field: 'Technology',
        subField: 'Software Development'
      },
      {
        id: 'user2',
        name: 'Jane',
        surname: 'Smith',
        email: 'jane.smith@example.com',
        role: 'user',
        education: 'Bachelor of Business Administration',
        experience: '1-2 years',
        location: 'Johannesburg',
        field: 'Business',
        subField: 'Marketing'
      }
    ];

    // Create sample companies with Firebase Auth
    const companiesData = [
      {
        id: 'company1',
        name: 'TechCorp',
        email: 'hr@techcorp.com',
        industry: 'Technology',
        location: 'Cape Town',
        description: 'Leading technology company',
        registrationNumber: 'REG001'
      },
      {
        id: 'company2',
        name: 'BusinessSolutions',
        email: 'hr@businesssolutions.com',
        industry: 'Consulting',
        location: 'Johannesburg',
        description: 'Business consulting firm',
        registrationNumber: 'REG002'
      }
    ];

    // Create sample jobs
    const jobsData = [
      {
        id: 'job1',
        title: 'Software Developer',
        companyId: 'company1',
        companyName: 'TechCorp',
        description: 'Develop web applications',
        requirements: 'Bachelor degree in Computer Science',
        location: 'Cape Town',
        category: 'Technology',
        subcategory: 'Software Development',
        experienceLevel: '2-3 years',
        status: 'active',
        postedDate: admin.firestore.Timestamp.now()
      },
      {
        id: 'job2',
        title: 'Marketing Specialist',
        companyId: 'company2',
        companyName: 'BusinessSolutions',
        description: 'Manage marketing campaigns',
        requirements: 'Bachelor degree in Marketing',
        location: 'Johannesburg',
        category: 'Business',
        subcategory: 'Marketing',
        experienceLevel: '1-2 years',
        status: 'active',
        postedDate: admin.firestore.Timestamp.now()
      }
    ];

    // Create Firebase Auth users and Firestore documents
    console.log('Creating Firebase Auth users and Firestore documents...');

    // Create admin user first
    try {
      const adminUser = await auth.createUser({
        email: 'admin@careerworx.com',
        password: 'admin123',
        displayName: 'Admin User'
      });
      console.log(`✅ Created admin Firebase Auth user: admin@careerworx.com`);
    } catch (error) {
      if (error.code === 'auth/email-already-exists') {
        console.log(`ℹ️  Admin user already exists in Firebase Auth`);
      } else {
        console.error(`❌ Error creating admin user:`, error.message);
      }
    }

    // Add users with Firebase Auth
    for (const user of usersData) {
      try {
        // Create Firebase Auth user
        const userRecord = await auth.createUser({
          email: user.email,
          password: defaultPassword,
          displayName: `${user.name} ${user.surname}`
        });

        // Add to Firestore with the Firebase Auth UID
        const userWithId = { ...user, id: userRecord.uid };
        await db.collection('users').doc(userRecord.uid).set(userWithId);
        console.log(`✅ Created user: ${user.name} ${user.surname} (${user.email})`);
      } catch (error) {
        if (error.code === 'auth/email-already-exists') {
          console.log(`ℹ️  User ${user.email} already exists in Firebase Auth`);
          // Try to find existing user and update Firestore
          try {
            const existingUser = await auth.getUserByEmail(user.email);
            const userWithId = { ...user, id: existingUser.uid };
            await db.collection('users').doc(existingUser.uid).set(userWithId);
            console.log(`✅ Updated Firestore for existing user: ${user.name} ${user.surname}`);
          } catch (firestoreError) {
            console.error(`❌ Error updating Firestore for ${user.email}:`, firestoreError.message);
          }
        } else {
          console.error(`❌ Error creating user ${user.email}:`, error.message);
        }
      }
    }

    // Add companies with Firebase Auth
    for (const company of companiesData) {
      try {
        // Create Firebase Auth user for company
        const userRecord = await auth.createUser({
          email: company.email,
          password: defaultPassword,
          displayName: company.name
        });

        // Add to Firestore with the Firebase Auth UID
        const companyWithUserId = { ...company, userId: userRecord.uid };
        await db.collection('companies').doc(company.registrationNumber).set(companyWithUserId);
        console.log(`✅ Created company: ${company.name} (${company.email})`);
      } catch (error) {
        if (error.code === 'auth/email-already-exists') {
          console.log(`ℹ️  Company ${company.email} already exists in Firebase Auth`);
          // Try to find existing user and update Firestore
          try {
            const existingUser = await auth.getUserByEmail(company.email);
            const companyWithUserId = { ...company, userId: existingUser.uid };
            await db.collection('companies').doc(company.registrationNumber).set(companyWithUserId);
            console.log(`✅ Updated Firestore for existing company: ${company.name}`);
          } catch (firestoreError) {
            console.error(`❌ Error updating Firestore for ${company.email}:`, firestoreError.message);
          }
        } else {
          console.error(`❌ Error creating company ${company.email}:`, error.message);
        }
      }
    }

    // Add jobs
    for (const job of jobsData) {
      await db.collection('jobs').doc(job.id).set(job);
      console.log(`Created job: ${job.title}`);
    }

    console.log('\n🎉 All sample data created successfully!');
    console.log('📊 Collections created:');
    console.log('   - users (2 documents)');
    console.log('   - companies (2 documents)');
    console.log('   - jobs (2 documents)');
    console.log('\n🔐 Test login credentials:');
    console.log('   📚 Students:');
    console.log('      Email: john.doe@example.com | Password: password123');
    console.log('      Email: jane.smith@example.com | Password: password123');
    console.log('   🏢 Companies:');
    console.log('      Email: hr@techcorp.com | Password: password123');
    console.log('      Email: hr@businesssolutions.com | Password: password123');
    console.log('   👨‍💼 Admin:');
    console.log('      Email: admin@careerworx.com | Password: admin123');
    console.log('\n✨ All users now have Firebase Authentication accounts and can login!');

  } catch (error) {
    console.error('Error setting up data:', error);
  }
}

setupFirestoreData();
