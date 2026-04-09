# Collection Scheduler Callback Fix

## Problem

The vanilla statistics collection service was failing to initialize with the error:

```
[SEVERE] Failed to initialize vanilla statistic collection service: 
java.lang.IllegalStateException: Collection callback must be set before starting
```

## Root Cause

The `CollectionScheduler` requires a collection callback to be set via `setCollectionCallback()` before calling `start()`. The `VanillaStatisticCollectionService.initialize()` method was calling `scheduler.start()` without first setting the callback.

### Code Flow

1. `VanillaStatisticCollectionService.initialize()` is called
2. Cache is loaded
3. Event listeners are registered
4. `scheduler.start()` is called **without setting callback first**
5. `CollectionScheduler.start()` checks if callback is set
6. Throws `IllegalStateException` because callback is null

## Solution

Set the collection callback before starting the scheduler in the `initialize()` method:

```java
// Set collection callback before starting scheduler
scheduler.setCollectionCallback(() -> {
    collectAll().exceptionally(error -> {
        LOGGER.warning("Scheduled collection failed: " + error.getMessage());
        return CollectionResult.empty();
    });
});

// Start scheduled collection
scheduler.start();
```

### Why This Works

1. **Callback Set First**: The callback is now set before `start()` is called
2. **Async Collection**: The callback invokes `collectAll()` which returns a `CompletableFuture`
3. **Error Handling**: Exceptions are caught and logged, returning an empty result
4. **Non-Blocking**: The callback doesn't block the scheduler thread

## Implementation Details

### Collection Callback

The callback performs the following:

1. Calls `collectAll()` to collect statistics from all online players
2. Uses batch processing for efficiency
3. Respects backpressure from the queue manager
4. Handles errors gracefully with exception handling
5. Returns `CollectionResult.empty()` on failure

### Scheduler Behavior

The scheduler will:

1. Execute the callback at the configured frequency (default: 300 seconds)
2. Check TPS throttling before each execution
3. Skip collection if TPS is too low
4. Log any errors that occur during collection
5. Continue scheduling even if a collection fails

## Testing

To verify the fix works:

1. **Start Server**: The vanilla statistics service should initialize without errors
2. **Check Logs**: Look for successful initialization message:
   ```
   [INFO] Vanilla statistic collection service initialized successfully
   [INFO] Started collection scheduler with frequency: 300s
   ```
3. **Wait for Collection**: After the configured frequency, check for collection logs:
   ```
   [INFO] Collected X statistics from Y players
   ```
4. **Monitor Performance**: Ensure collections don't impact server TPS

## Configuration

The collection frequency can be configured in the vanilla statistics config:

```yaml
vanilla-statistics:
  enabled: true
  collection-frequency: 300  # seconds (5 minutes)
  enable-tps-throttling: true
  min-tps-threshold: 18.0
```

## Related Files

- `RCore/src/main/java/com/raindropcentral/core/service/statistics/vanilla/VanillaStatisticCollectionService.java` - Fixed initialization
- `RCore/src/main/java/com/raindropcentral/core/service/statistics/vanilla/scheduler/CollectionScheduler.java` - Requires callback

## Benefits

1. **Proper Initialization**: Service now initializes correctly
2. **Scheduled Collection**: Statistics are collected periodically as designed
3. **Error Resilience**: Failed collections don't crash the service
4. **Performance Aware**: TPS throttling prevents server lag
5. **Graceful Degradation**: Returns empty results on failure instead of crashing

## Future Improvements

Consider adding:

1. **Category-Specific Callbacks**: Set callbacks for individual statistic categories
2. **Dynamic Frequency**: Adjust collection frequency based on player count
3. **Collection Metrics**: Track success/failure rates
4. **Retry Logic**: Retry failed collections with exponential backoff
