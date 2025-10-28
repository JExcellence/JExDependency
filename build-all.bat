@echo off
setlocal enabledelayedexpansion

REM ========================================================================
REM RAINDROP PLUGINS BUILD SYSTEM
REM ========================================================================
REM Multi-module Build Strategy Script for Windows with IntelliJ Java Detection
REM Executes clean, build, and maven local install tasks in dependency order
REM Optional: Deploys premium JARs to specified plugin directory
REM ========================================================================

set "PLUGIN_DIR=C:\Users\Privat\Desktop\Spigot Minecraft Server\plugins"
set "RAW_ARG=%1"

:main
call :print_header "RAINDROP PLUGINS BUILD SYSTEM"
call :print_info "Initializing build environment..."

REM Change to repo root
cd ..

REM Remove surrounding quotes if they exist
if not "%~1"=="" (
  set "PLUGIN_DIR=%~1"
  call :print_info "Plugin deployment directory: '%PLUGIN_DIR%'"

  if not exist "%PLUGIN_DIR%" (
    call :print_error "Plugin directory does not exist: '%PLUGIN_DIR%'"
    call :print_section "TROUBLESHOOTING STEPS"
    echo 1. Check if the directory exists manually
    echo 2. Try creating the directory
    echo 3. Alternative command formats to try
    echo.
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

echo.
call :print_info "Checking Java environment..."
set "FOUND_JAVA="
set "JAVA_TO_USE="

REM Method 1: Check if JAVA_HOME is already set and working
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

REM Method 2: Check PATH for java
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

REM Method 3: Look for IntelliJ IDEA's bundled Java
if not defined FOUND_JAVA (
  call :print_debug "Searching for IntelliJ IDEA's bundled Java..."

  for /d %%d in ("%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-U\ch-0\*") do (
    if exist "%%d\jbr\bin\java.exe" (
      call :print_success "Found IntelliJ Java at: %%d\jbr\bin\java.exe"
      set "JAVA_TO_USE=%%d\jbr\bin\java.exe"
      set "JAVA_HOME=%%d\jbr"
      set "FOUND_JAVA=1"
      goto :java_found
    )
  )

  for /d %%d in ("%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\ch-0\*") do (
    if exist "%%d\jbr\bin\java.exe" (
      call :print_success "Found IntelliJ Java at: %%d\jbr\bin\java.exe"
      set "JAVA_TO_USE=%%d\jbr\bin\java.exe"
      set "JAVA_HOME=%%d\jbr"
      set "FOUND_JAVA=1"
      goto :java_found
    )
  )
)

REM Method 4: Look for any Java installation in common locations
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

REM If no Java found, show error
if not defined FOUND_JAVA (
  goto :java_error
)

:java_found
call :print_info "Using Java: %JAVA_TO_USE%"
if defined JAVA_HOME (
  call :print_debug "JAVA_HOME set to: %JAVA_HOME%"
)

echo.
call :print_info "Testing Java installation..."
"%JAVA_TO_USE%" -version
if !errorlevel! neq 0 (
  call :print_error "Java test failed"
  goto :java_error
)

REM Check for Gradle wrapper availability
echo.
call :print_info "Checking Gradle availability..."
set "GRADLE_CMD="
if exist "RCore\gradlew.bat" (
  set "GRADLE_CMD=RCore\gradlew.bat"
  call :print_success "Found Gradle wrapper in RCore directory"
) else if exist "gradlew.bat" (
  set "GRADLE_CMD=gradlew.bat"
  call :print_success "Found Gradle wrapper in current directory"
) else (
  where gradle >nul 2>nul
  if !errorlevel! equ 0 (
    set "GRADLE_CMD=gradle"
    call :print_success "Using system Gradle installation"
  ) else (
    call :print_error "Gradle wrapper not found in RCore or current directory"
    echo Please ensure you are running this script from the repository root
    pause
    exit /b 1
  )
)

goto :build_modules

:java_error
echo.
call :print_error "JAVA SETUP REQUIRED"
echo.
echo No working Java installation found. Please try one of these options:
echo.
call :print_section "OPTION 1: INSTALL INTELLIJ IDEA"
echo Download from: https://www.jetbrains.com/idea/
echo This script will automatically detect IntelliJ's Java
echo.
call :print_section "OPTION 2: INSTALL JAVA MANUALLY"
echo Download from: https://adoptium.net/
echo Set JAVA_HOME environment variable
echo.
call :print_section "OPTION 3: USE INTELLIJ'S TERMINAL"
echo Open your project in IntelliJ IDEA
echo Use Terminal tab at the bottom
echo Run: gradlew clean build publishToMavenLocal
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

REM Use RCore's gradlew if available, otherwise use the determined GRADLE_CMD
if exist "..\RCore\gradlew.bat" (
  call ..\RCore\gradlew.bat %task%
) else if "%GRADLE_CMD%"=="gradlew.bat" (
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
REM Step 1: JExDependency - compileJava, publishToMavenLocal
echo.
call :print_step "1" "Building JExDependency"
call :run_gradle_task "JExDependency" "compileJava publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

REM Step 2: JExCommand - compileJava, publishToMavenLocal
echo.
call :print_step "2" "Building JExCommand"
call :run_gradle_task "JExCommand" "compileJava publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

REM Step 3: JExTranslate - compileJava, publishToMavenLocal
echo.
call :print_step "3" "Building JExTranslate"
call :run_gradle_task "JExTranslate" "compileJava publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

REM Step 4: RPlatform - compileJava, publishToMavenLocal
echo.
call :print_step "4" "Building RPlatform"
call :run_gradle_task "RPlatform" "compileJava publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

REM Step 5: JExEconomy - compileJava, build, publishToMavenLocal
echo.
call :print_step "5" "Building JExEconomy"
call :run_gradle_task "JExEconomy" "compileJava build publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

REM Step 6: RCore - compileJava, build, publishToMavenLocal
echo.
call :print_step "6" "Building RCore"
call :run_gradle_task "RCore" "compileJava build publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

REM Step 7: RDQ - compileJava, build, publishToMavenLocal
echo.
call :print_step "7" "Building RDQ"
call :run_gradle_task "RDQ" "compileJava build publishToMavenLocal"
if !errorlevel! neq 0 exit /b 1

echo.
call :print_success "ALL MODULES BUILT SUCCESSFULLY!"
echo.
call :print_section "BUILD ARTIFACTS"
echo RCore: RCore\build\libs\RCore-Premium-2.0.0.jar
echo RDQ Free: RDQ\build\libs\RDQ-Free-6.0.0.jar
echo RDQ Premium: RDQ\build\libs\RDQ-Premium-6.0.0.jar

REM Deploy premium JARs if plugin directory is specified
if defined PLUGIN_DIR (
  echo.
  call :print_section "DEPLOYMENT"
  call :deploy_jar "RCore\build\libs\RCore-Premium-2.0.0.jar" "RCore-Premium-2.0.0.jar"
  call :deploy_jar "RDQ\build\libs\RDQ-Premium-6.0.0.jar" "RDQ-Premium-6.0.0.jar"
  echo.
  call :print_info "Plugin directory: '%PLUGIN_DIR%'"
) else (
  echo.
  call :print_section "DEPLOYMENT INSTRUCTIONS"
  echo To automatically deploy premium JARs, run with plugin directory:
  echo.
  echo Command Prompt: build-all.bat "C:\Users\Privat\Desktop\Minecraft Server\plugins"
  echo PowerShell: .\build-all.bat "C:\Users\Privat\Desktop\Minecraft Server\plugins"
)

echo.
call :print_header "BUILD COMPLETE"
pause
exit /b 0

REM ========================================================================
REM UTILITY FUNCTIONS
REM ========================================================================

:print_header
echo.
echo ========================================================================
echo %~1
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
