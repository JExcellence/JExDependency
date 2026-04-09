# Implementation Plan

## Overview

This implementation plan breaks down the Minecraft Statistics Enhancement into manageable tasks. Tasks are organized by component and should be implemented in the order presented to ensure dependencies are satisfied.

## Task List

- [x] 1. Create core data structures and configuration






  - [x] 1.1 Create StatisticCategory enum

    - Create `com.raindropcentral.core.service.statistics.vanilla.StatisticCategory` enum
    - Define categories: BLOCKS, ITEMS, MOBS, TRAVEL, GENERAL, INTERACTIONS
    - _Requirements: 1.1, 2.3, 7.1_
  
  - [x] 1.2 Create VanillaStatisticConfig class


    - Create `com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig`
    - Include collection frequency, category settings, delta threshold, batch size, parallel threads
    - Include TPS throttling settings, privacy settings, custom aggregates
    - Add YAML configuration loading from plugin config
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_
  
  - [x] 1.3 Create CollectionResult record


    - Create `com.raindropcentral.core.service.statistics.vanilla.CollectionResult` record
    - Include statistics list, collection timestamp, duration, player count
    - _Requirements: 1.1, 10.1_
  
  - [x] 1.4 Create CollectionStatistics record


    - Create `com.raindropcentral.core.service.statistics.vanilla.CollectionStatistics` record
    - Include total collections, total statistics, average duration, cache size
    - _Requirements: 10.1, 10.4_

- [x] 2. Implement version compatibility layer





  - [x] 2.1 Create MinecraftVersionDetector


    - Create `com.raindropcentral.core.service.statistics.vanilla.version.MinecraftVersionDetector`
    - Use ServerEnvironment class.
    - Detect Minecraft version on startup
    - Parse version string to extract major/minor version numbers
    - _Requirements: 11.1, 11.5_
  

  - [x] 2.2 Create StatisticAvailabilityChecker

    - Create `com.raindropcentral.core.service.statistics.vanilla.version.StatisticAvailabilityChecker`
    - Check if specific statistics exist in current version
    - Handle IllegalArgumentException gracefully
    - Log warnings for unavailable statistics
    - _Requirements: 1.6, 11.2, 11.6_
  
  - [x] 2.3 Create StatisticMapper


    - Create `com.raindropcentral.core.service.statistics.vanilla.version.StatisticMapper`
    - Map version-specific statistic names to consistent identifiers
    - Handle renamed statistics across versions
    - _Requirements: 1.7, 11.3, 11.4_

- [x] 3. Implement statistic cache management





  - [x] 3.1 Create StatisticCacheManager

    - Create `com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager`
    - Implement in-memory cache using ConcurrentHashMap<UUID, Map<String, Integer>>
    - Implement `getDelta()` method for computing deltas
    - Implement `updateCache()` method for updating cached values
    - Implement `clearPlayer()` method for removing player from cache
    - _Requirements: 3.1, 3.2, 3.3, 3.5_
  

  - [x] 3.2 Implement cache persistence


    - Implement `persistCache()` method using JSON serialization
    - Save to `plugins/RCore/vanilla-stats-cache.json`
    - Implement `loadCache()` method for loading on startup
    - Schedule periodic persistence every 5 minutes
    - _Requirements: 3.6, 3.7_
  

  - [x] 3.3 Implement delta threshold filtering


    - Apply configurable delta threshold in `getDelta()` method
    - Only include statistics where |delta| >= threshold
    - _Requirements: 3.4_

