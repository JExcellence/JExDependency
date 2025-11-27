# Design Document: Server API Authentication System

## Overview

This design implements a secure API key authentication system connecting Minecraft servers to the RaindropCentral backend. Server owners register on the web platform, receive a time-limited API key, then authenticate their server. Upon success, all entities (players, inventories, statistics) link to the authenticated RCentralServer.

The design also migrates RPlayerInventory from the non-existent RServer entity to RCentralServer.

## Architecture

### High-Level Flow

```
1. Backend: User creates server → Generate API key → Display for 15 minutes
2. Plugin: Admin enters API key → Send auth request with owner UUID
3. Backend: Validate API key + owner UUID → Update server status
4. Plugin: Receive success → Start heartbeat loop
5. Runtime: Link all entities to RCentralServer
```

### Component Layers

**Backend**: Server Registration Service, Authentication Service, Heartbeat Service
**Plugin**: Authentication Command, Authentication Client, Heartbeat Task, Server Context
**Data**: RCentralServer, RPlayer, RPlayerInventory, RPlayerStatistic

## Components and Interfaces

### Backend Services

```java
public interface ServerRegistrationService {
    ServerRegistrationResult createServerRegistration(String ownerMinecraftUuid);
    Optional<ServerDetails> getServerDetails(UUID serverUuid);
}

public record ServerRegistrationResult(UUID serverUuid, String plaintextApiKey, LocalDateTime expiresAt) {}

public interface AuthenticationService {
    AuthenticationResult authenticateServer(AuthenticationRequest request);
    boolean validateApiKey(String plaintextKey, String storedHash);
}

public record AuthenticationRequest(UUID serverUuid, String apiKey, String requesterMinecraftUuid) {}
public record AuthenticationResult(boolean success, String message, AuthenticationError error) {}
public enum AuthenticationError { INVALID_API_KEY, UNAUTHORIZED_OWNER, SERVER_NOT_FOUND, RATE_LIMITED }

public interface HeartbeatService {
    HeartbeatResponse processHeartbeat(ServerHeartbeat heartbeat);
    void detectDisconnectedServers();
}

public record ServerHeartbeat(UUID serverUuid, int currentPlayers, int maxPlayers, double tps, String serverVersion, String pluginVersion) {}
```

### Plugin Components

```java
public class AuthenticationCommand extends BukkitCommand {
    // Command: /rcconnect <api-key>
}

public interface AuthenticationClient {
    CompletableFuture<AuthenticationResult> authenticate(String apiKey, UUID requesterUuid);
}

public class HeartbeatTask implements Runnable {
    // Runs every 30 seconds
}

public class ServerContext {
    private UUID serverUuid;
    private boolean authenticated;
    private RCentralServer cachedServer;
}
```

## Data Models

### RCentralServer (Existing - No Changes)
Already has: serverUuid, ownerMinecraftUuid, apiKeyHash, connectionStatus, lastHeartbeat, firstConnectedAt, failedHeartbeatCount, metrics

### RPlayerInventory (Migration Required)
Change: `RServer rServer` → `RCentralServer rCentralServer`

### RPlayerStatistic (Enhancement Required)
Add: `@ManyToOne RCentralServer rCentralServer`

### API Key Generation

