# Backend Integration Prompt: Enhanced Vanilla Minecraft Statistics

## Overview

The RCore plugin now includes an enhanced vanilla Minecraft statistics collection system that transmits native Minecraft statistics (blocks mined, mobs killed, distance traveled, etc.) to the RaindropCentral backend. This document provides the technical specifications needed to implement backend support for receiving, processing, storing, and displaying these statistics.

## System Architecture

### Client-Side (RCore Plugin)

The plugin collects vanilla Minecraft statistics through:
- **Scheduled Collection**: Periodic collection every 60 seconds (configurable 10-600s)
- **Event-Driven Collection**: Immediate collection on critical events (player disconnect, death, etc.)
- **Delta Transmission**: Only changed statistics are transmitted to reduce bandwidth
- **Batch Processing**: Multiple players' statistics are batched together
- **TPS Throttling**: Collection pauses/reduces when server TPS drops below thresholds

### Statistic Categories

Statistics are organized into 6 categories:

1. **BLOCKS** - Block mining, breaking, and placing
2. **ITEMS** - Item usage, crafting, breaking
3. **MOBS** - Entity kills, deaths, damage dealt/taken
4. **TRAVEL** - Distance traveled by various methods
5. **GENERAL** - General gameplay statistics (jumps, deaths, playtime)
6. **INTERACTION** - Interactions with game objects (chests, furnaces, etc.)

## Data Format

### Statistic Entry Structure

Each statistic is transmitted as a `QueuedStatistic` object with the following structure:

```json
{
  "playerId": "uuid-string",
  "statisticKey": "minecraft.blocks.mined.stone",
  "value": 1500,
  "dataType": "INTEGER",
  "timestamp": 1234567890000,
  "priority": "NORMAL",
  "delta": true,
  "metadata": "category=BLOCKS;version=1.20.4"
}
```

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `playerId` | UUID | Player's unique identifier (may be anonymized if privacy mode enabled) |
| `statisticKey` | String | Namespaced statistic identifier (format: `minecraft.category.action.target`) |
| `value` | Number | Current value or delta value if `delta=true` |
| `dataType` | Enum | Data type: `INTEGER`, `LONG`, `DOUBLE`, `STRING` |
| `timestamp` | Long | Unix timestamp in milliseconds when collected |
| `priority` | Enum | Delivery priority: `CRITICAL`, `HIGH`, `NORMAL`, `LOW`, `BULK` |
| `delta` | Boolean | If true, value represents change since last transmission |
| `metadata` | String | Semicolon-separated key=value pairs with additional context |

### Statistic Key Format

Statistic keys follow the pattern: `minecraft.{category}.{action}.{target}`

Examples:
- `minecraft.blocks.mined.stone` - Stone blocks mined
- `minecraft.mobs.killed.zombie` - Zombies killed
- `minecraft.travel.walk` - Distance walked (in centimeters)
- `minecraft.items.used.diamond_pickaxe` - Diamond pickaxe uses
- `minecraft.general.deaths` - Player deaths
- `minecraft.interaction.open_chest` - Chests opened

### Batch Payload Structure

Statistics are transmitted in batches:

```json
{
  "serverId": "server-uuid",
  "serverName": "survival-01",
  "timestamp": 1234567890000,
  "statistics": [
    {
      "playerId": "player-uuid-1",
      "statisticKey": "minecraft.blocks.mined.stone",
      "value": 150,
      "dataType": "INTEGER",
      "timestamp": 1234567890000,
      "priority": "NORMAL",
      "delta": true,
      "metadata": "category=BLOCKS"
    },
    {
      "playerId": "player-uuid-2",
      "statisticKey": "minecraft.mobs.killed.zombie",
      "value": 25,
      "dataType": "INTEGER",
      "timestamp": 1234567891000,
      "priority": "NORMAL",
      "delta": true,
      "metadata": "category=MOBS"
    }
  ],
  "aggregates": {
    "total_blocks_interacted": 5000,
    "total_combat": 1200,
    "total_distance": 500000
  },
  "metadata": {
    "collectionDuration": 1250,
    "playerCount": 45,
    "minecraftVersion": "1.20.4",
    "compressionEnabled": true
  }
}
```

## Backend Requirements

### 1. API Endpoint

**Endpoint**: `POST /api/v1/statistics/vanilla`

**Authentication**: Server API key in header

