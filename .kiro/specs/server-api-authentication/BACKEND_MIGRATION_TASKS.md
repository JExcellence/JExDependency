# Backend Migration Tasks

## Overview
These files should be moved to the RaindropCentral backend (website) project as they contain backend-only logic for server registration and authentication validation.

## Files to Move to Backend Project

### Core Services
1. **ApiKeyGenerator.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/util/ApiKeyGenerator.java`
   - Purpose: Generate and hash API keys (backend only)
   - Move to: Backend `utils` or `security` package

2. **ServerRegistrationService.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/ServerRegistrationService.java`
   - Purpose: Create server registrations and generate API keys
   - Move to: Backend `services` package

3. **ServerRegistrationServiceImpl.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/ServerRegistrationServiceImpl.java`
   - Purpose: Implementation of server registration
   - Move to: Backend `services` package

4. **AuthenticationService.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/AuthenticationService.java`
   - Purpose: Validate API keys and authenticate servers
   - Move to: Backend `services` package

5. **AuthenticationServiceImpl.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/AuthenticationServiceImpl.java`
   - Purpose: Implementation of authentication validation
   - Move to: Backend `services` package

6. **RateLimiter.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/util/RateLimiter.java`
   - Purpose: Rate limiting for authentication attempts
   - Move to: Backend `utils` or `security` package

7. **HeartbeatService.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/HeartbeatService.java`
   - Purpose: Process heartbeats from servers
   - Move to: Backend `services` package

8. **HeartbeatServiceImpl.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/HeartbeatServiceImpl.java`
   - Purpose: Implementation of heartbeat processing
   - Move to: Backend `services` package

9. **DisconnectDetectionScheduler.java**
   - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/DisconnectDetectionScheduler.java`
   - Purpose: Detect servers that stopped sending heartbeats
   - Move to: Backend `services` or `schedulers` package

10. **HealthCheckService.java**
    - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/HealthCheckService.java`
    - Purpose: Health check endpoints for monitoring
    - Move to: Backend `services` package

11. **MetricsTrackingService.java**
    - Location: `RCore/rcore-common/src/main/java/com/raindropcentral/core/service/central/MetricsTrackingService.java`
    - Purpose: Track authentication and heartbeat metrics
    - Move to: Backend `services` package

### Test Files
12. **ApiKeyGeneratorTest.java**
    - Location: `RCore/rcore-common/src/test/java/com/raindropcentral/core/util/ApiKeyGeneratorTest.java`
    - Move to: Backend test directory

13. **RateLimiterTest.java**
    - Location: `RCore/rcore-common/src/test/java/com/raindropcentral/core/util/RateLimiterTest.java`
    - Move to: Backend test directory

14. **AuthenticationServiceImplTest.java**
    - Location: `RCore/rcore-common/src/test/java/com/raindropcentral/core/service/central/AuthenticationServiceImplTest.java`
    - Move to: Backend test directory

15. **HeartbeatServiceImplTest.java**
    - Location: `RCore/rcore-common/src/test/java/com/raindropcentral/core/service/central/HeartbeatServiceImplTest.java`
    - Move to: Backend test directory

16. **ServerAuthenticationIntegrationTest.java**
    - Location: `RCore/rcore-common/src/test/java/com/raindropcentral/core/integration/ServerAuthenticationIntegrationTest.java`
    - Move to: Backend test directory

## Files to Keep in Plugin Project

### Plugin-Side Services
- **RCentralService.java** - Manages plugin-side connection state
- **RCentralApiClient.java** - HTTP client for backend communication
- **ServerContext.java** - Maintains authenticated server state
- **HeartbeatScheduler.java** - Sends periodic heartbeats to backend
- **MetricsCollector.java** - Collects server metrics for heartbeats

### Commands
- **RCConnectCommand.java** - Command to enter API key
- **RCDisconnectCommand.java** - Command to disconnect

### Listeners
- **PlayerServerAssociationListener.java** - Associates players with servers

### Repositories
- **RCentralServerRepository.java** - Local server entity repository
- **RPlayerInventoryRepository.java** - Inventory repository with server filtering
- **RPlayerStatisticRepository.java** - Statistics repository with server filtering

### Entities
- **RCentralServer.java** - Server entity (used by both plugin and backend)
- **RPlayer.java** - Player entity
- **RPlayerInventory.java** - Inventory entity
- **RPlayerStatistic.java** - Statistics entity

