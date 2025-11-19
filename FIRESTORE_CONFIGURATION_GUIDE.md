# Firestore Configuration Guide

## 📋 Overview

Your app now has two important Firestore configuration files:

1. **firestore.indexes.json** - Defines database indexes for query optimization
2. **firestore.rules** - Defines security rules and access control

---

## 🗂️ firestore.indexes.json Explained

### What It Does
Defines **composite indexes** that allow Firestore to efficiently execute complex queries (filtering + sorting).

### Why You Need It
Without proper indexes:
- ❌ Queries with multiple conditions will **fail**
- ❌ Error: "The query requires an index"
- ❌ Category filtering won't work
- ❌ Sorting by likes/views will fail

### Current Indexes in Your File

#### 1. Category + Timestamp
```json
{
  "fields": [
    {"fieldPath": "category", "order": "ASCENDING"},
    {"fieldPath": "timestamp", "order": "DESCENDING"}
  ]
}
```
**Used for**: Filtering posts by category and showing recent posts first
**Example query**: Health posts sorted by newest first

#### 2. Category + Likes
```json
{
  "fields": [
    {"fieldPath": "category", "order": "ASCENDING"},
    {"fieldPath": "likes", "order": "DESCENDING"}
  ]
}
```
**Used for**: "Most Voted" sorting within a category
**Example query**: Most liked Health posts

#### 3. Category + Views
```json
{
  "fields": [
    {"fieldPath": "category", "order": "ASCENDING"},
    {"fieldPath": "views", "order": "DESCENDING"}
  ]
}
```
**Used for**: "Most Viewed" sorting within a category
**Example query**: Most viewed Emergency posts

#### 4. Category + Priority
```json
{
  "fields": [
    {"fieldPath": "category", "order": "ASCENDING"},
    {"fieldPath": "priority", "order": "DESCENDING"}
  ]
}
```
**Used for**: Priority sorting for issues
**Example query**: Highest priority Infrastructure issues

#### 5. LocalOnly + Timestamp
```json
{
  "fields": [
    {"fieldPath": "localOnly", "order": "ASCENDING"},
    {"fieldPath": "timestamp", "order": "DESCENDING"}
  ]
}
```
**Used for**: Fetching only local community posts
**Example query**: Recent posts marked as local-only

#### 6. UserId + Timestamp
```json
{
  "fields": [
    {"fieldPath": "userId", "order": "ASCENDING"},
    {"fieldPath": "timestamp", "order": "DESCENDING"}
  ]
}
```
**Used for**: User profile page (showing their posts)
**Example query**: All posts by specific user, newest first

#### 7. Type + Timestamp
```json
{
  "fields": [
    {"fieldPath": "type", "order": "ASCENDING"},
    {"fieldPath": "timestamp", "order": "DESCENDING"}
  ]
}
```
**Used for**: Filtering by post type (ISSUE, CELEBRATION, etc.)
**Example query**: All ISSUE posts sorted by newest

---

## 🚀 How to Deploy Indexes

### Method 1: Firebase CLI (Recommended - Automatic)

```cmd
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase in your project (if not done)
cd "C:\Users\Ullas N\AndroidStudioProjects\Local_Connect"
firebase init firestore

# Deploy indexes
firebase deploy --only firestore:indexes
```

**Output you'll see:**
```
=== Deploying to 'your-project-id'...

i  firestore: deploying indexes
✔  firestore: indexes deployed successfully

✔  Deploy complete!
```

**Time to build**: 5-10 minutes (Firebase builds indexes in background)

---

### Method 2: Firebase Console (Manual)

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Click **Firestore Database** in left menu
4. Click **Indexes** tab
5. Click **Add Index**

**Create each index:**

**Index 1: Category + Timestamp**
- Collection ID: `posts`
- Fields:
  - `category` → Ascending
  - `timestamp` → Descending
- Query scope: Collection
- Click **Create**

**Repeat for all 7 indexes** listed above.

**⏱️ Build time**: 5-10 minutes per index

---

