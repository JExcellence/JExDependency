# Design Document (Continued)

## Error Handling

### Exception Hierarchy

```java
public sealed class BountyException extends RuntimeException 
    permits BountyNotFoundException, BountyExpiredException, InsufficientFundsException, 
            BountyLimitExceededException, InvalidTargetException {}

public final class BountyNotFoundException extends BountyException {
    public BountyNotFoundException(Long bountyId) {
        super("Bounty not found: " + bountyId);
    }
}

public final class BountyExpiredException extends BountyException {
    public BountyExpiredException(Long bountyId) {
        super("Bounty has expired: " + bountyId);
    }
}

public final class InsufficientFundsException extends BountyException {
    public InsufficientFundsException(String currency, double required, double available) {
        super(String.format("Insufficient %s: required %.2f, available %.2f", 
                          currency, required, available));
    }
}

public final class BountyLimitExceededException extends BountyException {
    public BountyLimitExceededException(int limit) {
        super("Bounty limit exceeded: " + limit);
    }
}

public final class InvalidTargetException extends BountyException {
    public InvalidTargetException(String reason) {
        super("Invalid bounty target: " + reason);
    }
}
```

### Error Handling Strategy

1. **Service Layer**: Wrap checked exceptions in CompletableFuture.exceptionally()
2. **View Layer**: Display user-friendly messages via JExTranslate
3. **Repository Layer**: Let JPA exceptions propagate to service layer
4. **Async Operations**: Use CompletableFuture.handle() for graceful degradation

```java
// Example error handling in service
public CompletableFuture<Bounty> createBounty(BountyCreationRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        // Validation
        if (request.targetUuid().equals(request.commissionerUuid())) {
            throw new InvalidTargetException("Cannot target yourself");
        }
        
        // Business logic
        return performCreation(request);
    }, executor).exceptionally(ex -> {
        logger.error("Failed to create bounty", ex);
        throw new BountyException("Bounty creation failed", ex);
    });
}

// Example error handling in view
private void handleBountyCreation(SlotClickContext ctx) {
    bountyService.createBounty(request)
        .thenAccept(bounty -> {
            i18n("creation.success", player)
                .with(Placeholder.of("target", bounty.targetName()))
                .send();
        })
        .exceptionally(ex -> {
            if (ex.getCause() instanceof InvalidTargetException) {
                i18n("creation.invalid_target", player).send();
            } else {
                i18n("creation.failed", player).send();
            }
            return null;
        });
}
```

## Testing Strategy

### Unit Testing

Unit tests will cover specific examples, edge cases, and integration points:

**Entity Tests**:
- Bounty entity state transitions (active → claimed, active → expired)
- BountyHunterStats calculations (incrementing counters, updating highest value)
- Record validation (BountyCreationRequest defensive copies)

**Service Tests**:
- BountyService method contracts
- Edition-specific behavior (premium vs free limits)
- Async operation completion
- Error handling and exception throwing

**Repository Tests**:
- Custom query correctness
- Pagination behavior
- Index usage verification

**View Tests**:
- State management (mutable state updates, computed state derivation)
- Navigation flow (view transitions, back button behavior)
- Item rendering (button states, conditional display)

### Property-Based Testing

Property-based tests will verify universal properties across all inputs using **jqwik** (Java property-based testing library):

**Configuration**:
```java
@PropertyDefaults(tries = 100, edgeCases = EdgeCasesMode.MIXIN)
public class BountyPropertyTests {
    // Properties defined here
}
```

**Key Properties to Test**:

1. **Item Merging**: Similar items always stack correctly
2. **State Persistence**: Navigation preserves state
3. **Claim Attribution**: Each claim mode correctly attributes kills
4. **Distribution Modes**: Each mode delivers rewards correctly
5. **Expiration Logic**: Expired bounties are always detected
6. **Statistics Updates**: Claims always update hunter stats correctly
7. **Edition Limits**: Free edition never exceeds 1 bounty, premium has no limit
8. **Currency Validation**: Invalid balances always rejected
9. **Async Completion**: All async operations eventually complete or fail
10. **Visual Indicators**: Active bounties always have indicators, inactive never do

**Example Property Test**:
```java
@Property
void similarItemsAlwaysMerge(@ForAll("rewardItems") Set<RewardItem> items) {
    // Given: A set of reward items with some duplicates
    var merged = bountyService.mergeRewardItems(items);
    
    // Then: Total amount is preserved
    int originalTotal = items.stream().mapToInt(RewardItem::amount).sum();
    int mergedTotal = merged.stream().mapToInt(RewardItem::amount).sum();
    assertThat(mergedTotal).isEqualTo(originalTotal);
    
    // And: No duplicate items remain
    var itemTypes = merged.stream()
        .map(item -> item.item().getType())
        .collect(Collectors.toSet());
    assertThat(itemTypes).hasSize(merged.size());
}

@Provide
Arbitrary<Set<RewardItem>> rewardItems() {
    return Arbitraries.of(Material.values())
        .filter(Material::isItem)
        .map(material -> new ItemStack(material))
        .map(item -> new RewardItem(item, 
                                    Arbitraries.integers().between(1, 64).sample(),
                                    Arbitraries.doubles().between(1.0, 1000.0).sample()))
        .set().ofMinSize(1).ofMaxSize(10);
}
```

### Integration Testing

Integration tests will verify component interactions:

- **Service + Repository**: Database operations complete correctly
- **View + Service**: UI actions trigger correct service calls
- **Event + Service**: Player death events trigger bounty claims
- **Config + Service**: Configuration changes affect behavior

