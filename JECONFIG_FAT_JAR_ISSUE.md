# JEConfig Fat JAR Migration Issue

## Problem

Attempted to migrate from three separate JEConfig dependencies to a single fat JAR, but the fat JAR doesn't contain the expected classes.

## What We Tried

### Migration Attempt
```toml
# Tried to replace:
jeconfig-evaluable = "1.0.1"
jeconfig-gpeee = "1.0.1"
jeconfig-mapper = "1.0.1"

# With:
jeconfig = "1.0.1"  # Fat JAR: de.jexcellence.config:JEConfig:1.0.1
```

## Error Encountered

```
Package de.jexcellence.evaluable ist nicht vorhanden
import de.jexcellence.evaluable.CommandUpdater;
```

### Missing Classes
The JEConfig fat JAR (version 1.0.1) does not contain:
- `de.jexcellence.evaluable.CommandUpdater`
- `de.jexcellence.evaluable.ItemBuilder`
- `de.jexcellence.configmapper.sections.*`
- Other classes from the individual JARs

## Root Cause

The `de.jexcellence.config:JEConfig:1.0.1` fat JAR either:
1. **Doesn't exist** in the Maven repository
2. **Wasn't built correctly** - missing classes from Evaluable
3. **Uses different package names** - classes were relocated
4. **Is a different artifact** - not actually a fat JAR

## Current Status

✅ **REVERTED** - Back to using three separate dependencies:
- `de.jexcellence.config:Evaluable:1.0.1`
- `de.jexcellence.config:GPEEE:1.0.1`
- `de.jexcellence.config:ConfigMapper:1.0.1`

## Solution Options

### Option 1: Fix the Fat JAR (Recommended)
The JEConfig fat JAR needs to be rebuilt to include all classes from the three libraries:

**Required contents:**
```
de.jexcellence.evaluable.*     (from Evaluable)
de.jexcellence.gpeee.*         (from GPEEE)
de.jexcellence.configmapper.*  (from ConfigMapper)
```

**Build configuration needed:**
```gradle
// In JEConfig fat JAR build
shadowJar {
    // Include all three libraries
    configurations = [project.configurations.runtimeClasspath]
    
    // Don't relocate packages
    relocate 'de.jexcellence', 'de.jexcellence'  // Keep original packages
}
```

### Option 2: Use Correct Artifact Name
If the fat JAR exists under a different name or version:
```toml
jeconfig = { module = "de.jexcellence.config:JEConfig-all", version = "1.0.1" }
# or
jeconfig = { module = "de.jexcellence.config:JEConfig", version = "1.0.1-all" }
```

### Option 3: Keep Using Individual JARs (Current)
Continue using the three separate dependencies via the bundle:
```kotlin
implementation(libs.bundles.jeconfig)
```

## Verification Steps

Before attempting migration again:

1. **Check Maven Repository**
   ```bash
   # Verify artifact exists and contains classes
   mvn dependency:get -Dartifact=de.jexcellence.config:JEConfig:1.0.1
   ```

2. **Inspect JAR Contents**
   ```bash
   jar tf JEConfig-1.0.1.jar | grep -E "(CommandUpdater|ItemBuilder|ConfigMapper)"
   ```

3. **Test Compilation**
   ```bash
   ./gradlew :RPlatform:compileJava
   ```

## Files Affected

### Reverted Changes
- ✅ `gradle/libs.versions.toml` - Back to three separate versions
- ✅ **40+ build.gradle.kts files** - Back to `libs.bundles.jeconfig`

### No Code Changes Needed
The revert was purely dependency configuration - no Java code was affected.

## Next Steps

1. **Contact JExcellence** - Verify if JEConfig fat JAR exists and is properly built
2. **Check Documentation** - Look for official migration guide
3. **Test Locally** - Build fat JAR locally to verify it works
4. **Update When Ready** - Once fat JAR is confirmed working, retry migration

## Temporary Workaround

The current setup with three separate JARs works fine. The migration to a fat JAR is an optimization, not a requirement.

**Current working configuration:**
```kotlin
dependencies {
    implementation(libs.bundles.jeconfig) {
        // This includes:
        // - de.jexcellence.config:Evaluable:1.0.1
        // - de.jexcellence.config:GPEEE:1.0.1
        // - de.jexcellence.config:ConfigMapper:1.0.1
    }
}
```

## Lessons Learned

1. Always verify fat JAR contents before migration
2. Test compilation after dependency changes
3. Have rollback plan ready
4. Document expected package structure