**Request Headers**:
```
Authorization: Bearer {server-api-key}
Content-Type: application/json
Content-Encoding: gzip (if compressed)
X-Server-Id: {server-uuid}
X-Signature: {hmac-sha256-signature} (if payload signing enabled)
```

**Response**:
```json
{
  "success": true,
  "received": 150,
  "processed": 148,
  "rejected": 2,
  "errors": [
    {
      "index": 45,
      "reason": "Invalid statistic key format"
    }
  ]
}
```

### 2. Database Schema

#### Statistics Table

```sql
CREATE TABLE vanilla_statistics (
    id BIGSERIAL PRIMARY KEY,
    player_id UUID NOT NULL,
    server_id UUID NOT NULL,
    statistic_key VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    value BIGINT NOT NULL,
    is_delta BOOLEAN DEFAULT false,
    collected_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    INDEX idx_player_statistic (player_id, statistic_key),
    INDEX idx_server_time (server_id, collected_at),
    INDEX idx_category (category, collected_at)
);
```

#### Aggregated Statistics Table (for performance)

```sql
CREATE TABLE vanilla_statistics_aggregated (
    id BIGSERIAL PRIMARY KEY,
    player_id UUID NOT NULL,
    server_id UUID,
    statistic_key VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    total_value BIGINT NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    period VARCHAR(20) NOT NULL, -- 'hourly', 'daily', 'weekly', 'monthly', 'all_time'
    period_start TIMESTAMP NOT NULL,
    UNIQUE (player_id, server_id, statistic_key, period, period_start)
);
```

### 3. Data Processing Pipeline

#### Step 1: Validation
- Verify server authentication
- Validate payload signature (if enabled)
- Check statistic key format
- Validate data types
- Verify timestamp is within acceptable range

#### Step 2: Delta Resolution
For statistics with `delta=true`:
1. Fetch current total from `vanilla_statistics_aggregated`
2. Add delta value to current total
3. Store both delta and new total

#### Step 3: Aggregation
- Update hourly aggregates
- Update daily aggregates
- Update weekly aggregates
- Update monthly aggregates
- Update all-time totals

#### Step 4: Cross-Server Synchronization
- Merge statistics from multiple servers for the same player
- Handle conflicts using configured strategy (LATEST_WINS, SUM_MERGE, etc.)
- Update global player statistics

### 4. API Queries

#### Get Player Statistics

**Endpoint**: `GET /api/v1/players/{playerId}/statistics/vanilla`

**Query Parameters**:
- `category` - Filter by category (BLOCKS, ITEMS, MOBS, TRAVEL, GENERAL, INTERACTION)
- `period` - Time period (hourly, daily, weekly, monthly, all_time)
- `startDate` - Start date for range queries
- `endDate` - End date for range queries
- `serverId` - Filter by specific server
- `top` - Limit to top N statistics by value

**Response**:
```json
{
  "playerId": "uuid",
  "period": "all_time",
  "statistics": {
    "minecraft.blocks.mined.stone": 15000,
    "minecraft.mobs.killed.zombie": 2500,
    "minecraft.travel.walk": 5000000
  },
  "aggregates": {
    "total_blocks_mined": 50000,
    "total_mobs_killed": 10000,
    "total_distance_traveled": 15000000
  },
  "lastUpdated": "2026-04-05T12:00:00Z"
}
```

#### Get Leaderboard

**Endpoint**: `GET /api/v1/statistics/vanilla/leaderboard`

**Query Parameters**:
- `statisticKey` - Specific statistic to rank by
- `category` - Category to rank by (uses aggregate)
- `period` - Time period
- `serverId` - Server-specific or global
- `limit` - Number of results (default 100)

**Response**:
```json
{
  "statisticKey": "minecraft.blocks.mined.stone",
  "period": "monthly",
  "leaderboard": [
    {
      "rank": 1,
      "playerId": "uuid-1",
      "playerName": "Player1",
      "value": 50000,
      "lastUpdated": "2026-04-05T12:00:00Z"
    },
    {
      "rank": 2,
      "playerId": "uuid-2",
      "playerName": "Player2",
      "value": 45000,
      "lastUpdated": "2026-04-05T11:30:00Z"
    }
  ]
}
```

### 5. Performance Considerations

#### Indexing Strategy
- Index on `(player_id, statistic_key)` for player queries
- Index on `(category, collected_at)` for category-based queries
- Index on `(server_id, collected_at)` for server analytics
- Partial index on `is_delta=true` for delta processing