```java
public class ApiKeyGenerator {
    public static String generateApiKey() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    public static String hashApiKey(String plaintextKey) {
        return BCrypt.hashpw(plaintextKey, BCrypt.gensalt(12));
    }
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Server Registration Properties

Property 1: UUID uniqueness
*For any* set of server registrations, all generated server UUIDs should be distinct.
**Validates: Requirements 1.1**

Property 2: Owner persistence
*For any* server registration, retrieving it should return the same owner UUID.
**Validates: Requirements 1.2**

Property 3: API key entropy
*For any* set of generated API keys, all should be distinct and at least 32 characters.
**Validates: Requirements 1.3**

Property 4: Bcrypt hash validation
*For any* generated API key, the plaintext should validate against its stored hash.
**Validates: Requirements 1.4**

Property 5: API key single display
*For any* server registration, after initial retrieval, subsequent queries should not return the plaintext key.
**Validates: Requirements 1.5**

Property 6: Time-based key hiding
*For any* server registration, querying after 15 minutes should return null/hidden.
**Validates: Requirements 1.6**

Property 7: Permanent key hiding
*For any* hidden API key, no future queries should ever return the plaintext.
**Validates: Requirements 1.7**

Property 8: Initial disconnected state
*For any* newly created server, ConnectionStatus should be DISCONNECTED.
**Validates: Requirements 1.8**

### Authentication Properties

Property 9: Authentication request completeness
*For any* authentication command, the HTTP request should contain non-null API key, server UUID, and requester UUID.
**Validates: Requirements 2.1, 2.2, 2.3**

Property 10: Owner authorization check
*For any* authentication where requester UUID doesn't match owner UUID, authentication should fail.
**Validates: Requirements 2.5, 2.7**

Property 11: Invalid key rejection
*For any* authentication with mismatched API key, response should indicate failure.
**Validates: Requirements 2.6**

### Connection State Properties

Property 12: Successful authentication state transition
*For any* successful authentication, ConnectionStatus should be CONNECTED.
**Validates: Requirements 3.1**

Property 13: First connection timestamp immutability
*For any* server authentication, if firstConnectedAt is null set it, otherwise leave unchanged.
**Validates: Requirements 3.2**

Property 14: Heartbeat count reset
*For any* successful authentication, failedHeartbeatCount should be 0.
**Validates: Requirements 3.3**

Property 15: Heartbeat timestamp update
*For any* heartbeat received, lastHeartbeat should be within the last minute.
**Validates: Requirements 3.7**

Property 16: Metrics synchronization
*For any* heartbeat with metrics, RCentralServer should reflect those exact values.
**Validates: Requirements 3.8**

### Entity Migration Properties

Property 17: Inventory server type correctness
*For any* newly created RPlayerInventory, the server reference should be RCentralServer type.
**Validates: Requirements 4.1**

### Player-Server Association Properties

Property 18: Player entity existence
*For any* player join on connected server, RPlayer with that UUID should exist after.
**Validates: Requirements 5.1**

Property 19: Server association addition
*For any* player join, RCentralServer should be in player's serversJoined set.
**Validates: Requirements 5.2**

Property 20: Association persistence
*For any* player with server in serversJoined, persisting then retrieving should preserve it.
**Validates: Requirements 5.3**

### Inventory-Server Association Properties

Property 21: Inventory server association
*For any* inventory snapshot created, server reference should match current authenticated server.
**Validates: Requirements 6.1**

Property 22: Inventory restore filtering
*For any* inventory restore, only snapshots matching current server should be considered.
**Validates: Requirements 6.2**

Property 23: Inventory query filtering
*For any* inventory query, results should match both player ID and server ID.
**Validates: Requirements 6.3**

### Statistics-Server Association Properties

Property 24: Statistic server association
*For any* statistic recorded, server reference should match current authenticated server.
**Validates: Requirements 7.1**

Property 25: Statistics query filtering
*For any* statistics query with server filter, results should match that server ID.
**Validates: Requirements 7.2**

Property 26: Per-server statistics independence
*For any* player with statistics on multiple servers, modifying one shouldn't affect others.
**Validates: Requirements 7.3**

### Connection Management Properties

Property 27: Failed heartbeat increment
*For any* server with expired lastHeartbeat, detection should increment failedHeartbeatCount.
**Validates: Requirements 8.1**

Property 28: Disconnect threshold enforcement
*For any* server with failedHeartbeatCount exceeding threshold, status should be DISCONNECTED.
**Validates: Requirements 8.2**

Property 29: Reconnection heartbeat reset
*For any* server reconnecting after DISCONNECTED, failedHeartbeatCount should be 0.
**Validates: Requirements 8.3**

### Security Properties

Property 30: API key unpredictability
*For any* sequence of generated keys, they should have high entropy and no patterns.
**Validates: Requirements 9.1**

Property 31: Bcrypt work factor compliance
*For any* stored hash, it should be valid bcrypt with work factor ≥ 10.
**Validates: Requirements 9.2**

Property 32: Server existence validation
*For any* authentication with non-existent server UUID, authentication should fail.
**Validates: Requirements 9.4**

Property 33: Rate limiting enforcement
*For any* rapid authentication attempts exceeding threshold, subsequent requests should be rejected.
**Validates: Requirements 9.7**


## Error Handling

### Backend Errors
- **Invalid API Key**: 401 Unauthorized, warn log
- **Unauthorized Owner**: 403 Forbidden, warn log
- **Server Not Found**: 404 Not Found, info log
- **Rate Limited**: 429 Too Many Requests, warn log
- **Database Errors**: 500 Internal Server Error, error log with stack trace

### Plugin Errors
- **Network Failure**: Exponential backoff retry (1s, 2s, 4s, 8s, max 30s)
- **Invalid Response**: Display error to sender, error log
- **Permission Denied**: Display permission error, debug log
- **Already Authenticated**: Display warning, info log

### Data Layer Errors
- **Constraint Violation**: Rollback transaction, error log
- **Optimistic Lock Failure**: Retry with fresh entity, debug log
- **Connection Pool Exhaustion**: Queue or reject with timeout, warn log

## Testing Strategy

### Unit Testing
- API key generation uniqueness and format
- Bcrypt hashing and validation
- Authentication logic (valid/invalid inputs)
- Owner UUID validation
- Heartbeat processing and metrics
- Disconnect detection
- Rate limiting

### Property-Based Testing

Use **jqwik** (Java property-based testing library).

**Configuration:**
- Minimum 100 iterations per property (`@Property(tries = 100)`)
- Tag each test: `// Feature: server-api-authentication, Property N: <name>`

