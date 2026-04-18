# Machine Listeners Refactor - Complete

## Overview
Refactored all machine system listeners to use dependency injection pattern with auto-registration, simplifying the codebase and making it easier to maintain.

## Changes Made

### 1. Created BaseMachineListener
New base class that provides:
- Auto-registration with Bukkit plugin manager
- Single constructor parameter (RDQ instance)
- Access to all dependencies via RDQ getters

**File**: `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/BaseMachineListener.java`

```java
public abstract class BaseMachineListener implements Listener {
    protected final RDQ rdq;
    protected final JavaPlugin plugin;

    protected BaseMachineListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.plugin = rdq.getPlugin();
        
        // Auto-register this listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
```

### 2. Added Machine System Fields to RDQ
Added fields for machine system components:
```java
private MachineService machineService;
private StructureDetector structureDetector;
private MachineItemFactory machineItemFactory;
private MachineSystemSection machineSystemConfig;
private FabricatorSection fabricatorConfig;
```

These are automatically accessible via Lombok's `@Getter` annotation.

### 3. Refactored Machine Listeners

#### MachineBlockListener
**Before**:
```java
public MachineBlockListener(
    JavaPlugin plugin,
    IMachineService machineService,
    MachineManager machineManager,
    StructureDetector structureDetector,
    MachineSystemSection config,
    MachineItemFactory machineItemFactory
)
```

**After**:
```java
public MachineBlockListener(@NotNull final RDQ rdq) {
    super(rdq);
    this.machineService = rdq.getMachineService();
    this.machineManager = rdq.getMachineManager();
    this.structureDetector = rdq.getStructureDetector();
    this.config = rdq.getMachineSystemConfig();
    this.machineItemFactory = rdq.getMachineItemFactory();
}
```

#### MachineInteractListener
**Before**:
```java
public MachineInteractListener(
    JavaPlugin plugin,
    IMachineService machineService,
    MachineManager machineManager,
    StructureDetector structureDetector,
    ViewFrame viewFrame
)
```

**After**:
```java
public MachineInteractListener(@NotNull final RDQ rdq) {
    super(rdq);
    this.machineService = rdq.getMachineService();
    this.machineManager = rdq.getMachineManager();
    this.structureDetector = rdq.getStructureDetector();
    this.viewFrame = rdq.getViewFrame();
}
```

#### MachineChunkListener
**Before**:
```java
public MachineChunkListener(
    JavaPlugin plugin,
    MachineManager machineManager,
    MachineRepository machineRepository,
    MachineCache machineCache
)
```

**After**:
```java
public MachineChunkListener(@NotNull final RDQ rdq) {
    super(rdq);
    this.machineManager = rdq.getMachineManager();
    this.machineRepository = rdq.getMachineRepository();
    this.machineCache = rdq.getMachineCache();
}
```

### 4. Simplified RDQ Initialization
**Before** - Manual registration with many parameters:
```java
private void registerMachineListeners(
    MachineSystemSection systemConfig,
    FabricatorSection fabricatorConfig
) {
    // Create dependencies locally
    MachineFactory factory = new MachineFactory(...);
    MachineService machineService = new MachineService(...);
    StructureDetector structureDetector = new StructureDetector(...);
    MachineItemFactory machineItemFactory = new MachineItemFactory(...);
    
    // Register each listener manually
    pluginManager.registerEvents(new MachineBlockListener(...), plugin);
    pluginManager.registerEvents(new MachineInteractListener(...), plugin);
    pluginManager.registerEvents(new MachineChunkListener(...), plugin);
}
```

**After** - Auto-registration with single line per listener:
```java
// Initialize dependencies as fields
this.machineService = new MachineService(...);
this.structureDetector = new StructureDetector(...);
this.machineItemFactory = new MachineItemFactory(this.plugin);

// Auto-register listeners (registration happens in constructor)
new MachineBlockListener(this);
new MachineInteractListener(this);
new MachineChunkListener(this);
```

## Benefits

### 1. Simplified Constructor Signatures
- All listeners now have a single constructor parameter: `RDQ rdq`
- No need to manually pass multiple dependencies
- Easier to add new dependencies without changing all listener instantiations

### 2. Auto-Registration
- Listeners automatically register themselves when instantiated
- No need for manual `pluginManager.registerEvents()` calls
- Reduces boilerplate code

### 3. Centralized Dependency Management
- All dependencies are managed in RDQ class
- Easy to see what's available via getters
- Consistent access pattern across all listeners

### 4. Easier Testing
- Can mock RDQ instance for unit tests
- All dependencies accessible through single interface
- Simpler test setup

### 5. Better Maintainability
- Adding new listeners is trivial: extend BaseMachineListener
- Changing dependencies doesn't require updating multiple constructors
- Clear separation of concerns

## Build Status
✅ Build successful with only deprecation warnings (unrelated to this refactor)
✅ JAR file generated: `RDQ/rdq-premium/build/libs/RDQ-6.0.0-Alpha-Build-14-Premium.jar`

## Files Modified
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/BaseMachineListener.java` (new)
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/MachineBlockListener.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/MachineInteractListener.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/MachineChunkListener.java`

## Pattern for Future Listeners
When creating new machine listeners:

```java
public class MyNewListener extends BaseMachineListener {
    
    private final SomeDependency dependency;
    
    public MyNewListener(@NotNull final RDQ rdq) {
        super(rdq);  // Auto-registers the listener
        this.dependency = rdq.getSomeDependency();
    }
    
    @EventHandler
    public void onSomeEvent(SomeEvent event) {
        // Handle event
    }
}

// In RDQ.java initialization:
new MyNewListener(this);  // That's it!
```

## Deployment
The new JAR is ready at: `RDQ/rdq-premium/build/libs/RDQ-6.0.0-Alpha-Build-14-Premium.jar`

Deploy it to your server and the machine system will work with the new simplified listener architecture!
