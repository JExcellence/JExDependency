# Implementation Plan

- [x] 1. Create core infrastructure and configuration

  - [x] 1.1 Create StatisticsDeliveryConfig class with all configuration options


    - Create `com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig`
    - Include delivery intervals, queue settings, batch settings, rate limiting, filtering, event thresholds, cross-server sync, and security options
    - Add YAML configuration loading from plugin config
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 11.3_
  - [x] 1.2 Create DeliveryPriority enum and QueuedStatistic record


    - Create `com.raindropcentral.core.service.statistics.queue.DeliveryPriority` enum with CRITICAL, HIGH, NORMAL, LOW, BULK tiers
    - Create `com.raindropcentral.core.service.statistics.queue.QueuedStatistic` record with playerUuid, statisticKey, value, dataType, collectionTimestamp, priority, isDelta, sourcePlugin
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.3 Create StatisticEntry and BatchPayload records

    - Create `com.raindropcentral.core.service.statistics.delivery.StatisticEntry` record
    - Create `com.raindropcentral.core.service.statistics.delivery.BatchPayload` record with serverUuid, batchId, timestamp, compressed flag, entryCount, entries list, serverMetrics, continuationToken, checksum
    - _Requirements: 6.1, 6.2, 6.5_
  - [x] 1.4 Create DeliveryResult and DeliveryReceipt records


    - Create `com.raindropcentral.core.service.statistics.delivery.DeliveryResult` record
    - Create `com.raindropcentral.core.service.statistics.delivery.DeliveryReceipt` record
    - _Requirements: 3.5, 9.4_

- [ ] 2. Implement queue management system
  - [x] 2.1 Create BackpressureController with level evaluation


    - Create `com.raindropcentral.core.service.statistics.queue.BackpressureController`
    - Implement WARNING (5000), CRITICAL (7500), OVERFLOW (10000) thresholds
    - Implement `shouldCollect(DeliveryPriority)` method that throttles LOW/BULK at WARNING level
    - Implement `getCollectionRateMultiplier()` returning 1.0, 0.5, 0.25, 0.0 based on level
    - _Requirements: 1.6, 1.7_

  - [x] 2.2 Create QueuePersistenceManager for disk persistence

    - Create `com.raindropcentral.core.service.statistics.queue.QueuePersistenceManager`
    - Implement JSON-based queue serialization to `plugins/RCore/statistics-queue.json`
    - Implement write-ahead log for durability
    - Implement `validateAndRepair()` for startup integrity checks
    - _Requirements: 1.8, 10.2, 10.7_
  - [x] 2.3 Create StatisticsQueueManager with multi-tier queues


    - Create `com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager`
    - Implement ConcurrentLinkedQueue for each DeliveryPriority tier
    - Implement `enqueue()`, `enqueueBatch()`, `dequeue()`, `dequeueByPlayer()` methods
    - Integrate BackpressureController for queue size monitoring
    - Integrate QueuePersistenceManager for periodic persistence (every 60 seconds)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_

  - [ ] 2.4 Write unit tests for queue management
    - Test priority ordering in dequeue operations
    - Test backpressure level transitions
    - Test queue persistence and recovery
    - _Requirements: 1.1, 1.6, 1.8_

