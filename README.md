# Local Connect - Unified Project Guide

This is the single source of truth for architecture, setup, Firebase configuration, staff management, running, and release.

## Index

1. [Project Summary](#project-summary)
2. [Tech Stack](#tech-stack)
3. [Feature Set](#feature-set)
4. [Repository Structure](#repository-structure)
5. [Architecture](#architecture)
6. [Firestore Data Model](#firestore-data-model)
7. [Execution Order (End-to-End)](#execution-order-end-to-end)
8. [Environment Setup](#environment-setup)
9. [Firebase Setup and Deployment](#firebase-setup-and-deployment)
10. [Staff Authentication Setup](#staff-authentication-setup)
11. [Build and Run](#build-and-run)
12. [Release Process](#release-process)
13. [Security Checklist](#security-checklist)
14. [Troubleshooting](#troubleshooting)

## Project Summary

Local Connect is a Kotlin + Jetpack Compose Android application for local community posting and issue reporting. It uses Firebase for authentication, Firestore data storage, Firebase Storage/media links, messaging, and analytics. The app includes user and staff flows.

## Tech Stack

- Kotlin, Jetpack Compose
- Android SDK: compile 36, min 28
- Firebase: Auth, Firestore, Storage, Messaging, Analytics
- Paging 3
- OSMDroid
- Gradle (Kotlin DSL), Gradle Wrapper (enforced: 8.13)

## Feature Set

- User signup/login/profile
- Home feed + post detail + my posts
- Post creation with media
- Comments and replies
- Aggregate counters (likes, comments, views, upvotes)
- Notifications and settings
- Staff login and moderation dashboard

## Repository Structure

```text
Local_Connect/
	app/
		src/main/java/com/example/localconnect/
			data/
			presentation/
			service/
			util/
	scripts/
		setStaffClaim.js
		verifyUser.js
		listUsers.js
		seedPosts.js
	firestore.rules
	firestore.indexes.json
	gradle/
	build.gradle.kts
	settings.gradle.kts
	gradlew
	gradlew.bat
```

## Architecture

The app follows a layered flow:

1. UI Layer (Compose screens)
2. ViewModel Layer (state + orchestration)
3. Repository Layer (Firestore/Auth/Storage operations)
4. Firestore (posts, comments, counters, subcollections)

### Core Data Flow

- Add comment:
	- UI action -> ViewModel -> CommentRepository transaction
	- Writes new comment document
	- Increments post-level comments counter atomically
	- Real-time listener updates UI
- Like post:
	- UI action -> ViewModel -> PostRepository transaction
	- Increments likes counter
	- Updates timestamp/stats

### Why aggregate counters

Counters stored on the post document provide fast $O(1)$ reads for likes/comments/views instead of scanning entire subcollections.

## Firestore Data Model

```text
posts/{postId}
	likes: number
	comments: number
	views: number
	upvotes: number
	userId: string
	timestamp: number
	...

posts/{postId}/comments/{commentId}
	commentId: string
	postId: string
	userId: string
	text: string
	timestamp: number
	likes: number
	parentCommentId: string|null
```

Subcollections are used for scalable comment storage and pagination.

## Execution Order (End-to-End)

Follow this exact order for first-time project setup:

1. Configure local Android environment.
2. Add Firebase app config (`app/google-services.json`).
3. Deploy Firestore rules.
4. Deploy Firestore indexes.
5. (Optional) Configure staff claims via scripts.
6. Build and run debug app.
7. Create signing setup for release.
8. Build release APK/AAB.

## Environment Setup

### Prerequisites

- Android Studio (latest stable)
- JDK 11
- Android SDK for compile SDK 36
- Firebase project
- Node.js (only for admin scripts)

### Local setup

1. Clone repository.
2. Ensure `local.properties` points to SDK path.
3. Place Firebase config in `app/google-services.json`.

## Firebase Setup and Deployment

### Firestore indexes

This repository includes required composite indexes in `firestore.indexes.json` for category/type/localOnly/user queries with sorting.

Deploy:

```bash
firebase login
firebase deploy --only firestore:indexes
```

### Firestore rules

Rules are in `firestore.rules` with authenticated access model and ownership/staff checks.

Deploy:

```bash
firebase deploy --only firestore:rules
```

### Verify deployment

1. Open Firebase Console -> Firestore -> Indexes.
2. Confirm indexes are built.
3. Run app category/sort queries and verify no index errors.

## Staff Authentication Setup

Staff access is based on Firebase custom claim `staff: true`.

### Prepare scripts

1. Put Firebase Admin key at `scripts/serviceAccountKey.json`.
2. Install dependency:

```bash
npm install firebase-admin
```

### Create or promote staff user

1. Create user in Firebase Auth (email/password).
2. Grant claim:

```bash
node scripts/setStaffClaim.js staff@yourcompany.com true
```

3. Verify claim:

```bash
node scripts/verifyUser.js staff@yourcompany.com
```

4. Ask staff user to sign out and sign in again to refresh token.

### Revoke staff access

```bash
node scripts/setStaffClaim.js staff@yourcompany.com false
```

## Build and Run

Always use Gradle wrapper. Gradle version `8.13` is enforced.

### Windows

```powershell
.\gradlew.bat clean build
.\gradlew.bat installDebug
```

### macOS/Linux

```bash
./gradlew clean build
./gradlew installDebug
```

## Release Process

### 1. Generate keystore (one time)

Option A: run helper script

```cmd
generate-keystore.bat
```

Option B: keytool manually

```cmd
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkey -v -keystore localconnect-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias localconnect-key
```

### 2. Create `keystore.properties`

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=localconnect-key
storeFile=localconnect-release.jks
```

### 3. Build release artifacts

```powershell
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

Outputs:

- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## Security Checklist

Never commit:

- `app/google-services.json`
- `scripts/serviceAccountKey.json`
- `keystore.properties`
- `*.jks`, `*.keystore`
- build/cache/log artifacts

Current `.gitignore` is configured to exclude these.

## Troubleshooting

### Gradle version failure

If build fails with "Expected Gradle version 8.13", use only `gradlew` or `gradlew.bat`.

### Staff claim not working

1. Verify claim with `verifyUser.js`.
2. Force re-login to refresh token.
3. Re-check deployed Firestore rules.

### Firestore query/index errors

1. Deploy indexes again.
2. Wait for index build completion in Firebase Console.
3. Re-run failing query.

### Images not visible

1. Validate media URLs in Firestore.
2. Check network/logcat for 403/404.
3. Verify access and authentication state.
