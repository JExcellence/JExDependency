# Requirements Document

## Introduction

This document defines the requirements for an Enterprise Statistics Delivery System that enables Minecraft servers running RCore to transmit comprehensive player statistics, server metrics, and Minecraft native client statistics to the RaindropCentral backend API. The system is designed to handle high-volume scenarios with 10,000+ statistics per server, supporting multi-tier queuing, intelligent batching, priority-based delivery, and real-time analytics integration. It extends the existing server-data communication infrastructure to provide a complete telemetry pipeline for centralized dashboards, cross-server progression tracking, and behavioral analytics.

## Glossary

- **Statistics_Delivery_System**: The enterprise subsystem responsible for collecting, queuing, batching, prioritizing, and transmitting all forms of statistics from Minecraft servers to the RaindropCentral backend API.
- **RCentralApiClient**: The HTTP client component that handles all communication with the RaindropCentral backend.
- **EStatisticType**: The enumeration defining all custom trackable statistic types with their data types, categories, and default values.
- **RPlayerStatistic**: The aggregate entity storing a player's custom statistics locally on the server.
- **StatisticCategory**: A grouping classification for custom statistics (Core, Gameplay, Social, Economy, Progression, PvP, Building, System, Perks, RDQ, Minigames).
- **Minecraft_Native_Statistic**: Statistics tracked by the vanilla Minecraft client including blocks broken, blocks placed, items crafted, distance traveled, mob kills, and other built-in counters.
- **Batch_Payload**: A JSON structure containing multiple player statistics grouped for efficient transmission.
- **Delivery_Queue**: A multi-tier priority queue system managing statistics awaiting transmission.
- **Priority_Level**: Classification determining delivery urgency (CRITICAL, HIGH, NORMAL, LOW, BULK).
- **Delivery_Interval**: The configurable time period between automatic statistics transmissions.
- **Delta_Statistics**: Statistics that have changed since the last successful delivery.
- **Aggregated_Statistic**: A computed statistic derived from multiple source values (averages, totals, rates).
- **Delivery_Window**: A time-bounded period during which statistics are collected before transmission.
- **Backpressure**: A flow control mechanism that slows statistic collection when queues approach capacity.
- **Statistic_Snapshot**: A point-in-time capture of all statistics for a player or server.
- **Delivery_Receipt**: Confirmation from the backend acknowledging successful statistic ingestion.
- **Rate_Limiter**: Component controlling the frequency of API requests to prevent backend overload.

## Requirements

### Requirement 1: Multi-Tier Queue Management

**User Story:** As a server administrator running a high-population server, I want statistics to be queued intelligently based on priority and volume, so that critical data is delivered promptly while bulk data is transmitted efficiently without overwhelming the backend.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL maintain a Delivery_Queue with five Priority_Level tiers: CRITICAL, HIGH, NORMAL, LOW, and BULK.
2. THE Statistics_Delivery_System SHALL process CRITICAL priority statistics within 2 seconds of queuing.
3. THE Statistics_Delivery_System SHALL process HIGH priority statistics within 10 seconds of queuing.
4. THE Statistics_Delivery_System SHALL process NORMAL priority statistics within the configured Delivery_Interval.
5. THE Statistics_Delivery_System SHALL process LOW and BULK priority statistics during periods of low server activity or when queue depth permits.
6. WHILE the Delivery_Queue contains more than 5000 entries, THE Statistics_Delivery_System SHALL activate Backpressure mode, reducing collection frequency by 50 percent.
7. IF the Delivery_Queue reaches 10000 entries, THEN THE Statistics_Delivery_System SHALL pause LOW and BULK priority collection until queue depth falls below 7500.
8. THE Statistics_Delivery_System SHALL persist the Delivery_Queue to disk every 60 seconds to survive server restarts.

### Requirement 2: Minecraft Native Statistics Collection

