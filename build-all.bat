@echo off
echo ========================================
echo RaindropCentral Build Script
echo ========================================
echo.

echo [1/10] Cleaning project...
call gradlew clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [2/10] Building and publishing JExDependency...
call gradlew :JExDependency:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExDependency build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [3/10] Building and publishing JExCommand...
call gradlew :JExCommand:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExCommand build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [4/10] Building and publishing JExTranslate...
call gradlew :JExTranslate:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExTranslate build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [5/10] Building and publishing RPlatform...
call gradlew :RPlatform:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RPlatform build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [6/10] Building and publishing JExEconomy-common...
call gradlew :JExEconomy:publishLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JExEconomy build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [7/10] Building and publishing RCore...
call gradlew :RCore:publishLocal
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RCore build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [8/10] Building RDQ (buildAll)...
call gradlew :RDQ:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDQ buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [9/10] Building RDR (buildAll)...
call gradlew :RDR:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDR buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo [10/10] Building RDS (buildAll)...
call gradlew :RDS:buildAll
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: RDS buildAll failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo ========================================
echo Build completed successfully!
echo ========================================
pause
