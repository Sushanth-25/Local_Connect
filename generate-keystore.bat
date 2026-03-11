@echo off
REM Generate Android Release Keystore
REM This script uses the Java keytool from Android Studio

SET KEYTOOL="C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

echo ========================================
echo  Android Release Keystore Generator
echo ========================================
echo.
echo This will create a keystore file for signing your Android app.
echo.
echo You'll be asked for:
echo  - Keystore password (remember this!)
echo  - Key password (can be same as keystore password)
echo  - Your name and organization details
echo.
pause

%KEYTOOL% -genkey -v -keystore localconnect-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias localconnect-key

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo  SUCCESS! Keystore created successfully!
    echo ========================================
    echo.
    echo File created: localconnect-release.jks
    echo.
    echo NEXT STEPS:
    echo 1. Create keystore.properties file with your passwords
    echo 2. Run: gradlew assembleRelease
    echo.
    echo WARNING: Back up your keystore file and NEVER lose your passwords!
    echo.
) else (
    echo.
    echo ========================================
    echo  ERROR: Failed to create keystore
    echo ========================================
    echo.
)

pause

