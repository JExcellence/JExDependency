@echo off
setlocal enabledelayedexpansion

:: ========================================================================
::                    RAINDROP PLUGINS BUILD SYSTEM
:: ========================================================================
:: Multi-module Build Strategy Script for Windows with IntelliJ Java Detection
:: Executes clean, build, and maven local install tasks in dependency order
:: Optional: Deploys premium JARs to specified plugin directory
:: ========================================================================

:: Initialize color codes for enhanced output
for /f %%A in ('"prompt $H &echo on &for %%B in (1) do rem"') do set BS=%%A

:: Parse command line arguments with better debugging and quote handling
set "PLUGIN_DIR=C:\Users\Privat\Desktop\Spigot Minecraft Server\plugins"
set "RAW_ARG=%1"

:main
call :print_header "RAINDROP PLUGINS BUILD SYSTEM"
call :print_info "Initializing build environment..."

:: Optional Cleanup - Delete specific folders before building (uncomment to activate)
:: WARNING: This will permanently delete files and folders. Only enable with caution.
:: rd /s /q "%PLUGIN_DIR%\RCore\database"
:: rd /s /q "%PLUGIN_DIR%\RDQ\bounty"
:: rd /s /q "%PLUGIN_DIR%\RDQ\commands"
:: rd /s /q "%PLUGIN_DIR%\RDQ\database"
rd /s /q "%PLUGIN_DIR%\RDQ\logs" 2>nul
rd /s /q "%PLUGIN_DIR%\RDQ\translations" 2>nul

:: Remove surrounding quotes if they exist
if not "%~1"=="" (
    :: Use %~1 to automatically remove quotes
    set "PLUGIN_DIR=%~1"

    call :print_info "Plugin deployment directory: '%PLUGIN_DIR%'"
    call :print_debug "Raw argument received: %RAW_ARG%"
    call :print_debug "Processed argument: '%~1'"

    :: Debug: Show what we're checking
    call :print_debug "Checking if directory exists: '%PLUGIN_DIR%'"

    :: Validate that the directory exists
    if not exist "%PLUGIN_DIR%" (
        call :print_error "Plugin directory does not exist: '%PLUGIN_DIR%'"
        echo.
        call :print_section "DEBUGGING INFORMATION"
        echo   ^> Raw argument: %RAW_ARG%
        echo   ^> Processed path: "%PLUGIN_DIR%"
        echo   ^> Current directory: "%CD%"
        echo.
        call :print_section "TROUBLESHOOTING STEPS"
        echo   1. Check if the directory exists manually:
        echo      dir "%PLUGIN_DIR%"
        echo.
        echo   2. Try creating the directory:
        echo      mkdir "%PLUGIN_DIR%"
        echo.

        :: Try to create the directory if it doesn't exist
        call :print_info "Attempting to create the directory..."
        mkdir "%PLUGIN_DIR%" 2>nul
        if exist "%PLUGIN_DIR%" (
            call :print_success "Successfully created plugin directory: '%PLUGIN_DIR%'"
        ) else (
            call :print_error "Failed to create directory. Please check the path and permissions."
            pause
            exit /b 1
        )
    ) else (
        call :print_success "Plugin directory verified: '%PLUGIN_DIR%'"
    )
) else (
    call :print_warning "No plugin directory specified - JARs will not be deployed"
)

call :print_section "BUILD ENVIRONMENT SETUP"
call :print_info "Working directory: %CD%"

:: Check Java availability with IntelliJ detection
echo.
call :print_info "Checking Java environment..."

set "FOUND_JAVA="
set "JAVA_TO_USE="

:: Method 1: Check if JAVA_HOME is already set and working
if defined JAVA_HOME (
    call :print_debug "JAVA_HOME is set to: %JAVA_HOME%"
    if exist "%JAVA_HOME%\bin\java.exe" (
        call :print_success "Java found at JAVA_HOME"
        set "JAVA_TO_USE=%JAVA_HOME%\bin\java.exe"
        set "FOUND_JAVA=1"
        goto :java_found
    ) else (
        call :print_warning "JAVA_HOME is set but java.exe not found at %JAVA_HOME%\bin\"
    )
)

