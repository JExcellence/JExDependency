# Requirements Document

## Introduction

This document specifies requirements for enhancing the RCore Statistics Delivery System to comprehensively collect and transmit all vanilla Minecraft statistics with configurable frequency. The system will capture the complete set of 300+ native Minecraft statistics tracked by the client, including detailed breakdowns by material type, entity type, and action category. This enhancement builds upon the existing Statistics Delivery System to provide complete telemetry for player behavior analysis, progression tracking, and cross-server leaderboards.

## Glossary

- **Vanilla_Statistic**: A statistic tracked natively by the Minecraft client via the `org.bukkit.Statistic` enum
- **Material_Statistic**: A vanilla statistic that tracks actions per material type (e.g., blocks broken per block type)
- **Entity_Statistic**: A vanilla statistic that tracks interactions per entity type (e.g., mobs killed per mob type)
- **Statistic_Collector**: Component responsible for reading vanilla statistics from online players
- **Statistic_Delta**: The change in a statistic value since the last collection
- **Collection_Frequency**: The interval at which vanilla statistics are collected from players
- **Statistic_Aggregation**: Computed summary values derived from detailed statistics (e.g., total blocks broken)
- **Statistic_Category**: Grouping of related statistics (Blocks, Items, Mobs, Travel, General, Combat)
- **Batch_Collection**: Collecting statistics from multiple players in a single operation
- **Incremental_Collection**: Collecting only changed statistics since last collection
- **Full_Snapshot**: Complete capture of all statistics for a player at a point in time

## Requirements

### Requirement 1: Comprehensive Vanilla Statistic Collection

**User Story:** As a server administrator, I want all vanilla Minecraft statistics collected from players, so that I have complete visibility into player behavior and can create detailed analytics dashboards.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL collect all statistics from the `org.bukkit.Statistic` enum for each online player.
2. THE Statistic_Collector SHALL collect Material_Statistic values for all applicable materials including: MINE_BLOCK (all mineable blocks), USE_ITEM (all usable items), BREAK_ITEM (all breakable items), CRAFT_ITEM (all craftable items), PICKUP (all items), and DROP (all items).
3. THE Statistic_Collector SHALL collect Entity_Statistic values for all entity types including: KILL_ENTITY (all hostile, neutral, and passive mobs), ENTITY_KILLED_BY (all entities that can kill players).
4. THE Statistic_Collector SHALL collect general statistics including: DEATHS, PLAYER_KILLS, MOB_KILLS, ANIMALS_BRED, FISH_CAUGHT, TREASURE_FISHED, JUNK_FISHED, LEAVE_GAME, PLAY_ONE_MINUTE, TOTAL_WORLD_TIME, TIME_SINCE_DEATH, TIME_SINCE_REST, SNEAK_TIME, JUMP, SPRINT_ONE_CM, CROUCH_ONE_CM, WALK_ONE_CM, WALK_ON_WATER_ONE_CM, FALL_ONE_CM, CLIMB_ONE_CM, FLY_ONE_CM, WALK_UNDER_WATER_ONE_CM, MINECART_ONE_CM, BOAT_ONE_CM, PIG_ONE_CM, HORSE_ONE_CM, AVIATE_ONE_CM, SWIM_ONE_CM, STRIDER_ONE_CM.
5. THE Statistic_Collector SHALL collect item interaction statistics including: ITEM_ENCHANTED, TRADED_WITH_VILLAGER, TARGET_HIT, INTERACT_WITH_ANVIL, INTERACT_WITH_BEACON, INTERACT_WITH_BLAST_FURNACE, INTERACT_WITH_BREWINGSTAND, INTERACT_WITH_CAMPFIRE, INTERACT_WITH_CARTOGRAPHY_TABLE, INTERACT_WITH_CRAFTING_TABLE, INTERACT_WITH_FURNACE, INTERACT_WITH_GRINDSTONE, INTERACT_WITH_LECTERN, INTERACT_WITH_LOOM, INTERACT_WITH_SMITHING_TABLE, INTERACT_WITH_SMOKER, INTERACT_WITH_STONECUTTER.
6. THE Statistic_Collector SHALL handle statistics that do not exist in older Minecraft versions gracefully by skipping them without errors.
7. THE Statistic_Collector SHALL map vanilla statistic names to consistent identifiers for backend storage (e.g., "minecraft.blocks_broken.stone" for MINE_BLOCK with Material.STONE).

### Requirement 2: Configurable Collection Frequency