## 🔒 firestore.rules Explained

### What It Does
Controls **who can read/write** your Firestore data. Security rules prevent unauthorized access.

### What I Updated

#### Before (Less Secure)
```javascript
allow read: if true;  // Anyone can read unlimited posts
```

#### After (Optimized & Secure)
```javascript
// Separate list and get permissions
allow list: if true && request.query.limit <= 100;  // Max 100 posts per query
allow get: if true;  // Single document reads always allowed
```

### Key Improvements

#### 1. Query Limit Protection
```javascript
request.query.limit <= 100
```
**Prevents abuse**: Nobody can query 10,000 posts at once and overload your database

#### 2. Required Field Validation
```javascript
allow create: if isSignedIn()
  && request.resource.data.timestamp is number
  && request.resource.data.latitude is number
  && request.resource.data.longitude is number
  && request.resource.data.locationName is string;
```
**Ensures**: All posts have required fields for pagination and location filtering

#### 3. Immutable Field Protection
```javascript
allow update: if isSignedIn()
  && request.resource.data.userId == resource.data.userId
  && request.resource.data.postId == resource.data.postId;
```
**Prevents**: Users from changing post ownership or ID after creation

---

## 🎯 How Pagination Uses These Files

### Scenario: User Opens "Health" Category

#### Step 1: Query Construction
```kotlin
// In PostsPagingSource.kt
query = firestore.collection("posts")
    .whereEqualTo("category", "Health")  // Filter
    .orderBy("timestamp", Query.Direction.DESCENDING)  // Sort
    .limit(20)  // Pagination
```

#### Step 2: Firestore Checks Rules
```javascript
// firestore.rules validates:
1. Is limit <= 100? ✅ (limit is 20)
2. User authenticated? ✅ (optional for reads)
3. Query allowed? ✅
```

#### Step 3: Firestore Uses Index
```json
// firestore.indexes.json provides:
{
  "fields": [
    {"fieldPath": "category", "order": "ASCENDING"},
    {"fieldPath": "timestamp", "order": "DESCENDING"}
  ]
}
```

#### Step 4: Efficient Query Execution
- ⚡ Uses index → Super fast (milliseconds)
- 📊 Returns 20 posts
- 💰 Charges for 20 reads only

#### Without Index:
- ❌ Query fails with error
- ❌ Or scans entire collection (slow + expensive)

---

## 🔍 Testing Your Configuration

### Test 1: Verify Rules Deployment

```cmd
firebase deploy --only firestore:rules
```

**Expected output:**
```
✔  firestore: rules deployed successfully
```

### Test 2: Check Index Status

**Firebase Console Method:**
1. Go to Firestore → Indexes
2. Look for status:
   - 🟢 **Green checkmark** = Index ready
   - 🟡 **Building** = Wait 5-10 minutes
   - 🔴 **Error** = Check configuration

**CLI Method:**
```cmd
firebase firestore:indexes
```

### Test 3: Query in App

Run your app and:
1. Open HomeScreen
2. Select a category (e.g., "Health")
3. Try different sort options

**Success indicators:**
- ✅ Posts load instantly
- ✅ No "index required" errors in Logcat
- ✅ Smooth scrolling with pagination

**Error indicators:**
- ❌ "The query requires an index" in Logcat
- ❌ Click the link in error to auto-create index in console
- ❌ Wait 5-10 minutes for index to build

---

## 📊 Performance Impact

### With Proper Indexes

| Query Type | Time | Reads | Cost |
|------------|------|-------|------|
| Get 20 posts | 50-100ms | 20 | $0.0006 |
| Filter by category | 50-150ms | 20 | $0.0006 |
| Sort by likes | 50-150ms | 20 | $0.0006 |

### Without Indexes

| Query Type | Time | Reads | Cost |
|------------|------|-------|------|
| Get 20 posts | 50-100ms | 20 | $0.0006 |
| Filter by category | ❌ FAILS | ❌ | ❌ |
| Sort by likes | ❌ FAILS | ❌ | ❌ |

