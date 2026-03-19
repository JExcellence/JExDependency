# Design Document

## Overview

This design implements a player-scoped perk caching system that loads all PlayerPerk entities into memory when a player joins, operates on cached data during gameplay, and persists changes back to the database when the player disconnects. This approach eliminates database transaction conflicts, reduces query load, and improves performance.

The core strategy is to create a `PlayerPerkCache` class that manages a `ConcurrentHashMap` of player UUIDs to their perk data. Each player's cache entry contains all their PlayerPerk entities and tracks which ones have been modified (dirty tracking). The cache integrates seamlessly with existing services through minimal API changes.

## Architecture

### High-Level Flow

```
Player Join Event
    ↓
Load PlayerPerks from DB (async)
    ↓
Store in PlayerPerkCache
    ↓
Player Gameplay
    ↓
Perk Operations (toggle, activate, etc.)
    ↓
├─ Read from Cache
├─ Modify in Cache
└─ Mark as Dirty
    ↓
Player Disconnect Event
    ↓
Persist Dirty PlayerPerks to DB
    ↓
Remove from Cache
```

### Component Interaction

```
PlayerJoinListener
    ↓
PlayerPerkCache.loadPlayerCache(player)
    ↓
PlayerPerkRepository.findAll() [filtered by player]
    ↓
Store in ConcurrentHashMap<UUID, PlayerCacheEntry>

PerkManagementService
    ↓
PlayerPerkCache.getPlayerPerk(player, perk)
    ↓
Return cached PlayerPerk (no DB query)

PerkActivationService
    ↓
PlayerPerkCache.updatePlayerPerk(playerPerk)
    ↓
Mark as dirty in cache

PlayerQuitListener
    ↓
PlayerPerkCache.saveAndUnloadPlayerCache(player)
    ↓
PlayerPerkRepository.update() [only dirty entities]
    ↓
Remove from cache
```

## Components and Interfaces

### 1. PlayerPerkCache Class

The central cache management class.

```java
public class PlayerPerkCache {
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    // Cache storage: UUID -> PlayerCacheEntry
    private final ConcurrentHashMap<UUID, PlayerCacheEntry> cache;
    
    // Repository for DB operations
    private final PlayerPerkRepository repository;
    
    // Configuration
    private final boolean cacheEnabled;
    private final int maxRetries;
    
    /**
     * Loads all PlayerPerks for a player into cache.
     * @return CompletableFuture that completes when loading is done
     */
    public CompletableFuture<Void> loadPlayerCache(UUID playerId);
    
    /**
     * Gets a PlayerPerk from cache.
     * @return Optional containing the PlayerPerk, or empty if not found
     */
    public Optional<PlayerPerk> getPlayerPerk(UUID playerId, Long perkId);
    
    /**
     * Gets all PlayerPerks for a player from cache.
     * @return List of PlayerPerks, or empty list if player not cached
     */
    public List<PlayerPerk> getAllPlayerPerks(UUID playerId);
    
    /**
     * Gets filtered PlayerPerks from cache.
     */
    public List<PlayerPerk> getPlayerPerks(UUID playerId, Predicate<PlayerPerk> filter);
    
    /**
     * Updates a PlayerPerk in cache and marks it as dirty.
     */
    public void updatePlayerPerk(UUID playerId, PlayerPerk playerPerk);
    
    /**
     * Saves dirty PlayerPerks to DB and removes player from cache.
     * @return CompletableFuture that completes when save is done
     */
    public CompletableFuture<Void> saveAndUnloadPlayerCache(UUID playerId);
    
    /**
     * Checks if a player's cache is loaded.
     */
    public boolean isCacheLoaded(UUID playerId);
    
    /**
     * Manually flushes cache for a player (admin command).
     */
    public CompletableFuture<Void> flushPlayerCache(UUID playerId);
    
    /**
     * Saves all caches (for server shutdown).
     */
    public CompletableFuture<Void> saveAllCaches();
}
```

### 2. PlayerCacheEntry Class

Holds cached data for a single player.

```java
class PlayerCacheEntry {
    // Map of perk ID -> PlayerPerk entity
    private final ConcurrentHashMap<Long, PlayerPerk> perks;
    
    // Set of dirty perk IDs that need persistence
    private final Set<Long> dirtyPerks;
    
    // Lock for atomic operations
    private final ReentrantReadWriteLock lock;
    
    // Timestamp when cache was loaded
    private final long loadedAt;
    
    /**
     * Gets a PlayerPerk by perk ID.
     */
    public Optional<PlayerPerk> getPerk(Long perkId);
    
    /**
     * Gets all PlayerPerks.
     */
    public List<PlayerPerk> getAllPerks();
    
    /**
     * Gets filtered PlayerPerks.
     */
    public List<PlayerPerk> getPerks(Predicate<PlayerPerk> filter);
    
    /**
     * Updates a PlayerPerk and marks it as dirty.
     */
    public void updatePerk(PlayerPerk playerPerk);
    
    /**
     * Gets all dirty PlayerPerks that need persistence.
     */
    public List<PlayerPerk> getDirtyPerks();
    
    /**
     * Clears dirty flags after successful persistence.
     */
    public void clearDirtyFlags();
    
    /**
     * Adds a new PlayerPerk to the cache.
     */
    public void addPerk(PlayerPerk playerPerk);
    
    /**
     * Removes a PlayerPerk from the cache.
     */
    public void removePerk(Long perkId);
}
```

