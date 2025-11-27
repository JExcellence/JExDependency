# Implementation Plan

- [x] 1. Migrate RPlayerInventory from RServer to RCentralServer


  - Update RPlayerInventory entity to reference RCentralServer instead of RServer
  - Update field name from `rServer` to `rCentralServer`
  - Update all getter/setter methods
  - Update constructor to accept RCentralServer
  - Update toString() method
  - _Requirements: 4.1, 4.2, 4.4_

- [ ]* 1.1 Write property test for inventory server type
  - **Property 17: Inventory server type correctness**
  - **Validates: Requirements 4.1**

- [x] 2. Add server association to RPlayerStatistic


  - Add `@ManyToOne` field for RCentralServer in RPlayerStatistic
  - Add getter/setter methods for server association
  - Update database schema with migration script
  - _Requirements: 7.1, 7.2, 7.3_

- [ ]* 2.1 Write property test for statistic server association
  - **Property 24: Statistic server association**
  - **Validates: Requirements 7.1**

- [x] 3. Create API key generation utilities


  - Implement ApiKeyGenerator class with generateApiKey() method
  - Implement hashApiKey() method using BCrypt with work factor 12
  - Use SecureRandom for cryptographic security
  - Generate Base64-encoded keys of 32 bytes
  - _Requirements: 1.3, 1.4, 9.1, 9.2_

- [ ]* 3.1 Write property test for API key entropy
  - **Property 3: API key entropy**
  - **Validates: Requirements 1.3**

- [ ]* 3.2 Write property test for bcrypt hash validation
  - **Property 4: Bcrypt hash validation**
  - **Validates: Requirements 1.4**

- [ ]* 3.3 Write property test for bcrypt work factor
  - **Property 31: Bcrypt work factor compliance**
  - **Validates: Requirements 9.2**

- [x] 4. Implement ServerRegistrationService (Backend)


  - Create ServerRegistrationService interface
  - Implement createServerRegistration() method
  - Generate unique server UUID
  - Generate and hash API key
  - Store RCentralServer with DISCONNECTED status
  - Set owner Minecraft UUID
  - Return ServerRegistrationResult with plaintext key and expiration
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.8_

- [ ]* 4.1 Write property test for UUID uniqueness
  - **Property 1: UUID uniqueness**
  - **Validates: Requirements 1.1**

- [ ]* 4.2 Write property test for owner persistence
  - **Property 2: Owner persistence**
  - **Validates: Requirements 1.2**

- [ ]* 4.3 Write property test for initial disconnected state
  - **Property 8: Initial disconnected state**
  - **Validates: Requirements 1.8**

- [x] 5. Implement API key display time limit (Backend)


  - Add `apiKeyDisplayedUntil` column to r_central_server table
  - Set to `now() + 15 minutes` on registration
  - Implement getServerDetails() to check expiration
  - Return null for plaintext key if expired
  - _Requirements: 1.5, 1.6, 1.7_

- [ ]* 5.1 Write property test for API key single display
  - **Property 5: API key single display**
  - **Validates: Requirements 1.5**

- [ ]* 5.2 Write property test for time-based key hiding
  - **Property 6: Time-based key hiding**
  - **Validates: Requirements 1.6**

- [ ]* 5.3 Write property test for permanent key hiding
  - **Property 7: Permanent key hiding**
  - **Validates: Requirements 1.7**

- [x] 6. Implement AuthenticationService (Backend)


  - Create AuthenticationService interface
  - Implement authenticateServer() method
  - Validate API key format
  - Verify server UUID exists
  - Validate API key against stored hash using BCrypt
  - Verify requester UUID matches owner UUID
  - Return appropriate AuthenticationResult with error codes
  - _Requirements: 2.4, 2.5, 2.6, 2.7, 9.3, 9.4, 9.5_

- [ ]* 6.1 Write property test for owner authorization
  - **Property 10: Owner authorization check**
  - **Validates: Requirements 2.5, 2.7**

- [ ]* 6.2 Write property test for invalid key rejection
  - **Property 11: Invalid key rejection**
  - **Validates: Requirements 2.6**

- [ ]* 6.3 Write property test for server existence validation
  - **Property 32: Server existence validation**
  - **Validates: Requirements 9.4**


- [x] 7. Implement successful authentication state updates (Backend)

  - Update ConnectionStatus to CONNECTED on success
  - Set firstConnectedAt if null
  - Reset failedHeartbeatCount to 0
  - Return success response
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ]* 7.1 Write property test for authentication state transition
  - **Property 12: Successful authentication state transition**
  - **Validates: Requirements 3.1**

- [ ]* 7.2 Write property test for first connection timestamp
  - **Property 13: First connection timestamp immutability**
  - **Validates: Requirements 3.2**

- [ ]* 7.3 Write property test for heartbeat count reset
  - **Property 14: Heartbeat count reset**
  - **Validates: Requirements 3.3**

- [x] 8. Implement rate limiting for authentication (Backend)


  - Implement token bucket algorithm
  - Configure 5 attempts per minute per server UUID
  - Store rate limit state in cache (Redis or in-memory)
  - Return RATE_LIMITED error when exceeded
  - _Requirements: 9.7_

- [ ]* 8.1 Write property test for rate limiting enforcement
  - **Property 33: Rate limiting enforcement**
  - **Validates: Requirements 9.7**

- [x] 9. Implement HeartbeatService (Backend)


  - Create HeartbeatService interface
  - Implement processHeartbeat() method
  - Update lastHeartbeat timestamp
  - Update server metrics (currentPlayers, maxPlayers, tps)
  - Update serverVersion and pluginVersion
  - _Requirements: 3.7, 3.8_

- [ ]* 9.1 Write property test for heartbeat timestamp update
  - **Property 15: Heartbeat timestamp update**
  - **Validates: Requirements 3.7**

