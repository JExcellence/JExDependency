# Implementation Plan

## Backend Tasks (Java Spring Boot)

- [ ] 1. Project Setup and Configuration
  - [ ] 1.1 Initialize Spring Boot project with dependencies
    - Create Spring Boot 3.x project with Web, JPA, Security, Validation, WebSocket
    - Add PostgreSQL driver, Flyway for migrations, Redis for caching
    - Configure application.yml with database, Redis, and security settings
    - _Requirements: 1.1, 7.1_

  - [ ] 1.2 Create database schema and migrations
    - Create Flyway migration for servers table
    - Create Flyway migration for statistics table with partitioning
    - Create Flyway migration for statistics_hourly and statistics_daily tables
    - Create Flyway migration for server_metrics table
    - Create indexes for query optimization
    - _Requirements: 6.1, 6.2_

- [ ] 2. Implement Core Domain Models
  - [ ] 2.1 Create JPA entities
    - Create `ServerEntity` with id, apiKeyHash, name, ownerId, timestamps
    - Create `StatisticEntity` with all fields from design
    - Create `StatisticHourlyEntity` for hourly aggregates
    - Create `StatisticDailyEntity` for daily aggregates
    - Create `ServerMetricsEntity` for server metrics history
    - _Requirements: 1.2, 3.2_

  - [ ] 2.2 Create DTOs for API requests/responses
    - Create `BatchPayloadDTO` matching the wire format from RCore
    - Create `StatisticEntryDTO` for individual statistics
    - Create `ServerMetricsDTO` for server metrics
    - Create `PluginMetricsDTO` for plugin metrics
    - Create `AggregatedStatisticsDTO` for aggregates
    - Create `DeliveryReceiptDTO` for delivery responses
    - _Requirements: 1.2, 1.6_

  - [ ] 2.3 Create repository interfaces
    - Create `ServerRepository` with findByApiKeyHash method
    - Create `StatisticRepository` with custom queries for filtering
    - Create `StatisticHourlyRepository` for hourly data
    - Create `StatisticDailyRepository` for daily data
    - Create `ServerMetricsRepository` for metrics history
    - _Requirements: 2.1, 3.1_

- [ ] 3. Implement Statistics Delivery API
  - [ ] 3.1 Create StatisticsController
    - Implement `POST /api/statistics/deliver` endpoint
    - Handle both JSON and GZIP compressed payloads
    - Extract API key from X-API-Key header
    - Return DeliveryReceipt response
    - _Requirements: 1.1, 1.2, 1.3, 1.6_

  - [ ] 3.2 Create StatisticsDeliveryService
    - Implement batch payload validation
    - Implement checksum verification using SHA-256
    - Implement HMAC signature verification
    - Implement batch processing and database insertion
    - Handle continuation tokens for split batches
    - _Requirements: 1.4, 1.5_

  - [ ] 3.3 Implement GZIP decompression
    - Create GzipDecompressionFilter for Content-Encoding: gzip
    - Handle decompression errors gracefully
    - _Requirements: 1.3_

  - [ ] 3.4 Implement rate limiting
    - Create RateLimitingFilter using Redis
    - Track requests per API key with sliding window
    - Return 429 with Retry-After header when exceeded
    - _Requirements: 1.7_

- [ ] 4. Implement Query APIs
  - [ ] 4.1 Create PlayerStatisticsController
    - Implement `GET /api/statistics/player/{uuid}` endpoint
    - Support query parameters: from, to, keys, source, aggregate
    - Implement pagination with limit and offset
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 4.2 Create ServerStatisticsController
    - Implement `GET /api/statistics/server/{serverUuid}` endpoint
    - Return latest server metrics and plugin metrics
    - Support time-range filtering
    - Return time-series data for historical queries
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 4.3 Create LeaderboardController
    - Implement `GET /api/statistics/leaderboard/{statisticKey}` endpoint
    - Support limit and timeRange parameters
    - Calculate rankings efficiently using database queries
    - _Requirements: 5.1, 5.2_

- [ ] 5. Implement Data Aggregation
  - [ ] 5.1 Create AggregationService
    - Implement hourly aggregation job
    - Implement daily aggregation job
    - Calculate sum, count, min, max, avg for numeric statistics
    - _Requirements: 6.1, 6.2_

  - [ ] 5.2 Create scheduled aggregation tasks
    - Schedule hourly aggregation to run every hour
    - Schedule daily aggregation to run at midnight
    - Configure low-traffic execution windows
    - _Requirements: 6.4_

  - [ ] 5.3 Implement data retention
    - Create job to delete raw statistics older than 90 days
    - Preserve aggregated data indefinitely
    - Log deletion counts for monitoring
    - _Requirements: 6.3_

- [ ] 6. Implement Security
  - [ ] 6.1 Create API key authentication
    - Create ApiKeyAuthenticationFilter
    - Validate API key against hashed values in database
    - Set authentication context for downstream processing
    - _Requirements: 7.1_

  - [ ] 6.2 Implement HMAC signature verification
    - Create SignatureVerificationService
    - Verify HMAC-SHA256 signatures on incoming payloads
    - Generate signatures for response receipts
    - _Requirements: 1.4, 1.5_

  - [ ] 6.3 Configure Spring Security
    - Configure security filter chain
    - Set up CORS for frontend origins
    - Configure rate limiting integration
    - _Requirements: 7.1, 7.2_