#### Partitioning
- Partition `vanilla_statistics` table by month
- Keep recent data (3-6 months) in hot storage
- Archive older data to cold storage
- Use time-series database (TimescaleDB, InfluxDB) for better performance

#### Caching
- Cache aggregated statistics for 5 minutes
- Cache leaderboards for 15 minutes
- Invalidate cache on new data arrival
- Use Redis for distributed caching

#### Batch Processing
- Process statistics in batches of 1000
- Use bulk insert operations
- Run aggregation jobs asynchronously
- Schedule heavy aggregations during off-peak hours

### 6. Privacy & Security

#### Anonymization
When `anonymizeUuids=true` in client config:
- Player UUIDs are hashed using HMAC-SHA256
- Original UUID cannot be recovered
- Same player always gets same anonymized UUID
- Store mapping separately if needed for admin purposes

#### Data Retention
- Raw statistics: 90 days
- Hourly aggregates: 1 year
- Daily aggregates: 3 years
- Monthly aggregates: Indefinite
- All-time totals: Indefinite

#### Access Control
- Players can view their own statistics
- Server admins can view server-wide statistics
- Global admins can view all statistics
- Implement opt-out mechanism per GDPR requirements

### 7. Monitoring & Alerts

#### Metrics to Track
- Statistics received per minute
- Processing latency (p50, p95, p99)
- Error rate by error type
- Storage growth rate
- Query performance

#### Alerts
- Alert if error rate > 5%
- Alert if processing latency > 5 seconds
- Alert if storage growth exceeds projections
- Alert if specific server stops sending data

## Configuration Options

The client sends these configuration details in metadata:

```yaml
vanilla-statistics:
  enabled: true
  collection-frequency: 60  # seconds
  delta-threshold: 5  # minimum change to transmit
  batch-size: 100
  tps-throttling:
    enabled: true
    pause-below: 15.0
    reduce-below: 18.0
  privacy:
    enabled: false
    anonymize-uuids: false
  categories:
    blocks: true
    items: true
    mobs: true
    travel: true
    general: true
    interaction: true
```

## Testing Recommendations

### Unit Tests
- Test delta resolution logic
- Test aggregation calculations
- Test conflict resolution strategies
- Test data validation

### Integration Tests
- Test full ingestion pipeline
- Test query performance with large datasets
- Test cross-server synchronization
- Test privacy/anonymization

### Load Tests
- Simulate 1000 concurrent servers
- Test with 100,000 statistics per batch
- Measure query performance under load
- Test cache effectiveness

## Migration Strategy

### Phase 1: Infrastructure Setup
1. Create database tables and indexes
2. Set up API endpoints
3. Implement basic validation and storage

### Phase 2: Core Features
1. Implement delta resolution
2. Implement aggregation pipeline
3. Implement query APIs

### Phase 3: Advanced Features
1. Implement cross-server sync
2. Implement leaderboards
3. Implement analytics dashboard

### Phase 4: Optimization
1. Implement caching layer
2. Optimize database queries
3. Set up monitoring and alerts

## Example Queries

### Get player's total blocks mined
```sql
SELECT SUM(value) as total_blocks_mined
FROM vanilla_statistics
WHERE player_id = 'uuid'
  AND category = 'BLOCKS'
  AND statistic_key LIKE 'minecraft.blocks.mined.%';
```

### Get top 10 zombie killers this month
```sql
SELECT player_id, total_value
FROM vanilla_statistics_aggregated
WHERE statistic_key = 'minecraft.mobs.killed.zombie'
  AND period = 'monthly'
  AND period_start = DATE_TRUNC('month', CURRENT_DATE)
ORDER BY total_value DESC
LIMIT 10;
```

### Get player's statistics growth over time
```sql
SELECT period_start, total_value
FROM vanilla_statistics_aggregated
WHERE player_id = 'uuid'
  AND statistic_key = 'minecraft.blocks.mined.stone'
  AND period = 'daily'
  AND period_start >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY period_start;
```

## Support & Documentation

For questions or issues:
- Technical documentation: See design.md and requirements.md
- Client implementation: RCore/src/main/java/com/raindropcentral/core/service/statistics/vanilla/
- Configuration: RCore/src/main/resources/statistics-delivery-config.yml

## Version History

- **v1.0.0** (2026-04-05): Initial implementation with core features
