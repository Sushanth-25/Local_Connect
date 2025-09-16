# Local Connect

## Gradle Version Enforcement

This project uses Gradle 8.13. To ensure all team members use the same build environment, we have configured the project to:

1. Lock the Gradle wrapper to version 8.13
2. Enforce version checking in the build scripts

## Important Guidelines for All Team Members

### ✅ Always use the Gradle wrapper
To ensure consistent builds across all development environments, **always** use the Gradle wrapper instead of your locally installed Gradle:

- On Windows: `.\gradlew.bat <task>`
- On Mac/Linux: `./gradlew <task>`

### ❌ Never use the `gradle` command directly
Using your local Gradle installation may cause build inconsistencies between team members.

## Common Gradle Tasks

- Build the project: `./gradlew build`
- Run tests: `./gradlew test`
- Install debug APK: `./gradlew installDebug`
- Clean the project: `./gradlew clean`

## Troubleshooting

If you see an error like this:
```
Expected Gradle version 8.13 but found X.X. Please use the Gradle wrapper (gradlew) instead of your local Gradle installation.
```

Make sure you're using the wrapper (`gradlew` or `gradlew.bat`) and not your local Gradle installation.
