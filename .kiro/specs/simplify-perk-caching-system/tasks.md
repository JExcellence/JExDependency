# Simplify Perk Caching System - Tasks

## Phase 1: Create SimplePerkCache

### Task 1.1: Create SimplePerkCache Class
- [ ] Create `SimplePerkCache.java` in `perk.cache` package
- [ ] Add `ConcurrentHashMap<UUID, List<PlayerPerk>>` for cache
- [ ] Add `Set<UUID>` for dirty tracking
- [ ] Add constructor with repository dependency

### Task 1.2: Implement Load Method
- [ ] Create `loadPlayer(UUID playerId)` method
- [ ] Query all PlayerPerks for player from DB
- [ ] Store in cache HashMap
- [ ] Log load time if performance logging enabled
- [ ] Handle database errors gracefully

### Task 1.3: Implement Save Method
- [ ] Create `savePlayer(UUID playerId)` method
- [ ] Check if player is dirty
- [ ] Get perks from cache
- [ ] Batch update to DB
- [ ] Clear dirty flag
- [ ] Remove from cache
- [ ] Log save time if performance logging enabled

### Task 1.4: Implement Cache Operations
- [ ] Create `getPerks(UUID playerId)` - return from cache
- [ ] Create `getPerk(UUID playerId, Long perkId)` - find in cache
- [ ] Create `updatePerk(UUID playerId, PlayerPerk perk)` - update in cache
- [ ] Create `markDirty(UUID playerId)` - add to dirty set
- [ ] Create `isLoaded(UUID playerId)` - check if in cache

### Task 1.5: Implement Auto-Save
- [ ] Create `autoSaveAll()` method
- [ ] Iterate through dirty players
- [ ] Save each dirty player
- [ ] Log auto-save statistics
- [ ] Schedule task in PerkActivationService

## Phase 2: Update PerkManagementService

### Task 2.1: Inject SimplePerkCache
- [ ] Add SimplePerkCache field
- [ ] Update constructor
- [ ] Remove old PlayerPerkCache references

### Task 2.2: Simplify Toggle Methods
- [ ] Update `enablePerk()` to use cache only
- [ ] Update `disablePerk()` to use cache only
- [ ] Remove CompletableFuture wrapping
- [ ] Return boolean immediately
- [ ] Mark player as dirty

### Task 2.3: Simplify Query Methods
- [ ] Update `getPlayerPerks()` to use cache
- [ ] Update `hasPerk()` to use cache
- [ ] Update `isEnabled()` to use cache
- [ ] Remove DB queries for these methods
- [ ] Return results immediately

### Task 2.4: Keep Unlock as DB Operation
- [ ] Keep `unlockPerk()` as async DB operation
- [ ] After DB save, add to cache
- [ ] Mark player as dirty
- [ ] Return CompletableFuture

### Task 2.5: Remove RetryableOperation
- [ ] Remove all RetryableOperation usage
- [ ] Replace with simple try-catch
- [ ] Log errors without retry
- [ ] Simplify error handling

## Phase 3: Update Listeners and Services

### Task 3.1: Simplify PerkCacheListener
- [ ] Update `onPlayerJoin()` to call `cache.loadPlayer()`
- [ ] Update `onPlayerQuit()` to call `cache.savePlayer()`
- [ ] Remove complex retry logic
- [ ] Add performance logging

### Task 3.2: Update PerkActivationService
- [ ] Update to use SimplePerkCache
- [ ] Schedule auto-save task (every 5 minutes)
- [ ] Remove old cache references
- [ ] Simplify activation logic

### Task 3.3: Update EventPerkHandler
- [ ] Update to check cache for active perks
- [ ] Remove DB queries during event processing
- [ ] Use cache for perk status checks

### Task 3.4: Update Views
- [ ] Update PerkOverviewView to use cache
- [ ] Update PerkDetailView to use cache
- [ ] Remove loading indicators (instant now)
- [ ] Update success messages

## Phase 4: Cleanup and Testing

### Task 4.1: Delete Old Code
- [ ] Delete `RetryableOperation.java`
- [ ] Delete old `PlayerPerkCache.java` (if fully replaced)
- [ ] Delete `PlayerCacheEntry.java` (if not needed)
- [ ] Remove unused imports

### Task 4.2: Update Configuration
- [ ] Add cache settings to PerkSystemSection
- [ ] Add auto_save_interval_minutes
- [ ] Add save_timeout_seconds
- [ ] Add cache_enabled flag

### Task 4.3: Add Logging
- [ ] Add cache hit/miss logging
- [ ] Add performance metrics
- [ ] Add auto-save statistics
- [ ] Use CentralLogger consistently

### Task 4.4: Testing
- [ ] Test player join (perks load)
- [ ] Test toggle perk (instant)
- [ ] Test unlock perk (DB write)
- [ ] Test player leave (perks save)
- [ ] Test auto-save (periodic)
- [ ] Test server crash recovery
- [ ] Test concurrent access

### Task 4.5: Documentation
- [ ] Update JavaDoc for SimplePerkCache
- [ ] Update JavaDoc for PerkManagementService
- [ ] Add code comments for complex logic
- [ ] Update README if needed

## Phase 5: Performance Validation

### Task 5.1: Measure Performance
- [ ] Measure toggle perk time (should be <50ms)
- [ ] Measure unlock perk time (should be <1s)
- [ ] Measure check status time (should be <10ms)
- [ ] Measure load on join time (should be <500ms)

### Task 5.2: Load Testing
- [ ] Test with 10 players
- [ ] Test with 50 players
- [ ] Test with 100 players
- [ ] Verify memory usage is acceptable

### Task 5.3: Optimize if Needed
- [ ] Profile slow operations
- [ ] Optimize DB queries if needed
- [ ] Adjust auto-save interval if needed
- [ ] Tune cache size if needed

## Rollout Plan

1. **Development**: Implement all phases in development environment
2. **Testing**: Thorough testing with multiple players
3. **Staging**: Deploy to staging server for 24 hours
4. **Production**: Deploy during low-traffic period
5. **Monitor**: Watch logs for errors and performance issues
6. **Rollback**: Keep old code available for quick rollback if needed

## Success Metrics

- [ ] Perk toggle < 50ms
- [ ] Perk unlock < 1 second
- [ ] No data loss on player leave
- [ ] No errors in logs
- [ ] Positive player feedback
- [ ] Code is simpler and easier to maintain