- [x] 4. Implement category-specific collectors






  - [x] 4.1 Create BlockStatisticCollector

    - Create `com.raindropcentral.core.service.statistics.vanilla.collector.BlockStatisticCollector`
    - Implement `collectBlockStatistics(Player)` method
    - Collect MINE_BLOCK for all mineable materials
    - Collect USE_ITEM for placeable blocks
    - Compute aggregate: total_blocks_mined
    - Map to identifiers: "minecraft.blocks.mined.<material>"
    - _Requirements: 1.2, 1.7, 4.1_
  

  - [x] 4.2 Create ItemStatisticCollector


    - Create `com.raindropcentral.core.service.statistics.vanilla.collector.ItemStatisticCollector`
    - Implement `collectItemStatistics(Player)` method
    - Collect CRAFT_ITEM, USE_ITEM, BREAK_ITEM, PICKUP, DROP for all materials
    - Compute aggregates: total_items_crafted, total_items_used
    - Map to identifiers: "minecraft.items.<action>.<material>"
    - _Requirements: 1.2, 1.7, 4.1_
  

  - [x] 4.3 Create MobStatisticCollector

    - Create `com.raindropcentral.core.service.statistics.vanilla.collector.MobStatisticCollector`
    - Implement `collectMobStatistics(Player)` method
    - Collect KILL_ENTITY, ENTITY_KILLED_BY for all entity types
    - Compute aggregates: total_mobs_killed, total_deaths_by_mob
    - Map to identifiers: "minecraft.mobs.<action>.<entity>"
    - _Requirements: 1.3, 1.7, 4.1_
  

  - [x] 4.4 Create TravelStatisticCollector

    - Create `com.raindropcentral.core.service.statistics.vanilla.collector.TravelStatisticCollector`
    - Implement `collectTravelStatistics(Player)` method
    - Collect all movement statistics: WALK_ONE_CM, SPRINT_ONE_CM, SWIM_ONE_CM, FLY_ONE_CM, etc.
    - Compute aggregate: total_distance_traveled
    - Map to identifiers: "minecraft.travel.<method>"
    - _Requirements: 1.4, 1.7, 4.1_
  
  - [x] 4.5 Create GeneralStatisticCollector


    - Create `com.raindropcentral.core.service.statistics.vanilla.collector.GeneralStatisticCollector`
    - Implement `collectGeneralStatistics(Player)` method
    - Collect DEATHS, PLAYER_KILLS, MOB_KILLS, ANIMALS_BRED, FISH_CAUGHT, etc.
    - Collect PLAY_ONE_MINUTE, TIME_SINCE_DEATH, TIME_SINCE_REST, SNEAK_TIME, JUMP
    - Map to identifiers: "minecraft.general.<statistic>"
    - _Requirements: 1.4, 1.7, 4.1_
  
  - [x] 4.6 Create InteractionStatisticCollector


    - Create `com.raindropcentral.core.service.statistics.vanilla.collector.InteractionStatisticCollector`
    - Implement `collectInteractionStatistics(Player)` method
    - Collect all INTERACT_WITH_* statistics
    - Collect ITEM_ENCHANTED, TRADED_WITH_VILLAGER, TARGET_HIT
    - Map to identifiers: "minecraft.interactions.<interaction>"
    - _Requirements: 1.5, 1.7, 4.1_

- [x] 5. Implement core collection logic




  - [x] 5.1 Create VanillaStatisticCollector

    - Create `com.raindropcentral.core.service.statistics.vanilla.VanillaStatisticCollector`
    - Inject all category-specific collectors
    - Inject StatisticAvailabilityChecker and StatisticMapper
    - Implement `collectForPlayer(Player, StatisticCategory)` method
    - Implement `collectAllForPlayer(Player)` method
    - Implement filtering based on VanillaStatisticConfig
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 7.1, 7.2, 7.3_
  
  - [x] 5.2 Implement statistic filtering

    - Apply category enable/disable from config
    - Apply material/entity whitelist/blacklist from config
    - Apply excluded statistics from privacy config
    - _Requirements: 7.1, 7.2, 7.3, 7.6_

