@echo off
echo ========================================
echo RaindropCentral Build Script
echo ========================================
echo.

echo [1/12] Validating commit counter and syncing module build numbers...
call gradlew validateCommitCounter syncBuildNumbersFromCommitCounter
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Version sync failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [2/12] Cleaning project...
call gradlew clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [3/12] Building and publishing JExDependency...
call gradlew :JExDependency:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExDependency build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [4/12] Building and publishing JExCommand...
call gradlew :JExCommand:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExCommand build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [5/12] Building and publishing JExTranslate...
call gradlew :JExTranslate:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExTranslate build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [6/12] Building and publishing RPlatform...
call gradlew :RPlatform:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RPlatform build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [7/12] Building and publishing JExEconomy-common...
call gradlew :JExEconomy:publishLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExEconomy build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [8/12] Building and publishing RCore...
call gradlew :RCore:publishLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RCore build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [9/12] Building RDQ (buildAll)...
call gradlew :RDQ:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDQ buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [10/12] Building RDR (buildAll)...
call gradlew :RDR:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDR buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [11/12] Building RDS (buildAll)...
call gradlew :RDS:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDS buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [12/12] Building RDT (buildAll)...
call gradlew :RDT:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDT buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo ========================================
echo Build completed successfully!
echo ========================================
