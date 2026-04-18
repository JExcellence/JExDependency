# Machine System Compilation Fixes

## Summary
The machine system has 89 compilation errors that need to be fixed. The main issues are:

1. Config section constructors need `EvaluationEnvironmentBuilder` parameter
2. MachineFactory constructor signature mismatch  
3. MachineService constructor signature mismatch
4. StructureDetector constructor needs Function parameter
5. MachineItemFactory needs JavaPlugin parameter
6. Various method signature mismatches in components and views
7. Missing LOGGER field in MachineManager
8. CachedRepository method signature changes
9. View API changes for item builders

## Critical Fixes Needed

### 1. RDQ.java - Config Section Initialization (Lines 587-589)
```java
// BEFORE:
new com.raindropcentral.rdq.machine.config.MachineSystemSection();
new com.raindropcentral.rdq.machine.config.FabricatorSection();

// AFTER:
new com.raindropcentral.rdq.machine.config.MachineSystemSection(new de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder());
new com.raindropcentral.rdq.machine.config.FabricatorSection(new de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder());
```

### 2. RDQ.java - MachineFactory Constructor (Line 603)
```java
// BEFORE:
MachineFactory factory = new MachineFactory(
    plugin,
    machineRepository,
    machineCache,
    fabricatorConfig
);

// AFTER:
MachineFactory factory = new MachineFactory(
    machineRepository,
    fabricatorConfig
);
```

### 3. RDQ.java - MachineService Constructor (Line 650)
```java
// BEFORE:
MachineService machineService = new MachineService(
    machineManager,
    machineRepository,
    machineCache
);

// AFTER:
MachineService machineService = new MachineService(
    machineManager,
    factory,
    machineCache,
    machineRepository
);
```

### 4. RDQ.java - StructureDetector Constructor (Line 658)
```java
// BEFORE:
new com.raindropcentral.rdq.machine.structure.StructureDetector(fabricatorConfig);

// AFTER:
new com.raindropcentral.rdq.machine.structure.StructureDetector(
    type -> fabricatorConfig.getStructure().toMultiBlockStructure()
);
```

### 5. RDQ.java - MachineItemFactory Constructor (Line 662)
```java
// BEFORE:
new com.raindropcentral.rdq.machine.item.MachineItemFactory();

// AFTER:
new com.raindropcentral.rdq.machine.item.MachineItemFactory(plugin);
```

### 6. MachineManager.java - Add LOGGER Field
```java
// Add at top of class:
private static final Logger LOGGER = Logger.getLogger(MachineManager.class.getName());
```

### 7. MachineManager.java - RecipeComponent Constructor (Line 204)
```java
// BEFORE:
new RecipeComponent(machine),

// AFTER:
new RecipeComponent(machine, fabricatorConfig),
```

### 8. MachineCache.java - Fix CachedRepository Method Calls
- Replace `cache()` with direct cache map operations
- Replace `getAllCached()` with cache map values
- Replace `getCached()` with cache map get
- Replace `evict()` with cache map remove
- Fix `getCacheSize()` return type to `long`
- Replace `clearCache()` with cache map clear

### 9. Config Sections - Fix afterParsing() Override
Remove `@Override` annotation and add `List<Field>` parameter to match parent signature

### 10. MachineFactory.java - Fix thenCompose Return Types
The lambda should return `CompletableFuture.completedFuture(machine)` instead of just `machine`

### 11. Views - Fix withItem() Lambda Syntax
The inventory framework changed - need to check correct API usage

### 12. MachineCommand.java - Fix RDQ Type Issues
RDQ needs to extend JavaPlugin or provide getLogger() method

## Files Requiring Changes

1. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java
2. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineManager.java
3. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineFactory.java
4. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineService.java
5. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/repository/MachineCache.java
6. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/config/MachineSystemSection.java
7. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/config/FabricatorSection.java
8. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/config/MachineStructureSection.java
9. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/view/*.java (all views)
10. RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/machine/MachineCommand.java

## Next Steps

Due to the large number of interconnected changes, I recommend:
1. Review the existing similar code patterns in RDQ
2. Apply fixes systematically starting with foundational classes
3. Recompile after each major fix to verify progress
4. Test the machine system after all fixes are applied
