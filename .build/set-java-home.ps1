#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Sets JAVA_HOME environment variable permanently

.DESCRIPTION
    This script sets the JAVA_HOME environment variable for the current user.
    It will persist across sessions.

.PARAMETER JavaPath
    Path to the JDK installation directory (without \bin)

.EXAMPLE
    .\set-java-home.ps1 -JavaPath "C:\Users\Privat\.jdks\openjdk-24.0.2+12-54"
#>

param(
    [Parameter(Mandatory=$false)]
    [string]$JavaPath = "C:\Users\Privat\.jdks\openjdk-24.0.2+12-54"
)

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  JAVA_HOME Configuration" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host ""

# Remove trailing \bin if present
if ($JavaPath -match '\\bin\\?$') {
    $JavaPath = $JavaPath -replace '\\bin\\?$', ''
    Write-Host "[INFO] Removed trailing \bin from path" -ForegroundColor Blue
}

# Verify Java installation
$javaExe = Join-Path $JavaPath "bin\java.exe"
if (-not (Test-Path $javaExe)) {
    Write-Host "[ERROR] Java executable not found at: $javaExe" -ForegroundColor Red
    Write-Host "[ERROR] Please verify the Java installation path" -ForegroundColor Red
    exit 1
}

# Get Java version
try {
    $javaVersion = & $javaExe -version 2>&1 | Select-Object -First 1
    Write-Host "[INFO] Found Java: $javaVersion" -ForegroundColor Blue
} catch {
    Write-Host "[ERROR] Could not execute Java at: $javaExe" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[INFO] Setting JAVA_HOME to: $JavaPath" -ForegroundColor Blue

# Set for current session
$env:JAVA_HOME = $JavaPath
Write-Host "[SUCCESS] Set JAVA_HOME for current session" -ForegroundColor Green

# Set permanently for current user
try {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $JavaPath, "User")
    Write-Host "[SUCCESS] Set JAVA_HOME permanently for current user" -ForegroundColor Green
    Write-Host ""
    Write-Host "[INFO] The change will take effect in new PowerShell/CMD sessions" -ForegroundColor Yellow
    Write-Host "[INFO] Current session is already updated" -ForegroundColor Yellow
} catch {
    Write-Host "[ERROR] Failed to set permanent environment variable: $_" -ForegroundColor Red
    Write-Host "[INFO] You may need to run PowerShell as Administrator" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  Configuration Complete" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Current JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
Write-Host ""