**User Story:** As a server administrator, I want to configure how frequently vanilla statistics are collected, so that I can balance data freshness with server performance.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL support configurable Collection_Frequency between 10 and 600 seconds with a default of 60 seconds.
2. THE Statistic_Collector SHALL support different collection frequencies for different Statistic_Category groups (e.g., collect travel stats every 30s, block stats every 60s).
3. THE Statistic_Collector SHALL support disabling collection for specific Statistic_Category groups to reduce overhead.
4. WHEN Collection_Frequency is set below 30 seconds, THE Statistic_Collector SHALL log a warning about potential performance impact.
5. THE Statistic_Collector SHALL support on-demand collection via command for immediate statistics capture.
6. THE Statistic_Collector SHALL automatically adjust collection frequency based on server TPS (reduce frequency when TPS drops below 18).

### Requirement 3: Incremental Delta Collection

**User Story:** As a backend developer, I want only changed statistics transmitted to reduce payload size and database writes, so that the system scales efficiently with large player counts.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL maintain a cache of previous statistic values for each online player.
2. WHEN collecting statistics, THE Statistic_Collector SHALL compute Statistic_Delta values by comparing current values with cached values.
3. THE Statistic_Collector SHALL only queue statistics for delivery when the delta is non-zero.
4. THE Statistic_Collector SHALL support a configurable minimum delta threshold to filter out insignificant changes (e.g., only send if delta > 5).
5. THE Statistic_Collector SHALL clear cached values when a player disconnects to free memory.
6. THE Statistic_Collector SHALL persist cached values to disk every 5 minutes to survive server restarts.
7. WHEN a player joins the server, THE Statistic_Collector SHALL load cached values from disk if available, otherwise perform a Full_Snapshot collection.

### Requirement 4: Statistic Aggregation and Summaries

**User Story:** As a dashboard user, I want pre-computed aggregate statistics, so that I can view high-level metrics without querying detailed data.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL compute aggregate statistics including: total_blocks_broken (sum of all MINE_BLOCK values), total_blocks_placed (sum of all USE_ITEM for placeable blocks), total_items_crafted (sum of all CRAFT_ITEM values), total_distance_traveled (sum of all travel statistics), total_mob_kills (sum of all KILL_ENTITY values), total_deaths (DEATHS statistic).
2. THE Statistic_Collector SHALL compute category-specific aggregates for each Statistic_Category.
3. THE Statistic_Collector SHALL include aggregates in the BatchPayload alongside detailed statistics.
4. THE Statistic_Collector SHALL compute rate statistics: blocks_per_minute, distance_per_minute, kills_per_minute based on play time.
5. THE Statistic_Collector SHALL support custom aggregate definitions via configuration (e.g., "total_ores_mined" = sum of diamond, iron, gold, etc.).

### Requirement 5: Batch Collection Optimization

**User Story:** As a server administrator running a high-population server, I want efficient batch collection of statistics from all online players, so that collection overhead is minimized.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL collect statistics from all online players in a single batch operation.
2. THE Statistic_Collector SHALL process players in parallel using a configurable thread pool (default 4 threads).
3. THE Statistic_Collector SHALL limit batch size to a configurable maximum number of players per batch (default 100).
4. WHEN the online player count exceeds the batch size limit, THE Statistic_Collector SHALL split collection into multiple sequential batches.
5. THE Statistic_Collector SHALL prioritize collection for players who have been online longest since last collection.
6. THE Statistic_Collector SHALL skip collection for players who have been AFK for more than a configurable duration (default 10 minutes).
7. THE Statistic_Collector SHALL measure and log collection duration for performance monitoring.

### Requirement 6: Event-Driven Collection Triggers

**User Story:** As a server administrator, I want statistics collected immediately when significant events occur, so that critical data is captured in real-time.

#### Acceptance Criteria

1. WHEN a player disconnects, THE Statistic_Collector SHALL perform a Full_Snapshot collection with HIGH priority.
2. WHEN a player dies, THE Statistic_Collector SHALL collect death-related statistics with HIGH priority.
3. WHEN a player completes an advancement, THE Statistic_Collector SHALL collect related statistics with NORMAL priority.
4. WHEN a player's playtime reaches a milestone (configurable intervals, default every hour), THE Statistic_Collector SHALL perform a Full_Snapshot collection with NORMAL priority.
5. THE Statistic_Collector SHALL support custom event triggers via configuration (e.g., collect on specific custom events).
6. THE Statistic_Collector SHALL consolidate multiple event triggers within a 5-second window to avoid duplicate collections.

### Requirement 7: Statistic Filtering and Privacy

