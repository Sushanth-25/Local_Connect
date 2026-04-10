# Local Connect (Android)

Local Connect is a Jetpack Compose Android app for community posting and local issue reporting, with Firebase-backed auth, feed, media uploads, notifications, and staff moderation.

## Tech Stack

- Kotlin + Jetpack Compose
- Android SDK 36 (min SDK 28)
- Firebase: Auth, Firestore, Storage, Analytics, Messaging
- Paging 3 for feed pagination
- OSMDroid for map/location UI
- Gradle Kotlin DSL with Version Catalog

## Key Features

- User authentication and profile management
- Post creation with media upload support
- Home feed, post details, and user posts
- Comments and aggregate counters (likes/comments/views)
- Notifications and notification settings
- Location-aware/community-style flows
- Staff login and staff dashboard for moderation workflows

## Project Structure

```text
Local_Connect/
	app/                          # Android app module
		src/main/java/com/example/localconnect/
			data/                     # Models, repositories, paging
			presentation/             # Compose screens and view models
			service/                  # Firebase messaging service
			util/                     # App utilities (location, permissions, etc.)
	scripts/                      # Firebase Admin utility scripts
	firestore.rules               # Firestore security rules
	firestore.indexes.json        # Firestore index definitions
	gradle/                       # Gradle wrapper and version catalog
```

## Prerequisites

- Android Studio (latest stable recommended)
- JDK 11
- Firebase project configured for this app
- Android SDK for compile SDK 36

## Setup

1. Clone the repository.
2. Add Firebase config file at `app/google-services.json`.
3. Ensure `local.properties` points to your Android SDK.
4. Sync Gradle and build.

## Build and Run

Use the Gradle wrapper only.

- Windows:

```powershell
.\gradlew.bat clean build
.\gradlew.bat installDebug
```

- macOS/Linux:

```bash
./gradlew clean build
./gradlew installDebug
```

## Gradle Version Enforcement

This repository enforces Gradle `8.13` in `settings.gradle.kts`. If you use a local `gradle` binary with a different version, the build fails intentionally. Always use `gradlew` / `gradlew.bat`.

## Firebase Configuration

- Firestore rules: `firestore.rules`
- Firestore indexes: `firestore.indexes.json`

Useful guides in this repository:

- `FIRESTORE_CONFIGURATION_GUIDE.md`
- `FIRESTORE_RULES_WITH_STAFF.md`
- `STAFF_LOGIN_SETUP_GUIDE.md`

## Staff/Admin Scripts

The `scripts/` folder contains Firebase Admin helper scripts for staff claim management and seed/testing workflows.

Typical setup:

1. Place service account key at `scripts/serviceAccountKey.json` (do not commit).
2. Install dependencies in the project root:

```bash
npm install firebase-admin
```

## Release Build

For signed release APK/AAB setup, see `QUICK_RELEASE_STEPS.md`.

Quick commands:

```powershell
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

## Security and Secrets

Do not commit:

- `app/google-services.json`
- `scripts/serviceAccountKey.json`
- keystore files and `keystore.properties`
- local IDE/cache/build artifacts

The `.gitignore` already covers these.

## Troubleshooting

If you hit:

```text
Expected Gradle version 8.13 but found X.X
```

run builds using `gradlew`/`gradlew.bat` and avoid the system `gradle` command.