- [ ] 7. Implement WebSocket Support
  - [ ] 7.1 Create WebSocket configuration
    - Configure STOMP over WebSocket at /ws/statistics
    - Set up message broker for broadcasting
    - _Requirements: 8.1_

  - [ ] 7.2 Create real-time notification service
    - Broadcast new statistics to subscribed clients
    - Filter broadcasts by server UUID
    - _Requirements: 8.2_

- [ ] 8. Implement Export API
  - [ ] 8.1 Create ExportController
    - Implement `GET /api/statistics/export` endpoint
    - Support CSV and JSON formats via Accept header
    - Apply query filters for data selection
    - Limit export to 100,000 records
    - _Requirements: 9.1, 9.2, 9.4_

## Frontend Tasks (Next.js/React)

- [ ] 9. Project Setup
  - [ ] 9.1 Initialize Next.js project
    - Create Next.js 14 project with App Router
    - Install dependencies: TanStack Query, Recharts, Tailwind CSS, shadcn/ui
    - Configure TypeScript and ESLint
    - _Requirements: 4.1_

  - [ ] 9.2 Create API client
    - Create typed API client using fetch
    - Implement request interceptors for authentication
    - Handle error responses consistently
    - _Requirements: 4.1_

- [ ] 10. Implement Dashboard Pages
  - [ ] 10.1 Create Server Overview page
    - Display current TPS, memory, online players, uptime
    - Show server status indicator (online/offline)
    - Display last update timestamp
    - _Requirements: 4.1_

  - [ ] 10.2 Create Server Metrics charts
    - Implement TPS history chart using Recharts
    - Implement player count history chart
    - Implement memory usage chart
    - Add date range selector
    - _Requirements: 4.2, 4.5_

  - [ ] 10.3 Create Player Search page
    - Implement player search by name or UUID
    - Display search results with player avatars
    - Link to individual player statistics pages
    - _Requirements: 4.3_

  - [ ] 10.4 Create Player Statistics page
    - Display statistics grouped by category
    - Show progression, economy, social, gameplay, achievements sections
    - Display historical trends for key statistics
    - _Requirements: 4.4_

  - [ ] 10.5 Create Leaderboards page
    - Display leaderboard tables for various statistics
    - Implement time range selector (all-time, monthly, weekly, daily)
    - Highlight current user's position
    - _Requirements: 5.3, 5.4_

- [ ] 11. Implement Real-time Features
  - [ ] 11.1 Create WebSocket connection hook
    - Implement useWebSocket hook for STOMP connection
    - Handle connection lifecycle and reconnection
    - _Requirements: 8.1_

  - [ ] 11.2 Implement auto-refresh
    - Add 60-second auto-refresh for real-time metrics
    - Show refresh indicator in UI
    - Allow manual refresh
    - _Requirements: 4.6_

  - [ ] 11.3 Create Activity Feed component
    - Display recent player events
    - Show statistics changes in real-time
    - _Requirements: 8.3, 8.4_

- [ ] 12. Implement Export Features
  - [ ] 12.1 Create Export buttons
    - Add CSV export button to data tables
    - Add JSON export button for developers
    - Show download progress for large exports
    - _Requirements: 9.3_

## TypeScript Types (for Frontend)

```typescript
// types/statistics.ts

export interface BatchPayload {
  serverUuid: string;
  batchId: string;
  timestamp: number;
  compressed: boolean;
  entryCount: number;
  entries: StatisticEntry[];
  serverMetrics?: ServerMetrics;
  pluginMetrics?: PluginMetrics;
  aggregates?: AggregatedStatistics;
  continuationToken?: string;
  checksum?: string;
  signature?: string;
}

export interface StatisticEntry {
  playerUuid: string;
  statisticKey: string;
  value: number | string | boolean;
  dataType: 'NUMBER' | 'STRING' | 'BOOLEAN' | 'TIMESTAMP';
  collectionTimestamp: number;
  isDelta: boolean;
  sourcePlugin: string;
}

export interface ServerMetrics {
  tps1m: number;
  tps5m: number;
  tps15m: number;
  heapUsed: number;
  heapMax: number;
  nonHeapUsed: number;
  cpuUsage: number;
  onlinePlayers: number;
  maxPlayers: number;
  uptimeMs: number;
  worldCount: number;
  loadedChunks: number;
  entityCount: number;
  tileEntityCount: number;
}

export interface PluginMetrics {
  activeQuestCount: number;
  completedQuestsInPeriod: number;
  economyTransactionCount: number;
  economyTransactionVolume: number;
  perkActivationCount: number;
  activePerkCount: number;
  activeBountyCount: number;
  completedBountiesInPeriod: number;
}

export interface AggregatedStatistics {
  timestamp: number;
  totalPlayersTracked: number;
  averagePlaytimeMs: number;
  totalEconomyVolume: number;
  totalQuestCompletions: number;
  customAggregates: Record<string, unknown>;
}

export interface DeliveryReceipt {
  success: boolean;
  batchId: string;
  receivedCount: number;
  processedCount: number;
  timestamp: number;
  signature?: string;
  errorMessage?: string;
}

export interface LeaderboardEntry {
  rank: number;
  playerUuid: string;
  playerName: string;
  value: number;
}

export interface LeaderboardResponse {
  statisticKey: string;
  timeRange: 'all-time' | 'monthly' | 'weekly' | 'daily';
  entries: LeaderboardEntry[];
  totalParticipants: number;
}

export interface PlayerStatisticsResponse {
  playerUuid: string;
  statistics: StatisticEntry[];
  totalCount: number;
  limit: number;
  offset: number;
}
```
