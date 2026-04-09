# Design Document

## Overview

This design document details the architecture and implementation approach for enhancing the RCore Statistics Delivery System to comprehensively collect and transmit all vanilla Minecraft statistics. The enhancement integrates seamlessly with the existing statistics infrastructure while adding specialized components for efficient vanilla statistic collection, caching, and delivery.

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Minecraft Server (Bukkit/Paper)               │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         VanillaStatisticCollectionService                 │  │
│  │  (Main orchestrator for vanilla statistic collection)     │  │
│  └────────────┬─────────────────────────────────────────────┘  │
│               │                                                  │
│  ┌────────────▼──────────────┐  ┌──────────────────────────┐  │
│  │  VanillaStatisticCollector │  │  StatisticCacheManager   │  │
│  │  - collectAll()            │◄─┤  - cache previous values │  │
│  │  - collectDelta()          │  │  - compute deltas        │  │
│  │  - collectByCategory()     │  │  - persist to disk       │  │
│  └────────────┬───────────────┘  └──────────────────────────┘  │
│               │                                                  │
│  ┌────────────▼───────────────────────────────────────────┐   │
│  │         Category-Specific Collectors                    │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  BlockStatisticCollector    │  ItemStatisticCollector   │   │
│  │  - MINE_BLOCK by material   │  - CRAFT_ITEM by material │   │
│  │  - USE_ITEM (placeable)     │  - USE_ITEM by material   │   │
│  │                              │  - BREAK_ITEM by material │   │
│  ├──────────────────────────────┼───────────────────────────┤   │
│  │  MobStatisticCollector      │  TravelStatisticCollector │   │
│  │  - KILL_ENTITY by type      │  - All movement types     │   │
│  │  - ENTITY_KILLED_BY         │  - Aggregated totals      │   │
│  ├──────────────────────────────┼───────────────────────────┤   │
│  │  GeneralStatisticCollector  │  InteractionCollector     │   │
│  │  - DEATHS, JUMPS, etc.      │  - Anvil, Beacon, etc.    │   │
│  └─────────────────────────────┴───────────────────────────┘   │
│               │                                                  │
│  ┌────────────▼───────────────────────────────────────────┐   │
│  │         StatisticAggregationEngine                      │   │
│  │  - Compute totals (blocks, distance, kills)            │   │
│  │  - Compute rates (per minute)                          │   │
│  │  - Custom aggregates from config                       │   │
│  └────────────┬───────────────────────────────────────────┘   │
│               │                                                  │
│  ┌────────────▼───────────────────────────────────────────┐   │
│  │         Existing Statistics Delivery System             │   │
│  │  - StatisticsQueueManager (priority queuing)           │   │
│  │  - StatisticsDeliveryEngine (batching, compression)    │   │
│  │  - RCentralApiClient (backend communication)           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Backend API     │
                    │  /api/statistics │
                    └──────────────────┘
```

## Core Components

### 1. VanillaStatisticCollectionService

**Purpose**: Main orchestrator for vanilla statistic collection lifecycle.

**Responsibilities**:
- Initialize and manage all category-specific collectors
- Schedule periodic collection tasks
- Handle event-driven collection triggers
- Coordinate with cache manager for delta computation
- Interface with existing delivery system

**Key Methods**:
```java
public class VanillaStatisticCollectionService {
    void initialize();
    void shutdown();
    CompletableFuture<CollectionResult> collectAll();
    CompletableFuture<CollectionResult> collectForPlayer(UUID playerId);
    CompletableFuture<CollectionResult> collectDelta();
    void schedulePeriodicCollection();
    void handlePlayerDisconnect(Player player);
    void handlePlayerDeath(Player player);
    CollectionStatistics getStatistics();
}
```

**Configuration**:
```yaml
vanilla-statistics:
  enabled: true
  collection-frequency: 60  # seconds
  categories:
    blocks:
      enabled: true
      frequency: 60
    items:
      enabled: true
      frequency: 60
    mobs:
      enabled: true
      frequency: 60
    travel:
      enabled: true
      frequency: 30
    general:
      enabled: true
      frequency: 60
    interactions:
      enabled: true
      frequency: 120
  delta-threshold: 5
  batch-size: 100
  parallel-threads: 4
  tps-throttling:
    enabled: true
    pause-below: 15
    reduce-below: 18
