#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Raindrop Plugins Build System - PowerShell Edition

.DESCRIPTION
    Builds all Raindrop modules in correct dependency order using Gradle.
    Optionally deploys premium JARs to a specified plugin directory.

.PARAMETER PluginDir
    Optional path to the Minecraft server plugins directory for deployment.

.PARAMETER Clean
    Perform a clean build (removes all build artifacts first).

.PARAMETER Deploy
    Deploy premium JARs after building.

.EXAMPLE
    .\build-all.ps1
    Builds all modules without deployment.

.EXAMPLE
    .\build-all.ps1 -PluginDir "C:\Server\plugins" -Deploy
    Builds all modules and deploys premium JARs to the specified directory.

.EXAMPLE
    .\build-all.ps1 -Clean
    Performs a clean build of all modules.
#>

param(
    [Parameter(Mandatory=$false)]
    [string]$PluginDir,
    
    [Parameter(Mandatory=$false)]
    [switch]$Clean,
    
    [Parameter(Mandatory=$false)]
    [switch]$Deploy
)

# Color functions for better output
function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "========================================================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "========================================================================" -ForegroundColor Cyan
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "[STEP] $Message" -ForegroundColor Yellow
    Write-Host "------------------------------------------------------------------------" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

# Main execution
try {
    Write-Header "RAINDROP PLUGINS BUILD SYSTEM"
    
    # Check if we're in the correct directory
    if (-not (Test-Path "settings.gradle.kts")) {
        Write-Error-Custom "settings.gradle.kts not found. Please run this script from the project root."
        exit 1
    }
    
    Write-Info "Working directory: $PWD"
    
    # Configure JAVA_HOME
    $javaHome = $env:JAVA_HOME
    if ($javaHome) {
        # Remove trailing \bin if present
        if ($javaHome -match '\\bin\\?$') {
            $javaHome = $javaHome -replace '\\bin\\?$', ''
            Write-Info "Corrected JAVA_HOME from: $env:JAVA_HOME"
        }
        $env:JAVA_HOME = $javaHome
        Write-Info "Using JAVA_HOME: $env:JAVA_HOME"
    } else {
        # Try to find Java automatically
        $possibleJdks = @(
            "C:\Users\Privat\.jdks\openjdk-24.0.2+12-54",
            "C:\Program Files\Java\jdk-21",
            "C:\Program Files\Java\jdk-17"
        )
        
        foreach ($jdk in $possibleJdks) {
            if (Test-Path "$jdk\bin\java.exe") {
                $env:JAVA_HOME = $jdk
                Write-Info "Auto-detected JAVA_HOME: $env:JAVA_HOME"
                break
            }
        }
        
        if (-not $env:JAVA_HOME) {
            Write-Error-Custom "JAVA_HOME not set and could not auto-detect Java installation"
            Write-Info "Please set JAVA_HOME environment variable to your JDK installation directory"
            exit 1
        }
    }
    
    # Determine Gradle command
    $gradleCmd = if (Test-Path "gradlew.bat") {
        ".\gradlew.bat"
    } elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
        "gradle"
    } else {
        Write-Error-Custom "Neither gradlew.bat nor system Gradle found"
        exit 1
    }
    
    Write-Success "Using Gradle: $gradleCmd"
    
    # Build command
    if ($Clean) {
        Write-Step "Performing clean build"
        $buildTask = "clean", "buildAll"
    } else {
        Write-Step "Building all modules"
        $buildTask = "buildAll"
    }
    
    # Execute build
    Write-Info "Executing: $gradleCmd $buildTask"
    & $gradleCmd $buildTask
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error-Custom "Build failed with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
    
    Write-Success "Build completed successfully!"
    
    # Deployment
    if ($Deploy -and $PluginDir) {
        Write-Step "Deploying premium JARs"
        
        # Create directory if it doesn't exist
        if (-not (Test-Path $PluginDir)) {
            Write-Info "Creating plugin directory: $PluginDir"
            New-Item -ItemType Directory -Path $PluginDir -Force | Out-Null
        }
        
        Write-Info "Target directory: $PluginDir"
        
        # Define JARs to deploy
        $jars = @(
            "RCore\rcore-premium\build\libs\RCore-Premium-2.0.0.jar",
            "RDQ\rdq-premium\build\libs\RDQ-Premium-6.0.1.jar"
        )
        
        foreach ($jar in $jars) {
            if (Test-Path $jar) {
                $fileName = Split-Path $jar -Leaf
                Copy-Item $jar -Destination (Join-Path $PluginDir $fileName) -Force
                Write-Success "Deployed: $fileName"
            } else {
                Write-Error-Custom "Not found: $jar"
            }
        }
        
        Write-Success "Deployment complete!"
    } elseif ($Deploy -and -not $PluginDir) {
        Write-Info "Deployment requested but no plugin directory specified."
        Write-Info "Use: .\build-all.ps1 -PluginDir 'C:\path\to\plugins' -Deploy"
    }
    
    Write-Header "BUILD COMPLETE"
    
} catch {
    Write-Error-Custom "An error occurred: $_"
    exit 1
}
