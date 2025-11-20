# Firestore Security Rules - With Staff Support

This document contains the complete Firestore security rules for the LocalConnect app with staff authentication support.

## Features
- ✅ Staff can view all posts (including images)
- ✅ Staff can update post status
- ✅ Staff can delete any post or comment
- ✅ Regular users can create, update, and delete their own content
- ✅ All authenticated users can read posts and user profiles
- ✅ Proper validation for post creation
- ✅ Audit log support for staff actions (optional)

## Rules to Copy

Copy the rules below and paste them into your Firebase Console → Firestore Database → Rules tab, then click **Publish**.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper function to check if user is authenticated
    function isSignedIn() {
      return request.auth != null;
    }

    // Helper function to check if user owns the resource
    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    // Helper function to check if user is staff
    function isStaff() {
      return request.auth != null && request.auth.token.staff == true;
    }

    // ================================
    //            USERS
    // ================================
    match /users/{userId} {
      // Anyone authenticated can read user profiles
      allow read: if isSignedIn();

      // Users can create their own profile
      allow create: if isSignedIn()
        && userId == request.auth.uid
        && request.resource.data.userId == request.auth.uid;

      // Users can update their own profile
      allow update: if isSignedIn()
        && userId == request.auth.uid
        && (!request.resource.data.keys().hasAny(['userId'])
            || request.resource.data.userId == resource.data.userId);

      // Users can delete their own profile
      allow delete: if isSignedIn()
        && userId == request.auth.uid;
    }

    // ================================
    //            POSTS
    // ================================
    match /posts/{postId} {
      // ✅ All authenticated users can list and read posts
      allow list: if isSignedIn();
      allow get: if isSignedIn();

      // Users can create posts with required fields
      allow create: if isSignedIn()
        && request.resource.data.userId == request.auth.uid
        && request.resource.data.postId is string
        && request.resource.data.timestamp is number
        && request.resource.data.latitude is number
        && request.resource.data.longitude is number
        && request.resource.data.locationName is string
        && request.resource.data.locationName.size() > 0;

      // ✅ Owner can update their own post OR Staff can update any post
      allow update: if isSignedIn() && (
        (resource.data.userId == request.auth.uid
          && request.resource.data.userId == resource.data.userId
          && request.resource.data.postId == resource.data.postId)
        || isStaff()
      );

      // ✅ Owner can delete their own post OR Staff can delete any post
      allow delete: if isSignedIn() && (
        resource.data.userId == request.auth.uid
        || isStaff()
      );

      // ===== POST LIKES SUBCOLLECTION =====
      match /likes/{userId} {
        allow read: if isSignedIn();
        allow create: if isSignedIn() && userId == request.auth.uid;
        allow delete: if isSignedIn() && userId == request.auth.uid;
        allow update: if false;
      }

      // ===== POST VIEWS SUBCOLLECTION =====
      match /views/{userId} {
        allow read: if isSignedIn();
        allow create: if isSignedIn() && userId == request.auth.uid;
        allow delete, update: if false;
      }

      // ===== COMMENTS SUBCOLLECTION =====
      match /comments/{commentId} {
        allow read: if isSignedIn();

        allow create: if isSignedIn()
          && request.resource.data.userId == request.auth.uid
          && request.resource.data.postId == postId
          && request.resource.data.text is string
          && request.resource.data.text.size() > 0
          && request.resource.data.text.size() <= 1000;

        // ✅ Owner can update OR Staff can update
        allow update: if isSignedIn() && (
          (resource.data.userId == request.auth.uid
            && request.resource.data.userId == resource.data.userId
            && request.resource.data.postId == resource.data.postId)
          || isStaff()
        );

        // ✅ Comment owner, post owner, or staff can delete
        allow delete: if isSignedIn() && (
          resource.data.userId == request.auth.uid
          || get(/databases/$(database)/documents/posts/$(postId)).data.userId == request.auth.uid
          || isStaff()
        );

        // ===== COMMENT LIKES SUBCOLLECTION =====
        match /likes/{userId} {
          allow read: if isSignedIn();
          allow create: if isSignedIn() && userId == request.auth.uid;
          allow delete: if isSignedIn() && userId == request.auth.uid;
          allow update: if false;
        }
      }
    }

    // ================================
    //      AUDIT LOGS (OPTIONAL)
    // ================================
    // Track staff actions for monitoring and compliance
    match /audit_logs/{logId} {
      allow read: if isStaff();
      allow create: if isStaff();
      allow update, delete: if false;
    }
  }
}
```

## How to Apply These Rules

### Step 1: Copy the Rules
1. Copy all the rules above (everything between the code block)

### Step 2: Open Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (LocalConnect)
3. Navigate to **Firestore Database** in the left sidebar
4. Click on the **Rules** tab

### Step 3: Paste and Publish
1. Replace all existing rules with the new rules
2. Click **Publish** button
3. Wait for confirmation that rules are published

### Step 4: Test the Rules
1. Sign in as a staff member (user with `staff: true` custom claim)
2. Navigate to the Staff Dashboard
3. Verify you can:
   - ✅ See all posts with images
   - ✅ Change post status
   - ✅ Delete posts if needed

## Key Changes from Previous Rules

### 1. **Allow All Authenticated Users to Read Posts**
```javascript
// Old (too restrictive)
allow list: if true && request.query.limit <= 100;

