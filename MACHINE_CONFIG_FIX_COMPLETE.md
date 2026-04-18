# Machine Configuration Fix - Complete

## Problem
ConfigMapper was failing to load machine configurations with error:
```
java.lang.IllegalStateException: Unsupported type specified: interface java.util.Map 
(at value for key=level-1 of a map) (at path 'upgrades.efficiency.requirements')
```

## Root Cause
The `FabricatorSection.UpgradeDefinitionSection.requirements` field was defined as:
```java
private Map<String, Map<String, Object>> requirements;
```

ConfigMapper cannot handle `Map<String, Object>` as a value type - it requires concrete config section classes like `BaseRequirementSection`.

## Solution Applied

### 1. Fixed FabricatorSection.java
Changed the requirements field type:
```java
// Before
private Map<String, Map<String, Object>> requirements;

// After  
private Map<String, BaseRequirementSection> requirements;
```

Updated the getter method:
```java
// Before
public Map<String, Object> getRequirements(int level)

// After
public BaseRequirementSection getRequirements(int level)
```

Added import:
```java
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
```

### 2. Fixed UpgradeComponent.java
Updated method signature to match:
```java
// Before
@NotNull
public Map<String, Object> getNextLevelRequirements(final @NotNull EUpgradeType upgradeType)

// After
@Nullable
public BaseRequirementSection getNextLevelRequirements(final @NotNull EUpgradeType upgradeType)
```

Changed return values from `new HashMap<>()` to `null` for consistency with nullable return type.

Added imports:
```java
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import org.jetbrains.annotations.Nullable;
```

### 3. Fixed RDQ.java - MachineInteractListener Registration
Added missing `viewFrame` parameter to listener constructor:
```java
// Before
new MachineInteractListener(
    plugin,
    machineService,
    machineManager,
    structureDetector
)

// After
new MachineInteractListener(
    plugin,
    machineService,
    machineManager,
    structureDetector,
    this.viewFrame
)
```

## Build Status
✅ Build successful with only deprecation warnings (unrelated to this fix)
✅ JAR file generated: `RDQ/rdq-premium/build/libs/RDQ-6.0.0-Alpha-Build-14-Premium.jar`

## Deployment Instructions
1. **Stop your Minecraft server**
2. **Locate the new JAR**: `RDQ/rdq-premium/build/libs/RDQ-6.0.0-Alpha-Build-14-Premium.jar`
3. **Replace the old JAR** in your server's `plugins/` directory
4. **Start the server**

The machine system should now initialize correctly without errors.

## Files Modified
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/config/FabricatorSection.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/component/UpgradeComponent.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java`

## Pattern for Future Config Sections
When defining maps in config sections for ConfigMapper:
- ✅ Use `Map<String, ConcreteConfigSection>` 
- ❌ Don't use `Map<String, Object>` or `Map<String, Map<String, Object>>`

Example:
```java
// Good
private Map<String, BaseRequirementSection> requirements;

// Bad
private Map<String, Object> requirements;
private Map<String, Map<String, Object>> requirements;
```

## Expected Server Startup
After deploying the new JAR, you should see:
```
[RDQ] Initializing machine system...
[RDQ] Machine configurations loaded from files
[RDQ] Machine cache initialized
[RDQ] Machine registry initialized
[RDQ] Machine factory initialized
[RDQ] Machine manager initialized
[RDQ] Machine event listeners registered
[RDQ] Machine auto-save task started
[RDQ] Machine system initialized successfully!
```