**User Story:** As a server administrator, I want Minecraft's built-in player statistics (blocks broken, items crafted, distance traveled, etc.) collected and transmitted alongside custom statistics, so that I have a complete picture of player activity.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL collect Minecraft_Native_Statistic values for all online players including: blocks mined by type, blocks placed by type, items crafted by type, items used by type, items broken by type, items picked up by type, items dropped by type, distance traveled by method (walking, sprinting, swimming, flying, climbing, falling, elytra), mob kills by type, deaths by cause, damage dealt by type, damage taken by type, time played, jumps, and fish caught.
2. THE Statistics_Delivery_System SHALL map Minecraft_Native_Statistic values to corresponding EStatisticType entries where applicable.
3. THE Statistics_Delivery_System SHALL aggregate Minecraft_Native_Statistic values into summary statistics (total blocks broken, total distance traveled) in addition to detailed breakdowns.
4. WHEN collecting Minecraft_Native_Statistic values, THE Statistics_Delivery_System SHALL capture delta changes since the last collection rather than absolute values to minimize payload size.
5. THE Statistics_Delivery_System SHALL collect Minecraft_Native_Statistic values at a configurable interval between 30 and 300 seconds.

### Requirement 3: Periodic Statistics Delivery

**User Story:** As a server administrator, I want player statistics to be automatically sent to RaindropCentral at regular intervals, so that the central dashboard reflects up-to-date player data without manual intervention.

#### Acceptance Criteria

1. WHILE the server is connected to RaindropCentral, THE Statistics_Delivery_System SHALL transmit player statistics at a configurable Delivery_Interval between 30 and 3600 seconds with a default of 300 seconds.
2. WHEN the Delivery_Interval elapses, THE Statistics_Delivery_System SHALL collect Delta_Statistics for all online players and queue them for transmission.
3. THE Statistics_Delivery_System SHALL support configurable Delivery_Window sizes to batch statistics collected within a time range.
4. IF the statistics delivery fails due to network error, THEN THE Statistics_Delivery_System SHALL retry the delivery up to 5 times with exponential backoff starting at 2 seconds and capping at 60 seconds.
5. WHEN a delivery attempt succeeds, THE Statistics_Delivery_System SHALL record the delivery timestamp and Delivery_Receipt for each transmitted statistic.
6. THE Statistics_Delivery_System SHALL track delivery latency metrics for performance monitoring.

### Requirement 4: Event-Driven Statistics Delivery

**User Story:** As a server administrator, I want critical statistics to be sent immediately when significant events occur, so that time-sensitive data like player disconnections capture accurate final values.

#### Acceptance Criteria

1. WHEN a player disconnects from the server, THE Statistics_Delivery_System SHALL capture a complete Statistic_Snapshot and queue it with HIGH priority.
2. WHEN a player's statistic value changes by more than a configurable threshold percentage, THE Statistics_Delivery_System SHALL queue the statistic with NORMAL priority.
3. WHEN a player completes a quest, achievement, or level-up, THE Statistics_Delivery_System SHALL queue related statistics with HIGH priority.
4. WHEN a significant economy transaction occurs (configurable threshold), THE Statistics_Delivery_System SHALL queue economy statistics with HIGH priority.
5. IF multiple event-driven statistics are queued within 5 seconds for the same player, THEN THE Statistics_Delivery_System SHALL consolidate them into a single player batch.
6. WHEN a player joins the server, THE Statistics_Delivery_System SHALL request their latest statistics from the backend for cross-server synchronization.

### Requirement 5: Statistics Filtering and Selection

**User Story:** As a server administrator, I want granular control over which statistics are sent to RaindropCentral, so that I can manage bandwidth usage, protect sensitive data, and comply with privacy requirements.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL provide configuration options to include or exclude statistics by StatisticCategory.
2. THE Statistics_Delivery_System SHALL provide configuration options to include or exclude individual EStatisticType entries by key.
3. THE Statistics_Delivery_System SHALL provide configuration options to include or exclude Minecraft_Native_Statistic types by category (general, blocks, items, mobs, travel).
4. WHERE the server administrator has disabled a StatisticCategory, THE Statistics_Delivery_System SHALL exclude all statistics in that category from delivery.
5. THE Statistics_Delivery_System SHALL exclude statistics with StatisticCategory.SYSTEM from delivery by default.
6. THE Statistics_Delivery_System SHALL support regex patterns for bulk inclusion or exclusion of statistics by key.
7. THE Statistics_Delivery_System SHALL support per-player opt-out flags that exclude a player's statistics from delivery.