**User Story:** As a server administrator, I want granular control over which vanilla statistics are collected and transmitted, so that I can respect player privacy and comply with data regulations.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL support enabling/disabling collection by Statistic_Category (Blocks, Items, Mobs, Travel, General, Combat).
2. THE Statistic_Collector SHALL support blacklisting specific statistics by name (e.g., exclude LEAVE_GAME for privacy).
3. THE Statistic_Collector SHALL support whitelisting specific materials or entities for Material_Statistic and Entity_Statistic collection.
4. THE Statistic_Collector SHALL support per-player opt-out flags that exclude a player's statistics from collection.
5. THE Statistic_Collector SHALL anonymize player UUIDs in transmitted data when configured for privacy mode.
6. THE Statistic_Collector SHALL exclude statistics marked as sensitive by default (e.g., LEAVE_GAME, TIME_SINCE_REST).

### Requirement 8: Cross-Server Statistic Synchronization

**User Story:** As a network administrator running multiple servers, I want vanilla statistics synchronized across servers, so that players have consistent progression tracking.

#### Acceptance Criteria

1. WHEN a player joins a server, THE Statistic_Collector SHALL request the player's latest vanilla statistics from the backend.
2. THE Statistic_Collector SHALL merge received statistics with local values using the HIGHEST_WINS strategy (vanilla stats are cumulative).
3. THE Statistic_Collector SHALL support server-specific statistic tracking alongside global tracking.
4. THE Statistic_Collector SHALL include a server identifier with each statistic to enable per-server leaderboards.
5. WHEN statistics conflict between servers, THE Statistic_Collector SHALL log the conflict and resolution for audit purposes.
6. THE Statistic_Collector SHALL cache synchronized statistics for 5 minutes to avoid redundant backend requests.

### Requirement 9: Performance and Resource Management

**User Story:** As a server administrator, I want vanilla statistic collection to have minimal performance impact, so that gameplay is not affected.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL complete collection for a single player in less than 50 milliseconds on average.
2. THE Statistic_Collector SHALL complete batch collection for 100 players in less than 5 seconds on average.
3. THE Statistic_Collector SHALL use less than 10 MB of memory for caching statistics for 100 online players.
4. THE Statistic_Collector SHALL automatically reduce collection frequency when server TPS drops below 18.
5. THE Statistic_Collector SHALL pause collection when server TPS drops below 15.
6. THE Statistic_Collector SHALL resume normal collection when server TPS recovers above 19 for at least 30 seconds.
7. THE Statistic_Collector SHALL expose performance metrics via commands: average collection time, cache size, collection frequency.

### Requirement 10: Monitoring and Diagnostics

**User Story:** As a server administrator, I want comprehensive monitoring of vanilla statistic collection, so that I can troubleshoot issues and optimize configuration.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL expose collection status via commands showing: last collection time, statistics collected in last batch, collection duration, cache size, next scheduled collection.
2. THE Statistic_Collector SHALL log warnings when collection duration exceeds 100 milliseconds for a single player.
3. THE Statistic_Collector SHALL log errors when statistic access fails for a player.
4. THE Statistic_Collector SHALL maintain rolling statistics for the current session: total collections, total statistics collected, average collection time, cache hit rate.
5. THE Statistic_Collector SHALL support a diagnostic mode that logs detailed information about each collection operation.
6. THE Statistic_Collector SHALL expose a health check endpoint for monitoring systems.

### Requirement 11: Backward Compatibility and Version Support

**User Story:** As a server administrator running different Minecraft versions, I want the statistic collector to work across all supported versions, so that I can use the same plugin on all servers.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL detect the Minecraft server version on startup.
2. THE Statistic_Collector SHALL only collect statistics that exist in the current Minecraft version.
3. THE Statistic_Collector SHALL map version-specific statistic names to consistent identifiers for backend storage.
4. THE Statistic_Collector SHALL handle renamed or removed statistics gracefully across version upgrades.
5. THE Statistic_Collector SHALL support Minecraft versions 1.16 through 1.21+.
6. THE Statistic_Collector SHALL log warnings when attempting to collect statistics that don't exist in the current version.

### Requirement 12: Integration with Existing Statistics System

**User Story:** As a developer, I want vanilla statistics integrated seamlessly with the existing custom statistics system, so that all statistics are managed consistently.

#### Acceptance Criteria

1. THE Statistic_Collector SHALL use the existing StatisticsQueueManager for queuing vanilla statistics.
2. THE Statistic_Collector SHALL use the existing StatisticsDeliveryEngine for transmitting vanilla statistics.
3. THE Statistic_Collector SHALL use the existing priority system (CRITICAL, HIGH, NORMAL, LOW, BULK) for vanilla statistics.
4. THE Statistic_Collector SHALL integrate with the existing BackpressureController for flow control.
5. THE Statistic_Collector SHALL use the existing RCentralApiClient for backend communication.
6. THE Statistic_Collector SHALL store vanilla statistics in the same BatchPayload format as custom statistics.
7. THE Statistic_Collector SHALL use the existing configuration system for vanilla statistic settings.
