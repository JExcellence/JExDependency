# Machine System Async Performance Fix

## Issue
Server was experiencing lag despite machine system claiming to be async. Investigation revealed blocking `.join()` calls on the main thread.

## Root Cause
1. **MachineService.getMachine()** - Used `.join()` inside a `CompletableFuture.supplyAsync()`, blocking the async thread pool
2. **MachineCache.autoSaveAll()** - Sequential saves with `.join()` instead of parallel batch saves

## Fixes Applied

### 1. MachineService.getMachine() - Removed Blocking Call
**Before:**
```java
public CompletableFuture<Object> getMachine(Long machineId) {
    return CompletableFuture.supplyAsync(() -> {
        // ... checks ...
        return repository.findByIdAsync(machineId)
            .thenApply(opt -> (Object) opt.orElse(null))
            .join();  // ❌ BLOCKS THREAD!
    });
}
```

**After:**
```java
public CompletableFuture<Object> getMachine(Long machineId) {
    // Check active registry (fast, synchronous)
    Optional<Machine> machineOpt = manager.getActiveMachine(machineId);
    if (machineOpt.isPresent()) {
        return CompletableFuture.completedFuture(machineOpt.get());
    }

    // Check cache (fast, synchronous)
    Machine machine = cache.getCachedByKey().get(machineId);
    if (machine != null) {
        return CompletableFuture.completedFuture(machine);
    }

    // Load from database (truly async, no blocking!)
    return repository.findByIdAsync(machineId)
        .thenApply(opt -> (Object) opt.orElse(null));
}
```

### 2. MachineCache.autoSaveAll() - Parallel Batch Saves
**Before:**
```java
public CompletableFuture<Integer> autoSaveAll() {
    return CompletableFuture.supplyAsync(() -> {
        int savedCount = 0;
        for (Long machineId : toSave) {
            updateAsync(machine).join();  // Sequential, blocking
            savedCount++;
        }
        return savedCount;
    });
}
```

**After:**
```java
public CompletableFuture<Integer> autoSaveAll() {
    // Create save futures for all machines in parallel
    List<CompletableFuture<Boolean>> saveFutures = new ArrayList<>();
    for (Long machineId : toSave) {
        Machine machine = getCachedByKey().get(machineId);
        if (machine != null) {
            CompletableFuture<Boolean> saveFuture = updateAsync(machine)
                .thenApply(updated -> {
                    dirtyMachines.remove(machineId);
                    return true;
                })
                .exceptionally(ex -> {
                    LOGGER.warning("Auto-save failed: " + ex.getMessage());
                    return false;
                });
            saveFutures.add(saveFuture);
        }
    }

    // Wait for all saves to complete in parallel
    return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
        .thenApply(v -> {
            int savedCount = (int) saveFutures.stream()
                .map(CompletableFuture::join)
                .filter(success -> success)
                .count();
            return savedCount;
        });
}
```

## Performance Impact

### Before
- `getMachine()`: Blocked async thread waiting for database
- `autoSaveAll()`: Sequential saves (N machines × save_time)
- Potential for thread pool exhaustion

### After
- `getMachine()`: True async, no blocking
- `autoSaveAll()`: Parallel saves (max(save_time) instead of sum)
- Efficient thread pool usage

## Testing
Rebuild and test:
```bash
./gradlew clean :RDQ:rdq-premium:build
```

## Note on Statistics Lag
The 15-second lag in server logs was from RCore's VanillaStats system, not the machine system:
```
[VanillaStats-BatchProcessor] Slow batch collection: 1 players in 15120ms
```

This is a separate issue in the statistics collection system.

## Files Modified
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineService.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/repository/MachineCache.java`
