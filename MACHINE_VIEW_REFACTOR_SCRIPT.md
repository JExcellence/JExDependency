# Machine View Refactoring Script

## Pattern to Apply to All Machine Views

### 1. Constructor - Change to no-arg
```java
// OLD
public MachineRecipeView(IMachineService machineService, Machine machine, Class<? extends View> parentClazz) {
    super(parentClazz);
    this.machineService = machineService;
    this.machine = machine;
}

// NEW
public MachineRecipeView() {
    super();
}
```

### 2. Fields - Use State
```java
// OLD
private final IMachineService machineService;
private final Machine machine;

// NEW
private final State<IMachineService> machineService = initialState("machineService");
private final State<Machine> machine = initialState("machine");
```

### 3. Imports - Add State
```java
import me.devnatan.inventoryframework.state.State;
```

### 4. Usage - Add context parameter
```java
// In render methods: use (render)
machine.get(render).someMethod()
machineService.get(render).someMethod()

// In click handlers: use (click)
machine.get(click).someMethod()
machineService.get(click).someMethod()
```

## Files to Refactor

1. ✅ MachineMainView.java - DONE
2. ⏳ MachineRecipeView.java
3. ⏳ MachineStorageView.java  
4. ⏳ MachineTrustView.java
5. ⏳ MachineUpgradeView.java

## Search & Replace Pattern

For each file, find all occurrences of:
- `machine.` → `machine.get(context).`
- `machineService.` → `machineService.get(context).`

Where context is:
- `render` in onFirstRender/onRender/onPaginatedRender methods
- `click` in onClick handlers
