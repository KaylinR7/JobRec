@echo off
echo 📅 DUT Career Hub - Calendar Events Setup
echo ========================================
echo.

REM Check if Node.js is installed
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Node.js is not installed!
    echo.
    echo Please install Node.js first:
    echo 1. Go to https://nodejs.org/
    echo 2. Download and install the LTS version
    echo 3. Restart your command prompt
    echo 4. Run this script again
    echo.
    pause
    exit /b 1
)

echo ✅ Node.js is installed
echo.

REM Check if dependencies are installed
if not exist "node_modules" (
    echo 📦 Installing dependencies...
    npm install
    if %errorlevel% neq 0 (
        echo ❌ Failed to install dependencies
        pause
        exit /b 1
    )
    echo.
)

echo 🚀 Running calendar events setup...
echo.
node setup-calendar-events.js

if %errorlevel% equ 0 (
    echo.
    echo 🎉 Calendar events setup completed successfully!
    echo You can now open the app to see tomorrow's events.
) else (
    echo.
    echo ❌ Setup failed. Please check the error messages above.
)

echo.
pause
