# Statistics API Backend & Frontend Design

## Overview

This document describes the architecture and design for the RaindropCentral Statistics API Backend (Java Spring Boot) and Frontend (Next.js/React). The system receives player statistics from Minecraft servers, stores them in a PostgreSQL database, and provides a web dashboard for visualization and analysis.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           RaindropCentral Platform                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐       │
│  │  Minecraft      │     │  Spring Boot    │     │  Next.js        │       │
│  │  Server (RCore) │────▶│  Backend API    │◀────│  Frontend       │       │
│  └─────────────────┘     └────────┬────────┘     └─────────────────┘       │
│                                   │                                         │
│                          ┌────────▼────────┐                               │
│                          │   PostgreSQL    │                               │
│                          │   Database      │                               │
│                          └────────┬────────┘                               │
│                                   │                                         │
│                          ┌────────▼────────┐                               │
│                          │     Redis       │                               │
│                          │   (Cache/WS)    │                               │
│                          └─────────────────┘                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. REST API Endpoints

#### Statistics Delivery Endpoint

```
POST /api/statistics/deliver
Content-Type: application/json
Content-Encoding: gzip (optional)
X-API-Key: {apiKey}
X-Batch-Id: {batchId} (for compressed payloads)
```

**Request Body (BatchPayload):**
```json
{
  "serverUuid": "550e8400-e29b-41d4-a716-446655440000",
  "batchId": "batch-1702400000000-a1b2c3d4",
  "timestamp": 1702400000000,
  "compressed": false,
  "entryCount": 150,
  "entries": [
    {
      "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "statisticKey": "mc_deaths",
      "value": 42,
      "dataType": "NUMBER",
      "collectionTimestamp": 1702399900000,
      "isDelta": true,
      "sourcePlugin": "minecraft"
    }
  ],
  "serverMetrics": {
    "tps1m": 19.8,
    "tps5m": 19.5,
    "tps15m": 19.2,
    "heapUsed": 2147483648,
    "heapMax": 4294967296,
    "nonHeapUsed": 134217728,
    "cpuUsage": 45.5,
    "onlinePlayers": 50,
    "maxPlayers": 100,
    "uptimeMs": 86400000,
    "worldCount": 3,
    "loadedChunks": 5000,
    "entityCount": 2500,
    "tileEntityCount": 1000
  },
  "pluginMetrics": {
    "activeQuestCount": 25,
    "completedQuestsInPeriod": 10,
    "economyTransactionCount": 150,
    "economyTransactionVolume": 50000.0,
    "perkActivationCount": 30,
    "activePerkCount": 15,
    "activeBountyCount": 5,
    "completedBountiesInPeriod": 2
  },
  "aggregates": {
    "timestamp": 1702400000000,
    "totalPlayersTracked": 500,
    "averagePlaytimeMs": 7200000,
    "totalEconomyVolume": 1000000.0,
    "totalQuestCompletions": 250,
    "customAggregates": {
      "onlinePlayers": 50,
      "totalBlocksBroken": 100000,
      "totalMobKills": 50000
    }
  },
  "continuationToken": null,
  "checksum": "a1b2c3d4e5f6...",
  "signature": "hmac-sha256-signature..."
}
```

**Response (DeliveryReceipt):**
```json
{
  "success": true,
  "batchId": "batch-1702400000000-a1b2c3d4",
  "receivedCount": 150,
  "processedCount": 150,
  "timestamp": 1702400001000,
  "signature": "response-hmac-signature...",
  "errorMessage": null
}
```

#### Player Statistics Endpoint

```
GET /api/statistics/player/{playerUuid}
X-API-Key: {apiKey}
```

**Query Parameters:**
- `from` - Start timestamp (epoch ms)
- `to` - End timestamp (epoch ms)
- `keys` - Comma-separated statistic keys
- `source` - Filter by source plugin
- `aggregate` - Return aggregated values (boolean)
- `limit` - Max results (default 100, max 1000)
- `offset` - Pagination offset

**Response:**
```json
{
  "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "statistics": [
    {
      "statisticKey": "mc_deaths",
      "value": 42,
      "dataType": "NUMBER",
      "collectionTimestamp": 1702399900000,
      "sourcePlugin": "minecraft"
    }
  ],
  "totalCount": 150,
  "limit": 100,
  "offset": 0
}
```

#### Server Statistics Endpoint

```
GET /api/statistics/server/{serverUuid}
X-API-Key: {apiKey}
```

**Response:**
```json
{
  "serverUuid": "550e8400-e29b-41d4-a716-446655440000",
  "serverMetrics": { ... },
  "pluginMetrics": { ... },
  "aggregates": { ... },
  "lastUpdated": 1702400000000
}
```

#### Leaderboard Endpoint

```
GET /api/statistics/leaderboard/{statisticKey}
X-API-Key: {apiKey}
```

**Query Parameters:**
- `limit` - Max results (default 10, max 100)
- `timeRange` - all-time, monthly, weekly, daily
- `serverUuid` - Filter by server (optional)

