const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function setupFirestoreData() {
  console.log('Setting up Firestore collections and sample data...');

  try {
    // Create sample users
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

    // Create sample companies
    const companiesData = [
      {
        id: 'company1',
        name: 'TechCorp',
        email: 'hr@techcorp.com',
        industry: 'Technology',
        location: 'Cape Town',
        description: 'Leading technology company'
      },
      {
        id: 'company2',
        name: 'BusinessSolutions',
        email: 'hr@businesssolutions.com',
        industry: 'Consulting',
        location: 'Johannesburg',
        description: 'Business consulting firm'
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

    // Add users
    for (const user of usersData) {
      await db.collection('users').doc(user.id).set(user);
      console.log(`Created user: ${user.name} ${user.surname}`);
    }

    // Add companies
    for (const company of companiesData) {
      await db.collection('companies').doc(company.id).set(company);
      console.log(`Created company: ${company.name}`);
    }

    // Add jobs
    for (const job of jobsData) {
      await db.collection('jobs').doc(job.id).set(job);
      console.log(`Created job: ${job.title}`);
    }

    console.log('✅ All sample data created successfully!');
    console.log('Collections created:');
    console.log('- users (2 documents)');
    console.log('- companies (2 documents)');
    console.log('- jobs (2 documents)');

  } catch (error) {
    console.error('Error setting up data:', error);
  }
}

setupFirestoreData();