- [ ] 3. Implement collectors
  - [x] 3.1 Create PlayerStatisticCollector for custom statistics


    - Create `com.raindropcentral.core.service.statistics.collector.PlayerStatisticCollector`
    - Inject RPlayerStatisticRepository for data access
    - Implement `collectForPlayer(UUID)` returning List<QueuedStatistic>
    - Implement `collectDeltaForPlayer(UUID)` tracking changes since last delivery
    - Implement `collectAllOnlinePlayers()` for batch collection
    - Implement filtering based on StatisticsDeliveryConfig (categories, keys, regex)
    - Track last delivery timestamps per player/statistic for delta detection
    - _Requirements: 3.1, 3.2, 3.4, 3.5, 3.6, 5.1, 5.2, 5.3, 5.4_

  - [x] 3.2 Create NativeStatisticCollector for Minecraft statistics

    - Create `com.raindropcentral.core.service.statistics.collector.NativeStatisticCollector`
    - Create `NativeStatisticSnapshot` record for caching previous values
    - Implement `collectBlockStatistics(Player)` using Bukkit Statistic API for MINE_BLOCK, BREAK_ITEM
    - Implement `collectItemStatistics(Player)` for CRAFT_ITEM, USE_ITEM, PICKUP, DROP
    - Implement `collectMobStatistics(Player)` for KILL_ENTITY by EntityType
    - Implement `collectTravelStatistics(Player)` for WALK_ONE_CM, SPRINT_ONE_CM, SWIM_ONE_CM, FLY_ONE_CM, CLIMB_ONE_CM, FALL_ONE_CM, AVIATE_ONE_CM
    - Implement `collectGeneralStatistics(Player)` for DEATHS, PLAYER_KILLS, JUMP, TIME_PLAYED
    - Implement delta calculation by comparing with cached snapshots
    - Implement aggregation methods (totalBlocksBroken, totalDistanceTraveled, totalMobKills)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_


  - [x] 3.3 Create ServerMetricsCollector for server and plugin metrics




    - Create `com.raindropcentral.core.service.statistics.collector.ServerMetricsCollector`
    - Create `ServerMetrics` record with TPS (1m, 5m, 15m), memory, CPU, players, uptime, chunks, entities
    - Create `PluginMetrics` record with quest count, economy stats, perk stats
    - Implement TPS collection using Bukkit/Paper API
    - Implement memory collection using Runtime.getRuntime()


    - Implement custom metric provider registration via `registerMetricProvider(String, MetricProvider)`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_
  - [x] 3.4 Create EventDrivenCollector with Bukkit event listeners




    - Create `com.raindropcentral.core.service.statistics.collector.EventDrivenCollector`
    - Implement PlayerQuitEvent handler that captures full snapshot with HIGH priority
    - Implement PlayerJoinEvent handler that triggers cross-server sync request

    - Create internal event hooks for quest completion, level-up, economy transactions, perk activation
    - Implement threshold checking for significant value changes
    - Implement 5-second consolidation window for batching multiple events per player
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [ ] 3.5 Write unit tests for collectors
    - Test PlayerStatisticCollector filtering and delta detection


    - Test NativeStatisticCollector Minecraft statistic mapping
    - Test ServerMetricsCollector metric aggregation
    - _Requirements: 2.1, 3.1, 7.1_

