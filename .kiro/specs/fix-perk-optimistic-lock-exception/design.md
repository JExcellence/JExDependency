# Design Document

## Overview

This design addresses the `OptimisticLockException` issue in the RDQ perk system by implementing a robust retry mechanism with exponential backoff. The solution focuses on handling concurrent modifications gracefully while maintaining data integrity and providing clear feedback to users.

The core strategy is to catch optimistic lock exceptions, reload the entity with fresh data, reapply the intended state changes, and retry the update operation. This approach respects Hibernate's optimistic locking mechanism while providing resilience against transient concurrency conflicts.

## Architecture

### High-Level Flow

```
Player Action (Toggle Perk)
    ↓
PerkActivationService.activate/deactivate()
    ↓
Apply Effects (Potion/Special/Event)
    ↓
Update Entity State (recordActivation/recordDeactivation)
    ↓
Repository.updateWithRetry() ← NEW RETRY WRAPPER
    ↓
├─ Success → Log & Return
└─ OptimisticLockException
       ↓
   Retry Logic (max 3 attempts)
       ↓
   ├─ Reload Entity (fresh transaction)
   ├─ Reapply State Changes
   ├─ Exponential Backoff
   └─ Retry Update
           ↓
       ├─ Success → Log with retry count
       └─ Failure → Log error & notify player
```

### Component Interaction

1. **PerkActivationService**: Orchestrates perk activation/deactivation, delegates to retry-enabled repository methods
2. **RetryableRepositoryOperation**: New utility class providing retry logic for database operations
3. **PlayerPerkRepository**: Extended with retry-enabled update methods
4. **PlayerPerk Entity**: Maintains optimistic lock version field (@Version)

## Components and Interfaces

### 1. RetryableRepositoryOperation Utility Class

A new utility class that provides generic retry logic for repository operations.

```java
public class RetryableRepositoryOperation<T> {
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 50;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    private final int maxRetries;
    private final long baseDelayMs;
    private final double backoffMultiplier;
    
    // Constructor with configurable parameters
    // executeWithRetry() method that accepts:
    //   - Supplier<T> operation: The database operation to execute
    //   - Function<T, T> reloadFunction: Function to reload entity with fresh data
    //   - Consumer<T> reapplyChanges: Function to reapply intended changes
    //   - String operationName: Name for logging
    // Returns: CompletableFuture<T>
}
```

**Key Methods:**
- `executeWithRetry()`: Main retry loop with exponential backoff
- `shouldRetry(Throwable)`: Determines if exception is retryable (OptimisticLockException, StaleObjectStateException)
- `calculateDelay(int attempt)`: Calculates backoff delay for retry attempt
- `logRetryAttempt()`: Logs retry information
- `logFinalFailure()`: Logs exhausted retries

### 2. Enhanced PerkActivationService

Modify the `activate()` and `deactivate()` methods to use retry-enabled repository operations.

**Changes to activate() method:**
```java
public CompletableFuture<Boolean> activate(Player player, PlayerPerk playerPerk) {
    // ... validation logic (unchanged)
    
    // Apply effects
    boolean effectsApplied = applyPerkEffects(player, playerPerk);
    if (!effectsApplied) {
        return CompletableFuture.completedFuture(false);
    }
    
    // Update state
    playerPerk.recordActivation();
    
    // NEW: Use retry-enabled update
    return updatePlayerPerkWithRetry(
        playerPerk,
        perk -> perk.recordActivation(),
        "activate perk " + playerPerk.getPerk().getIdentifier() + " for " + player.getName()
    ).thenApply(updatedPerk -> {
        if (updatedPerk != null) {
            LOGGER.log(Level.INFO, "Activated perk {0} for player {1}",
                    new Object[]{perk.getIdentifier(), player.getName()});
            invalidateCache(player.getUniqueId());
            return true;
        }
        return false;
    }).exceptionally(throwable -> {
        LOGGER.log(Level.SEVERE, "Error activating perk after retries", throwable);
        sendErrorMessageToPlayer(player, "perk.error.activation_failed");
        return false;
    });
}
```

**New helper method:**
```java
private CompletableFuture<PlayerPerk> updatePlayerPerkWithRetry(
    PlayerPerk playerPerk,
    Consumer<PlayerPerk> stateChangeFunction,
    String operationDescription
) {
    RetryableRepositoryOperation<PlayerPerk> retryOp = 
        new RetryableRepositoryOperation<>(3, 50, 2.0);
    
    return retryOp.executeWithRetry(
        () -> playerPerkRepository.update(playerPerk),
        id -> playerPerkRepository.findById(id).orElse(null),
        stateChangeFunction,
        operationDescription
    );
}
```

### 3. Repository Layer Enhancement

Since the repository is in a compiled JAR, we'll create a wrapper service that adds retry logic.

**New Class: PlayerPerkRepositoryService**
```java
public class PlayerPerkRepositoryService {
    private final PlayerPerkRepository repository;
    private final RetryableRepositoryOperation<PlayerPerk> retryOperation;
    
    public CompletableFuture<PlayerPerk> updateWithRetry(
        PlayerPerk playerPerk,
        Consumer<PlayerPerk> stateChangeFunction,
        String operationName
    ) {
        return retryOperation.executeWithRetry(
            () -> repository.update(playerPerk),
            id -> repository.findById(id).orElse(null),
            stateChangeFunction,
            operationName
        );
    }
}
```

