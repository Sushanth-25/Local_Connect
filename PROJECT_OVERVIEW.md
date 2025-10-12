# Local Connect Project Documentation

## Project Overview

Local Connect is an Android application designed to connect users locally and globally through posts. Posts marked as `localOnly` are visible to nearby users in the Community tab and to all users in the Explore tab. The app uses Firebase Firestore for data storage and Firebase Storage for image uploads.

---

## Directory and File Structure

### Root Directory
- **build.gradle.kts**: Project-level Gradle build configuration.
- **settings.gradle.kts**: Specifies included modules and project settings.
- **gradle.properties**: Global Gradle properties (e.g., JVM args, cache settings).
- **gradlew / gradlew.bat**: Gradle wrapper scripts for Unix/Windows.
- **local.properties**: Local environment settings (SDK paths, not versioned).
- **README.md**: Basic project info and setup instructions.
- **hs_err_pid*.log**: JVM crash logs (if any).

### app/
- **build.gradle.kts**: App module build configuration (dependencies, plugins).
- **google-services.json**: Firebase configuration (API keys, project info).
- **proguard-rules.pro**: ProGuard rules for code shrinking/obfuscation.
- **src/**: Main source code and resources.
  - **main/**: Production code and resources.
    - **AndroidManifest.xml**: Declares app components, permissions, and metadata.
    - **logo_launcher-playstore.png**: App icon for Play Store.
    - **java/**: Contains Kotlin/Java source code.
    - **res/**: App resources (layouts, images, strings, etc.).
  - **androidTest/**: Instrumented tests (run on device/emulator).
  - **test/**: Unit tests (run on JVM).
- **build/**: Generated build outputs and intermediates.

### gradle/
- **libs.versions.toml**: Centralized dependency version management.
- **wrapper/**: Gradle wrapper files.

---

## Key Source Files (by convention)

### Domain Layer
- **domain/repository/StorageRepository.kt**: Interface for uploading images to Firebase Storage. Defines the contract for image upload functionality.
- **domain/repository/PostRepository.kt**: (Assumed) Interface for CRUD operations on posts, including Firestore interactions.

### Data Layer
- **data/remote/FirebaseService.kt**: (Assumed) Handles Firebase initialization and provides Firestore/Storage instances.

### Presentation Layer
- **presentation/CreatePostScreen.kt**: (Assumed) UI for creating posts. Sets the `localOnly` attribute when a post is created.
- **presentation/CommunityTab.kt**: (Assumed) UI for displaying posts visible to nearby users (`localOnly == true`).
- **presentation/ExploreTab.kt**: (Assumed) UI for displaying all posts, including those with `localOnly == true`.

### Model Layer
- **model/Post.kt**: (Assumed) Data class representing a post, including attributes like `localOnly`, content, author, timestamp, etc.

---

## Firestore Data Fetching & Connection

### Firebase Initialization
Firebase is initialized using `google-services.json` and the Firebase SDK. Typically, in your app's `Application` class or main activity:

```kotlin
FirebaseApp.initializeApp(context)
```

### Establishing Firestore Connection
Get a Firestore instance:

```kotlin
val firestore = FirebaseFirestore.getInstance()
```

### Fetching Data
To fetch posts from Firestore:

```kotlin
firestore.collection("posts")
    .get()
    .addOnSuccessListener { result ->
        // Iterate over result.documents to get post data
    }
    .addOnFailureListener { exception ->
        // Handle errors
    }
```

### Filtering for Tabs
- **Community Tab**: Filter posts where `localOnly == true` and location is nearby.
- **Explore Tab**: Show all posts, including those with `localOnly == true`.

### Uploading Images
Handled by `StorageRepository`. Images are uploaded to Firebase Storage, and the download URL is saved in Firestore with the post data.

---

## How to Understand the Project

1. **Start with the UI files** (`CreatePostScreen.kt`, `CommunityTab.kt`, `ExploreTab.kt`) to see how data is displayed and interacted with.
2. **Check repository classes** (`PostRepository.kt`, `StorageRepository.kt`) for data operations.
3. **Look at Firebase service classes** for initialization and connection logic.
4. **Review the model classes** (e.g., `Post.kt`) to understand the data structure.

---

## Additional Notes
- All Firebase operations are asynchronous.
- Permissions for location and internet are required in `AndroidManifest.xml`.
- The `localOnly` attribute controls post visibility.

---