### Test Coverage Goals

- **Line Coverage**: Minimum 80%
- **Branch Coverage**: Minimum 75%
- **Property Coverage**: All testable acceptance criteria have corresponding properties
- **Edge Case Coverage**: All identified edge cases have explicit tests

## Performance Considerations

### Database Optimization

1. **Indexing**: All frequently queried columns indexed (target UUID, commissioner UUID, active status)
2. **Pagination**: Large result sets paginated to avoid memory issues
3. **Eager vs Lazy Loading**: Reward collections loaded eagerly to avoid N+1 queries
4. **Connection Pooling**: HikariCP for efficient connection management

### Async Operation Optimization

1. **Thread Pool**: Dedicated executor for bounty operations
2. **Batch Operations**: Multiple bounty updates batched when possible
3. **Caching**: Frequently accessed data (leaderboards) cached with TTL
4. **Timeout Handling**: All async operations have reasonable timeouts

### View Performance

1. **Lazy Rendering**: Items rendered only when visible
2. **State Caching**: Computed states cached until dependencies change
3. **Pagination**: Large lists paginated to reduce rendering overhead
4. **Debouncing**: Rapid state changes debounced to prevent excessive re-renders

## Security Considerations

### Input Validation

1. **Target Validation**: Prevent self-targeting, offline players, invalid UUIDs
2. **Amount Validation**: Prevent negative amounts, overflow, underflow
3. **Permission Checks**: Verify player has permission to create/claim bounties
4. **Rate Limiting**: Prevent bounty spam by limiting creation frequency

### Data Integrity

1. **Transaction Management**: Bounty creation/claiming wrapped in transactions
2. **Optimistic Locking**: Prevent concurrent modification issues
3. **Referential Integrity**: Foreign key constraints on bounty relationships
4. **Audit Trail**: Track all bounty state changes for debugging

### Economy Integration

1. **Balance Verification**: Always verify sufficient funds before deduction
2. **Atomic Operations**: Currency deduction and bounty creation atomic
3. **Rollback Support**: Failed operations rollback all changes
4. **Double-Spend Prevention**: Concurrent claim attempts handled correctly

## Deployment Considerations

### Database Migration

1. **Schema Changes**: Liquibase/Flyway for versioned migrations
2. **Data Migration**: Scripts to migrate existing bounty data
3. **Backward Compatibility**: Support reading old bounty format during transition
4. **Rollback Plan**: Ability to revert to old system if needed

### Configuration Migration

1. **Config Conversion**: Tool to convert old bounty.yml to new format
2. **Default Values**: Sensible defaults for new configuration options
3. **Validation**: Startup validation of configuration values
4. **Hot Reload**: Support reloading configuration without restart

### Edition Deployment

1. **Free Edition**: Deployed as separate JAR with limited features
2. **Premium Edition**: Full feature set with database persistence
3. **Feature Flags**: Runtime toggling of premium features
4. **License Validation**: Check for valid premium license on startup

## Monitoring and Observability

### Metrics

1. **Bounty Metrics**: Total active, created per hour, claimed per hour
2. **Performance Metrics**: Async operation duration, database query time
3. **Error Metrics**: Exception counts by type, failed operations
4. **User Metrics**: Unique creators, unique hunters, average bounty value

### Logging

1. **Structured Logging**: JSON format for easy parsing
2. **Log Levels**: DEBUG for development, INFO for production
3. **Contextual Information**: Include player UUID, bounty ID in all logs
4. **Performance Logging**: Log slow operations (>100ms)

### Health Checks

1. **Database Connectivity**: Verify database accessible
2. **Service Availability**: Check BountyService responding
3. **Repository Health**: Verify repositories can query data
4. **Cache Health**: Check cache hit rates, eviction rates

## Future Enhancements

### Planned Features

1. **Bounty Pooling**: Multiple players contribute to single bounty
2. **Bounty Contracts**: Time-limited exclusive hunting rights
3. **Bounty Tiers**: Bronze/Silver/Gold bounties with different rewards
4. **Bounty Chains**: Completing one bounty unlocks another
5. **Bounty Guilds**: Team-based bounty hunting with shared rewards

### Technical Improvements

1. **GraphQL API**: Expose bounty data via GraphQL for external tools
2. **WebSocket Updates**: Real-time bounty updates in web dashboard
3. **Machine Learning**: Predict bounty claim likelihood
4. **Blockchain Integration**: Immutable bounty history on blockchain
5. **Cross-Server Bounties**: Bounties that span multiple servers

## Appendix

### Glossary of Terms

- **Bounty**: A reward offered for eliminating a specific player
- **Commissioner**: The player who creates and funds a bounty
- **Target**: The player who has a bounty placed on them
- **Hunter**: A player who claims bounties by eliminating targets
- **Claim Mode**: The method used to determine who gets credit for a kill
- **Distribution Mode**: The method used to deliver rewards to hunters
- **Static Bounty**: A pre-configured bounty loaded from configuration (free edition)
- **Dynamic Bounty**: A player-created bounty with custom rewards (premium edition)

### References

- [inventory-framework Documentation](https://github.com/DevNatan/inventory-framework/wiki)
- [JExTranslate API](https://github.com/JExcellence/JExTranslate)
- [jqwik User Guide](https://jqwik.net/docs/current/user-guide.html)
- [CompletableFuture Guide](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
- [JPA Best Practices](https://vladmihalcea.com/tutorials/hibernate/)