:: Method 2: Check PATH for java
if not defined FOUND_JAVA (
    call :print_debug "Checking PATH for Java..."
    where java >nul 2>nul
    if !errorlevel! equ 0 (
        call :print_success "Java found in PATH"
        set "JAVA_TO_USE=java"
        set "FOUND_JAVA=1"
        goto :java_found
    )
)

:: Method 3: Look for IntelliJ IDEA's bundled Java
if not defined FOUND_JAVA (
    call :print_debug "Searching for IntelliJ IDEA's bundled Java..."

    :: Common IntelliJ installation paths
    set "INTELLIJ_PATHS[0]=%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-U\ch-0"
    set "INTELLIJ_PATHS[1]=%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\ch-0"
    set "INTELLIJ_PATHS[2]=%ProgramFiles%\JetBrains\IntelliJ IDEA Community Edition"
    set "INTELLIJ_PATHS[3]=%ProgramFiles%\JetBrains\IntelliJ IDEA Ultimate"
    set "INTELLIJ_PATHS[4]=%ProgramFiles(x86)%\JetBrains\IntelliJ IDEA Community Edition"
    set "INTELLIJ_PATHS[5]=%ProgramFiles(x86)%\JetBrains\IntelliJ IDEA Ultimate"

    for /L %%i in (0,1,5) do (
        call set "CURRENT_PATH=%%INTELLIJ_PATHS[%%i]%%"
        if exist "!CURRENT_PATH!" (
            call :print_debug "Checking: !CURRENT_PATH!"

            :: Look for JBR (JetBrains Runtime) in different possible locations
            for /d %%d in ("!CURRENT_PATH!\*") do (
                if exist "%%d\jbr\bin\java.exe" (
                    call :print_success "Found IntelliJ Java at: %%d\jbr\bin\java.exe"
                    set "JAVA_TO_USE=%%d\jbr\bin\java.exe"
                    set "JAVA_HOME=%%d\jbr"
                    set "FOUND_JAVA=1"
                    goto :java_found
                )
                if exist "%%d\bin\jbr\bin\java.exe" (
                    call :print_success "Found IntelliJ Java at: %%d\bin\jbr\bin\java.exe"
                    set "JAVA_TO_USE=%%d\bin\jbr\bin\java.exe"
                    set "JAVA_HOME=%%d\bin\jbr"
                    set "FOUND_JAVA=1"
                    goto :java_found
                )
            )
        )
    )
)

:: Method 4: Look for any Java installation in common locations
if not defined FOUND_JAVA (
    call :print_debug "Searching common Java installation locations..."

    for /d %%d in ("C:\Program Files\Java\*") do (
        if exist "%%d\bin\java.exe" (
            call :print_success "Found Java at: %%d\bin\java.exe"
            set "JAVA_TO_USE=%%d\bin\java.exe"
            set "JAVA_HOME=%%d"
            set "FOUND_JAVA=1"
            goto :java_found
        )
    )

    for /d %%d in ("C:\Program Files (x86)\Java\*") do (
        if exist "%%d\bin\java.exe" (
            call :print_success "Found Java at: %%d\bin\java.exe"
            set "JAVA_TO_USE=%%d\bin\java.exe"
            set "JAVA_HOME=%%d"
            set "FOUND_JAVA=1"
            goto :java_found
        )
    )
)

:: If no Java found, show error
if not defined FOUND_JAVA (
    goto :java_error
)

:java_found
call :print_info "Using Java: %JAVA_TO_USE%"
if defined JAVA_HOME (
    call :print_debug "JAVA_HOME set to: %JAVA_HOME%"
)

:: Test Java version
echo.
call :print_info "Testing Java installation..."
"%JAVA_TO_USE%" -version
if !errorlevel! neq 0 (
    call :print_error "Java test failed"
    goto :java_error
)