- [ ]* 9.2 Write property test for metrics synchronization
  - **Property 16: Metrics synchronization**
  - **Validates: Requirements 3.8**

- [x] 10. Implement disconnect detection (Backend)


  - Implement detectDisconnectedServers() method
  - Query servers with lastHeartbeat older than 90 seconds
  - Increment failedHeartbeatCount
  - Set ConnectionStatus to DISCONNECTED if count exceeds 3
  - Schedule task to run every 60 seconds
  - _Requirements: 8.1, 8.2_

- [ ]* 10.1 Write property test for failed heartbeat increment
  - **Property 27: Failed heartbeat increment**
  - **Validates: Requirements 8.1**

- [ ]* 10.2 Write property test for disconnect threshold
  - **Property 28: Disconnect threshold enforcement**
  - **Validates: Requirements 8.2**

- [x] 11. Implement AuthenticationCommand (Plugin)


  - Create /rcconnect command
  - Validate sender has permission
  - Extract API key from arguments
  - Get sender's Minecraft UUID
  - Call AuthenticationClient.authenticate()
  - Display success/error message to sender
  - _Requirements: 2.1, 2.2, 2.3_

- [ ]* 11.1 Write property test for authentication request completeness
  - **Property 9: Authentication request completeness**
  - **Validates: Requirements 2.1, 2.2, 2.3**

- [x] 12. Implement AuthenticationClient (Plugin)


  - Create AuthenticationClient interface
  - Implement authenticate() method
  - Build HTTP POST request to backend auth endpoint
  - Include server UUID, API key, and requester UUID in request body
  - Handle network errors with exponential backoff retry
  - Parse AuthenticationResult from response
  - Return CompletableFuture
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 13. Implement ServerContext (Plugin)


  - Create ServerContext class
  - Add fields for serverUuid, authenticated flag, cached RCentralServer
  - Implement setAuthenticated() method
  - Load RCentralServer from repository on authentication
  - Implement getCurrentServer() method
  - _Requirements: 3.5_


- [x] 14. Implement HeartbeatTask (Plugin)

  - Create HeartbeatTask implementing Runnable
  - Schedule to run every 30 seconds
  - Check if server is authenticated before executing
  - Collect current players, max players, TPS
  - Get server version and plugin version
  - Build ServerHeartbeat record
  - Send to backend via HTTP client
  - _Requirements: 3.6, 3.7, 3.8_

- [x] 15. Implement player-server association on join


  - Listen to PlayerJoinEvent
  - Create or retrieve RPlayer entity
  - Add current RCentralServer to player's serversJoined set
  - Persist player entity
  - Update lastSeen timestamp on quit
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ]* 15.1 Write property test for player entity existence
  - **Property 18: Player entity existence**
  - **Validates: Requirements 5.1**

- [ ]* 15.2 Write property test for server association addition
  - **Property 19: Server association addition**
  - **Validates: Requirements 5.2**

- [ ]* 15.3 Write property test for association persistence
  - **Property 20: Association persistence**
  - **Validates: Requirements 5.3**

- [x] 16. Update inventory snapshot creation to use RCentralServer


  - Modify RPlayerInventory constructor to accept RCentralServer
  - Get current authenticated server from ServerContext
  - Associate inventory snapshot with current server
  - _Requirements: 6.1_

- [ ]* 16.1 Write property test for inventory server association
  - **Property 21: Inventory server association**
  - **Validates: Requirements 6.1**

- [x] 17. Update inventory restore to filter by server


  - Modify inventory repository queries to filter by server ID
  - Only restore inventories matching current authenticated server
  - Update service layer to pass server filter
  - _Requirements: 6.2, 6.3_

- [ ]* 17.1 Write property test for inventory restore filtering
  - **Property 22: Inventory restore filtering**
  - **Validates: Requirements 6.2**

- [ ]* 17.2 Write property test for inventory query filtering
  - **Property 23: Inventory query filtering**
  - **Validates: Requirements 6.3**

- [x] 18. Update statistics recording to associate with server


  - Modify statistic recording to include current RCentralServer
  - Get current authenticated server from ServerContext
  - Set server reference when creating/updating statistics
  - _Requirements: 7.1_

- [x] 19. Update statistics queries to support server filtering


  - Add server filter parameter to statistics repository methods
  - Implement per-server and cross-server aggregation queries
  - Update service layer to use server-filtered queries
  - _Requirements: 7.2, 7.3, 7.4_

- [ ]* 19.1 Write property test for statistics query filtering
  - **Property 25: Statistics query filtering**
  - **Validates: Requirements 7.2**

- [ ]* 19.2 Write property test for per-server statistics independence
  - **Property 26: Per-server statistics independence**
  - **Validates: Requirements 7.3**

- [x] 20. Create database migration scripts


  - Create script to update r_player_inventory foreign key
  - Create script to add server_id column to r_player_statistic
  - Create indexes for performance
  - Add apiKeyDisplayedUntil column to r_central_server
  - _Requirements: 4.4, 6.5_

- [x] 21. Implement error handling and logging


  - Add error handling for all backend services
  - Add error handling for plugin components
  - Implement appropriate logging levels (DEBUG, INFO, WARN, ERROR)
  - Log authentication failures with details
  - Log connection status changes
  - _Requirements: 8.6, 9.6_

- [x] 22. Add monitoring and health checks


  - Implement /health/auth endpoint
  - Implement /health/db endpoint
  - Add metrics tracking for auth success/failure rate
  - Add metrics for heartbeat processing time
  - Add metrics for connected servers count
  - _Requirements: 8.6_

- [x] 23. Final checkpoint - Ensure all tests pass



  - Ensure all tests pass, ask the user if questions arise.
