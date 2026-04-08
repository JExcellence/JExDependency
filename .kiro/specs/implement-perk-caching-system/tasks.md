# Implementation Plan

- [x] 1. Create PlayerCacheEntry class


  - Create `PlayerCacheEntry` class with concurrent data structures for storing perks
  - Implement `ConcurrentHashMap<Long, PlayerPerk>` for perk storage
  - Implement `ConcurrentHashMap.newKeySet()` for dirty perk tracking
  - Add `ReentrantReadWriteLock` for thread-safe operations
  - Implement `getPerk()`, `getAllPerks()`, and `getPerks()` methods
  - Implement `updatePerk()` method with dirty tracking
  - Implement `getDirtyPerks()` and `clearDirtyFlags()` methods
  - Implement `addPerk()` and `removePerk()` methods
  - _Requirements: 1.2, 3.1, 3.2, 3.3_



- [ ] 2. Create PlayerPerkCache class
  - Create `PlayerPerkCache` class with `ConcurrentHashMap<UUID, PlayerCacheEntry>` storage
  - Inject `PlayerPerkRepository` and configuration parameters
  - Implement `loadPlayerCache()` method to load all PlayerPerks for a player from DB
  - Implement `getPlayerPerk()` method to retrieve single perk from cache
  - Implement `getAllPlayerPerks()` method to retrieve all perks for a player
  - Implement `getPlayerPerks()` method with predicate filtering
  - Implement `updatePlayerPerk()` method to update cache and mark dirty


  - Implement `isCacheLoaded()` method to check cache status
  - _Requirements: 1.1, 1.2, 2.1, 3.1_

- [ ] 3. Implement cache persistence logic
  - Implement `saveAndUnloadPlayerCache()` method with retry logic
  - Implement exponential backoff for failed save attempts
  - Implement `getDirtyPerks()` filtering to only persist modified entities

  - Implement `clearDirtyFlags()` after successful persistence
  - Implement cache removal after successful save
  - Add comprehensive logging for save operations
  - _Requirements: 1.3, 2.2, 2.3, 2.4, 2.5_

- [x] 4. Implement cache utility methods


  - Implement `flushPlayerCache()` method for manual cache flush
  - Implement `saveAllCaches()` method for server shutdown
  - Add timeout handling for save operations
  - Add performance metrics logging
  - _Requirements: 4.5, 6.2, 6.3_



- [ ] 5. Add cache configuration
  - Add cache configuration section to `PerkSystemSection` config class
  - Add fields for `enabled`, `maxRetries`, `retryDelayMs`, `saveTimeoutSeconds`
  - Add fields for `logPerformance` and `performanceThresholdMs`
  - Load configuration in `PlayerPerkCache` constructor
  - _Requirements: 5.5_


- [ ] 6. Create event listeners for cache lifecycle
  - Create `PerkCacheListener` class implementing Bukkit `Listener`
  - Implement `onPlayerJoin()` event handler with `EventPriority.LOW`
  - Call `PlayerPerkCache.loadPlayerCache()` on player join
  - Implement `onPlayerQuit()` event handler with `EventPriority.MONITOR`
  - Call `PlayerPerkCache.saveAndUnloadPlayerCache()` on player quit



  - Add error handling and logging for both events
  - Register listener in plugin main class
  - _Requirements: 1.1, 1.3, 1.5_

- [ ] 7. Add server shutdown hook
  - Add `onDisable()` override in RDQ plugin main class
  - Call `PlayerPerkCache.saveAllCaches()` on server shutdown
  - Add timeout handling (30 seconds max wait)


  - Add logging for shutdown save operations
  - _Requirements: 4.3_

- [ ] 8. Update PerkManagementService to use cache
  - Inject `PlayerPerkCache` into `PerkManagementService` constructor
  - Update `findByPlayerAndPerk()` to check cache first


  - Add fallback to DB query if cache not loaded
  - Update `grantPerk()` to use cache for existing perks
  - Update `grantPerk()` to add new perks to cache after DB save
  - Update `revokePerk()` to remove from cache
  - Update `enablePerk()` to use cache

  - Update `disablePerk()` to use cache
  - _Requirements: 1.2, 4.1, 5.1, 5.3_

- [ ] 9. Update helper methods in PerkManagementService
  - Update `findUnlockedByPlayer()` to use cache
  - Update `findEnabledByPlayer()` to use cache
  - Update `findActiveByPlayer()` to use cache


  - Add cache-aware filtering using `PlayerPerkCache.getPlayerPerks()`
  - Keep DB fallback for when cache is not loaded
  - _Requirements: 1.2, 5.1_

- [ ] 10. Update PerkActivationService to use cache
  - Update `activate()` method to mark perks as dirty after state changes
  - Update `deactivate()` method to mark perks as dirty after state changes
  - Ensure `recordActivation()` and `recordDeactivation()` are called before cache update
  - Remove direct repository update calls (cache will handle persistence)
  - _Requirements: 1.2, 2.1, 5.2_

- [ ] 11. Add cache performance logging
  - Add logging for cache load time in `loadPlayerCache()`
  - Add logging for cache save time in `saveAndUnloadPlayerCache()`
  - Add logging for cache hit/miss in `getPlayerPerk()`
  - Add warning logs when operations exceed performance threshold
  - Add cache size metrics logging
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 12. Add error handling for edge cases
  - Handle cache load failure by creating empty cache entry
  - Handle cache save failure with retry and final error logging
  - Handle quick reconnect scenario (player rejoins before cache is unloaded)
  - Add validation to prevent operations on unloaded cache
  - _Requirements: 4.1, 4.2, 4.4_

- [ ]* 13. Create admin commands for cache management
  - Create `/rdq cache stats` command to show cache statistics
  - Create `/rdq cache flush <player>` command to manually flush player cache
  - Create `/rdq cache reload <player>` command to reload player cache
  - Add permission checks for admin commands
  - _Requirements: 4.5_

- [ ]* 14. Write unit tests for PlayerCacheEntry
  - Test adding perks to cache entry
  - Test updating perks and verifying dirty tracking
  - Test removing perks from cache entry
  - Test filtering perks with predicates
  - Test concurrent access with multiple threads
  - _Requirements: 3.1, 3.2, 3.3_

- [ ]* 15. Write unit tests for PlayerPerkCache
  - Test loading player cache from repository
  - Test getting perks from cache
  - Test updating perks in cache
  - Test saving and unloading cache
  - Test cache miss scenarios with DB fallback
  - Test concurrent access by multiple threads
  - _Requirements: 1.1, 1.2, 1.3, 3.1_

- [ ]* 16. Write integration tests for cache lifecycle
  - Test complete player join -> perk operations -> player quit flow
  - Verify cache is loaded on join
  - Verify perk operations update cache
  - Verify dirty perks are saved on quit
  - Verify DB state matches cache state after save
  - _Requirements: 1.1, 1.2, 1.3, 2.2_

- [ ]* 17. Write integration tests for failure scenarios
  - Test cache load failure handling
  - Test cache save failure with retry logic
  - Test database unavailable during save
  - Test server shutdown with unsaved caches
  - Verify data integrity in all failure scenarios
  - _Requirements: 4.1, 4.2, 4.3_
