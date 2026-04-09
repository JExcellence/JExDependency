# DataFixer Freeze Fix

## Problem

The server was freezing for 10+ seconds when players disconnected, with the error:

```
[ERROR]: The server has not responded for 10 seconds! Creating thread dump
```

The stack trace showed the freeze occurring in:
```
RCore//com.raindropcentral.core.service.statistics.collector.NativeStatisticCollector.collectBlockStatistics(NativeStatisticCollector.java:212)
RCore//com.raindropcentral.core.service.statistics.collector.NativeStatisticCollector.collectItemStatistics(NativeStatisticCollector.java:314)
```

Specifically at the lines:
```java
if (!material.isBlock()) continue;  // Line 212
if (!material.isItem()) continue;   // Line 314
```

## Root Cause

Both `Material.isBlock()` and `Material.isItem()` trigger Mojang's DataFixer system initialization on first access. The DataFixer is a complex system that converts old Minecraft data formats to new ones, and its initialization can take 10+ seconds.

When this initialization happens on the main server thread (during player disconnect event), it blocks all server operations, causing the freeze.

### Why It Happens

1. Player disconnects
2. `EventDrivenCollector.onPlayerQuit()` is called on main thread
3. `NativeStatisticCollector.collectBlockStatistics()` or `collectItemStatistics()` is called
4. Loop through all `Material.values()` checking `material.isBlock()` or `material.isItem()`
5. First call to these methods triggers DataFixer initialization
6. DataFixer takes 10+ seconds to initialize
7. Main thread is blocked, server freezes

## Solution

Cache both block and item materials in static sets that are initialized asynchronously, preventing the DataFixer from being triggered on the main thread.

### Implementation

```java
/**
 * Cached set of block materials to avoid triggering DataFixer on main thread.
 */
private static Set<Material> blockMaterials = null;

/**
 * Cached set of item materials to avoid triggering DataFixer on main thread.
 */
private static Set<Material> itemMaterials = null;

/**
 * Initializes the block materials cache asynchronously.
 */
private static synchronized void initializeBlockMaterialsCache() {
    if (blockMaterials != null) return;
    blockMaterials = new HashSet<>();
    
    CompletableFuture.runAsync(() -> {
        Set<Material> blocks = new HashSet<>();
        for (Material material : Material.values()) {
            try {
                if (material.isBlock()) {
                    blocks.add(material);
                }
            } catch (Exception e) {
                // Ignore materials that can't be checked
            }
        }
        blockMaterials = blocks;
    });
}

/**
 * Initializes the item materials cache asynchronously.
 */
private static synchronized void initializeItemMaterialsCache() {
    if (itemMaterials != null) return;
    itemMaterials = new HashSet<>();
    
    CompletableFuture.runAsync(() -> {
        Set<Material> items = new HashSet<>();
        for (Material material : Material.values()) {
            try {
                if (material.isItem()) {
                    items.add(material);
                }
            } catch (Exception e) {
                // Ignore materials that can't be checked
            }
        }
        itemMaterials = items;
    });
}

// Usage in collection methods
private List<QueuedStatistic> collectBlockStatistics(...) {
    if (blockMaterials == null) {
        initializeBlockMaterialsCache();
        return new ArrayList<>();
    }
    
    for (Material material : blockMaterials) {
        // ... collect statistics
    }
}

private List<QueuedStatistic> collectItemStatistics(...) {
    if (itemMaterials == null) {
        initializeItemMaterialsCache();
        return new ArrayList<>();
    }
    
    for (Material material : itemMaterials) {
        // ... collect statistics
    }
}
```

### How It Works

1. **Lazy Initialization**: Caches are only initialized when first needed
2. **Async Population**: The expensive `isBlock()` and `isItem()` checks happen off the main thread
3. **Empty Set Guard**: Returns empty set immediately to prevent multiple initializations
4. **Graceful Degradation**: Returns empty statistics list if cache not ready yet
5. **One-Time Cost**: DataFixer initialization happens once, asynchronously

## Benefits

1. **No Main Thread Blocking**: DataFixer initialization happens asynchronously
2. **No Server Freezes**: Player disconnects no longer cause 10+ second freezes
3. **Better Performance**: Materials are cached, reducing repeated checks
4. **Graceful Handling**: Returns empty list if cache not ready instead of crashing
5. **Minimal Impact**: First collection may be empty, but subsequent ones work normally
6. **Complete Coverage**: Both block and item statistics are protected

## Testing

To verify the fix:

1. **Start Server**: Watch for async initialization in logs
2. **Player Disconnect**: Should be instant, no freeze
3. **Check Statistics**: Both block and item statistics should be collected normally after caches are ready
4. **Monitor TPS**: Should remain stable during player disconnects

## Related Files

- `RCore/src/main/java/com/raindropcentral/core/service/statistics/collector/NativeStatisticCollector.java` - Fixed file
- `RCore/src/main/java/com/raindropcentral/core/service/statistics/collector/EventDrivenCollector.java` - Calls the collector

## Migration Path

This fix is a temporary solution for the old statistics system. The long-term solution is to:

1. Complete the new vanilla statistics system integration
2. Disable the old `NativeStatisticCollector`
3. Remove the old system entirely once the new one is stable

The new vanilla statistics system already handles this correctly by using the Paper API's statistic methods which don't trigger DataFixer initialization.