---

## 🛠️ Maintenance

### When to Update Indexes

Add new indexes when you add:
- New filter fields
- New sort fields
- Combined filters + sorts

**Example**: If you add "upvotes" field:
```json
{
  "fields": [
    {"fieldPath": "category", "order": "ASCENDING"},
    {"fieldPath": "upvotes", "order": "DESCENDING"}
  ]
}
```

### When to Update Rules

Update rules when you:
- Add new collections
- Change security requirements
- Add new user roles
- Need stricter validation

---

## 🚨 Common Issues & Solutions

### Issue 1: "The query requires an index"

**Cause**: Missing composite index
**Solution**: 
1. Click the link in the error message (auto-creates index)
2. OR manually add to firestore.indexes.json and deploy
3. Wait 5-10 minutes for build

### Issue 2: "Permission denied"

**Cause**: Firestore rules blocking access
**Solution**:
1. Check firestore.rules
2. Ensure `allow list: if true` for posts
3. Deploy rules: `firebase deploy --only firestore:rules`

### Issue 3: Slow queries

**Cause**: No index or inefficient query
**Solution**:
1. Check Firebase Console → Firestore → Usage
2. Look for "Collection Scans" (bad)
3. Add missing indexes
4. Reduce query complexity

### Issue 4: Index build failed

**Cause**: Invalid field name or configuration
**Solution**:
1. Check field names match your Post model exactly
2. Verify JSON syntax
3. Redeploy with corrections

---

## 📝 Deployment Checklist

Before launching your app:

- [ ] Deploy Firestore indexes: `firebase deploy --only firestore:indexes`
- [ ] Deploy Firestore rules: `firebase deploy --only firestore:rules`
- [ ] Wait 10 minutes for all indexes to build
- [ ] Test all filter + sort combinations
- [ ] Check Firebase Console → Firestore → Indexes (all green)
- [ ] Test with 100+ posts in database
- [ ] Verify pagination works smoothly
- [ ] Check Logcat for no index errors

---

## 🎓 Best Practices

### 1. Always Deploy Indexes First
Deploy indexes BEFORE releasing new app versions that use them.

### 2. Monitor Index Usage
Firebase Console → Firestore → Usage tab
- Check "Index Entries" size
- Delete unused indexes to save costs

### 3. Test Rules Thoroughly
Use Firebase Emulator Suite for local testing:
```cmd
firebase emulators:start --only firestore
```

### 4. Keep Rules Updated
Review rules quarterly for security improvements

### 5. Document Custom Indexes
Add comments to firestore.indexes.json:
```json
// For user profile page
{
  "collectionGroup": "posts",
  "fields": [...]
}
```

---

## 🔗 Quick Commands Reference

```cmd
# Deploy indexes only
firebase deploy --only firestore:indexes

# Deploy rules only
firebase deploy --only firestore:rules

# Deploy both
firebase deploy --only firestore

# List all indexes
firebase firestore:indexes

# Delete an index
firebase firestore:indexes:delete <index-id>

# Test rules locally
firebase emulators:start --only firestore
```

---

## ✅ Summary

### firestore.indexes.json
- ✅ **Purpose**: Optimize complex queries
- ✅ **Required**: For pagination with filters
- ✅ **Deploy**: `firebase deploy --only firestore:indexes`
- ✅ **Build time**: 5-10 minutes per index

### firestore.rules
- ✅ **Purpose**: Security and access control
- ✅ **Updated**: Added pagination optimizations
- ✅ **Deploy**: `firebase deploy --only firestore:rules`
- ✅ **Takes effect**: Immediately

### Your Action Items
1. ✅ Install Firebase CLI
2. ✅ Login: `firebase login`
3. ✅ Deploy indexes: `firebase deploy --only firestore:indexes`
4. ✅ Deploy rules: `firebase deploy --only firestore:rules`
5. ✅ Wait 10 minutes for indexes to build
6. ✅ Test your app

**Both files are critical for your app to work correctly with the new pagination system!**