## Data Models

### PlayerPerk Entity (Existing)

No changes required. The entity already has:
- `@Version` field for optimistic locking
- State change methods: `recordActivation()`, `recordDeactivation()`
- Proper equals/hashCode implementation

### Retry Context (New)

```java
public class RetryContext {
    private final String operationName;
    private final String entityType;
    private final Long entityId;
    private final int attemptNumber;
    private final long delayMs;
    private final Throwable lastException;
    
    // Used for structured logging
}
```

## Error Handling

### Exception Hierarchy

```
Throwable
└── Exception
    └── RuntimeException
        └── PersistenceException
            ├── OptimisticLockException (RETRYABLE)
            └── StaleObjectStateException (RETRYABLE)
```

### Retry Decision Logic

```java
private boolean shouldRetry(Throwable throwable) {
    if (throwable instanceof OptimisticLockException) {
        return true;
    }
    if (throwable instanceof StaleObjectStateException) {
        return true;
    }
    if (throwable instanceof CompletionException) {
        return shouldRetry(throwable.getCause());
    }
    return false;
}
```

### Error Messages

**For Players:**
- Success: "Perk {name} has been {activated/deactivated}"
- Temporary failure (retrying): No message (silent retry)
- Permanent failure: "Unable to toggle perk. Please try again in a moment."

**For Logs:**
- Retry attempt: `[INFO] Retrying perk update for player {name}, perk {id}, attempt {n}/3`
- Success after retry: `[INFO] Perk update succeeded after {n} retries for player {name}`
- Final failure: `[SEVERE] Perk update failed after 3 retries for player {name}, perk {id}: {exception}`

### Rollback Strategy

When an update fails after all retries:
1. Log the complete failure with context
2. Ensure no partial state is persisted
3. The transaction rollback is handled by Hibernate automatically
4. Effects already applied (potions, special abilities) remain active until next server restart or manual deactivation
5. Consider adding a cleanup task to detect and fix inconsistent states

## Testing Strategy

### Unit Tests

1. **RetryableRepositoryOperation Tests**
   - Test successful operation on first attempt
   - Test retry on OptimisticLockException
   - Test retry on StaleObjectStateException
   - Test exponential backoff calculation
   - Test max retries exhaustion
   - Test non-retryable exception handling

2. **PerkActivationService Tests**
   - Test activate() with successful update
   - Test activate() with retry and success
   - Test activate() with retry exhaustion
   - Test deactivate() with concurrent modification
   - Test state reapplication after reload

### Integration Tests

1. **Concurrent Modification Simulation**
   - Simulate two threads toggling the same perk
   - Verify both operations eventually succeed
   - Verify final state is consistent

2. **Database Transaction Tests**
   - Verify transaction isolation
   - Verify proper rollback on failure
   - Verify entity refresh loads latest data

### Load Tests

1. **High Concurrency Scenario**
   - 100 players toggling perks simultaneously
   - Measure retry rate
   - Measure success rate
   - Verify no data corruption

2. **Performance Impact**
   - Measure latency with and without retries
   - Verify exponential backoff doesn't cause excessive delays
   - Monitor database connection pool usage

## Implementation Notes

### Thread Safety

- `RetryableRepositoryOperation` is stateless and thread-safe
- Each retry attempt uses a fresh transaction
- Entity reloading ensures latest data is fetched
- No shared mutable state between retry attempts

### Performance Considerations

- Exponential backoff prevents thundering herd
- Maximum 3 retries limits worst-case latency to ~350ms (50 + 100 + 200)
- Async operations prevent blocking game thread
- Cache invalidation ensures consistency

### Configuration

Retry parameters should be configurable via config file:

```yaml
perk-system:
  retry:
    max-attempts: 3
    base-delay-ms: 50
    backoff-multiplier: 2.0
    enabled: true
```

### Monitoring

Add metrics for:
- Total retry attempts
- Retry success rate
- Average retries per operation
- Operations failing after max retries

### Migration Path

1. Deploy `RetryableRepositoryOperation` utility class
2. Create `PlayerPerkRepositoryService` wrapper
3. Update `PerkActivationService` to use retry-enabled methods
4. Add configuration options
5. Monitor retry metrics in production
6. Adjust retry parameters based on observed behavior

## Alternative Approaches Considered

### 1. Pessimistic Locking
**Rejected**: Would require database-level locks, reducing concurrency and potentially causing deadlocks.

### 2. Queue-Based Updates
**Rejected**: Adds complexity with message queues, increases latency, and doesn't solve the fundamental concurrency issue.

### 3. Last-Write-Wins
**Rejected**: Could lose important state changes (e.g., activation counts, usage time).

### 4. Version-Based Merge
**Considered for future**: Merge conflicting changes intelligently, but requires complex conflict resolution logic.

## Future Enhancements

1. **Adaptive Retry Strategy**: Adjust retry parameters based on observed conflict rates
2. **Conflict Metrics Dashboard**: Real-time monitoring of concurrency conflicts
3. **Distributed Locking**: For multi-server deployments, consider Redis-based distributed locks
4. **Event Sourcing**: Store all state changes as events for complete audit trail and easier conflict resolution