**Generators:**
- UUID: `Arbitraries.create(UUID::randomUUID)`
- API Key: Random Base64 strings (32-64 chars)
- Timestamps: LocalDateTime in reasonable ranges
- Metrics: Realistic values (players: 0-1000, TPS: 0-20)

**Example:**
```java
@Property(tries = 100)
// Feature: server-api-authentication, Property 1: UUID uniqueness
void generatedServerUuidsAreUnique(@ForAll("serverRegistrations") List<ServerRegistration> registrations) {
    Set<UUID> uuids = registrations.stream().map(ServerRegistration::serverUuid).collect(Collectors.toSet());
    assertThat(uuids).hasSize(registrations.size());
}
```

### Integration Testing
- End-to-end authentication flow
- Entity association flow (player, inventory, statistics)
- Disconnect detection flow
- Migration validation (no RServer references)

## Implementation Notes

### Configuration
- **Heartbeat Interval**: 30 seconds
- **Heartbeat Timeout**: 90 seconds (3 missed)
- **Failed Threshold**: 3
- **Detection Interval**: 60 seconds
- **API Key Display**: 15 minutes
- **Rate Limit**: 5 attempts per minute per server

### Database Indexes
```sql
CREATE INDEX idx_central_server_uuid ON r_central_server(server_uuid);
CREATE INDEX idx_central_server_status ON r_central_server(connection_status);
CREATE INDEX idx_central_server_heartbeat ON r_central_server(last_heartbeat);
CREATE INDEX idx_inventory_player_server ON r_player_inventory(player_id, server_id);
CREATE INDEX idx_statistic_player_server ON r_player_statistic(player_id, server_id);
```

### Migration Path
1. Add RCentralServer references alongside RServer
2. Dual-write during transition
3. Migrate existing data
4. Update all queries
5. Remove RServer entity

### Monitoring
**Metrics**: Auth success/failure rate, latency, heartbeat processing time, connected servers, rate limit triggers
**Logging**: DEBUG (heartbeat processing), INFO (successful auth), WARN (failures), ERROR (database/network errors)
**Health Checks**: `/health/auth`, `/health/db`, heartbeat task status
