# Server API Authentication - Architecture

## System Overview

The server authentication system connects Minecraft servers (running the RCore plugin) to the RaindropCentral backend (website). The architecture follows a client-server model where the backend is the source of truth for authentication and connection state.

## Component Separation

### Backend (Website) - Server Side
**Responsibilities:**
- Generate and store API keys
- Validate authentication requests
- Track server connection status
- Detect disconnections via missed heartbeats
- Process all server lifecycle events

**Components:**
- `ApiKeyGenerator` - Generate secure API keys with BCrypt hashing
- `ServerRegistrationService` - Create server registrations
- `AuthenticationService` - Validate API keys and authenticate servers
- `HeartbeatService` - Process heartbeat pings and update metrics
- `DisconnectDetectionScheduler` - Detect servers that stopped responding
- `RateLimiter` - Prevent brute force authentication attempts
- `HealthCheckService` - Monitor system health
- `MetricsTrackingService` - Track authentication and heartbeat metrics

### Plugin (Minecraft Server) - Client Side
**Responsibilities:**
- Accept API key from admin
- Send authentication requests
- Maintain local connection state
- Send periodic heartbeats
- Notify backend of lifecycle events (shutdown, wakeup)

**Components:**
- `RCConnectCommand` - Command interface for entering API key
- `RCentralApiClient` - HTTP client for backend communication
- `RCentralService` - Manages connection lifecycle
- `ServerContext` - Maintains authenticated server state
- `HeartbeatScheduler` - Sends periodic heartbeats
- `PlayerServerAssociationListener` - Associates players with servers

## Authentication Flow

```
┌─────────────┐                                    ┌─────────────┐
│   Website   │                                    │   Plugin    │
│  (Backend)  │                                    │  (Client)   │
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │ 1. Admin creates server registration            │
       │    - Generate server UUID                       │
       │    - Generate API key                           │
       │    - Hash API key (BCrypt)                      │
       │    - Store in database                          │
       │    - Display API key (15 min window)            │
       │                                                  │
       │ 2. Admin copies API key                         │
       │◄─────────────────────────────────────────────────┤
       │                                                  │
       │                                                  │ 3. Admin runs:
       │                                                  │    /rcconnect <api-key>
       │                                                  │
       │ 4. POST /api/server/authenticate                │
       │◄─────────────────────────────────────────────────┤
       │    {                                             │
       │      serverUuid: "...",                          │
       │      apiKey: "...",                              │
       │      requesterMinecraftUuid: "..."               │
       │    }                                             │
       │                                                  │
       │ 5. Validate:                                    │
       │    - API key format                             │
       │    - Server UUID exists                         │
       │    - API key matches hash                       │
       │    - Requester UUID = owner UUID                │
       │    - Rate limit not exceeded                    │
       │                                                  │
       │ 6. Update server:                               │
       │    - Status = CONNECTED                         │
       │    - Set firstConnectedAt                       │
       │    - Reset failedHeartbeatCount                 │
       │                                                  │
       │ 7. Response: { success: true }                  │
       ├─────────────────────────────────────────────────►│
       │                                                  │
       │                                                  │ 8. Create local entity
       │                                                  │    Start heartbeat (30s)
       │                                                  │
```

## Heartbeat Flow

```
┌─────────────┐                                    ┌─────────────┐
│   Backend   │                                    │   Plugin    │
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │                                                  │ Every 30 seconds:
       │                                                  │
       │ POST /api/server/heartbeat                      │
       │◄─────────────────────────────────────────────────┤
       │    {                                             │
       │      serverUuid: "...",                          │
       │      currentPlayers: 10,                         │
       │      maxPlayers: 100,                            │
       │      tps: 19.8,                                  │
       │      serverVersion: "1.20.1",                    │
       │      pluginVersion: "1.0.0"                      │
       │    }                                             │
       │                                                  │
       │ Update server:                                  │
       │    - lastHeartbeat = now()                      │
       │    - Update metrics                             │
       │    - Reset failedHeartbeatCount                 │
       │                                                  │
       │ Response: { accepted: true }                    │
       ├─────────────────────────────────────────────────►│
       │                                                  │
```

