@echo off
echo ========================================
echo Getting SHA-1 and SHA-256 fingerprints
echo for Release Keystore
echo ========================================
echo.

REM Try to find keytool in common Java locations
set KEYTOOL=keytool

REM Check if keytool is in PATH
where keytool >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :run_keytool

REM Try to find JAVA_HOME
if defined JAVA_HOME (
    set KEYTOOL="%JAVA_HOME%\bin\keytool.exe"
    goto :run_keytool
)

REM Search common JDK locations
for /d %%i in ("C:\Program Files\Java\jdk*") do (
    if exist "%%i\bin\keytool.exe" (
        set KEYTOOL="%%i\bin\keytool.exe"
        goto :run_keytool
    )
)

for /d %%i in ("C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe") do (
    if exist "%%i" (
        set KEYTOOL="%%i"
        goto :run_keytool
    )
)

for /d %%i in ("%LOCALAPPDATA%\Android\Sdk\*") do (
    if exist "%%i\bin\keytool.exe" (
        set KEYTOOL="%%i\bin\keytool.exe"
        goto :run_keytool
    )
)

echo ERROR: Could not find keytool!
echo.
echo Please make sure Java JDK is installed.
echo You can also set JAVA_HOME environment variable.
echo.
echo Alternative: Run this command manually after finding keytool:
echo keytool -list -v -keystore localconnect-release.jks -alias localconnect-key -storepass Localconnect -keypass Localconnect
pause
exit /b 1

:run_keytool
echo Using keytool: %KEYTOOL%
echo.
%KEYTOOL% -list -v -keystore localconnect-release.jks -alias localconnect-key -storepass Localconnect -keypass Localconnect

echo.
echo ========================================
echo Copy the SHA-1 and SHA-256 values above
echo and add them to Firebase Console
echo ========================================
echo.
pause