- [x] 6. Implement aggregation engine




  - [x] 6.1 Create StatisticAggregationEngine


    - Create `com.raindropcentral.core.service.statistics.vanilla.aggregation.StatisticAggregationEngine`
    - Implement `computeAggregates(List<QueuedStatistic>, Player)` method
    - Compute total_blocks_broken, total_blocks_placed, total_items_crafted
    - Compute total_distance_traveled, total_mob_kills, total_deaths
    - _Requirements: 4.1, 4.2_
  
  - [x] 6.2 Implement rate statistics


    - Compute blocks_per_minute, distance_per_minute, kills_per_minute
    - Use PLAY_ONE_MINUTE statistic for time base
    - _Requirements: 4.4_
  
  - [x] 6.3 Implement custom aggregates


    - Load custom aggregate definitions from config
    - Support "sum" type aggregates
    - Support "formula" type aggregates (optional)
    - _Requirements: 4.5_

- [x] 7. Implement collection scheduling



  - [x] 7.1 Create CollectionScheduler

    - Create `com.raindropcentral.core.service.statistics.vanilla.scheduler.CollectionScheduler`
    - Use ScheduledExecutorService for periodic tasks
    - Schedule main collection task at configured frequency
    - Schedule category-specific tasks if different frequencies configured
    - Schedule cache persistence task every 5 minutes
    - _Requirements: 2.1, 2.2, 3.6_
  

  - [x] 7.2 Implement TPS-based throttling

    - Create `com.raindropcentral.core.service.statistics.vanilla.scheduler.TPSThrottler`
    - Check server TPS before collection
    - Pause collection if TPS < pause threshold
    - Reduce frequency if TPS < reduce threshold
    - _Requirements: 2.6, 9.4, 9.5, 9.6_



- [x] 8. Implement batch collection


  - [x] 8.1 Create BatchCollectionProcessor

    - Create `com.raindropcentral.core.service.statistics.vanilla.batch.BatchCollectionProcessor`
    - Implement `collectAllPlayers()` method
    - Filter out AFK players based on config
    - Split players into batches based on config batch size
    - Process batches in parallel using thread pool
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6_
  
  - [x] 8.2 Implement AFK detection

    - Create `isAFK(Player, int threshold)` method
    - Check player's last movement/interaction time
    - _Requirements: 5.6_
  
  - [x] 8.3 Implement priority-based collection

    - Prioritize players who have been online longest since last collection
    - Track last collection time per player
    - _Requirements: 5.5_

- [x] 9. Implement event-driven collection




  - [x] 9.1 Create EventDrivenCollectionHandler

    - Create `com.raindropcentral.core.service.statistics.vanilla.event.EventDrivenCollectionHandler`
    - Implement Bukkit Listener interface
    - Register with plugin on initialization
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [x] 9.2 Implement PlayerQuitEvent handler

    - Capture full snapshot on player disconnect
    - Queue with HIGH priority
    - Clear player from cache
    - _Requirements: 6.1_
  
  - [x] 9.3 Implement PlayerDeathEvent handler

    - Collect death-related statistics
    - Queue with HIGH priority
    - _Requirements: 6.2_
  
  - [x] 9.4 Implement PlayerAdvancementDoneEvent handler

    - Collect related statistics on advancement completion
    - Queue with NORMAL priority
    - _Requirements: 6.3_
  

  - [x] 9.5 Implement playtime milestone detection


    - Check PLAY_ONE_MINUTE statistic
    - Trigger full snapshot at configured intervals (default every hour)
    - Queue with NORMAL priority
    - _Requirements: 6.4_
  
  - [x] 9.6 Implement event consolidation

    - Track events per player within 5-second window
    - Consolidate multiple events into single collection
    - _Requirements: 6.6_