## Backend Implementation Tasks

### 1. Create Backend API Endpoints

#### POST /api/server/register
- Generate unique server UUID
- Generate cryptographically secure API key
- Hash API key with BCrypt
- Store server registration with DISCONNECTED status
- Return plaintext API key (display for 15 minutes only)
- **Service**: ServerRegistrationService

#### POST /api/server/authenticate
- Validate API key format
- Verify server UUID exists
- Validate API key against stored hash
- Verify requester UUID matches owner UUID
- Update server status to CONNECTED
- Set firstConnectedAt if null
- Reset failedHeartbeatCount
- Return success/error response
- **Service**: AuthenticationService

#### POST /api/server/heartbeat
- Validate server is authenticated
- Update lastHeartbeat timestamp
- Update server metrics (players, TPS, versions)
- Reset failedHeartbeatCount if > 0
- Return acknowledgment
- **Service**: HeartbeatService

#### POST /api/server/shutdown
- Validate server is authenticated
- Update server status to DISCONNECTED
- Log shutdown event
- Return acknowledgment
- **New endpoint needed**

#### POST /api/server/wakeup
- Validate server is authenticated
- Update server status to CONNECTED
- Update lastHeartbeat timestamp
- Reset failedHeartbeatCount
- Return acknowledgment
- **New endpoint needed**

### 2. Implement Scheduled Tasks

#### Disconnect Detection Task
- Run every 60 seconds
- Query servers with lastHeartbeat > 90 seconds old
- Increment failedHeartbeatCount
- Mark as DISCONNECTED if count >= 3
- **Service**: DisconnectDetectionScheduler

### 3. Database Schema
Ensure backend database has these tables:
- `r_central_server` - Server registrations
- `r_player` - Player profiles
- `r_player_inventory` - Server-scoped inventories
- `r_player_statistic` - Server-scoped statistics
- `r_player_servers` - Many-to-many join table

### 4. Configuration
- Heartbeat timeout: 90 seconds
- Failed heartbeat threshold: 3
- Detection interval: 60 seconds
- API key display duration: 15 minutes
- Rate limit: 5 attempts per minute per server

## Migration Steps

1. **Copy files** from plugin project to backend project
2. **Update package names** to match backend structure
3. **Update imports** to use backend dependencies
4. **Implement API endpoints** using backend framework (Spring Boot, Express, etc.)
5. **Set up scheduled tasks** for disconnect detection
6. **Run tests** to verify functionality
7. **Deploy backend** with new endpoints
8. **Update plugin** to use new endpoints
9. **Remove backend files** from plugin project

## Dependencies Needed in Backend

- BCrypt library (for password hashing)
- HTTP framework (Spring Boot, Express, etc.)
- Database ORM (Hibernate, TypeORM, etc.)
- Scheduler (Spring @Scheduled, node-cron, etc.)
- Logging framework
- Testing framework (JUnit, Jest, etc.)

## API Request/Response Formats

### Authentication Request
```json
{
  "serverUuid": "uuid-string",
  "apiKey": "base64-api-key",
  "requesterMinecraftUuid": "minecraft-uuid"
}
```

### Authentication Response (Success)
```json
{
  "success": true,
  "message": "Authentication successful",
  "serverUuid": "uuid-string"
}
```

### Authentication Response (Error)
```json
{
  "success": false,
  "message": "Invalid API key",
  "error": "INVALID_API_KEY"
}
```

### Heartbeat Request
```json
{
  "serverUuid": "uuid-string",
  "currentPlayers": 10,
  "maxPlayers": 100,
  "tps": 19.8,
  "serverVersion": "1.20.1",
  "pluginVersion": "1.0.0"
}
```

### Shutdown Request
```json
{
  "serverUuid": "uuid-string",
  "reason": "Server stopping"
}
```

### Wakeup Request
```json
{
  "serverUuid": "uuid-string",
  "serverVersion": "1.20.1",
  "pluginVersion": "1.0.0"
}
```

## Notes

- The plugin should only handle client-side logic (sending requests, managing local state)
- The backend should handle all validation, authentication, and server-side logic
- API keys are generated and stored only on the backend
- The plugin never generates or hashes API keys, only sends them for validation
- Rate limiting should be enforced on the backend
- Disconnect detection runs on the backend via scheduled task
