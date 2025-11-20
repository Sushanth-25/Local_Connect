const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require(path.join(__dirname, 'serviceAccountKey.json'));
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

async function listAllUsers() {
  try {
    const listUsersResult = await admin.auth().listUsers(1000);

    console.log('\n=== All Users ===\n');
    listUsersResult.users.forEach((userRecord, index) => {
      console.log(`${index + 1}. ${userRecord.email}`);
      console.log(`   UID: ${userRecord.uid}`);
      console.log(`   Claims: ${JSON.stringify(userRecord.customClaims || {})}`);
      console.log('');
    });

    console.log(`Total users: ${listUsersResult.users.length}`);
    process.exit(0);
  } catch (error) {
    console.error('Error listing users:', error);
    process.exit(1);
  }
}

listAllUsers();