- [x] 10. Implement main orchestrator service


  - [x] 10.1 Create VanillaStatisticCollectionService


    - Create `com.raindropcentral.core.service.statistics.vanilla.VanillaStatisticCollectionService`
    - Wire all components: collectors, cache manager, aggregation engine, scheduler
    - Implement `initialize()` method
    - Implement `shutdown()` method
    - Implement `collectAll()` method
    - Implement `collectForPlayer(UUID)` method
    - Implement `collectDelta()` method
    - _Requirements: 1.1, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_
  


  - [x] 10.2 Integrate with StatisticsQueueManager
    - Queue collected statistics using existing queue manager
    - Use appropriate priority levels (CRITICAL, HIGH, NORMAL, LOW, BULK)
    - _Requirements: 12.1_

  

  - [x] 10.3 Integrate with StatisticsDeliveryEngine

    - Use existing delivery engine for transmission
    - Ensure vanilla statistics use same BatchPayload format
    - _Requirements: 12.2, 12.6_
  


  - [x] 10.4 Integrate with BackpressureController

    - Check backpressure status before collection
    - Reduce collection frequency if backpressure active
    - _Requirements: 12.4_

- [x] 11. Implement cross-server synchronization






  - [x] 11.1 Implement player join synchronization

    - On PlayerJoinEvent, request latest statistics from backend
    - Use existing RCentralApiClient for request
    - _Requirements: 8.1_
  

  - [x] 11.2 Implement statistic merging

    - Merge received statistics with local values
    - Use HIGHEST_WINS strategy (vanilla stats are cumulative)
    - _Requirements: 8.2_
  

  - [x] 11.3 Implement server-specific tracking

    - Include server identifier with each statistic
    - Support both global and server-specific tracking
    - _Requirements: 8.3, 8.4_
  

  - [x] 11.4 Implement conflict logging

    - Log conflicts when statistics differ between servers
    - Log resolution strategy used
    - _Requirements: 8.5_
  
  - [x] 11.5 Implement sync caching


    - Cache synchronized statistics for 5 minutes
    - Avoid redundant backend requests
    - _Requirements: 8.6_

- [x] 12. Implement monitoring and diagnostics






  - [x] 12.1 Create CollectionMetrics tracker

    - Create `com.raindropcentral.core.service.statistics.vanilla.monitoring.CollectionMetrics`
    - Track total collections, total statistics, total duration
    - Track cache size, cache hit rate
    - Implement `getStatistics()` method
    - _Requirements: 10.1, 10.4_
  

  - [x] 12.2 Implement performance logging

    - Log warnings when collection duration exceeds 100ms
    - Log errors when statistic access fails
    - _Requirements: 10.2, 10.3_
  

  - [x] 12.3 Create VanillaStatisticsCommand

    - Create `/rcstats vanilla status` subcommand
    - Create `/rcstats vanilla collect [player]` subcommand
    - Create `/rcstats vanilla cache clear [player]` subcommand
    - Create `/rcstats vanilla metrics` subcommand
    - _Requirements: 10.1, 10.5_
  

  - [x] 12.4 Implement health check endpoint

    - Expose collection health status
    - Include in existing health check system
    - _Requirements: 10.6_

- [x] 13. Implement privacy features






  - [x] 13.1 Implement UUID anonymization



    - Create `anonymizeUUID(UUID)` method
    - Apply when privacy mode enabled
    - _Requirements: 7.5_
  
  - [x] 13.2 Implement per-player opt-out


    - Check player opt-out flag before collection
    - Store opt-out flags in player data
    - _Requirements: 7.4_
  

  - [x] 13.3 Implement sensitive statistic filtering

    - Exclude sensitive statistics by default (LEAVE_GAME, TIME_SINCE_REST)
    - Apply additional exclusions from config
    - _Requirements: 7.6_