- [ ] 4. Implement delivery engine
  - [x] 4.1 Create RateLimiter with adaptive throttling


    - Create `com.raindropcentral.core.service.statistics.delivery.RateLimiter`
    - Implement token bucket algorithm with configurable requests per minute (default 60)
    - Implement `tryAcquire()` returning boolean for permit availability
    - Implement `handleRateLimitResponse(int retryAfterSeconds)` for 429 responses
    - Implement `adaptToErrorRate(double)` reducing rate when errors exceed 10%
    - Track request timestamps for sliding window calculation
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [x] 4.2 Create RetryHandler with exponential backoff

    - Create `com.raindropcentral.core.service.statistics.delivery.RetryHandler`
    - Implement `executeWithRetry(Supplier<CompletableFuture<T>>)` with max 5 retries
    - Implement exponential backoff starting at 2 seconds, capping at 60 seconds
    - Implement `shouldRetry(Throwable, int)` checking for retryable errors (network, 5xx)

    - _Requirements: 3.4, 11.2_

  - [x] 4.3 Create BatchProcessor for batch creation and deduplication


    - Create `com.raindropcentral.core.service.statistics.delivery.BatchProcessor`
    - Implement `process(List<QueuedStatistic>, DeliveryPriority)` creating BatchPayload objects
    - Implement batch size limits: 500 for CRITICAL/HIGH, 2000 for NORMAL/LOW/BULK
    - Implement `deduplicate(BatchPayload)` keeping most recent value per player-statistic pair

    - Implement `split(BatchPayload)` for oversized batches with continuation tokens




    - _Requirements: 6.3, 6.4, 6.5, 6.6_
  - [ ] 4.4 Create PayloadCompressor for GZIP compression
    - Create `com.raindropcentral.core.service.statistics.delivery.PayloadCompressor`
    - Implement `shouldCompress(BatchPayload)` checking 5KB threshold

    - Implement `compress(BatchPayload)` using GZIPOutputStream
    - Return CompressionResult with compressed bytes and compression ratio
    - _Requirements: 6.3_
  - [x] 4.5 Create StatisticsDeliveryEngine orchestrating delivery

    - Create `com.raindropcentral.core.service.statistics.delivery.StatisticsDeliveryEngine`
    - Inject RateLimiter, RetryHandler, BatchProcessor, PayloadCompressor
    - Implement `deliver(List<QueuedStatistic>)` returning CompletableFuture<DeliveryResult>
    - Implement `deliverWithPriority(List<QueuedStatistic>, DeliveryPriority)` for priority-specific handling
    - Implement checksum calculation using SHA-256
    - Track delivery metrics (latency, throughput, success/failure counts)
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 9.4_
  - [ ] 4.6 Write unit tests for delivery engine
    - Test RateLimiter permit acquisition and pause handling
    - Test RetryHandler backoff calculation
    - Test BatchProcessor splitting and deduplication
    - Test PayloadCompressor threshold and compression
    - _Requirements: 6.3, 6.6, 11.3_

- [ ] 5. Extend RCentralApiClient with statistics endpoints
  - [x] 5.1 Add deliverStatistics method to RCentralApiClient


    - Add `deliverStatistics(String apiKey, BatchPayload payload)` method
    - Serialize BatchPayload to JSON using Gson
    - POST to `/api/statistics/deliver` endpoint
    - Parse DeliveryReceipt from response
    - _Requirements: 6.1, 6.2_

  - [x] 5.2 Add deliverStatisticsCompressed method for compressed payloads

    - Add `deliverStatisticsCompressed(String apiKey, byte[] compressedPayload, String batchId)` method
    - Set Content-Encoding: gzip header
    - POST to `/api/statistics/deliver` endpoint



    - _Requirements: 6.3_

  - [ ] 5.3 Add requestPlayerStatistics method for cross-server sync
    - Add `requestPlayerStatistics(String apiKey, UUID playerUuid)` method
    - GET from `/api/statistics/player/{uuid}` endpoint
    - Parse response into List<StatisticEntry>
    - _Requirements: 12.1_
  - [ ] 5.4 Write integration tests for API client extensions
    - Test statistics delivery with mock server
    - Test compressed payload handling
    - Test player statistics request
    - _Requirements: 6.1, 6.3, 12.1_

- [ ] 6. Implement aggregation engine
  - [x] 6.1 Create StatisticsAggregator for computed statistics


    - Create `com.raindropcentral.core.service.statistics.aggregation.StatisticsAggregator`
    - Create `AggregatedStatistics` record with timestamp, totalPlayersTracked, averagePlaytime, totalEconomyVolume, totalQuestCompletions, customAggregates map
    - Implement `computeServerAggregates()` calculating server-wide totals
    - Implement `computePercentiles(String statisticKey, double... percentiles)` for distribution metrics
    - Implement `computeRates()` for kills/hour, blocks/session metrics
    - _Requirements: 8.1, 8.3, 8.4_

  - [x] 6.2 Create TimeWindowedAccumulator for hourly/daily aggregates

    - Create `com.raindropcentral.core.service.statistics.aggregation.TimeWindowedAccumulator`
    - Implement sliding window storage for hourly and daily periods
    - Implement `computeHourlyAggregates()` and `computeDailyAggregates()`
    - Support custom aggregate definitions via configuration
    - _Requirements: 8.2, 8.5_
  - [ ] 6.3 Write unit tests for aggregation
    - Test server-wide aggregate calculations
    - Test percentile computation accuracy
    - Test time-windowed accumulation
    - _Requirements: 8.1, 8.2, 8.3_

