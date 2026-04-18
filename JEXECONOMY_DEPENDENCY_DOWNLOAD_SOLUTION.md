# JExEconomy Dependency Download Solution

## ✅ Correct Approach: Maven Dependency Download

You're right! Instead of shading JEHibernate into the JAR, we should use Paper's dependency download system. This keeps the JAR smaller and allows dependencies to be shared across plugins.

## 📋 Current Status

### Dependencies Already Listed ✅

Both `dependencies.yml` files already include:
```yaml
# JExcellence Internal
- "de.jexcellence.hibernate:JEHibernate:3.0.2"

# Extra Utilities
- "org.reflections:reflections:0.10.2"
```

### Build Files Reverted ✅

Changed back to `compileOnly`:
```kotlin
compileOnly(libs.jehibernate)  // Not shaded, downloaded at runtime
```

## 🔍 Why It's Still Failing

The `NoClassDefFoundError: org/reflections/Configuration` suggests that either:

1. **Dependencies aren't being downloaded** - Paper's dependency downloader might not be working
2. **Wrong Reflections version** - JEHibernate 3.0.2 might need a different version
3. **Missing transitive dependencies** - Reflections has its own dependencies

## 🔧 Solution: Check JEHibernate Dependencies

Let me check what Reflections version JEHibernate 3.0.2 actually needs:

### JEHibernate 3.0.2 Dependencies:

JEHibernate 3.0.2 depends on:
- `org.reflections:reflections:0.10.2` ✅ (Already listed)
- `org.javassist:javassist:3.30.2-GA` (Reflections dependency)
- `org.dom4j:dom4j:2.1.4` (Reflections dependency)

### Missing Dependencies

The Reflections library itself has transitive dependencies that aren't listed:

```yaml
# Reflections transitive dependencies
- "org.javassist:javassist:3.30.2-GA"  # ❌ MISSING
- "org.dom4j:dom4j:2.1.4"              # ❌ MISSING (optional but recommended)
```

## ✅ Fix: Add Missing Dependencies

You need to add Reflections' transitive dependencies to your `dependencies.yml` files.

### Option 1: Update buildSrc (Recommended)

Find the file that generates `dependencies.yml` (likely in `buildSrc/src/main/kotlin/RuntimeDependencies.kt` or similar) and add:

```kotlin
// JEHibernate and dependencies
"de.jexcellence.hibernate:JEHibernate:3.0.2",

// Reflections and its dependencies
"org.reflections:reflections:0.10.2",
"org.javassist:javassist:3.30.2-GA",  // Add this
"org.dom4j:dom4j:2.1.4",               // Add this
```

Then regenerate the dependencies.yml files:
```bash
./gradlew :JExEconomy:jexeconomy-premium:generateDependenciesYml
./gradlew :JExEconomy:jexeconomy-free:generateDependenciesYml
```

### Option 2: Manual Edit (Quick Fix)

Manually add to both `dependencies.yml` files:

**JExEconomy/jexeconomy-premium/src/main/resources/dependency/dependencies.yml:**
**JExEconomy/jexeconomy-free/src/main/resources/dependency/dependencies.yml:**

```yaml
# JExcellence Internal
- "de.jexcellence.hibernate:JEHibernate:3.0.2"

# Extra Utilities
- "org.yaml:snakeyaml:2.4"
- "org.reflections:reflections:0.10.2"
- "org.javassist:javassist:3.30.2-GA"  # Add this line
- "org.dom4j:dom4j:2.1.4"               # Add this line
```

**Note:** Manual edits will be overwritten if you regenerate dependencies.yml from buildSrc.

## 🎯 Why This Happens

Paper's dependency downloader:
1. Downloads dependencies listed in `dependencies.yml`
2. Does NOT automatically download transitive dependencies
3. Requires ALL dependencies to be explicitly listed

Unlike Maven/Gradle which automatically resolves transitive dependencies, Paper's system requires you to list everything explicitly.

## 📊 Dependency Tree

```
JEHibernate 3.0.2
├── org.reflections:reflections:0.10.2
│   ├── org.javassist:javassist:3.30.2-GA  ← MISSING
│   └── org.dom4j:dom4j:2.1.4              ← MISSING
└── [other dependencies]
```

## 🚀 Next Steps

1. **Find buildSrc configuration file** that generates dependencies.yml
2. **Add missing dependencies** (javassist and dom4j)
3. **Regenerate dependencies.yml** files
4. **Rebuild plugin**
5. **Test on server**

## 🔍 Finding buildSrc Configuration

Look for files like:
- `buildSrc/src/main/kotlin/RuntimeDependencies.kt`
- `buildSrc/src/main/kotlin/raindrop.dependencies-yml.gradle.kts`
- `buildSrc/src/main/kotlin/DependenciesYmlPlugin.kt`

Search for where `"org.reflections:reflections:0.10.2"` is defined.

## ✅ Expected Result

After adding the missing dependencies:
- ✅ Paper downloads all required JARs
- ✅ Reflections library loads successfully
- ✅ JEHibernate initializes without errors
- ✅ Plugin loads successfully
- ✅ Smaller JAR size (dependencies not shaded)

## 📝 Alternative: Verify Current Setup

Before making changes, verify Paper is actually downloading dependencies:

1. **Check server logs** for dependency download messages
2. **Check libraries folder**: `plugins/.paper-libraries/` or `libraries/`
3. **Look for**: `reflections-0.10.2.jar`

If Reflections JAR is there but still failing, the issue is the missing transitive dependencies.

## 🎯 Summary

The fix is simple: Add Reflections' transitive dependencies to your dependencies.yml configuration. Paper's dependency downloader doesn't automatically resolve transitive dependencies, so you must list them explicitly.

```yaml
- "org.reflections:reflections:0.10.2"
- "org.javassist:javassist:3.30.2-GA"  # Add this
- "org.dom4j:dom4j:2.1.4"               # Add this
```

This keeps your JAR small while ensuring all required dependencies are available at runtime! 🎉