- [x] 14. Integration with RCore lifecycle








  - [x] 14.1 Integrate into RCoreFreeImpl/RCorePremiumImpl


    - Initialize VanillaStatisticCollectionService after existing services
    - Register EventDrivenCollectionHandler listeners
    - Wire shutdown notification to flush statistics


    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_
  
  - [ ] 14.2 Add configuration section to RCore config.yml
    - Add `vanilla-statistics` section under `statistics-delivery`


    - Include all configuration options from design
    - Set sensible defaults
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_
  
  - [ ] 14.3 Add configuration validation
    - Validate frequency ranges (10-600 seconds)
    - Validate batch size and thread count
    - Validate TPS thresholds
    - Log warnings for invalid configurations
    - _Requirements: 2.1, 2.4_

- [ ] 15. Testing and validation
  - [ ] 15.1 Write unit tests for cache manager
    - Test delta computation
    - Test cache persistence and loading
    - Test cache clearing
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  
  - [ ] 15.2 Write unit tests for collectors
    - Test each category-specific collector
    - Test statistic identifier mapping
    - Test filtering logic
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_
  
  - [ ] 15.3 Write unit tests for aggregation engine
    - Test aggregate calculations
    - Test rate calculations
    - Test custom aggregates
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ] 15.4 Write integration tests for collection flow
    - Test full collection flow with mock players
    - Test event-driven collection
    - Test batch processing
    - Test queue integration
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [ ] 15.5 Write performance tests
    - Test collection duration for 100 players
    - Test memory usage with large caches
    - Measure TPS impact
    - _Requirements: 9.1, 9.2, 9.3, 9.7_
  
  - [ ] 15.6 Write version compatibility tests
    - Test with Minecraft 1.16, 1.17, 1.18, 1.19, 1.20, 1.21
    - Verify graceful handling of missing statistics
    - Verify identifier mapping
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

## Implementation Notes

### Execution Order

1. Start with core data structures and configuration (Task 1)
2. Implement version compatibility layer (Task 2)
3. Implement cache management (Task 3)
4. Implement category-specific collectors (Task 4)
5. Implement core collection logic (Task 5)
6. Implement aggregation engine (Task 6)
7. Implement scheduling (Task 7)
8. Implement batch collection (Task 8)
9. Implement event-driven collection (Task 9)
10. Implement main orchestrator (Task 10)
11. Implement cross-server sync (Task 11)
12. Implement monitoring (Task 12)
13. Implement privacy features (Task 13)
14. Integrate with RCore (Task 14)
15. Testing and validation (Task 15)

### Dependencies

- Tasks 4-6 depend on Tasks 1-3
- Tasks 7-9 depend on Tasks 4-6
- Task 10 depends on Tasks 7-9
- Tasks 11-13 can be done in parallel after Task 10
- Task 14 depends on all previous tasks
- Task 15 should be done incrementally alongside implementation

### Testing Strategy

- Write unit tests for each component as it's implemented
- Write integration tests after Task 10
- Write performance tests after Task 14
- Test on development server before production deployment

### Performance Targets

- Collection duration: < 50ms per player
- Batch collection: < 5s for 100 players
- Memory usage: < 10 MB for 100 players
- TPS impact: < 0.5 TPS drop during collection

### Configuration Defaults

```yaml
vanilla-statistics:
  enabled: true
  collection-frequency: 60
  delta-threshold: 5
  batch-size: 100
  parallel-threads: 4
  afk-threshold: 600
  tps-throttling:
    enabled: true
    pause-below: 15.0
    reduce-below: 18.0
```

## Completion Criteria

The implementation is complete when:

1. All tasks are marked as complete
2. All unit tests pass
3. All integration tests pass
4. Performance tests meet targets
5. Version compatibility tests pass for all supported versions
6. Documentation is updated
7. Configuration examples are provided
8. Successfully deployed and tested on development server
9. Monitoring shows expected behavior
10. No critical bugs reported

## Post-Implementation

After implementation:

1. Monitor collection metrics on production servers
2. Gather feedback from server administrators
3. Optimize based on real-world performance data
4. Consider additional features:
   - Custom statistic definitions
   - Advanced filtering rules
   - Real-time statistic streaming
   - Machine learning-based anomaly detection