## Disconnect Detection Flow

```
┌─────────────┐
│   Backend   │
│  Scheduler  │
└──────┬──────┘
       │
       │ Every 60 seconds:
       │
       │ 1. Query servers with:
       │    lastHeartbeat < (now - 90 seconds)
       │
       │ 2. For each expired server:
       │    - Increment failedHeartbeatCount
       │    - If count >= 3:
       │      - Status = DISCONNECTED
       │      - Log disconnect event
       │
```

## Shutdown Flow

```
┌─────────────┐                                    ┌─────────────┐
│   Backend   │                                    │   Plugin    │
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │                                                  │ Server stopping...
       │                                                  │
       │ POST /api/server/shutdown                       │
       │◄─────────────────────────────────────────────────┤
       │    {                                             │
       │      serverUuid: "...",                          │
       │      reason: "Server stopping"                   │
       │    }                                             │
       │                                                  │
       │ Update server:                                  │
       │    - Status = DISCONNECTED                      │
       │    - Log shutdown event                         │
       │                                                  │
       │ Response: { success: true }                     │
       ├─────────────────────────────────────────────────►│
       │                                                  │
       │                                                  │ Stop heartbeat
       │                                                  │ Server shuts down
       │                                                  │
```

## Wakeup Flow

```
┌─────────────┐                                    ┌─────────────┐
│   Backend   │                                    │   Plugin    │
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │                                                  │ Server starting...
       │                                                  │ Detect saved API key
       │                                                  │
       │ POST /api/server/wakeup                         │
       │◄─────────────────────────────────────────────────┤
       │    {                                             │
       │      serverUuid: "...",                          │
       │      serverVersion: "1.20.1",                    │
       │      pluginVersion: "1.0.0"                      │
       │    }                                             │
       │                                                  │
       │ Validate API key                                │
       │                                                  │
       │ Update server:                                  │
       │    - Status = CONNECTED                         │
       │    - lastHeartbeat = now()                      │
       │    - Reset failedHeartbeatCount                 │
       │                                                  │
       │ Response: { success: true }                     │
       ├─────────────────────────────────────────────────►│
       │                                                  │
       │                                                  │ Start heartbeat (30s)
       │                                                  │
```

## Data Flow

### Server Registration (Backend)
```
User Request
    ↓
Generate UUID
    ↓
Generate API Key (32 bytes, Base64)
    ↓
Hash API Key (BCrypt, work factor 12)
    ↓
Create RCentralServer Entity
    - serverUuid
    - ownerMinecraftUuid
    - apiKeyHash
    - connectionStatus = DISCONNECTED
    - apiKeyDisplayedUntil = now + 15 minutes
    ↓
Save to Database
    ↓
Return Plaintext API Key (one time only)
```

### Authentication (Backend)
```
Plugin Request
    ↓
Validate API Key Format
    ↓
Check Rate Limit (5 attempts/minute)
    ↓
Find Server by UUID
    ↓
Validate API Key (BCrypt.checkpw)
    ↓
Verify Owner UUID
    ↓
Update Server State
    - connectionStatus = CONNECTED
    - firstConnectedAt = now (if null)
    - failedHeartbeatCount = 0
    ↓
Save to Database
    ↓
Return Success Response
```

### Heartbeat Processing (Backend)
```
Plugin Heartbeat
    ↓
Find Server by UUID
    ↓
Update Server Metrics
    - lastHeartbeat = now
    - currentPlayers
    - maxPlayers
    - tps
    - serverVersion
    - pluginVersion
    - failedHeartbeatCount = 0 (if > 0)
    ↓
Save to Database
    ↓
Return Acknowledgment
```

## Security Measures