### 3. Modified PerkManagementService

Update to use cache instead of direct DB queries.

```java
public class PerkManagementService {
    private final PlayerPerkCache cache;
    private final PlayerPerkRepository playerPerkRepository;
    
    // Modified method - now uses cache
    private CompletableFuture<Optional<PlayerPerk>> findByPlayerAndPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        UUID playerId = player.getUniqueId();
        
        // Check if cache is loaded
        if (!cache.isCacheLoaded(playerId)) {
            LOGGER.log(Level.WARNING, "Cache not loaded for player {0}, falling back to DB", playerId);
            return findByPlayerAndPerkFromDB(player, perk);
        }
        
        // Get from cache
        return CompletableFuture.completedFuture(
            cache.getPlayerPerk(playerId, perk.getId())
        );
    }
    
    // Modified method - now uses cache
    public CompletableFuture<PlayerPerk> grantPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk,
            final boolean autoEnable
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(existingOpt -> {
                    if (existingOpt.isPresent()) {
                        PlayerPerk existing = existingOpt.get();
                        if (!existing.isUnlocked()) {
                            existing.setUnlocked(true);
                            if (autoEnable) {
                                existing.setEnabled(true);
                            }
                            // Update in cache (marks as dirty)
                            cache.updatePlayerPerk(player.getUniqueId(), existing);
                            LOGGER.log(Level.INFO, "Granted perk {0} to player {1}", 
                                    new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        }
                        return CompletableFuture.completedFuture(existing);
                    }
                    
                    // Create new PlayerPerk
                    PlayerPerk playerPerk = new PlayerPerk(player, perk);
                    playerPerk.setUnlocked(true);
                    if (autoEnable) {
                        playerPerk.setEnabled(true);
                    }
                    
                    return CompletableFuture.supplyAsync(() -> {
                        // Save to DB immediately for new entities
                        PlayerPerk saved = playerPerkRepository.save(playerPerk);
                        // Add to cache
                        cache.updatePlayerPerk(player.getUniqueId(), saved);
                        LOGGER.log(Level.INFO, "Granted perk {0} to player {1}", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return saved;
                    });
                });
    }
    
    // Similar updates for enablePerk, disablePerk, etc.
}
```

### 4. Event Listeners

```java
public class PerkCacheListener implements Listener {
    private final PlayerPerkCache cache;
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        LOGGER.log(Level.INFO, "Loading perk cache for player {0}", player.getName());
        
        cache.loadPlayerCache(playerId)
            .thenRun(() -> {
                LOGGER.log(Level.INFO, "Perk cache loaded for player {0}", player.getName());
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to load perk cache for player " + player.getName(), throwable);
                return null;
            });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        LOGGER.log(Level.INFO, "Saving and unloading perk cache for player {0}", player.getName());
        
        cache.saveAndUnloadPlayerCache(playerId)
            .thenRun(() -> {
                LOGGER.log(Level.INFO, "Perk cache saved for player {0}", player.getName());
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to save perk cache for player " + player.getName(), throwable);
                return null;
            });
    }
}
```

### 5. Server Shutdown Hook

```java
public class RDQPlugin extends JavaPlugin {
    private PlayerPerkCache perkCache;
    
    @Override
    public void onDisable() {
        LOGGER.log(Level.INFO, "Saving all perk caches before shutdown...");
        
        try {
            perkCache.saveAllCaches().get(30, TimeUnit.SECONDS);
            LOGGER.log(Level.INFO, "All perk caches saved successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save all perk caches during shutdown", e);
        }
    }
}
```

## Data Models

### PlayerPerk Entity (Existing)

No changes required. The entity already has all necessary fields.

### Cache Configuration

```yaml
perk-system:
  cache:
    enabled: true
    max-retries: 3
    retry-delay-ms: 100
    save-timeout-seconds: 10
    log-performance: true
    performance-threshold-ms: 500
```

## Error Handling

### Cache Load Failure

```java
public CompletableFuture<Void> loadPlayerCache(UUID playerId) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            List<PlayerPerk> playerPerks = repository.findAll().stream()
                .filter(pp -> pp.getPlayer().getUniqueId().equals(playerId))
                .collect(Collectors.toList());
            
            PlayerCacheEntry entry = new PlayerCacheEntry();
            for (PlayerPerk pp : playerPerks) {
                entry.addPerk(pp);
            }
            
            cache.put(playerId, entry);
            LOGGER.log(Level.INFO, "Loaded {0} perks for player {1}", 
                    new Object[]{playerPerks.size(), playerId});
            
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load cache for player " + playerId, e);
            // Create empty cache entry to allow player to join
            cache.put(playerId, new PlayerCacheEntry());
            throw new CompletionException(e);
        }
    });
}
```

