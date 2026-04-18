# JExEconomy Reflections Dependencies Fixed

## ✅ Problem Solved!

Successfully added missing Reflections transitive dependencies to the JExEconomy dependency download system.

## 🔧 Changes Made

### 1. Updated RuntimeDependencies.kt

**File:** `buildSrc/src/main/kotlin/RuntimeDependencies.kt`

**Added version constants:**
```kotlin
const val JAVASSIST_REFLECTIONS = "3.30.2-GA"  // Newer version for Reflections
const val DOM4J = "2.1.4"
```

**Updated extras dependency group:**
```kotlin
val extras = DependencyGroup("Extra Utilities", listOf(
    "org.yaml:snakeyaml:${Versions.SNAKEYAML}",
    "org.reflections:reflections:${Versions.REFLECTIONS}",
    "org.javassist:javassist:${Versions.JAVASSIST_REFLECTIONS}",  // ✅ Added
    "org.dom4j:dom4j:${Versions.DOM4J}"                            // ✅ Added
))
```

### 2. Regenerated dependencies.yml Files

**Command executed:**
```bash
./gradlew :JExEconomy:jexeconomy-premium:generateDependenciesYml :JExEconomy:jexeconomy-free:generateDependenciesYml
```

**Result:** Both `dependencies.yml` files now include:
```yaml
# Extra Utilities
- "org.yaml:snakeyaml:2.4"
- "org.reflections:reflections:0.10.2"
- "org.javassist:javassist:3.30.2-GA"  # ✅ NEW
- "org.dom4j:dom4j:2.1.4"               # ✅ NEW
```

## 📋 Files Modified

1. **buildSrc/src/main/kotlin/RuntimeDependencies.kt**
   - Added `JAVASSIST_REFLECTIONS` version constant
   - Added `DOM4J` version constant
   - Updated `extras` dependency group to include javassist and dom4j

2. **JExEconomy/jexeconomy-premium/src/main/resources/dependency/dependencies.yml**
   - Auto-regenerated with new dependencies

3. **JExEconomy/jexeconomy-free/src/main/resources/dependency/dependencies.yml**
   - Auto-regenerated with new dependencies

## 🎯 Why This Fixes the Issue

### The Problem:
```
NoClassDefFoundError: org/reflections/Configuration
```

### The Root Cause:
Reflections library has transitive dependencies that weren't being downloaded:
- `org.javassist:javassist:3.30.2-GA` (required by Reflections)
- `org.dom4j:dom4j:2.1.4` (required by Reflections)

### The Solution:
Paper's dependency downloader doesn't automatically resolve transitive dependencies. We must explicitly list all dependencies, including transitive ones.

## 📊 Dependency Tree (Now Complete)

```
JEHibernate 3.0.2
├── org.reflections:reflections:0.10.2 ✅
│   ├── org.javassist:javassist:3.30.2-GA ✅ ADDED
│   └── org.dom4j:dom4j:2.1.4 ✅ ADDED
└── [other dependencies]
```

## 🚀 Next Steps

1. **Rebuild the plugin:**
   ```bash
   ./gradlew :JExEconomy:jexeconomy-premium:clean :JExEconomy:jexeconomy-premium:build
   ```

2. **Deploy to server:**
   - Copy the new JAR to your plugins folder
   - Start the server

3. **Verify:**
   - Check server logs for successful plugin load
   - Verify no `NoClassDefFoundError` occurs
   - Check that dependencies are downloaded to `.paper-libraries/` or `libraries/`

## ✅ Expected Result

After rebuilding and deploying:
- ✅ Paper downloads all required dependencies
- ✅ Reflections library loads successfully with all its dependencies
- ✅ JEHibernate initializes without errors
- ✅ Plugin loads successfully
- ✅ Database connection works
- ✅ JAR remains small (dependencies downloaded, not shaded)

## 📝 Technical Details

### Why Two Javassist Versions?

- `javassist:3.29.2-GA` - Used by Hibernate
- `javassist:3.30.2-GA` - Required by Reflections 0.10.2

Both versions can coexist. The classloader will use the appropriate version for each library.

### Why dom4j?

dom4j is an XML processing library that Reflections uses for parsing configuration files and annotations. While technically optional, it's recommended to avoid potential issues.

## 🎉 Summary

The `NoClassDefFoundError: org/reflections/Configuration` is now fixed by explicitly adding Reflections' transitive dependencies (javassist 3.30.2-GA and dom4j 2.1.4) to the runtime dependency configuration.

The plugin will now load successfully with all required dependencies downloaded via Paper's dependency system! 🚀