### API Key Security
- Generated using `SecureRandom` (cryptographically secure)
- 32 bytes encoded as Base64 URL-safe (43 characters)
- Hashed with BCrypt (work factor 12) before storage
- Never stored in plaintext on backend
- Displayed only once for 15 minutes after generation

### Authentication Security
- Owner UUID validation (only server owner can authenticate)
- Rate limiting (5 attempts per minute per server UUID)
- API key format validation before processing
- BCrypt constant-time comparison (prevents timing attacks)
- Failed authentication logging for security auditing

### Connection Security
- Heartbeat-based liveness detection
- Automatic disconnect after 3 missed heartbeats (90 seconds)
- Graceful shutdown notification
- Connection state tracking and logging

## Configuration

### Backend Settings
```
Heartbeat Timeout: 90 seconds
Failed Heartbeat Threshold: 3
Disconnect Detection Interval: 60 seconds
API Key Display Duration: 15 minutes
Rate Limit Capacity: 5 attempts
Rate Limit Refill: 1 token per minute
BCrypt Work Factor: 12
```

### Plugin Settings
```
Heartbeat Send Interval: 30 seconds
Backend URL: https://raindropcentral.com (or localhost:3000 for dev)
Development Mode: Auto-detect based on server port
```

## Database Schema

### r_central_server
```sql
CREATE TABLE r_central_server (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    server_uuid VARCHAR(36) UNIQUE NOT NULL,
    owner_minecraft_uuid VARCHAR(36),
    api_key_hash VARCHAR(60),
    api_key_displayed_until TIMESTAMP,
    connection_status VARCHAR(20) NOT NULL,
    last_heartbeat TIMESTAMP,
    first_connected_at TIMESTAMP,
    failed_heartbeat_count INT DEFAULT 0,
    current_players INT DEFAULT 0,
    max_players INT DEFAULT 0,
    tps DOUBLE DEFAULT 20.0,
    server_version VARCHAR(50),
    plugin_version VARCHAR(20),
    is_public BOOLEAN DEFAULT FALSE,
    share_player_list BOOLEAN DEFAULT TRUE,
    share_metrics BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_central_server_uuid ON r_central_server(server_uuid);
CREATE INDEX idx_central_server_status ON r_central_server(connection_status);
CREATE INDEX idx_central_server_heartbeat ON r_central_server(last_heartbeat);
CREATE INDEX idx_central_server_owner ON r_central_server(owner_minecraft_uuid);
```

## API Endpoints (Backend)

### POST /api/server/register
Create a new server registration and generate API key.

### POST /api/server/authenticate
Authenticate a server with API key and owner UUID.

### POST /api/server/heartbeat
Process heartbeat and update server metrics.

### POST /api/server/shutdown
Mark server as disconnected (graceful shutdown).

### POST /api/server/wakeup
Mark server as connected (startup after previous connection).

### GET /health/auth
Health check for authentication service.

### GET /health/db
Health check for database connectivity.

## Error Handling

### Backend Error Responses
- `401 Unauthorized` - Invalid API key
- `403 Forbidden` - Unauthorized owner (UUID mismatch)
- `404 Not Found` - Server UUID not found
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Backend error

### Plugin Error Handling
- Network failures: Exponential backoff retry (1s, 2s, 4s, 8s, max 30s)
- Invalid responses: Display error to admin, log details
- Authentication failures: Clear saved API key if 401/403
- Heartbeat failures: Continue retrying, stop after 3 consecutive failures

## Monitoring and Observability

### Metrics Tracked
- Authentication success/failure rate
- Authentication latency
- Heartbeat processing time
- Connected server count
- Rate limit trigger frequency
- Failed heartbeat count distribution

### Logging Levels
- `DEBUG` - Heartbeat processing, cache operations
- `INFO` - Successful authentication, status changes, wakeup/shutdown
- `WARN` - Authentication failures, rate limiting, missed heartbeats
- `ERROR` - Database errors, network failures, unexpected exceptions

### Health Checks
- Authentication service availability
- Database connectivity and query performance
- Heartbeat processing status
- Disconnect detection scheduler status