:: Check for Gradle wrapper availability
echo.
call :print_info "Checking Gradle availability..."
set "GRADLE_CMD="
if exist "gradlew.bat" (
    set "GRADLE_CMD=gradlew.bat"
    call :print_success "Found Gradle wrapper in current directory"
) else (
    where gradle >nul 2>nul
    if !errorlevel! equ 0 (
        set "GRADLE_CMD=gradle"
        call :print_success "Using system Gradle installation"
    ) else (
        call :print_error "Neither gradlew.bat nor system Gradle found"
        echo Please ensure Gradle is installed or Gradle wrapper is available
        pause
        exit /b 1
    )
)

:: Function to run gradle task and check for success
goto :build_modules

:java_error
echo.
call :print_error "JAVA SETUP REQUIRED"
echo.
echo No working Java installation found. Please try one of these options:
echo.
call :print_section "OPTION 1: INSTALL INTELLIJ IDEA"
echo   ^> Download from: https://www.jetbrains.com/idea/
echo   ^> This script will automatically detect IntelliJ's Java
echo.
call :print_section "OPTION 2: INSTALL JAVA MANUALLY"
echo   ^> Download from: https://adoptium.net/
echo   ^> Set JAVA_HOME environment variable
echo.
call :print_section "OPTION 3: USE INTELLIJ'S TERMINAL"
echo   ^> Open your project in IntelliJ IDEA
echo   ^> Use Terminal tab at the bottom
echo   ^> Run: gradlew clean build publishToMavenLocal
echo.
call :print_section "SEARCH LOCATIONS CHECKED"
echo   ^> JAVA_HOME environment variable
echo   ^> System PATH
echo   ^> IntelliJ IDEA installations
echo   ^> C:\Program Files\Java\
echo   ^> C:\Program Files (x86)\Java\
echo.
pause
exit /b 1

:run_gradle_task
set "module=%~1"
set "task=%~2"
echo.
call :print_task "Running '%task%' for module: %module%"

if not exist "%module%" (
    call :print_error "Module directory '%module%' not found"
    exit /b 1
)

cd "%module%"

:: Set JAVA_HOME for Gradle if we found it
if defined JAVA_HOME (
    set "ORIGINAL_JAVA_HOME=%JAVA_HOME%"
)

:: Use the determined Gradle command
if "%GRADLE_CMD%"=="gradlew.bat" (
    call ..\gradlew.bat %task%
) else (
    call %GRADLE_CMD% %task%
)

if !errorlevel! neq 0 (
    call :print_error "Failed to complete '%task%' for %module%"
    cd ..
    exit /b 1
)
call :print_success "Successfully completed '%task%' for %module%"
cd ..
goto :eof

:deploy_jar
set "jar_path=%~1"
set "jar_name=%~2"

if not defined PLUGIN_DIR (
    call :print_debug "PLUGIN_DIR not defined, skipping deployment"
    goto :eof
)

call :print_debug "Attempting to deploy: %jar_path%"

if not exist "%jar_path%" (
    call :print_warning "JAR file not found: %jar_path%"
    call :print_debug "Current directory: %CD%"
    if exist "%~dp1" (
        call :print_debug "Directory exists, listing contents:"
        dir "%~dp1*.jar" /b 2>nul
    ) else (
        call :print_debug "Directory does not exist: %~dp1"
    )
    goto :eof
)

call :print_info "Deploying %jar_name% to plugin directory..."
copy "%jar_path%" "%PLUGIN_DIR%\" >nul
if !errorlevel! equ 0 (
    call :print_success "Successfully deployed %jar_name% to '%PLUGIN_DIR%'"
) else (
    call :print_error "Failed to deploy %jar_name%"
)
goto :eof

:build_modules
:: Step 1: JExCommand - clean, publishToMavenLocal
echo.
call :print_step "1" "Building JExCommand"
call :run_gradle_task "JExCommand" "clean"
if !errorlevel! neq 0 exit /b 1

call :run_gradle_task "JExCommand" "publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

