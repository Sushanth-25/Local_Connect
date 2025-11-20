const admin = require('firebase-admin');
const path = require('path');

const KEY_PATH = path.join(__dirname, 'serviceAccountKey.json');

try {
  const serviceAccount = require(KEY_PATH);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
} catch (err) {
  console.error('Failed to initialize Firebase Admin');
  process.exit(1);
}

async function verifyUser(email) {
  try {
    const user = await admin.auth().getUserByEmail(email);
    console.log('\n=== User Information ===');
    console.log('Email:', user.email);
    console.log('UID:', user.uid);
    console.log('Email Verified:', user.emailVerified);
    console.log('Disabled:', user.disabled);
    console.log('Custom Claims:', user.customClaims || 'None');
    console.log('Created:', new Date(user.metadata.creationTime));
    console.log('Last Sign In:', new Date(user.metadata.lastSignInTime));
    console.log('========================\n');

    process.exit(0);
  } catch (err) {
    console.error('Error:', err.message);
    process.exit(1);
  }
}

const email = process.argv[2];
if (!email) {
  console.error('Usage: node scripts/verifyUser.js <email>');
  console.error('Example: node scripts/verifyUser.js staff@example.com');
  process.exit(1);
}

verifyUser(email);