```

### 2. VanillaStatisticCollector

**Purpose**: Core collection logic for reading Bukkit statistics.

**Responsibilities**:
- Access player statistics via Bukkit API
- Handle version-specific statistic availability
- Map statistic names to consistent identifiers
- Filter statistics based on configuration

**Key Methods**:
```java
public class VanillaStatisticCollector {
    List<QueuedStatistic> collectForPlayer(Player player, StatisticCategory category);
    List<QueuedStatistic> collectAllForPlayer(Player player);
    boolean isStatisticAvailable(Statistic stat);
    String mapStatisticIdentifier(Statistic stat, Material material, EntityType entity);
    boolean shouldCollect(Statistic stat, StatisticCategory category);
}
```

**Statistic Identifier Mapping**:
```
Format: minecraft.<category>.<statistic>[.<subtype>]

Examples:
- MINE_BLOCK + STONE → "minecraft.blocks.mined.stone"
- KILL_ENTITY + ZOMBIE → "minecraft.mobs.killed.zombie"
- WALK_ONE_CM → "minecraft.travel.walk"
- DEATHS → "minecraft.general.deaths"
- CRAFT_ITEM + DIAMOND_SWORD → "minecraft.items.crafted.diamond_sword"
```

### 3. Category-Specific Collectors

#### BlockStatisticCollector

**Purpose**: Collect block-related statistics.

**Statistics Collected**:
- `MINE_BLOCK` for all mineable materials
- `USE_ITEM` for placeable blocks
- Aggregates: total_blocks_mined, total_blocks_placed

**Implementation**:
```java
public class BlockStatisticCollector {
    List<QueuedStatistic> collectBlockStatistics(Player player) {
        List<QueuedStatistic> stats = new ArrayList<>();
        
        // Collect MINE_BLOCK for all materials
        for (Material material : Material.values()) {
            if (material.isBlock() && isMineable(material)) {
                int value = player.getStatistic(Statistic.MINE_BLOCK, material);
                if (value > 0) {
                    stats.add(createStatistic(
                        "minecraft.blocks.mined." + material.name().toLowerCase(),
                        value,
                        player.getUniqueId()
                    ));
                }
            }
        }
        
        // Aggregate total
        int totalMined = stats.stream()
            .mapToInt(s -> (Integer) s.value())
            .sum();
        stats.add(createStatistic("minecraft.blocks.mined.total", totalMined, player.getUniqueId()));
        
        return stats;
    }
}
```

#### ItemStatisticCollector

**Purpose**: Collect item-related statistics.

**Statistics Collected**:
- `CRAFT_ITEM` for all craftable items
- `USE_ITEM` for all usable items
- `BREAK_ITEM` for all breakable items
- `PICKUP` for all items
- `DROP` for all items
- Aggregates: total_items_crafted, total_items_used

#### MobStatisticCollector

**Purpose**: Collect mob-related statistics.

**Statistics Collected**:
- `KILL_ENTITY` for all entity types
- `ENTITY_KILLED_BY` for all entity types
- Aggregates: total_mobs_killed, total_deaths_by_mob

#### TravelStatisticCollector

**Purpose**: Collect travel/movement statistics.

**Statistics Collected**:
- All movement types: WALK, SPRINT, SWIM, FLY, CLIMB, FALL, AVIATE, etc.
- Aggregates: total_distance_traveled

#### GeneralStatisticCollector

**Purpose**: Collect general gameplay statistics.

**Statistics Collected**:
- DEATHS, PLAYER_KILLS, MOB_KILLS
- ANIMALS_BRED, FISH_CAUGHT
- PLAY_ONE_MINUTE, TIME_SINCE_DEATH
- JUMP, SNEAK_TIME

#### InteractionStatisticCollector

**Purpose**: Collect block interaction statistics.

**Statistics Collected**:
- INTERACT_WITH_ANVIL, INTERACT_WITH_BEACON
- INTERACT_WITH_CRAFTING_TABLE, INTERACT_WITH_FURNACE
- All other interaction statistics

### 4. StatisticCacheManager

**Purpose**: Manage caching of previous statistic values for delta computation.

**Responsibilities**:
- Cache previous values per player
- Compute deltas between current and cached values
- Persist cache to disk for server restart survival
- Clear cache on player disconnect

**Data Structure**:
```java
public class StatisticCacheManager {
    // In-memory cache: UUID -> (StatisticKey -> Value)
    private final ConcurrentHashMap<UUID, Map<String, Integer>> cache;
    
    // Disk persistence path
    private final Path cacheFile = Paths.get("plugins/RCore/vanilla-stats-cache.json");
    
