# Build Scripts

This directory contains build automation scripts for the Raindrop Plugins project.

## Scripts

### `build-all.ps1` (PowerShell)
Main build script with advanced features.

**Usage:**
```powershell
# Simple build
powershell -ExecutionPolicy Bypass -File .\build-all.ps1

# Clean build
powershell -ExecutionPolicy Bypass -File .\build-all.ps1 -Clean

# Build and deploy to server
powershell -ExecutionPolicy Bypass -File .\build-all.ps1 -Deploy -PluginDir "C:\Server\plugins"
```

**Features:**
- Automatic JAVA_HOME detection and correction
- Color-coded output
- Clean build support
- Deployment to plugin directory
- Detailed error reporting

### `build-all.bat` (Batch)
Legacy batch script for Windows environments without PowerShell.

**Usage:**
```batch
build-all.bat
```

### `set-java-home.ps1`
Helper script to permanently set JAVA_HOME environment variable.

**Usage:**
```powershell
powershell -ExecutionPolicy Bypass -File .\set-java-home.ps1
powershell -ExecutionPolicy Bypass -File .\set-java-home.ps1 -JavaPath "C:\path\to\jdk"
```

## Convenience Wrappers

The root directory contains wrapper scripts that forward to these build scripts:
- `build.ps1` → `.build/build-all.ps1`
- `build.bat` → `.build/build-all.bat`

This allows you to run builds from the root directory without navigating to `.build/`.

## Requirements

- Java 21 or higher
- Gradle (included via wrapper)
- PowerShell 5.1+ (for .ps1 scripts)

## Build Output

Built JARs are located in:
- `RCore/rcore-free/build/libs/RCore-*.jar`
- `RCore/rcore-premium/build/libs/RCore-*.jar`
- `RDQ/rdq-free/build/libs/RDQ-*.jar`
- `RDQ/rdq-premium/build/libs/RDQ-*.jar`
- `JExEconomy/build/libs/JExEconomy-*.jar`