### Requirement 6: Intelligent Batching and Compression

**User Story:** As a backend developer, I want statistics delivered in optimized batches with intelligent compression, so that the API can process high volumes efficiently while minimizing bandwidth consumption.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL structure each Batch_Payload as a JSON object containing: server UUID, batch ID, timestamp, compression flag, entry count, and an array of player statistic entries.
2. THE Statistics_Delivery_System SHALL include for each statistic entry: player UUID, statistic key, value, data type, collection timestamp, and delta flag.
3. THE Statistics_Delivery_System SHALL compress Batch_Payload data using GZIP when the uncompressed size exceeds 5 kilobytes.
4. THE Statistics_Delivery_System SHALL limit each Batch_Payload to a maximum of 500 statistic entries for CRITICAL and HIGH priority, and 2000 entries for NORMAL and below.
5. THE Statistics_Delivery_System SHALL split large statistic sets into multiple sequential batches with continuation tokens.
6. THE Statistics_Delivery_System SHALL deduplicate statistics within a batch, keeping only the most recent value for each player-statistic pair.
7. THE Statistics_Delivery_System SHALL support binary payload encoding as an alternative to JSON for high-volume scenarios.

### Requirement 7: Server and Plugin Metrics Delivery

**User Story:** As a server administrator, I want comprehensive server-level and plugin-specific metrics sent alongside player statistics, so that I can correlate player behavior with server performance and plugin activity.

#### Acceptance Criteria

1. WHEN transmitting a Batch_Payload, THE Statistics_Delivery_System SHALL include current server metrics: TPS (1-minute, 5-minute, 15-minute averages), memory usage (heap used, heap max, non-heap), CPU usage percentage, online player count, max player count, uptime in milliseconds, and world count.
2. THE Statistics_Delivery_System SHALL include Minecraft server metrics: loaded chunks count, entity count by type, tile entity count, and scheduled tick count.
3. THE Statistics_Delivery_System SHALL include plugin-specific metrics: active quest count, completed quests in period, economy transaction count, economy transaction volume, perk activation count, and active perk count.
4. THE Statistics_Delivery_System SHALL include RDQ-specific metrics: active bounties, completed bounties, bounty rewards distributed, and daily quest completion rate.
5. WHERE the server has custom metrics registered via the metrics registration API, THE Statistics_Delivery_System SHALL include those metrics in the server metrics section.
6. THE Statistics_Delivery_System SHALL compute and include rate metrics: statistics per second, deliveries per minute, and queue throughput.

### Requirement 8: Aggregated and Computed Statistics

**User Story:** As a data analyst, I want pre-computed aggregate statistics delivered alongside raw data, so that dashboards can display summary information without requiring backend computation.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL compute and deliver server-wide aggregates: total players tracked, average playtime, total economy volume, and total quest completions.
2. THE Statistics_Delivery_System SHALL compute and deliver time-windowed aggregates: hourly, daily, and weekly summaries for key metrics.
3. THE Statistics_Delivery_System SHALL compute and deliver percentile statistics: median playtime, 90th percentile economy balance, and similar distribution metrics.
4. THE Statistics_Delivery_System SHALL compute and deliver rate statistics: kills per hour, blocks broken per session, and economy velocity.
5. THE Statistics_Delivery_System SHALL support custom aggregate definitions via configuration.

### Requirement 9: Delivery Status, Monitoring, and Diagnostics

**User Story:** As a server administrator, I want comprehensive visibility into statistics delivery status and performance, so that I can troubleshoot issues, optimize configuration, and verify data integrity.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL expose delivery status via commands showing: last successful delivery time, pending statistics count by priority, failed delivery count, retry count, queue depth, and Backpressure status.
2. THE Statistics_Delivery_System SHALL expose performance metrics via commands showing: average delivery latency, throughput rate, compression ratio, and batch sizes.
3. WHEN a delivery fails after all retry attempts, THE Statistics_Delivery_System SHALL log a warning message containing the failure reason, affected statistic count, and suggested remediation.
4. THE Statistics_Delivery_System SHALL maintain rolling statistics for the current session: successful deliveries, failed deliveries, total statistics transmitted, total bytes transmitted, and average latency.
5. THE Statistics_Delivery_System SHALL support a diagnostic mode that logs detailed information about each delivery attempt.
6. THE Statistics_Delivery_System SHALL expose a health check endpoint for monitoring systems.