:: Step 2: R18n - clean, publishToMavenLocal
echo.
call :print_step "2" "Building JExTranslate"
call :run_gradle_task "JExTranslate" "clean"
if !errorlevel! neq 0 exit /b 1

call :run_gradle_task "JExTranslate" "publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

:: Step 3: RPlatform - clean, publishToMavenLocal
echo.
call :print_step "3" "Building RPlatform"
call :run_gradle_task "RPlatform" "clean"
if !errorlevel! neq 0 exit /b 1

call :run_gradle_task "RPlatform" "publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

:: Step 4: RCore - clean, build, publishToMavenLocal
echo.
call :print_step "4" "Building RCore"
call :run_gradle_task "RCore" "clean"
if !errorlevel! neq 0 exit /b 1

call :run_gradle_task "RCore" "buildAll"
if !errorlevel! neq 0 exit /b 1

call :run_gradle_task "RCore" "publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

:: Step 5: RDQ - clean, build
echo.
call :print_step "5" "Building RDQ"
call :run_gradle_task "RDQ" "clean"
if !errorlevel! neq 0 exit /b 1

call :run_gradle_task "RDQ" "buildAll"
if !errorlevel! neq 0 exit /b 1

echo.
call :print_success "ALL MODULES BUILT SUCCESSFULLY!"
echo.
call :print_section "BUILD ARTIFACTS"

:: Deploy premium JARs if plugin directory is specified
if defined PLUGIN_DIR (
    echo.
    call :print_section "DEPLOYMENT"
    
    :: Debug: Show what files exist before deployment
    call :print_debug "Checking build artifacts before deployment..."
    if exist "RCore\build\libs\" (
        call :print_debug "RCore artifacts:"
        dir "RCore\build\libs\*.jar" /b 2>nul
    )
    if exist "JExEconomy\build\libs\" (
        call :print_debug "JExEconomy artifacts:"
        dir "JExEconomy\build\libs\*.jar" /b 2>nul
    )
    if exist "RDQ\build\libs\" (
        call :print_debug "RDQ artifacts:"
        dir "RDQ\build\libs\*.jar" /b 2>nul
    )
    
    call :deploy_jar "RCore\rcore-free\build\libs\RCore-Free-2.0.0.jar" "RCore-Free-2.0.0.jar"
    :: call :deploy_jar "JE\build\libs\JECurrency-Premium-2.0.0.jar" "JECurrency-Premium-2.0.0.jar"
    call :deploy_jar "RDQ\rdq-premium\build\libs\RDQ-Premium-6.0.0.jar" "RDQ-Premium-6.0.1.jar"
    echo.
    call :print_info "Plugin directory: '%PLUGIN_DIR%'"
) else (
    echo.
    call :print_section "DEPLOYMENT INSTRUCTIONS"
    echo To automatically deploy premium JARs, run with plugin directory:
    echo.
    echo   Command Prompt: build-all.bat "C:\Users\Privat\Desktop\Minecraft Server\plugins"
    echo   PowerShell:     .\build-all.bat "C:\Users\Privat\Desktop\Minecraft Server\plugins"
)

echo.
call :print_header "BUILD COMPLETE"
pause
exit /b 0

:: ========================================================================
::                           UTILITY FUNCTIONS
:: ========================================================================

:print_header
echo.
echo ========================================================================
echo                           %~1
echo ========================================================================
goto :eof

:print_section
echo.
echo [%~1]
echo ------------------------------------------------------------------------
goto :eof

:print_step
echo [STEP %~1] %~2
echo ------------------------------------------------------------------------
goto :eof

:print_success
echo [SUCCESS] %~1
goto :eof

:print_error
echo [ERROR] %~1
goto :eof

:print_warning
echo [WARNING] %~1
goto :eof

:print_info
echo [INFO] %~1
goto :eof

:print_debug
echo [DEBUG] %~1
goto :eof

:print_task
echo [TASK] %~1
echo ------------------------------------------------------------------------
goto :eof