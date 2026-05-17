@echo off
REM =====================================================
REM  BiliMusic Android APK Build Script (Windows)
REM =====================================================
title BiliMusic Build

setlocal enabledelayedexpansion

echo.
echo ============================================
echo   BiliMusic - Android APK Builder
echo ============================================
echo.

REM ---- Check Java ----
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Java not found. Install JDK 17+ and set JAVA_HOME.
    echo        Download: https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)
echo [OK] Java found.

REM ---- Get java.exe path and try to derive JAVA_HOME ----
if "%JAVA_HOME%"=="" (
    for /f "tokens=*" %%i in ('where java') do set "JAVA_PATH=%%i"
    if not "!JAVA_PATH!"=="" (
        for %%i in ("!JAVA_PATH!") do set "JAVA_HOME=%%~dpi.."
        if "!JAVA_HOME:~-1!"=="\" set "JAVA_HOME=!JAVA_HOME:~0,-1!"
    )
    if not exist "!JAVA_HOME!\lib\jrt-fs.jar" (
        echo [FAIL] Cannot determine JAVA_HOME. Please set JAVA_HOME manually.
        echo        e.g.: setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot"
        pause
        exit /b 1
    )
    echo [INFO] JAVA_HOME auto-detected: !JAVA_HOME!
) else (
    echo [OK] JAVA_HOME: %JAVA_HOME%
)

REM ---- Check Android SDK ----
if "%ANDROID_HOME%"=="" (
    if "%ANDROID_SDK_ROOT%"=="" (
        if exist "%LOCALAPPDATA%\Android\Sdk" (
            set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
        ) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk" (
            set "ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk"
        )
    ) else (
        set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
    )
)

if "%ANDROID_HOME%"=="" (
    echo [WARN] ANDROID_HOME not set. Please install Android Studio or set the path manually.
    echo        e.g.: setx ANDROID_HOME "C:\Users\YOUR_USER\AppData\Local\Android\Sdk"
    echo.
) else (
    echo [OK] Android SDK: "%ANDROID_HOME%"
    if not exist "%ANDROID_HOME%\platforms\android-34" (
        echo [WARN] Android SDK platform 34 not found.
        echo        Run: "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager" "platforms;android-34"
    )
)

REM ---- Delete stale local.properties if ANDROID_HOME is set ----
if exist local.properties (
    del local.properties
    echo [INFO] Removed old local.properties
)

REM ---- Create local.properties using PowerShell (handles spaces) ----
if not "%ANDROID_HOME%"=="" (
    powershell -Command "$p='%ANDROID_HOME:\=/%'; $p=$p.Replace(' ','\ '); Set-Content -Path local.properties -Value ('sdk.dir='+$p)"
    if exist local.properties (
        echo [OK] Created local.properties
    ) else (
        echo [WARN] Failed to create local.properties
    )
)

echo.
echo ---- Starting Gradle build ----
echo.

REM ---- Run Gradle wrapper to build APK ----
call gradlew.bat assembleDebug --no-daemon
set BUILD_RESULT=%ERRORLEVEL%

echo.
if %BUILD_RESULT% equ 0 (
    echo ============================================
    echo   BUILD SUCCESSFUL!
    echo ============================================
    echo.
    echo APK location:
    dir /s /b app\build\outputs\apk\debug\*.apk 2>nul
    echo.
    echo Install on device:
    echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
) else (
    echo ============================================
    echo   BUILD FAILED (exit code: %BUILD_RESULT%)
    echo ============================================
    echo.
    echo Check the errors above. Common issues:
    echo   - Android SDK not found / platform not installed
    echo   - Network timeout downloading dependencies
    echo   - Java version mismatch
)

echo.
pause
