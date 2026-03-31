@echo off
setlocal

pushd "%~dp0" >nul || (
    echo ERROR: Failed to switch to script directory.
    pause
    exit /b 1
)

set "GRADLEW=%CD%\gradlew.bat"
if not exist "%GRADLEW%" (
    echo ERROR: Gradle wrapper not found at "%GRADLEW%".
    popd
    pause
    exit /b 1
)

echo ========================================
echo RaindropCentral Build Script
echo ========================================
echo.

echo [1/12] Validating commit counter and syncing module build numbers...
call "%GRADLEW%" validateCommitCounter syncBuildNumbersFromCommitCounter
if %ERRORLEVEL% NEQ 0 (
    call :fail "Version sync failed!" %ERRORLEVEL%
)
echo.

echo [2/12] Cleaning project...
call "%GRADLEW%" clean
if %ERRORLEVEL% NEQ 0 (
    call :fail "Clean failed!" %ERRORLEVEL%
)
echo.

echo [3/12] Building and publishing JExDependency...
call "%GRADLEW%" :JExDependency:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    call :fail "JExDependency build failed!" %ERRORLEVEL%
)
echo.

echo [4/12] Building and publishing JExCommand...
call "%GRADLEW%" :JExCommand:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    call :fail "JExCommand build failed!" %ERRORLEVEL%
)
echo.

echo [5/12] Building and publishing JExTranslate...
call "%GRADLEW%" :JExTranslate:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    call :fail "JExTranslate build failed!" %ERRORLEVEL%
)
echo.

echo [6/12] Building and publishing RPlatform...
call "%GRADLEW%" :RPlatform:publishToMavenLocal
if %ERRORLEVEL% NEQ 0 (
    call :fail "RPlatform build failed!" %ERRORLEVEL%
)
echo.

echo [7/12] Building and publishing JExEconomy-common...
call "%GRADLEW%" :JExEconomy:publishLocal
if %ERRORLEVEL% NEQ 0 (
    call :fail "JExEconomy build failed!" %ERRORLEVEL%
)
echo.

echo [8/12] Building and publishing RCore...
call "%GRADLEW%" :RCore:publishLocal
if %ERRORLEVEL% NEQ 0 (
    call :fail "RCore build failed!" %ERRORLEVEL%
)
echo.

echo [9/12] Building RDQ (buildAll)...
call "%GRADLEW%" :RDQ:buildAll
if %ERRORLEVEL% NEQ 0 (
    call :fail "RDQ buildAll failed!" %ERRORLEVEL%
)
echo.

echo [10/12] Building RDR (buildAll)...
call "%GRADLEW%" :RDR:buildAll
if %ERRORLEVEL% NEQ 0 (
    call :fail "RDR buildAll failed!" %ERRORLEVEL%
)
echo.

echo [11/12] Building RDS (buildAll)...
call "%GRADLEW%" :RDS:buildAll
if %ERRORLEVEL% NEQ 0 (
    call :fail "RDS buildAll failed!" %ERRORLEVEL%
)
echo.

echo [12/12] Building RDT (buildAll)...
call "%GRADLEW%" :RDT:buildAll
if %ERRORLEVEL% NEQ 0 (
    call :fail "RDT buildAll failed!" %ERRORLEVEL%
)
echo.

echo ========================================
echo Build completed successfully!
echo ========================================

popd
endlocal
exit /b 0

:fail
echo ERROR: %~1
set "EXIT_CODE=%~2"
popd
pause
endlocal & exit /b %EXIT_CODE%