    Map<String, Integer> getDelta(UUID playerId, Map<String, Integer> currentValues);
    void updateCache(UUID playerId, Map<String, Integer> values);
    void persistCache();
    void loadCache();
    void clearPlayer(UUID playerId);
}
```

**Delta Computation**:
```java
public Map<String, Integer> getDelta(UUID playerId, Map<String, Integer> currentValues) {
    Map<String, Integer> cached = cache.getOrDefault(playerId, Collections.emptyMap());
    Map<String, Integer> deltas = new HashMap<>();
    
    for (Map.Entry<String, Integer> entry : currentValues.entrySet()) {
        String key = entry.getKey();
        int currentValue = entry.getValue();
        int cachedValue = cached.getOrDefault(key, 0);
        int delta = currentValue - cachedValue;
        
        // Only include if delta exceeds threshold
        if (Math.abs(delta) >= config.getDeltaThreshold()) {
            deltas.put(key, delta);
        }
    }
    
    return deltas;
}
```

**Persistence Format** (JSON):
```json
{
  "version": 1,
  "lastSaved": 1234567890,
  "players": {
    "uuid-1": {
      "minecraft.blocks.mined.stone": 1500,
      "minecraft.travel.walk": 50000,
      "minecraft.mobs.killed.zombie": 250
    },
    "uuid-2": {
      "minecraft.blocks.mined.diamond_ore": 45,
      "minecraft.items.crafted.diamond_sword": 3
    }
  }
}
```

### 5. StatisticAggregationEngine

**Purpose**: Compute aggregate and derived statistics.

**Responsibilities**:
- Compute totals across categories
- Compute rate statistics (per minute)
- Support custom aggregate definitions

**Aggregates**:
```java
public class StatisticAggregationEngine {
    AggregatedStatistics computeAggregates(List<QueuedStatistic> stats, Player player) {
        return AggregatedStatistics.builder()
            .totalBlocksBroken(sumByPrefix(stats, "minecraft.blocks.mined."))
            .totalBlocksPlaced(sumByPrefix(stats, "minecraft.blocks.placed."))
            .totalItemsCrafted(sumByPrefix(stats, "minecraft.items.crafted."))
            .totalDistanceTraveled(sumByPrefix(stats, "minecraft.travel."))
            .totalMobKills(sumByPrefix(stats, "minecraft.mobs.killed."))
            .blocksPerMinute(computeRate(totalBlocksBroken, player.getStatistic(Statistic.PLAY_ONE_MINUTE)))
            .distancePerMinute(computeRate(totalDistanceTraveled, player.getStatistic(Statistic.PLAY_ONE_MINUTE)))
            .customAggregates(computeCustomAggregates(stats))
            .build();
    }
}
```

**Custom Aggregates** (from config):
```yaml
custom-aggregates:
  total_ores_mined:
    type: sum
    statistics:
      - minecraft.blocks.mined.diamond_ore
      - minecraft.blocks.mined.iron_ore
      - minecraft.blocks.mined.gold_ore
      - minecraft.blocks.mined.coal_ore
  pvp_score:
    type: formula
    formula: "(player_kills * 10) - (deaths * 5)"
```

### 6. Collection Scheduling

**Periodic Collection**:
```java
public class CollectionScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    void schedulePeriodicCollection() {
        // Main collection task
        scheduler.scheduleAtFixedRate(
            this::collectAllPlayers,
            config.getCollectionFrequency(),
            config.getCollectionFrequency(),
            TimeUnit.SECONDS
        );
        
        // Category-specific tasks (if different frequencies)
        if (config.getTravelFrequency() != config.getCollectionFrequency()) {
            scheduler.scheduleAtFixedRate(
                () -> collectCategory(StatisticCategory.TRAVEL),
                config.getTravelFrequency(),
                config.getTravelFrequency(),
                TimeUnit.SECONDS
            );
        }
        
        // Cache persistence task
        scheduler.scheduleAtFixedRate(
            cacheManager::persistCache,
            5,
            5,
            TimeUnit.MINUTES
        );
    }
}
```

**Event-Driven Collection**:
```java
@EventHandler(priority = EventPriority.MONITOR)
public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    
    // Full snapshot with HIGH priority
    collectionService.collectForPlayer(player.getUniqueId())
        .thenAccept(result -> {
            queueManager.enqueueBatch(result.getStatistics(), DeliveryPriority.HIGH);
            cacheManager.clearPlayer(player.getUniqueId());
        });
}

