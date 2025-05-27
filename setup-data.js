const { initializeApp } = require('firebase/app');
const { getFirestore, collection, doc, setDoc, Timestamp } = require('firebase/firestore');

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

async function setupData() {
  console.log('🚀 Setting up Firestore collections and sample data...');

  try {
    // Sample users
    const users = [
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
      },
      {
        id: 'admin1',
        name: 'Admin',
        surname: 'User',
        email: 'admin@careerworx.com',
        role: 'admin'
      }
    ];

    // Sample companies
    const companies = [
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

    // Sample jobs
    const jobs = [
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
        postedDate: Timestamp.now()
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
        postedDate: Timestamp.now()
      }
    ];

    // Add users
    console.log('📝 Creating users...');
    for (const user of users) {
      await setDoc(doc(db, 'users', user.id), user);
      console.log(`✅ Created user: ${user.name} ${user.surname}`);
    }

    // Add companies
    console.log('🏢 Creating companies...');
    for (const company of companies) {
      await setDoc(doc(db, 'companies', company.id), company);
      console.log(`✅ Created company: ${company.name}`);
    }

    // Add jobs
    console.log('💼 Creating jobs...');
    for (const job of jobs) {
      await setDoc(doc(db, 'jobs', job.id), job);
      console.log(`✅ Created job: ${job.title}`);
    }

    console.log('\n🎉 All sample data created successfully!');
    console.log('📊 Collections created:');
    console.log('   - users (3 documents)');
    console.log('   - companies (2 documents)');
    console.log('   - jobs (2 documents)');
    console.log('\n🔐 Test accounts you can use:');
    console.log('   Student: john.doe@example.com');
    console.log('   Student: jane.smith@example.com');
    console.log('   Company: hr@techcorp.com');
    console.log('   Company: hr@businesssolutions.com');
    console.log('   Admin: admin@careerworx.com');

  } catch (error) {
    console.error('❌ Error setting up data:', error);
  }
}

setupData();
