#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Convenience wrapper for the main build script

.DESCRIPTION
    This script forwards all parameters to the main build script in .build/

.EXAMPLE
    .\build.ps1
    .\build.ps1 -Clean
    .\build.ps1 -Deploy -PluginDir "C:\Server\plugins"
#>

param(
    [Parameter(ValueFromRemainingArguments=$true)]
    $RemainingArgs
)

$buildScript = Join-Path $PSScriptRoot ".build\build-all.ps1"

if (-not (Test-Path $buildScript)) {
    Write-Host "[ERROR] Build script not found at: $buildScript" -ForegroundColor Red
    exit 1
}

& $buildScript @RemainingArgs