### Requirement 10: Offline Queuing and Recovery

**User Story:** As a server administrator, I want statistics collected during backend outages to be reliably stored and delivered when connectivity is restored, so that no player data is lost even during extended downtime.

#### Acceptance Criteria

1. IF the RaindropCentral backend is unreachable, THEN THE Statistics_Delivery_System SHALL queue statistics locally with a configurable maximum of 50000 entries.
2. THE Statistics_Delivery_System SHALL persist the offline queue to disk using a write-ahead log for durability.
3. WHEN backend connectivity is restored, THE Statistics_Delivery_System SHALL transmit queued statistics in chronological order with BULK priority before resuming normal delivery.
4. IF the local queue reaches maximum capacity, THEN THE Statistics_Delivery_System SHALL discard LOW priority entries first, then BULK, preserving CRITICAL and HIGH priority entries.
5. THE Statistics_Delivery_System SHALL log warnings when queue capacity exceeds 75 percent and errors when entries are discarded.
6. THE Statistics_Delivery_System SHALL support manual queue flush commands for administrative control.
7. THE Statistics_Delivery_System SHALL validate queue integrity on server startup and repair corrupted entries.

### Requirement 11: Rate Limiting and Flow Control

**User Story:** As a backend operator, I want the statistics delivery system to respect rate limits and implement flow control, so that individual servers cannot overwhelm the central API.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL implement a Rate_Limiter that respects backend-provided rate limit headers.
2. WHEN the backend returns a 429 (Too Many Requests) response, THE Statistics_Delivery_System SHALL pause delivery for the duration specified in the Retry-After header.
3. THE Statistics_Delivery_System SHALL implement client-side rate limiting with configurable requests per minute (default 60).
4. THE Statistics_Delivery_System SHALL implement adaptive rate limiting that reduces request frequency when error rates exceed 10 percent.
5. THE Statistics_Delivery_System SHALL support burst allowances for event-driven deliveries while maintaining average rate compliance.

### Requirement 12: Cross-Server Statistic Synchronization

**User Story:** As a network administrator running multiple servers, I want player statistics synchronized across servers, so that players have consistent progression regardless of which server they join.

#### Acceptance Criteria

1. WHEN a player joins a server, THE Statistics_Delivery_System SHALL request the player's latest statistics from the backend if the local cache is older than a configurable threshold.
2. THE Statistics_Delivery_System SHALL merge received statistics with local values using configurable conflict resolution: latest-wins, highest-wins, or sum-merge.
3. THE Statistics_Delivery_System SHALL support statistic scoping: global (network-wide), server-specific, or world-specific.
4. THE Statistics_Delivery_System SHALL include a server identifier with each statistic to enable server-specific tracking.
5. WHEN statistics conflict between servers, THE Statistics_Delivery_System SHALL log the conflict and resolution for audit purposes.

### Requirement 13: Security and Data Integrity

**User Story:** As a security-conscious administrator, I want statistics delivery to be secure and tamper-evident, so that I can trust the data integrity and protect player information.

#### Acceptance Criteria

1. THE Statistics_Delivery_System SHALL sign each Batch_Payload with an HMAC using the server's API key.
2. THE Statistics_Delivery_System SHALL include a checksum for each batch to detect transmission corruption.
3. THE Statistics_Delivery_System SHALL encrypt sensitive statistics (IP addresses, location data) before transmission.
4. THE Statistics_Delivery_System SHALL validate Delivery_Receipt signatures from the backend.
5. THE Statistics_Delivery_System SHALL support TLS 1.3 for all API communications.
6. THE Statistics_Delivery_System SHALL sanitize statistic values to prevent injection attacks.
