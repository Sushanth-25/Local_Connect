# Staff Login System - Firebase Setup Guide (FREE TIER - No Cloud Functions)

## Overview
This guide walks you through setting up a staff authentication system where authorized staff members can log in and manage user-reported issues by updating their status (Open, In Progress, Resolved, Closed).

**✅ FREE TIER COMPATIBLE** - No Cloud Functions needed!

## System Architecture
- **Authentication**: Firebase Auth with custom claims (`staff: true`)
- **Authorization**: Server-side enforcement via Firestore Security Rules only
- **Staff Management**: Manual assignment via admin scripts
- **Client**: Android app with separate staff login portal

---

## Part 1: Firebase Console Setup

### Step 1: Enable Required Services

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. Select your project: `LocalConnect`

#### Enable Authentication
1. Click **Authentication** in left sidebar
2. Click **Get Started** (if not already enabled)
3. Go to **Sign-in method** tab
4. Ensure **Email/Password** is enabled
   - Click on it → Toggle **Enable** → Save

#### Enable Firestore Database
1. Click **Firestore Database** in left sidebar
2. If not created, click **Create database**
   - Select **Production mode** (we'll add rules next)
   - Choose location closest to your users (e.g., `asia-south1` for India)
   - Click **Enable**

---

## Part 2: Set Up Firebase Admin SDK (For Server-Side Scripts)

### Step 1: Install Node.js
1. Download Node.js from https://nodejs.org/ (LTS version recommended)
2. Install it on your computer
3. Verify installation:
   ```bash
   node --version
   npm --version
   ```

### Step 2: Get Firebase Service Account Key

1. In Firebase Console, click **⚙️ (Settings icon)** → **Project settings**
2. Go to **Service accounts** tab
3. Click **Generate new private key**
4. Click **Generate key** in the dialog
5. A JSON file will download (e.g., `localconnect-xxxxx-firebase-adminsdk-xxxxx.json`)
6. **IMPORTANT**: Keep this file secure! Never commit it to version control

### Step 3: Create Project Structure

In your project root directory `D:\LocalConnect\`, create a `scripts` folder:

```
D:\LocalConnect\
├── app/
├── gradle/
├── scripts/          ← Create this folder
│   └── (we'll add files here)
└── ... other files
```

### Step 4: Move Service Account Key

1. Rename the downloaded JSON file to `serviceAccountKey.json`
2. Move it to `D:\LocalConnect\scripts\serviceAccountKey.json`

### Step 5: Initialize Node Project

Open Command Prompt in `D:\LocalConnect\` and run:

```bash
cd D:\LocalConnect
npm init -y
npm install firebase-admin
```

This creates `package.json` and installs Firebase Admin SDK.

### Step 6: Create Admin Script to Set Staff Claims

Create a file `D:\LocalConnect\scripts\setStaffClaim.js`:

```javascript
// scripts/setStaffClaim.js
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
```

### Step 7: Create User Verification Script (Optional but Recommended)

Create `D:\LocalConnect\scripts\verifyUser.js`:

```javascript
// scripts/verifyUser.js
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
```

---

## Part 3: Set Up Firestore Security Rules

### Update Firestore Rules (Server-Side Security)

1. In Firebase Console, go to **Firestore Database**
2. Click on the **Rules** tab
3. Replace with these rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is staff
    function isStaff() {
      return request.auth != null && request.auth.token.staff == true;
    }
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the resource
    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }
    
    // Posts collection
    match /posts/{postId} {
      // Anyone authenticated can read
      allow read: if isAuthenticated();
      
      // Anyone authenticated can create
      allow create: if isAuthenticated();
      
      // Owner can update their own post, staff can update any post
      allow update: if isAuthenticated() && 
                      (isOwner(resource.data.userId) || isStaff());
      
      // Owner can delete their own post, staff can delete any post
      allow delete: if isAuthenticated() && 
                      (isOwner(resource.data.userId) || isStaff());
    }
    
    // Users collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isAuthenticated() && request.auth.uid == userId;
    }
    
    // Comments collection
    match /posts/{postId}/comments/{commentId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && 
                               (isOwner(resource.data.userId) || isStaff());
    }
  }
}
```

4. Click **Publish**

---

## Part 4: Create Your First Staff Account

### Step 1: Create Auth Account in Firebase Console

1. Go to **Authentication** → **Users** tab
2. Click **Add user**
3. Enter:
   - **Email**: `staff@yourcompany.com` (use your actual email)
   - **Password**: Create a strong password (e.g., `StaffPass123!`)
4. Click **Add user**
5. Note: The user is now created but not yet staff

### Step 2: Set Staff Custom Claim

Open Command Prompt and run:

```bash
cd D:\LocalConnect
node scripts\setStaffClaim.js staff@yourcompany.com true
```

You should see output like:
```
Firebase Admin initialized successfully
Looking up user by email: staff@yourcompany.com
Found user: staff@yourcompany.com (UID: abc123xyz...)
Existing custom claims: {}
✓ Successfully SET staff claim for staff@yourcompany.com
New claims: { staff: true }

IMPORTANT: User must sign out and sign in again to refresh their token and see the updated claims.
```

### Step 3: Verify Staff Claim (Optional)

Verify the claim was set correctly:

```bash
node scripts\verifyUser.js staff@yourcompany.com
```

You should see the staff claim listed.

---

## Part 5: Test the System

### Test 1: Regular User Login
1. Build and run your Android app
2. Create a regular user account (or use existing one)
3. On login screen, login normally
4. You should be taken to the home screen (not staff dashboard)

### Test 2: Staff Login
1. On login screen, tap **"Staff Login"** button at the bottom
2. Enter staff credentials:
   - Email: `staff@yourcompany.com`
   - Password: (the password you set)
3. Tap **"LOGIN AS STAFF"**
4. You should be taken to the Staff Dashboard showing all posts

### Test 3: Update Post Status
1. In Staff Dashboard, scroll through the posts
2. Click **"Change Status"** button on any post
3. Select a new status from the dialog:
   - Open
   - In Progress
   - Under Review
   - Resolved
   - Closed
4. Click **"Update"**
5. Status should update immediately
6. The post list refreshes automatically

### Test 4: Non-Staff Access Prevention
1. Try logging in with a regular user email in the staff login screen
2. Should show error: "This account is not authorized as staff"
3. User is automatically signed out for security

### Test 5: Staff Filtering
1. In Staff Dashboard, tap the "Filters" section to expand
2. Try different filters:
   - By Status: All, Open, In Progress, Resolved, Closed
   - By Type: All, Issues, Events, Posts
3. Posts should filter accordingly

---

## Part 6: Managing Staff Accounts

### Add New Staff Member

#### Step 1: Create Firebase Auth Account
```bash
# In Firebase Console → Authentication → Add user
# Or programmatically (not recommended for production)
```

#### Step 2: Set Staff Claim
```bash
cd D:\LocalConnect
node scripts\setStaffClaim.js newstaff@example.com true
```

#### Step 3: Notify Staff Member
- Send them their credentials
- Tell them to use "Staff Login" button in the app
- First login will ask them to change password (recommended)

### Remove Staff Access

To revoke staff access from a user:

```bash
cd D:\LocalConnect
node scripts\setStaffClaim.js oldstaff@example.com false
```

**Important**: The user must sign out and sign back in for the change to take effect.

### List All Users (Optional Script)

Create `scripts/listUsers.js`:

```javascript
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
    
    process.exit(0);
  } catch (error) {
    console.error('Error listing users:', error);
    process.exit(1);
  }
}

listAllUsers();
```

Usage:
```bash
node scripts\listUsers.js
```

---

## Part 7: Security Best Practices

### 1. Protect Service Account Key
Add to `.gitignore`:
```
# Firebase service account keys
scripts/serviceAccountKey.json
**/serviceAccountKey*.json
```

### 2. Strong Passwords for Staff
- Minimum 12 characters
- Mix of uppercase, lowercase, numbers, symbols
- No dictionary words
- Use a password manager

### 3. Regular Audits
Periodically review:
- Who has staff access
- Recent login activity
- Status change logs

### 4. Limit Staff Accounts
- Only give staff access to trusted individuals
- Remove access when no longer needed
- Don't share staff credentials

### 5. Monitor Firestore Usage
Firebase Console → Firestore → Usage tab
- Watch for unusual activity
- Set up billing alerts (even on free tier)

---

## Part 8: Troubleshooting

### Issue 1: "This account is not authorized as staff"

**Symptoms**: Staff login fails, shows "not authorized" message

**Causes**:
- Custom claim not set
- User hasn't refreshed token (needs to sign out/in)
- Wrong email used

**Solutions**:
1. Verify claim is set:
   ```bash
   node scripts\verifyUser.js staff@example.com
   ```
2. If no claim, set it:
   ```bash
   node scripts\setStaffClaim.js staff@example.com true
   ```
3. User must sign out and sign back in
4. Clear app data if still not working

### Issue 2: "Permission denied" when updating post status

**Symptoms**: Status update fails with permission error

**Causes**:
- Firestore rules not published
- Staff claim not in token
- Network/connectivity issue

**Solutions**:
1. Verify rules in Firebase Console → Firestore → Rules
2. Check rules match the ones in Part 3
3. Click "Publish" if rules were edited
4. User signs out and back in to refresh token
5. Check internet connection

### Issue 3: Cannot find serviceAccountKey.json

**Symptoms**: Script fails with "Cannot find module" error

**Causes**:
- File not downloaded
- File in wrong location
- File renamed incorrectly

**Solutions**:
1. Download from Firebase Console → Settings → Service Accounts
2. Rename to exactly `serviceAccountKey.json`
3. Place in `D:\LocalConnect\scripts\` folder
4. Verify with: `dir D:\LocalConnect\scripts\`

### Issue 4: Posts not loading in Staff Dashboard

**Symptoms**: Dashboard shows "No posts found" but posts exist

**Causes**:
- Firestore rules blocking read
- No posts exist yet
- Query error

**Solutions**:
1. Check Firestore rules allow `allow read: if isAuthenticated()`
2. Verify posts exist in Firebase Console → Firestore
3. Check app logs for errors
4. Try creating a test post as regular user first

### Issue 5: Node.js command not found

**Symptoms**: `node` or `npm` commands don't work in terminal

**Causes**:
- Node.js not installed
- Not in system PATH

**Solutions**:
1. Download and install Node.js from https://nodejs.org/
2. Restart terminal/command prompt
3. Verify: `node --version` and `npm --version`

---

## Part 9: Advanced Configuration (Optional)

### Multiple Staff Roles

You can create different staff roles with different permissions:

```javascript
// Set different claims
node scripts\setStaffClaim.js moderator@example.com true
node scripts\setAdminClaim.js admin@example.com true
```

Create `scripts/setAdminClaim.js` (similar to setStaffClaim.js but sets `admin: true`).

Then in Firestore rules, differentiate:
```javascript
function isAdmin() {
  return request.auth != null && request.auth.token.admin == true;
}

function isModerator() {
  return request.auth != null && request.auth.token.moderator == true;
}

function isStaffOrHigher() {
  return request.auth != null && 
         (request.auth.token.staff == true || 
          request.auth.token.moderator == true || 
          request.auth.token.admin == true);
}
```

### Audit Logging

To track who changed what, add to your StaffRepository:

```kotlin
// After successful status update
firestore.collection("audit_logs").add(
    hashMapOf(
        "action" to "status_update",
        "postId" to postId,
        "oldStatus" to oldStatus,
        "newStatus" to newStatus,
        "staffUid" to auth.currentUser?.uid,
        "staffEmail" to auth.currentUser?.email,
        "timestamp" to System.currentTimeMillis()
    )
)
```

Add to Firestore rules:
```javascript
match /audit_logs/{logId} {
  allow read: if isStaff();
  allow write: if false; // Only write from code, not directly
}
```

---

## Summary

You've now set up a complete staff authentication system **without Cloud Functions**:

✅ **What's Working:**
1. Firebase Authentication with custom claims
2. Manual staff assignment via admin scripts
3. Firestore security rules for access control
4. Android app with staff login portal
5. Staff dashboard with post management
6. Status update functionality with server-side validation
7. Complete security - all enforced by Firebase Auth tokens and rules

✅ **What Staff Can Do:**
- Log in through dedicated staff portal
- View all user-reported issues and posts
- Filter by status (Open, In Progress, Resolved, Closed)
- Filter by type (Issues, Events, Posts)
- Update issue/post status
- All actions secured server-side

✅ **What's Secured:**
- Regular users cannot access staff features
- Status updates require `staff: true` custom claim
- All checks happen server-side (Firestore rules)
- Cannot be bypassed by client-side manipulation

✅ **Free Tier Friendly:**
- No Cloud Functions = $0/month
- Only uses Firestore reads/writes (generous free quota)
- Manual staff management via local scripts

---

## Quick Reference Commands

```bash
# Navigate to project
cd D:\LocalConnect

# Set staff claim
node scripts\setStaffClaim.js user@example.com true

# Remove staff claim
node scripts\setStaffClaim.js user@example.com false

# Verify user claims
node scripts\verifyUser.js user@example.com

# List all users (if script created)
node scripts\listUsers.js
```

---

## Next Steps

1. **Create your first staff account** (Part 4)
2. **Test the system** (Part 5)
3. **Add more staff as needed** (Part 6)
4. **Monitor usage** in Firebase Console
5. **Enjoy your staff management system!** 🎉

For questions or issues, refer to the Troubleshooting section (Part 8).