- [ ] 7. Implement cross-server synchronization
  - [x] 7.1 Create ConflictResolver with resolution strategies


    - Create `com.raindropcentral.core.service.statistics.sync.ConflictResolver`
    - Create `ConflictStrategy` enum: LATEST_WINS, HIGHEST_WINS, LOWEST_WINS, SUM_MERGE, LOCAL_WINS, REMOTE_WINS
    - Implement `resolve(String statisticKey, Object localValue, Object remoteValue, ConflictStrategy strategy)`
    - Support per-statistic strategy configuration
    - _Requirements: 12.2, 12.5_

  - [x] 7.2 Create CrossServerSyncManager for synchronization

    - Create `com.raindropcentral.core.service.statistics.sync.CrossServerSyncManager`
    - Create `StatisticScope` enum: GLOBAL, SERVER_SPECIFIC, WORLD_SPECIFIC
    - Implement `syncPlayerStatistics(UUID playerUuid)` fetching and merging remote statistics
    - Implement `requestLatestStatistics(UUID playerUuid)` for on-join sync
    - Implement cache validity checking (configurable duration, default 5 minutes)
    - Log conflicts and resolutions for audit purposes
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  - [ ] 7.3 Write unit tests for cross-server sync
    - Test conflict resolution strategies
    - Test cache validity checking
    - Test statistic merging
    - _Requirements: 12.2, 12.5_

- [ ] 8. Implement security features
  - [x] 8.1 Create PayloadSigner for HMAC signing


    - Create `com.raindropcentral.core.service.statistics.security.PayloadSigner`

    - Implement `sign(BatchPayload, String apiKey)` using HMAC-SHA256


    - Implement `verifyReceipt(DeliveryReceipt, String apiKey)` for receipt validation



    - _Requirements: 13.1, 13.4_


  - [ ] 8.2 Create SensitiveDataEncryptor for sensitive fields
    - Create `com.raindropcentral.core.service.statistics.security.SensitiveDataEncryptor`


    - Implement `encrypt(String value)` for IP addresses and location data
    - Use AES-256-GCM encryption with server-specific key derivation
    - _Requirements: 13.3_
  - [ ] 8.3 Create StatisticSanitizer for injection prevention
    - Create `com.raindropcentral.core.service.statistics.security.StatisticSanitizer`
    - Implement `sanitize(Object value)` removing potentially dangerous content

    - Validate statistic keys against allowed patterns
    - _Requirements: 13.6_
  - [x] 8.4 Write unit tests for security components

    - Test HMAC signature generation and verification


    - Test encryption/decryption round-trip
    - Test sanitization of malicious input

    - _Requirements: 13.1, 13.3, 13.6_

- [ ] 9. Create main orchestrator service
  - [ ] 9.1 Create StatisticsDeliveryService as main entry point
    - Create `com.raindropcentral.core.service.statistics.StatisticsDeliveryService`



    - Wire all components: QueueManager, DeliveryEngine, Collectors, Aggregator, SyncManager
    - Implement `initialize()` starting scheduled tasks and registering event listeners
    - Implement `shutdown()` flushing queues and persisting state
    - Implement `flushQueue()` for manual queue processing
    - Implement `deliverPlayerStatistics(UUID)` for on-demand delivery
    - Implement `pauseDelivery()` and `resumeDelivery()` for administrative control

    - _Requirements: 9.1, 9.6, 10.6_
  - [ ] 9.2 Implement scheduled delivery tasks
    - Create periodic task for NORMAL priority delivery at configured interval (default 300s)
    - Create periodic task for native statistic collection (default 60s)

    - Create periodic task for queue persistence (every 60s)
    - Create periodic task for LOW/BULK processing during low activity
    - Implement activity detection for LOW/BULK scheduling
    - _Requirements: 1.4, 1.5, 1.8, 2.5, 3.1, 3.2, 3.3_
  - [x] 9.3 Implement priority-based queue processing

    - Create dedicated processor for CRITICAL priority (2-second max delay)
    - Create dedicated processor for HIGH priority (10-second max delay)



    - Implement queue polling with priority ordering


    - _Requirements: 1.2, 1.3_
  - [ ] 9.4 Write integration tests for orchestrator
    - Test full delivery flow from collection to transmission

    - Test scheduled task execution


    - Test priority processing timing
    - _Requirements: 1.2, 1.3, 3.1_




