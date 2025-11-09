# Fresh Installation Guide

This guide helps new contributors set up the RaindropCentral project from a fresh clone.

## Prerequisites

- **Java Development Kit (JDK) 21 or higher**
  - Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
  - Set `JAVA_HOME` environment variable to your JDK installation directory
  - Verify: `java -version` should show version 21+

- **Git**
  - Download from [git-scm.com](https://git-scm.com/)
  - Verify: `git --version`

## Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Antimatter-Zone/RaindropCentral.git
cd RaindropCentral
```

### 2. Publish Internal Dependencies

**This is the most important step for fresh installations!**

The project has internal dependencies that must be published to your local Maven repository before building:

```bash
# Windows
.\gradlew publishDependencies

# Linux/Mac
./gradlew publishDependencies
```

This command publishes modules in the correct dependency order:
```
JExDependency → JExCommand → JExTranslate → RPlatform → JExEconomy → RCore
```

**Why is this needed?**
- `rdq-common` depends on `rcore-free` and `rcore-common`
- `rcore-free` depends on `rplatform`, `jexeconomy`, etc.
- These dependencies are resolved from Maven Local (`~/.m2/repository`)
- On a fresh clone, Maven Local doesn't have these artifacts yet

### 3. Build the Project

After publishing dependencies, you can build the entire project:

```bash
# Using the build script (recommended)
.\build.ps1              # Windows PowerShell
./build.sh               # Linux/Mac (if available)

# Or using Gradle directly
.\gradlew buildAll       # Windows
./gradlew buildAll       # Linux/Mac
```

The `buildAll` task automatically:
1. Runs `publishDependencies` first (if needed)
2. Builds all plugin variants (Free and Premium editions)
3. Creates shaded JARs ready for deployment

### 4. Verify Build Output

After a successful build, you should see:

```
RCore/rcore-free/build/libs/RCore-unspecified-free.jar
RCore/rcore-premium/build/libs/RCore-unspecified-premium.jar
RDQ/rdq-free/build/libs/RDQ-unspecified-free.jar
RDQ/rdq-premium/build/libs/RDQ-unspecified-premium.jar
```

## Common Issues

### Issue: "Could not find com.raindropcentral.core:rcore-free:2.0.0"

**Solution:** You skipped step 2! Run `.\gradlew publishDependencies` first.

### Issue: "JAVA_HOME not set"

**Solution:** Set the `JAVA_HOME` environment variable:

**Windows:**
```powershell
# Temporary (current session only)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# Permanent (requires admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "Machine")
```

**Linux/Mac:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### Issue: Build fails with "Execution failed for task ':RDQ:rdq-common:compileJava'"

**Solution:** This usually means dependencies weren't published. Run:
```bash
.\gradlew clean publishDependencies buildAll
```

### Issue: "gradlew: Permission denied" (Linux/Mac)

**Solution:** Make the Gradle wrapper executable:
```bash
chmod +x gradlew
```

## Development Workflow

### Making Changes

1. Make your code changes
2. Build affected modules:
   ```bash
   # Build specific module
   .\gradlew :RDQ:rdq-common:build
   
   # Build all RDQ variants
   .\gradlew :RDQ:buildAll
   ```

### Testing

```bash
# Run all tests
.\gradlew test

# Run tests for specific module
.\gradlew :RDQ:rdq-common:test
```

### Clean Build

If you encounter strange build issues:

```bash
# Clean and rebuild everything
.\gradlew clean buildAll

# Or using the build script
.\build.ps1 -Clean
```

## IDE Setup

### IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. **File → Open** → Select the `RaindropCentral` directory
3. IntelliJ will automatically detect the Gradle project
4. Wait for Gradle sync to complete
5. **Important:** After sync, run `publishDependencies`:
   - Open Gradle tool window (View → Tool Windows → Gradle)
   - Navigate to: `RaindropPlugins → Tasks → build → publishDependencies`
   - Double-click to run

### Eclipse

1. Install Buildship Gradle plugin
2. **File → Import → Gradle → Existing Gradle Project**
3. Select the `RaindropCentral` directory
4. After import, open terminal and run: `.\gradlew publishDependencies`

### VS Code

1. Install "Extension Pack for Java"
2. Open the `RaindropCentral` folder
3. VS Code will detect the Gradle project
4. Open integrated terminal and run: `.\gradlew publishDependencies`

## Next Steps

- Read the main [README.md](../README.md) for project overview
- Check [TESTING.md](TESTING.md) for testing guidelines
- Review [.build/README.md](../.build/README.md) for advanced build options
- Explore module-specific documentation in each subproject

## Getting Help

If you encounter issues not covered here:

1. Check if dependencies are published: `ls ~/.m2/repository/com/raindropcentral/`
2. Try a clean build: `.\gradlew clean publishDependencies buildAll`
3. Check Gradle logs: `.\gradlew buildAll --info`
4. Contact the development team

## Quick Reference

```bash
# First-time setup
.\gradlew publishDependencies

# Build everything
.\gradlew buildAll

# Clean build
.\gradlew clean buildAll

# Build specific module
.\gradlew :RDQ:rdq-free:build

# Run tests
.\gradlew test

# Deploy to server (premium editions)
.\gradlew deployPremium -PpluginDir="C:\Server\plugins"
```
