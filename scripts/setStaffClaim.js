const admin = require('firebase-admin');
const path = require('path');

const KEY_PATH = path.join(__dirname, 'serviceAccountKey.json');

try {
  const serviceAccount = require(KEY_PATH);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('Firebase Admin initialized successfully');
} catch (err) {
  console.error('Failed to load serviceAccountKey.json from scripts/');
  console.error('Please download it from Firebase Console → Project Settings → Service Accounts');
  process.exit(1);
}

async function main() {
  const args = process.argv.slice(2);
  if (args.length < 2) {
    console.error('Usage: node scripts/setStaffClaim.js <uid|email> <true|false>');
    console.error('Example: node scripts/setStaffClaim.js staff@example.com true');
    process.exit(1);
  }

  const identifier = args[0];
  const setValue = args[1].toLowerCase() === 'true';

  try {
    // Resolve user by UID or email
    let userRecord;
    if (identifier.includes('@')) {
      console.log(`Looking up user by email: ${identifier}`);
      userRecord = await admin.auth().getUserByEmail(identifier);
    } else {
      console.log(`Looking up user by UID: ${identifier}`);
      userRecord = await admin.auth().getUser(identifier);
    }

    const uid = userRecord.uid;
    console.log(`Found user: ${userRecord.email} (UID: ${uid})`);

    // Get existing claims
    const existingClaims = userRecord.customClaims || {};
    console.log('Existing custom claims:', existingClaims);

    // Update claims
    const newClaims = { ...existingClaims };
    if (setValue) {
      newClaims.staff = true;
    } else {
      delete newClaims.staff;
    }

    await admin.auth().setCustomUserClaims(uid, newClaims);
    console.log(`✓ Successfully ${setValue ? 'SET' : 'REMOVED'} staff claim for ${userRecord.email}`);
    console.log('New claims:', newClaims);
    console.log('\nIMPORTANT: User must sign out and sign in again to refresh their token and see the updated claims.');

    process.exit(0);
  } catch (err) {
    console.error('Error:', err.message || err);
    process.exit(1);
  }
}

main();

