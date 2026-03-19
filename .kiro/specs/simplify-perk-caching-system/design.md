# Simplify Perk Caching System - Design

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Player Joins Server                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Load All PlayerPerks from DB                    │
│              Store in ConcurrentHashMap                      │
│              Key: UUID, Value: List<PlayerPerk>              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  Player Interacts with Perks                 │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Toggle Perk  │  │ Unlock Perk  │  │ Check Status │      │
│  │ (Instant)    │  │ (DB Write)   │  │ (Memory)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Player Leaves Server                       │
│              Save All Modified PlayerPerks to DB             │
│              Clear from HashMap                              │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. SimplePerkCache

**Purpose**: In-memory storage of player perks

```java
public class SimplePerkCache {
    // UUID -> List of PlayerPerks
    private final ConcurrentHashMap<UUID, List<PlayerPerk>> cache;
    
    // Track which players have unsaved changes
    private final Set<UUID> dirtyPlayers;
    
    // Load player perks on join
    void loadPlayer(UUID playerId);
    
    // Get perks from memory (instant)
    List<PlayerPerk> getPerks(UUID playerId);
    
    // Update perk in memory (instant)
    void updatePerk(UUID playerId, PlayerPerk perk);
    
    // Save player perks on leave
    void savePlayer(UUID playerId);
    
    // Periodic auto-save for crash protection
    void autoSaveAll();
}
```

### 2. Simplified PerkManagementService

**Purpose**: Manage perk operations using the cache

```java
public class PerkManagementService {
    private final SimplePerkCache cache;
    
    // Toggle enabled/disabled (instant, memory only)
    boolean togglePerk(Player player, PlayerPerk perk) {
        perk.setEnabled(!perk.isEnabled());
        cache.updatePerk(player.getUniqueId(), perk);
        cache.markDirty(player.getUniqueId());
        return true; // Instant
    }
    
    // Unlock perk (DB write required)
    CompletableFuture<PlayerPerk> unlockPerk(Player player, Perk perk) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerPerk playerPerk = new PlayerPerk(player, perk);
            playerPerk.setUnlocked(true);
            playerPerk.setEnabled(true);
            
            // Save to DB
            PlayerPerk saved = repository.save(playerPerk);
            
            // Add to cache
            cache.updatePerk(player.getUniqueId(), saved);
            
            return saved;
        });
    }
    
    // Check if player has perk (instant, memory only)
    boolean hasPerk(UUID playerId, Long perkId) {
        return cache.getPerks(playerId).stream()
            .anyMatch(p -> p.getPerk().getId().equals(perkId));
    }
}
```

### 3. PerkCacheListener

**Purpose**: Handle player join/leave events

```java
public class PerkCacheListener implements Listener {
    private final SimplePerkCache cache;
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load perks asynchronously
        CompletableFuture.runAsync(() -> {
            cache.loadPlayer(event.getPlayer().getUniqueId());
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save perks synchronously (blocking)
        cache.savePlayer(event.getPlayer().getUniqueId());
    }
}
```

## Data Flow

### Toggle Perk (Instant)
```
1. Player clicks toggle button
2. Update PlayerPerk.enabled in memory
3. Mark player as dirty
4. Return success immediately
5. Save to DB on player leave
```

### Unlock Perk (DB Write)
```
1. Player clicks unlock button
2. Check requirements
3. Create new PlayerPerk entity
4. Save to DB (async)
5. Add to cache
6. Activate perk handlers
7. Return success
```

### Check Perk Status (Instant)
```
1. Get perks from cache
2. Filter/check in memory
3. Return result immediately
```

## Migration Strategy

### Phase 1: Create SimplePerkCache
- Create new `SimplePerkCache` class
- Implement load/save methods
- Add dirty tracking
- Add auto-save task

### Phase 2: Update PerkManagementService
- Replace complex caching with SimplePerkCache
- Remove RetryableOperation usage
- Simplify toggle/enable/disable methods
- Keep unlock as DB operation

### Phase 3: Update Listeners
- Simplify PerkCacheListener
- Add join/leave handlers
- Remove complex retry logic

### Phase 4: Cleanup
- Delete RetryableOperation class
- Remove old PlayerPerkCache complexity
- Update tests
- Update documentation

## Performance Improvements

| Operation | Before | After | Improvement |
|-----------|--------|-------|-------------|
| Toggle Perk | 500ms-2min | <50ms | 10-2400x faster |
| Check Status | 100-500ms | <10ms | 10-50x faster |
| Unlock Perk | 2+ minutes | <1s | 120x+ faster |
| Load on Join | N/A | 200-500ms | One-time cost |

## Error Handling

### Player Leave During Save
- Block player quit until save completes
- Timeout after 5 seconds
- Log warning if save fails

### Server Crash
- Auto-save every 5 minutes
- Most recent changes may be lost
- Acceptable trade-off for performance

### Database Errors
- Log error
- Keep data in memory
- Retry on next auto-save

## Configuration

```yaml
perk_system:
  cache:
    enabled: true
    auto_save_interval_minutes: 5
    save_timeout_seconds: 5
    log_performance: true
```

## Compatibility

- Works with existing PlayerPerk entities
- Compatible with current perk handlers
- No changes to perk configuration files
- No database schema changes needed