- [ ] 10. Implement monitoring and diagnostics
  - [ ] 10.1 Create DeliveryMetricsTracker for statistics tracking
    - Create `com.raindropcentral.core.service.statistics.monitoring.DeliveryMetricsTracker`
    - Track successful/failed deliveries, total statistics transmitted, total bytes, average latency
    - Implement rolling window for session statistics
    - Implement `getMetrics()` returning DeliveryMetrics record

    - _Requirements: 9.3, 9.4_
  - [ ] 10.2 Create DeliveryStatus and QueueStatistics records
    - Create `DeliveryStatus` record with lastSuccessTime, pendingCount by priority, failedCount, retryCount, queueDepth, backpressureStatus
    - Create `QueueStatistics` record with size per priority, total size, oldest entry age


    - _Requirements: 9.1, 9.2_
  - [ ] 10.3 Create StatisticsDeliveryCommand for admin commands
    - Create `/rcstats status` showing delivery status and queue statistics
    - Create `/rcstats metrics` showing performance metrics



    - Create `/rcstats flush` for manual queue flush
    - Create `/rcstats pause` and `/rcstats resume` for delivery control
    - Create `/rcstats diagnostic` enabling detailed logging mode
    - _Requirements: 9.1, 9.2, 9.5, 10.6_



  - [ ] 10.4 Write unit tests for monitoring
    - Test metrics tracking accuracy
    - Test status reporting
    - _Requirements: 9.1, 9.4_

- [ ] 11. Implement offline queuing and recovery
  - [x] 11.1 Enhance QueuePersistenceManager for offline scenarios


    - Implement increased capacity (50000 entries) for offline mode
    - Implement priority-based discard when capacity exceeded (LOW first, then BULK)



    - Implement warning logging at 75% capacity

    - Implement error logging when entries discarded

    - _Requirements: 10.1, 10.4, 10.5_


  - [ ] 11.2 Implement connectivity detection and recovery
    - Detect backend unreachability via failed delivery attempts
    - Switch to offline mode after 3 consecutive failures
    - Implement connectivity probe every 30 seconds during offline mode
    - On recovery, transmit queued statistics with BULK priority in chronological order
    - _Requirements: 10.1, 10.3_
  - [ ] 11.3 Write integration tests for offline recovery
    - Test queue persistence during simulated outage
    - Test recovery and chronological delivery
    - Test priority-based discard
    - _Requirements: 10.1, 10.3, 10.4_

- [ ] 12. Integration with RCore lifecycle
  - [ ] 12.1 Integrate StatisticsDeliveryService into RCoreFreeImpl/RCorePremiumImpl
    - Initialize StatisticsDeliveryService after RCentralService is ready
    - Register EventDrivenCollector listeners
    - Wire shutdown notification to flush statistics before disconnect
    - _Requirements: 4.1, 9.6_
  - [ ] 12.2 Add configuration section to RCore config.yml
    - Add `statistics-delivery` section with all configurable options
    - Add `statistics-delivery.enabled` toggle (default true)
    - Add `statistics-delivery.categories` for category filtering
    - Add `statistics-delivery.native-statistics` for Minecraft stat options
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - [ ] 12.3 Write integration tests for RCore integration
    - Test service initialization and shutdown
    - Test configuration loading
    - _Requirements: 3.1, 9.6_
