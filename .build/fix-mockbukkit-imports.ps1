#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Fixes MockBukkit imports in test files

.DESCRIPTION
    Updates all test files to use the correct MockBukkit package name
    for version 3.x (be.seeseemelk.mockbukkit instead of org.mockbukkit.mockbukkit)
#>

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  Fixing MockBukkit Imports" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path -Parent $PSScriptRoot
$testFiles = Get-ChildItem -Path $projectRoot -Recurse -Filter "*.java" | Where-Object {
    $_.FullName -match "\\src\\test\\java\\" -and 
    ((Select-String -Path $_.FullName -Pattern "org\.mockbukkit\.mockbukkit" -Quiet) -or
     (Select-String -Path $_.FullName -Pattern "com\.github\.seeseemelk\.mockbukkit" -Quiet))
}

$count = 0
foreach ($file in $testFiles) {
    Write-Host "[INFO] Processing: $($file.FullName.Replace($projectRoot, '.'))" -ForegroundColor Blue
    
    $content = Get-Content -Path $file.FullName -Raw
    $originalContent = $content
    
    # Replace org.mockbukkit.mockbukkit with be.seeseemelk.mockbukkit
    $content = $content -replace 'import org\.mockbukkit\.mockbukkit\.', 'import be.seeseemelk.mockbukkit.'
    
    # Replace com.github.seeseemelk.mockbukkit with be.seeseemelk.mockbukkit
    $content = $content -replace 'import com\.github\.seeseemelk\.mockbukkit\.', 'import be.seeseemelk.mockbukkit.'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "[SUCCESS] Updated: $($file.Name)" -ForegroundColor Green
        $count++
    }
}

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  Complete" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Updated $count file(s)" -ForegroundColor Green
Write-Host ""