@EventHandler(priority = EventPriority.MONITOR)
public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    
    // Collect death-related stats with HIGH priority
    List<QueuedStatistic> deathStats = List.of(
        collectStatistic(player, Statistic.DEATHS),
        collectStatistic(player, Statistic.TIME_SINCE_DEATH),
        // ... other death-related stats
    );
    
    queueManager.enqueueBatch(deathStats, DeliveryPriority.HIGH);
}
```

### 7. Performance Optimization

**Batch Collection**:
```java
public CompletableFuture<CollectionResult> collectAllPlayers() {
    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
    
    // Filter out AFK players
    onlinePlayers.removeIf(p -> isAFK(p, config.getAfkThreshold()));
    
    // Split into batches
    List<List<Player>> batches = Lists.partition(onlinePlayers, config.getBatchSize());
    
    // Process batches in parallel
    ExecutorService executor = Executors.newFixedThreadPool(config.getParallelThreads());
    
    List<CompletableFuture<List<QueuedStatistic>>> futures = batches.stream()
        .map(batch -> CompletableFuture.supplyAsync(() -> collectBatch(batch), executor))
        .toList();
    
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList())
        .thenApply(stats -> new CollectionResult(stats, System.currentTimeMillis()));
}
```

**TPS-Based Throttling**:
```java
public class TPSThrottler {
    private final Server server;
    
    boolean shouldCollect() {
        double tps = server.getTPS()[0]; // 1-minute average
        
        if (tps < config.getPauseThreshold()) {
            return false; // Pause collection
        }
        
        if (tps < config.getReduceThreshold()) {
            // Reduce frequency by 50%
            return ThreadLocalRandom.current().nextBoolean();
        }
        
        return true;
    }
}
```

### 8. Version Compatibility

**Version Detection**:
```java
public class MinecraftVersionDetector {
    private final String version;
    private final int majorVersion;
    private final int minorVersion;
    
    public MinecraftVersionDetector() {
        this.version = Bukkit.getVersion();
        // Parse version string: "1.20.4" -> major=20, minor=4
        this.majorVersion = parseVersion();
        this.minorVersion = parseMinorVersion();
    }
    
