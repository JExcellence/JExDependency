# Requirements Document

## Introduction

This document specifies the requirements for the RaindropCentral Statistics API Backend and Frontend. The system receives player statistics from Minecraft servers running the RCore plugin, stores them in a database, and provides a web dashboard for viewing and analyzing the data. The backend is built with Java Spring Boot and the frontend with Next.js/React.

## Glossary

- **Statistics_API**: The REST API endpoints that receive and serve player statistics
- **BatchPayload**: A collection of statistics sent from a Minecraft server in a single request
- **StatisticEntry**: A single player statistic data point
- **DeliveryReceipt**: Response confirming successful receipt of statistics
- **ServerMetrics**: Performance metrics from the Minecraft server (TPS, memory, players)
- **PluginMetrics**: Activity metrics from RCore plugins (quests, economy, perks)
- **AggregatedStatistics**: Pre-computed summary statistics from the server
- **Dashboard**: The web interface for viewing statistics and analytics
- **API_Key**: Authentication token used by Minecraft servers to authenticate requests

## Requirements

### Requirement 1: Statistics Ingestion API

**User Story:** As a Minecraft server administrator, I want my server to send player statistics to RaindropCentral, so that I can view and analyze player data on the web dashboard.

#### Acceptance Criteria

1. WHEN a POST request is received at `/api/statistics/deliver`, THE Statistics_API SHALL validate the API key from the `X-API-Key` header and return 401 if invalid.
2. WHEN a valid BatchPayload is received, THE Statistics_API SHALL parse and validate all StatisticEntry objects and store them in the database.
3. WHEN the payload has `compressed: true` or `Content-Encoding: gzip` header, THE Statistics_API SHALL decompress the GZIP payload before processing.
4. THE Statistics_API SHALL verify the payload checksum using SHA-256 and reject payloads with mismatched checksums.
5. THE Statistics_API SHALL verify the HMAC-SHA256 signature using the server's API key and reject payloads with invalid signatures.
6. WHEN statistics are successfully processed, THE Statistics_API SHALL return a DeliveryReceipt with `success: true`, batch ID, received count, and processed count.
7. IF the request rate exceeds 60 requests per minute per API key, THEN THE Statistics_API SHALL return HTTP 429 with a `Retry-After` header.

### Requirement 2: Player Statistics Query API

**User Story:** As a web dashboard user, I want to query player statistics, so that I can view individual player data and trends.

#### Acceptance Criteria

1. WHEN a GET request is received at `/api/statistics/player/{uuid}`, THE Statistics_API SHALL return all statistics for the specified player UUID.
2. THE Statistics_API SHALL support query parameters for filtering: `from` (timestamp), `to` (timestamp), `keys` (comma-separated statistic keys), `source` (plugin name).
3. WHEN the `aggregate` query parameter is `true`, THE Statistics_API SHALL return aggregated values instead of individual entries.
4. THE Statistics_API SHALL paginate results with `limit` (default 100, max 1000) and `offset` parameters.
5. WHEN no statistics exist for the player, THE Statistics_API SHALL return HTTP 404 with an appropriate error message.

### Requirement 3: Server Statistics Query API

**User Story:** As a web dashboard user, I want to view server-wide statistics and metrics, so that I can monitor server health and player activity.

#### Acceptance Criteria

1. WHEN a GET request is received at `/api/statistics/server/{serverUuid}`, THE Statistics_API SHALL return server metrics and aggregated player statistics.
2. THE Statistics_API SHALL return the most recent ServerMetrics (TPS, memory, players, uptime).
3. THE Statistics_API SHALL return the most recent PluginMetrics (quests, economy, perks, bounties).
4. THE Statistics_API SHALL support time-range filtering with `from` and `to` query parameters.
5. WHEN requesting historical data, THE Statistics_API SHALL return time-series data points at configurable intervals (hourly, daily).

### Requirement 4: Statistics Dashboard Frontend

**User Story:** As a server administrator, I want a web dashboard to visualize player and server statistics, so that I can make data-driven decisions about my server.

#### Acceptance Criteria

1. THE Dashboard SHALL display a server overview page showing current TPS, memory usage, online players, and uptime.
2. THE Dashboard SHALL display charts for TPS history, player count history, and memory usage over time.
3. THE Dashboard SHALL provide a player search feature to find and view individual player statistics.
4. THE Dashboard SHALL display player statistics in categorized sections: Progression, Economy, Social, Gameplay, Achievements.
5. THE Dashboard SHALL support date range selection for filtering historical data.
6. THE Dashboard SHALL auto-refresh data every 60 seconds when viewing real-time metrics.

### Requirement 5: Leaderboards and Rankings

**User Story:** As a server administrator, I want to display leaderboards for various statistics, so that players can compete and see their rankings.

#### Acceptance Criteria

1. WHEN a GET request is received at `/api/statistics/leaderboard/{statisticKey}`, THE Statistics_API SHALL return the top players for that statistic.
2. THE Statistics_API SHALL support `limit` (default 10, max 100) and `timeRange` (all-time, monthly, weekly, daily) parameters.
3. THE Dashboard SHALL display leaderboards with player names, values, and rank positions.
4. THE Dashboard SHALL highlight the current user's position in leaderboards if authenticated.

### Requirement 6: Data Retention and Aggregation

**User Story:** As a system administrator, I want automatic data aggregation and retention policies, so that storage is managed efficiently.

#### Acceptance Criteria

1. THE Statistics_API SHALL aggregate raw statistics older than 7 days into hourly summaries.
2. THE Statistics_API SHALL aggregate hourly summaries older than 30 days into daily summaries.
3. THE Statistics_API SHALL delete raw statistics older than 90 days while preserving aggregated data.
4. THE Statistics_API SHALL run aggregation jobs during low-traffic periods (configurable schedule).

### Requirement 7: Authentication and Authorization

**User Story:** As a server administrator, I want secure access to my server's statistics, so that only authorized users can view the data.

#### Acceptance Criteria

1. THE Statistics_API SHALL authenticate Minecraft servers using API keys in the `X-API-Key` header.
2. THE Dashboard SHALL authenticate users via OAuth (Discord, Microsoft) or email/password.
3. THE Dashboard SHALL only show statistics for servers the authenticated user owns or has access to.
4. THE Statistics_API SHALL support role-based access: Owner (full access), Admin (view all), Viewer (limited view).

### Requirement 8: Real-time Updates

**User Story:** As a dashboard user, I want to see real-time updates when new statistics arrive, so that I can monitor live server activity.

#### Acceptance Criteria

1. THE Statistics_API SHALL support WebSocket connections at `/ws/statistics/{serverUuid}` for real-time updates.
2. WHEN new statistics are received for a server, THE Statistics_API SHALL broadcast updates to connected WebSocket clients.
3. THE Dashboard SHALL display a live activity feed showing recent player events and statistics changes.
4. THE Dashboard SHALL update charts and metrics in real-time without full page refresh.

### Requirement 9: Export and Reporting

**User Story:** As a server administrator, I want to export statistics data, so that I can perform custom analysis or create reports.

#### Acceptance Criteria

1. THE Statistics_API SHALL support CSV export at `/api/statistics/export` with query parameters for filtering.
2. THE Statistics_API SHALL support JSON export for programmatic access.
3. THE Dashboard SHALL provide export buttons for current view data in CSV and JSON formats.
4. THE Statistics_API SHALL limit export size to 100,000 records per request.
