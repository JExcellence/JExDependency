# Project Structure Organization

This document explains the reorganized directory structure of the RaindropCentral project.

## Overview

The project has been reorganized to reduce clutter in the root directory and improve maintainability.

## Directory Structure

```
RaindropCentral/
├── .build/                      # Build automation (NEW)
│   ├── build-all.bat           # Main build script (Batch)
│   ├── build-all.ps1           # Main build script (PowerShell)
│   ├── set-java-home.ps1       # JAVA_HOME configuration helper
│   ├── README.md               # Build documentation
│   └── STRUCTURE.md            # This file
│
├── .config/                     # Configuration files (NEW)
│   ├── Dockerfile              # Docker configuration
│   └── default-code_style-scheme.xml  # IntelliJ code style
│
├── docs/                        # Documentation (NEW)
│   ├── AGENTS.md               # AI agent collaboration guide
│   └── README.md               # Documentation index
│
├── [Module Directories]         # Source code modules
│   ├── RCore/
│   ├── RDQ/
│   ├── RPlatform/
│   ├── JExCommand/
│   ├── JExDependency/
│   ├── JExEconomy/
│   └── JExTranslate/
│
├── buildSrc/                    # Gradle convention plugins
├── gradle/                      # Gradle wrapper
│
├── build.bat                    # Convenience wrapper → .build/build-all.bat
├── build.ps1                    # Convenience wrapper → .build/build-all.ps1
├── build.gradle.kts             # Root build configuration
├── settings.gradle.kts          # Gradle settings
├── gradle.properties            # Gradle properties
└── README.md                    # Main project README
```

## Changes Made

### Moved Files

| Old Location | New Location | Purpose |
|--------------|--------------|---------|
| `build-all.bat` | `.build/build-all.bat` | Build script |
| `build-all.ps1` | `.build/build-all.ps1` | Build script |
| `set-java-home.ps1` | `.build/set-java-home.ps1` | JAVA_HOME helper |
| `Dockerfile` | `.config/Dockerfile` | Docker config |
| `default-code_style-scheme.xml` | `.config/default-code_style-scheme.xml` | Code style |
| `AGENTS.md` | `docs/AGENTS.md` | Documentation |

### New Files

| File | Purpose |
|------|---------|
| `build.bat` | Convenience wrapper for `.build/build-all.bat` |
| `build.ps1` | Convenience wrapper for `.build/build-all.ps1` |
| `.build/README.md` | Build system documentation |
| `.build/STRUCTURE.md` | This file |
| `docs/README.md` | Documentation index |

## Benefits

1. **Cleaner Root Directory** - Only essential files remain in the root
2. **Better Organization** - Related files are grouped together
3. **Easier Navigation** - Clear separation of concerns
4. **Backward Compatibility** - Wrapper scripts maintain existing workflows
5. **Improved Documentation** - Each directory has its own README

## Usage

### Building

From the root directory:
```bash
# PowerShell
./build.ps1
./build.ps1 -Clean
./build.ps1 -Deploy -PluginDir "C:\Server\plugins"

# Batch
./build.bat
```

Or directly from `.build/`:
```bash
cd .build
powershell -ExecutionPolicy Bypass -File .\build-all.ps1
```

### Configuration

- **JAVA_HOME**: Run `.build/set-java-home.ps1`
- **Code Style**: Import `.config/default-code_style-scheme.xml` in IntelliJ
- **Docker**: Use `.config/Dockerfile` for containerization

### Documentation

- **Build Docs**: See `.build/README.md`
- **Project Docs**: See `docs/README.md`
- **Module Docs**: See individual module directories

## Migration Notes

If you have existing scripts or CI/CD pipelines that reference the old locations:

1. **Update paths** to use the new locations, OR
2. **Use the wrapper scripts** (`build.ps1` / `build.bat`) which maintain compatibility

Example CI/CD update:
```yaml
# Old
- run: powershell -File build-all.ps1

# New (Option 1 - Direct)
- run: powershell -File .build/build-all.ps1

# New (Option 2 - Wrapper)
- run: powershell -File build.ps1
```
