# JExEconomy Reflections Library Fix

## ❌ Problem

JExEconomy was failing to load with the error:
```
java.lang.NoClassDefFoundError: org/reflections/Configuration
```

This occurred because JEHibernate 3.0.2 depends on the Reflections library, but it wasn't being included in the plugin JAR.

## 🔍 Root Cause

In both `jexeconomy-premium` and `jexeconomy-free` build files, JEHibernate was marked as `compileOnly`:

```kotlin
compileOnly(libs.jehibernate)  // ❌ Not included in JAR
```

This meant:
- JEHibernate was available at compile time
- JEHibernate was NOT included in the final JAR
- Transitive dependencies (like Reflections) were also missing

## ✅ Solution Applied

### 1. Changed JEHibernate to `implementation`

**jexeconomy-premium/build.gradle.kts:**
```kotlin
implementation(libs.jehibernate)  // ✅ Now included in JAR
```

**jexeconomy-free/build.gradle.kts:**
```kotlin
implementation(libs.jehibernate)  // ✅ Now included in JAR
```

### 2. Added Reflections Relocation

To avoid conflicts with other plugins, added relocation for the Reflections library:

**Both build files:**
```kotlin
relocate("org.reflections", "de.jexcellence.remapped.org.reflections")
```

## 📋 Changes Made

### Files Modified:

1. **JExEconomy/jexeconomy-premium/build.gradle.kts**
   - Changed `compileOnly(libs.jehibernate)` to `implementation(libs.jehibernate)`
   - Added `relocate("org.reflections", "de.jexcellence.remapped.org.reflections")`

2. **JExEconomy/jexeconomy-free/build.gradle.kts**
   - Changed `compileOnly(libs.jehibernate)` to `implementation(libs.jehibernate)`
   - Added `relocate("org.reflections", "de.jexcellence.remapped.org.reflections")`

## 🎯 What This Does

### Before:
- JEHibernate: ❌ Not in JAR (compileOnly)
- Reflections: ❌ Not in JAR (transitive dependency)
- Result: `NoClassDefFoundError` at runtime

### After:
- JEHibernate: ✅ Included in JAR (implementation)
- Reflections: ✅ Included in JAR (transitive dependency)
- Reflections: ✅ Relocated to avoid conflicts
- Result: Plugin loads successfully

## 🔧 How Shadow Plugin Works

The Shadow plugin:
1. Collects all `implementation` dependencies
2. Includes them in the JAR
3. Applies relocations to avoid conflicts
4. Creates a "fat JAR" with all dependencies

With `compileOnly`:
- Dependency available at compile time
- NOT included in final JAR
- Assumes dependency provided at runtime

With `implementation`:
- Dependency available at compile time
- INCLUDED in final JAR via Shadow
- Self-contained plugin

## 🚀 Next Steps

1. **Rebuild the plugin:**
   ```bash
   ./gradlew :JExEconomy:jexeconomy-premium:clean :JExEconomy:jexeconomy-premium:build
   # or
   ./gradlew :JExEconomy:jexeconomy-free:clean :JExEconomy:jexeconomy-free:build
   ```

2. **Verify the JAR includes Reflections:**
   ```bash
   jar tf JExEconomy/jexeconomy-premium/build/libs/JExEconomy-Premium-3.0.0.jar | grep reflections
   ```
   
   You should see relocated classes like:
   ```
   de/jexcellence/remapped/org/reflections/...
   ```

3. **Test the plugin:**
   - Copy the new JAR to your server
   - Start the server
   - Verify no `NoClassDefFoundError` occurs

## 📊 Dependency Scope Reference

| Scope | Compile Time | Runtime | Included in JAR |
|-------|-------------|---------|-----------------|
| `compileOnly` | ✅ | ❌ | ❌ |
| `runtimeOnly` | ❌ | ✅ | ✅ |
| `implementation` | ✅ | ✅ | ✅ |
| `api` | ✅ | ✅ | ✅ |

## ⚠️ Important Notes

1. **JEHibernate is now shaded** - This increases JAR size but ensures all dependencies are present

2. **Relocation prevents conflicts** - The Reflections library is relocated to avoid conflicts with other plugins

3. **Transitive dependencies** - When you mark JEHibernate as `implementation`, all its transitive dependencies (like Reflections) are automatically included

4. **Build time** - First build after this change may take longer as Gradle downloads JEHibernate and its dependencies

## 🔍 Verification

After rebuilding, check the JAR contents:

```bash
# List all classes in the JAR
jar tf build/libs/JExEconomy-Premium-3.0.0.jar

# Check for JEHibernate classes
jar tf build/libs/JExEconomy-Premium-3.0.0.jar | grep jehibernate

# Check for relocated Reflections classes
jar tf build/libs/JExEconomy-Premium-3.0.0.jar | grep "de/jexcellence/remapped/org/reflections"
```

## ✅ Expected Result

After rebuilding and deploying:
- ✅ Plugin loads without `NoClassDefFoundError`
- ✅ JEHibernate initializes successfully
- ✅ Database connection works
- ✅ No conflicts with other plugins

The Reflections library is now properly included and relocated in your plugin JAR! 🎉