### Cache Save Failure with Retry

```java
public CompletableFuture<Void> saveAndUnloadPlayerCache(UUID playerId) {
    return CompletableFuture.supplyAsync(() -> {
        PlayerCacheEntry entry = cache.get(playerId);
        if (entry == null) {
            return null;
        }
        
        List<PlayerPerk> dirtyPerks = entry.getDirtyPerks();
        if (dirtyPerks.isEmpty()) {
            cache.remove(playerId);
            return null;
        }
        
        // Retry logic
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                for (PlayerPerk pp : dirtyPerks) {
                    repository.update(pp);
                }
                entry.clearDirtyFlags();
                cache.remove(playerId);
                LOGGER.log(Level.INFO, "Saved {0} dirty perks for player {1}", 
                        new Object[]{dirtyPerks.size(), playerId});
                return null;
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(100 * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        LOGGER.log(Level.SEVERE, "Failed to save cache for player " + playerId + 
                " after " + maxRetries + " attempts", lastException);
        throw new CompletionException(lastException);
    });
}
```

### Fallback to Database

If cache is not loaded for a player, fall back to direct DB queries:

```java
private CompletableFuture<Optional<PlayerPerk>> findByPlayerAndPerk(
        @NotNull final RDQPlayer player,
        @NotNull final Perk perk
) {
    UUID playerId = player.getUniqueId();
    
    if (!cache.isCacheLoaded(playerId)) {
        LOGGER.log(Level.WARNING, "Cache not loaded for player {0}, using DB fallback", playerId);
        return findByPlayerAndPerkFromDB(player, perk);
    }
    
    return CompletableFuture.completedFuture(
        cache.getPlayerPerk(playerId, perk.getId())
    );
}
```

## Testing Strategy

### Unit Tests

1. **PlayerCacheEntry Tests**
   - Test adding perks to cache
   - Test updating perks and dirty tracking
   - Test removing perks from cache
   - Test filtering perks
   - Test thread-safe operations

2. **PlayerPerkCache Tests**
   - Test loading player cache
   - Test getting perks from cache
   - Test updating perks in cache
   - Test saving and unloading cache
   - Test cache miss scenarios
   - Test concurrent access

### Integration Tests

1. **Cache Lifecycle Tests**
   - Simulate player join and verify cache load
   - Perform perk operations and verify cache updates
   - Simulate player quit and verify cache save
   - Verify DB state after cache save

2. **Concurrency Tests**
   - Multiple threads accessing same player's cache
   - Multiple players accessing cache simultaneously
   - Verify no race conditions or data corruption

3. **Failure Scenario Tests**
   - Cache load failure handling
   - Cache save failure with retry
   - Database unavailable during save
   - Server shutdown with unsaved caches

### Performance Tests

1. **Load Time Tests**
   - Measure cache load time for various perk counts
   - Verify load time is under threshold (< 500ms)

2. **Memory Usage Tests**
   - Measure memory usage with 100+ cached players
   - Verify no memory leaks

3. **Throughput Tests**
   - Measure perk operations per second with cache
   - Compare to direct DB query performance

## Implementation Notes

### Thread Safety

- Use `ConcurrentHashMap` for main cache storage
- Use `ReentrantReadWriteLock` for per-player cache entry operations
- Read operations use read lock (multiple concurrent reads)
- Write operations use write lock (exclusive access)

### Performance Considerations

- Cache load is async and doesn't block player join
- Only dirty entities are persisted on disconnect
- Batch updates for multiple dirty perks
- Cache operations are O(1) lookups

### Memory Management

- Cache entries are removed on player disconnect
- No cache expiration needed (players disconnect regularly)
- Estimated memory: ~1KB per PlayerPerk, ~10KB per player (assuming 10 perks average)
- 100 players = ~1MB memory usage

### Configuration

All cache behavior is configurable:
- Enable/disable caching
- Retry attempts and delays
- Performance logging thresholds
- Timeout values

### Migration Path

1. Deploy `PlayerPerkCache` and `PlayerCacheEntry` classes
2. Add event listeners for player join/quit
3. Update `PerkManagementService` to use cache
4. Update `PerkActivationService` to use cache
5. Add configuration options
6. Monitor cache performance metrics
7. Gradually enable for all players

## Alternative Approaches Considered

### 1. Write-Through Cache
**Rejected**: Would still cause DB transaction conflicts on concurrent writes.

### 2. Periodic Auto-Save
**Rejected**: Could lose data if server crashes between saves.

### 3. Redis Cache
**Rejected**: Adds external dependency and complexity for single-server deployment.

### 4. Event Sourcing
**Considered for future**: Store all state changes as events, but requires significant refactoring.

## Future Enhancements

1. **Cache Warming**: Pre-load caches for players likely to join soon
2. **Metrics Dashboard**: Real-time cache performance monitoring
3. **Admin Commands**: `/rdq cache stats`, `/rdq cache flush <player>`
4. **Multi-Server Support**: Sync caches across multiple servers using Redis
5. **Automatic Backup**: Periodic snapshots of cache state to disk
