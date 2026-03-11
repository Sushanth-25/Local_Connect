# Quick Release APK Steps

## ✅ Setup Complete!

Your project is now configured for signed releases. Here's what I've done:

1. ✅ Updated `build.gradle.kts` with signing configuration
2. ✅ Added `keystore.properties` to `.gitignore` 
3. ✅ Created comprehensive guides
4. ✅ Created helper script for keystore generation

---

## 🚀 Generate Your Signed APK

### Step 1: Create Keystore (One-time only)

**OPTION A: Use the Helper Script (Easiest)**

Simply double-click this file:
```
generate-keystore.bat
```

**OPTION B: Manual Command**

Open Command Prompt in the project root and run:

```cmd
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkey -v -keystore localconnect-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias localconnect-key
```

**You'll be asked for:**
- Keystore password (choose a strong one!)
- Key password (can be same as keystore password)
- Your name/organization info

**⚠️ IMPORTANT:** Save these passwords! You'll need them forever.

---

### Step 2: Create keystore.properties

Create a file named `keystore.properties` in the project root:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD_HERE
keyPassword=YOUR_KEY_PASSWORD_HERE
keyAlias=localconnect-key
storeFile=localconnect-release.jks
```

Replace the password placeholders with your actual passwords.

---

### Step 3: Build Signed APK

```cmd
.\gradlew assembleRelease
```

**Your signed APK will be at:**
```
app\build\outputs\apk\release\app-release.apk
```

---

## 📦 Alternative: Build AAB (For Google Play)

For Play Store, use Android App Bundle (AAB):

```cmd
.\gradlew bundleRelease
```

**Location:**
```
app\build\outputs\bundle\release\app-release.aab
```

---

## 🔐 Security Reminders

- ✅ `keystore.properties` is in `.gitignore` (won't be committed)
- ✅ `*.jks` files are in `.gitignore` (won't be committed)
- 🔒 Back up your `.jks` file to a secure location
- 🔒 Never lose your keystore - you can't update your app without it!

---

## 📱 Installing on Device

```cmd
adb install app\build\outputs\apk\release\app-release.apk
```

Or simply transfer the APK to your phone and install it.

---

## 🎉 You're All Set!

Run the commands above to generate your release APK.