**Response:**
```json
{
  "statisticKey": "mc_player_kills",
  "timeRange": "weekly",
  "entries": [
    {
      "rank": 1,
      "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "playerName": "Notch",
      "value": 1500
    }
  ],
  "totalParticipants": 500
}
```

## Data Models

### Database Schema (PostgreSQL)

```sql
-- Servers table
CREATE TABLE servers (
    id UUID PRIMARY KEY,
    api_key_hash VARCHAR(64) NOT NULL,
    name VARCHAR(255),
    owner_id UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    last_seen_at TIMESTAMP
);

-- Raw statistics (partitioned by time)
CREATE TABLE statistics (
    id BIGSERIAL,
    server_id UUID NOT NULL REFERENCES servers(id),
    player_uuid UUID NOT NULL,
    statistic_key VARCHAR(128) NOT NULL,
    value JSONB NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    collection_timestamp TIMESTAMP NOT NULL,
    is_delta BOOLEAN DEFAULT false,
    source_plugin VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (id, collection_timestamp)
) PARTITION BY RANGE (collection_timestamp);

-- Hourly aggregates
CREATE TABLE statistics_hourly (
    id BIGSERIAL PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES servers(id),
    player_uuid UUID NOT NULL,
    statistic_key VARCHAR(128) NOT NULL,
    hour_timestamp TIMESTAMP NOT NULL,
    sum_value DOUBLE PRECISION,
    count_value INTEGER,
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    avg_value DOUBLE PRECISION,
    UNIQUE (server_id, player_uuid, statistic_key, hour_timestamp)
);

-- Daily aggregates
CREATE TABLE statistics_daily (
    id BIGSERIAL PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES servers(id),
    player_uuid UUID NOT NULL,
    statistic_key VARCHAR(128) NOT NULL,
    day_timestamp DATE NOT NULL,
    sum_value DOUBLE PRECISION,
    count_value INTEGER,
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    avg_value DOUBLE PRECISION,
    UNIQUE (server_id, player_uuid, statistic_key, day_timestamp)
);

-- Server metrics history
CREATE TABLE server_metrics (
    id BIGSERIAL PRIMARY KEY,
    server_id UUID NOT NULL REFERENCES servers(id),
    timestamp TIMESTAMP NOT NULL,
    tps_1m DOUBLE PRECISION,
    tps_5m DOUBLE PRECISION,
    tps_15m DOUBLE PRECISION,
    heap_used BIGINT,
    heap_max BIGINT,
    cpu_usage DOUBLE PRECISION,
    online_players INTEGER,
    max_players INTEGER,
    uptime_ms BIGINT,
    loaded_chunks INTEGER,
    entity_count INTEGER
);

-- Indexes
CREATE INDEX idx_statistics_server_player ON statistics(server_id, player_uuid);
CREATE INDEX idx_statistics_key ON statistics(statistic_key);
CREATE INDEX idx_statistics_timestamp ON statistics(collection_timestamp);
CREATE INDEX idx_server_metrics_server ON server_metrics(server_id, timestamp);
```

### Spring Boot Entities

```java
@Entity
@Table(name = "statistics")
public class StatisticEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", nullable = false)
    private UUID serverId;
    
    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;
    
    @Column(name = "statistic_key", nullable = false)
    private String statisticKey;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Object value;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type")
    private StatisticDataType dataType;
    
    @Column(name = "collection_timestamp")
    private Instant collectionTimestamp;
    
    @Column(name = "is_delta")
    private boolean isDelta;
    
    @Column(name = "source_plugin")
    private String sourcePlugin;
}
```

## Error Handling

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created (batch received) |
| 400 | Bad Request (invalid payload) |
| 401 | Unauthorized (invalid API key) |
| 403 | Forbidden (signature mismatch) |
| 404 | Not Found (player/server not found) |
| 429 | Too Many Requests (rate limited) |
| 500 | Internal Server Error |

### Error Response Format

```json
{
  "error": true,
  "code": "INVALID_SIGNATURE",
  "message": "Payload signature verification failed",
  "timestamp": 1702400000000,
  "path": "/api/statistics/deliver"
}
```

## Testing Strategy

### Backend Tests
- Unit tests for service layer (statistics processing, aggregation)
- Integration tests for REST endpoints
- Repository tests with TestContainers (PostgreSQL)
- Security tests for authentication and authorization

### Frontend Tests
- Component tests with React Testing Library
- E2E tests with Playwright
- API mocking with MSW (Mock Service Worker)

## Security Considerations

1. **API Key Validation**: All requests must include valid API key
2. **HMAC Signature**: Verify payload integrity using HMAC-SHA256
3. **Rate Limiting**: 60 requests/minute per API key
4. **Input Sanitization**: Validate and sanitize all statistic keys and values
5. **SQL Injection Prevention**: Use parameterized queries
6. **CORS**: Configure allowed origins for frontend
