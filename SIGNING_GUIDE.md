# APK Signing Guide for LocalConnect

## Step 1: Generate a Keystore

Run this command in your project root directory:

```cmd
keytool -genkey -v -keystore localconnect-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias localconnect-key
```

**You'll be prompted for:**
- Keystore password (choose a strong password - remember this!)
- Key password (can be same as keystore password)
- Your name
- Organization unit
- Organization name
- City
- State
- Country code (e.g., US)

**IMPORTANT:** Save these credentials securely! You'll need them to update your app in the future.

## Step 2: Store Credentials Securely

After generating the keystore, I'll create a `keystore.properties` file (already configured in your .gitignore).

Create a file named `keystore.properties` in the project root with:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=localconnect-key
storeFile=localconnect-release.jks
```

Replace `YOUR_KEYSTORE_PASSWORD` and `YOUR_KEY_PASSWORD` with your actual passwords.

## Step 3: Build Signed APK

After setup, run:

```cmd
.\gradlew assembleRelease
```

Your signed APK will be at:
```
app\build\outputs\apk\release\app-release.apk
```

## Step 4: Verify the Signature

```cmd
keytool -printcert -jarfile app\build\outputs\apk\release\app-release.apk
```

## Important Notes

- **NEVER commit keystore.properties or .jks files to Git**
- Store your keystore file and passwords in a safe place
- If you lose the keystore, you cannot update your app on Play Store
- Consider backing up the .jks file to a secure cloud storage

## For Play Store Upload

You can either:
1. Upload the APK directly
2. Or better: Build an AAB (Android App Bundle) with: `.\gradlew bundleRelease`
   - AAB location: `app\build\outputs\bundle\release\app-release.aab`