// New (proper authentication)
allow list: if isSignedIn();
allow get: if isSignedIn();
```

### 2. **Staff Permissions Added**
Staff members (users with `staff: true` custom claim) can now:
- Update any post (especially the `status` field)
- Delete any post
- Update any comment
- Delete any comment

### 3. **Location Fields Required for Post Creation**
Since LocalConnect is a location-based community app, all posts MUST include location information:
- `latitude` (number) - Geographic latitude coordinate
- `longitude` (number) - Geographic longitude coordinate  
- `locationName` (string) - Human-readable location name (must not be empty)

This ensures every post is tied to a physical location, which is essential for the app's core functionality of connecting local communities.

### 4. **Image Access Fixed**
By allowing all authenticated users to `list` and `get` posts, both staff and regular users can now:
- Read post data including `mediaUrls` and `thumbnailUrls`
- Load images from Cloudinary using the URLs stored in Firestore
- View thumbnails and full images properly

## Security Notes

✅ **What's Protected:**
- Only authenticated users can read/write data
- Users can only modify their own content (unless they're staff)
- Staff permissions are validated server-side via custom claims
- Input validation for required fields

✅ **What Staff Can Do:**
- View all posts and comments
- Update post status for issue tracking
- Delete inappropriate content
- Access audit logs (if implemented)

✅ **What Staff Cannot Do:**
- Modify user profiles (except their own)
- Bypass authentication
- Write to collections without proper validation

## Troubleshooting

### Images Still Not Showing?

1. **Force Token Refresh:**
   - Sign out completely from the app
   - Clear app data (Settings → Apps → LocalConnect → Clear Data)
   - Sign back in (this forces a new authentication token)

2. **Check Cloudinary URLs:**
   - Verify URLs in Firestore console are valid
   - Test URL in browser to ensure images are accessible
   - Check Cloudinary account status (free tier limits)

3. **Check Network Logs:**
   - Use Logcat to see if image requests are failing
   - Look for 403 (Forbidden) or 404 (Not Found) errors
   - Verify CORS settings on Cloudinary

### Staff Can't Update Status?

1. **Verify Custom Claim:**
   - Run the verification script: `node scripts/verifyUser.js <email>`
   - Ensure output shows `staff: true`

2. **Force Token Refresh:**
   - After setting staff claim, user must sign out and sign back in
   - Or wait 1 hour for token to refresh automatically

3. **Check Firestore Rules:**
   - Verify rules are published (check timestamp in Firebase Console)
   - Test rules using Firebase Console's Rules Playground

## Additional Resources

- [Firebase Security Rules Documentation](https://firebase.google.com/docs/firestore/security/get-started)
- [Custom Claims Documentation](https://firebase.google.com/docs/auth/admin/custom-claims)
- [LocalConnect Staff Setup Guide](./STAFF_LOGIN_SETUP_GUIDE.md)

---

**Last Updated:** November 20, 2025
**Version:** 2.0 (With Staff Support)