    boolean isStatisticAvailable(Statistic stat) {
        // Check if statistic exists in current version
        try {
            Statistic.valueOf(stat.name());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

**Statistic Mapping**:
```java
public class StatisticMapper {
    private final Map<String, String> versionMappings = new HashMap<>();
    
    public StatisticMapper() {
        // Handle renamed statistics across versions
        versionMappings.put("PLAY_ONE_TICK", "PLAY_ONE_MINUTE"); // 1.13+
        // ... other mappings
    }
    
    String mapStatistic(Statistic stat, int version) {
        String name = stat.name();
        return versionMappings.getOrDefault(name, name);
    }
}
```

### 9. Integration with Existing System

**Queue Integration**:
```java
// Vanilla statistics use the same queue system
List<QueuedStatistic> vanillaStats = collector.collectForPlayer(player);

// Queue with appropriate priority
queueManager.enqueueBatch(vanillaStats, DeliveryPriority.NORMAL);

// Existing delivery engine handles transmission
deliveryEngine.deliver(vanillaStats);
```

**Payload Format**:
```json
{
  "serverUuid": "server-123",
  "batchId": "batch-456",
  "timestamp": 1234567890,
  "entries": [
    {
      "playerUuid": "player-uuid",
      "statisticKey": "minecraft.blocks.mined.stone",
      "value": 1500,
      "dataType": "NUMBER",
      "collectionTimestamp": 1234567890,
      "isDelta": true,
      "sourcePlugin": "RCore-Vanilla"
    }
  ],
  "aggregates": {
    "totalBlocksBroken": 5000,
    "totalDistanceTraveled": 100000,
    "blocksPerMinute": 25.5
  }
}
```

### 10. Monitoring and Diagnostics

**Collection Metrics**:
```java
public class CollectionMetrics {
    private final AtomicLong totalCollections = new AtomicLong();
    private final AtomicLong totalStatistics = new AtomicLong();
    private final AtomicLong totalDuration = new AtomicLong();
    private final AtomicInteger cacheSize = new AtomicInteger();
    
    public CollectionStatistics getStatistics() {
        long collections = totalCollections.get();
        return new CollectionStatistics(
            collections,
            totalStatistics.get(),
            collections > 0 ? totalDuration.get() / collections : 0,
            cacheSize.get()
        );
    }
}
```

**Admin Commands**:
```
/rcstats vanilla status
  - Last collection: 5 seconds ago
  - Statistics collected: 1,234
  - Collection duration: 45ms
  - Cache size: 150 players
  - Next collection: in 55 seconds

/rcstats vanilla collect [player]
  - Trigger immediate collection

/rcstats vanilla cache clear [player]
  - Clear cache for player or all players

/rcstats vanilla metrics
  - Total collections: 1,000
  - Total statistics: 500,000
  - Average duration: 42ms
  - Cache hit rate: 95%
```

## Data Flow

### Periodic Collection Flow

```
1. Scheduler triggers collection
2. TPSThrottler checks if collection should proceed
3. VanillaStatisticCollectionService.collectAll()
4. For each online player (in parallel batches):
   a. Category collectors gather statistics
   b. StatisticCacheManager computes deltas
   c. StatisticAggregationEngine computes aggregates
5. QueuedStatistics created with NORMAL priority
6. StatisticsQueueManager enqueues statistics
7. StatisticsDeliveryEngine batches and transmits
8. StatisticCacheManager updates cache
9. Metrics updated
```

### Event-Driven Collection Flow

```
1. Player disconnects (PlayerQuitEvent)
2. VanillaStatisticCollectionService.handlePlayerDisconnect()
3. Full snapshot collection (all categories)
4. QueuedStatistics created with HIGH priority
5. StatisticsQueueManager enqueues with priority
6. StatisticsDeliveryEngine processes immediately
7. StatisticCacheManager clears player cache
8. Metrics updated
```

## Configuration Schema

```yaml
statistics-delivery:
  vanilla-statistics:
    enabled: true
    
    # Collection frequency
    collection-frequency: 60  # seconds
    
    # Category-specific settings
    categories:
      blocks:
        enabled: true
        frequency: 60  # override global frequency
        materials:
          whitelist: []  # empty = all
          blacklist: []
      items:
        enabled: true
        frequency: 60
      mobs:
        enabled: true
        frequency: 60
        entities:
          whitelist: []
          blacklist: ["ENDER_DRAGON"]  # exclude boss mobs
      travel:
        enabled: true
        frequency: 30  # more frequent for travel
      general:
        enabled: true
        frequency: 60
      interactions:
        enabled: true
        frequency: 120  # less frequent
    
    # Delta computation
    delta-threshold: 5  # minimum change to transmit
    
    # Performance
    batch-size: 100  # players per batch
    parallel-threads: 4
    afk-threshold: 600  # seconds, skip AFK players
    
    # TPS throttling
    tps-throttling:
      enabled: true
      pause-below: 15.0
      reduce-below: 18.0
    
    # Privacy
    privacy:
      enabled: false
      anonymize-uuids: false
      excluded-statistics:
        - LEAVE_GAME
        - TIME_SINCE_REST
    
    # Custom aggregates
    custom-aggregates:
      total_ores_mined:
        type: sum
        statistics:
          - minecraft.blocks.mined.diamond_ore
          - minecraft.blocks.mined.iron_ore
          - minecraft.blocks.mined.gold_ore
```

## Error Handling

**Statistic Access Errors**:
```java
try {
    int value = player.getStatistic(stat, material);
    return createStatistic(key, value, playerId);
} catch (IllegalArgumentException e) {
    // Statistic doesn't exist in this version
    logger.fine("Statistic not available: " + stat.name());
    return null;
} catch (Exception e) {
    // Unexpected error
    logger.warning("Failed to collect statistic " + stat.name() + " for player " + playerId);
    return null;
}
```

**Collection Failures**:
```java
try {
    List<QueuedStatistic> stats = collectForPlayer(player);
    return CompletableFuture.completedFuture(stats);
} catch (Exception e) {
    logger.severe("Collection failed for player " + player.getName() + ": " + e.getMessage());
    metrics.recordFailure();
    return CompletableFuture.completedFuture(Collections.emptyList());
}
```

## Testing Strategy

**Unit Tests**:
- StatisticCacheManager delta computation
- StatisticAggregationEngine aggregate calculations
- Version compatibility checks
- Identifier mapping

**Integration Tests**:
- Full collection flow with mock players
- Event-driven collection triggers
- Queue integration
- Cache persistence and recovery

**Performance Tests**:
- Collection duration for 100 players
- Memory usage with large caches
- TPS impact measurement

## Migration Path

1. Deploy with `vanilla-statistics.enabled: false`
2. Enable for testing on development server
3. Monitor performance metrics
4. Gradually enable categories
5. Full rollout with monitoring

## Summary

This design provides a comprehensive, performant, and maintainable solution for collecting all vanilla Minecraft statistics. Key features:

- **Modular**: Category-specific collectors for maintainability
- **Efficient**: Delta-based transmission, batch processing, parallel collection
- **Performant**: TPS-based throttling, configurable frequencies, AFK filtering
- **Compatible**: Version detection, graceful handling of missing statistics
- **Integrated**: Seamless use of existing delivery infrastructure
- **Observable**: Comprehensive metrics and admin commands
- **Configurable**: Granular control over collection behavior
