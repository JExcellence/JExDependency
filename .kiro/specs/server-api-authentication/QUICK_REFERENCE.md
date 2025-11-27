# Server API Authentication - Quick Reference

## For Plugin Developers

### What the Plugin Does
✅ Accepts API key from admin via command  
✅ Sends authentication request to backend  
✅ Creates local server entity after successful auth  
✅ Sends heartbeat every 30 seconds  
✅ Sends shutdown notification on server stop  
✅ Sends wakeup ping on server start (auto-reconnect)  

### What the Plugin Does NOT Do
❌ Generate API keys (backend only)  
❌ Hash API keys (backend only)  
❌ Validate API keys (backend only)  
❌ Detect disconnections (backend only)  

### Key Plugin Files
- `RCentralService.java` - Connection lifecycle management
- `RCentralApiClient.java` - HTTP client (connect, heartbeat, shutdown, wakeup)
- `RCConnectCommand.java` - `/rcconnect <api-key>` command
- `ServerContext.java` - Authenticated server state
- `HeartbeatScheduler.java` - Periodic heartbeat sender
- `PlayerServerAssociationListener.java` - Player-server associations

### Plugin Flow
1. **Startup**: Check for saved API key → Send wakeup ping (if exists)
2. **Admin Command**: `/rcconnect <api-key>` → Authenticate with backend
3. **Connected**: Send heartbeat every 30 seconds
4. **Shutdown**: Send shutdown notification → Stop heartbeat

## For Backend Developers

### What the Backend Does
✅ Generate and store API keys  
✅ Validate authentication requests  
✅ Track server connection status  
✅ Process heartbeat pings  
✅ Detect disconnections (3 missed pings)  
✅ Handle shutdown/wakeup notifications  

### Backend Files to Implement
See `BACKEND_MIGRATION_TASKS.md` for complete list of files to move from plugin project.

### Key Backend Services
- `ApiKeyGenerator` - Generate secure API keys
- `ServerRegistrationService` - Create server registrations
- `AuthenticationService` - Validate credentials
- `HeartbeatService` - Process heartbeats
- `DisconnectDetectionScheduler` - Detect stale connections
- `RateLimiter` - Prevent brute force attacks

### Backend API Endpoints
- `POST /api/server/register` - Create server registration
- `POST /api/server/authenticate` - Validate API key
- `POST /api/server/heartbeat` - Process heartbeat
- `POST /api/server/shutdown` - Handle graceful shutdown
- `POST /api/server/wakeup` - Handle server startup
- `GET /health/auth` - Authentication health check
- `GET /health/db` - Database health check

### Backend Flow
1. **Registration**: Generate UUID + API key → Store hashed → Display for 15 min
2. **Authentication**: Validate API key + owner UUID → Update status to CONNECTED
3. **Heartbeat**: Update lastHeartbeat + metrics → Reset failed count
4. **Disconnect Detection**: Check lastHeartbeat > 90s → Increment failed count → Mark DISCONNECTED if >= 3
5. **Shutdown**: Mark as DISCONNECTED immediately
6. **Wakeup**: Mark as CONNECTED immediately

## Connection States

### CONNECTED
- Server successfully authenticated
- Sending regular heartbeats
- All features active

### DISCONNECTED
- Server not authenticated, or
- Missed 3+ heartbeats, or
- Graceful shutdown notification received

### ERROR
- Connection error occurred
- Requires manual intervention

## Timing Configuration

| Event | Interval/Timeout |
|-------|------------------|
| Heartbeat Send | 30 seconds |
| Heartbeat Timeout | 90 seconds |
| Failed Threshold | 3 missed heartbeats |
| Disconnect Detection | 60 seconds |
| API Key Display | 15 minutes |
| Rate Limit | 5 attempts/minute |

## Common Scenarios

### First Time Setup
1. Admin creates server on website
2. Website generates and displays API key
3. Admin copies API key (15 minute window)
4. Admin runs `/rcconnect <api-key>` in Minecraft
5. Plugin authenticates with backend
6. Server marked as CONNECTED
7. Heartbeats begin

### Server Restart
1. Server shuts down → Shutdown notification sent
2. Backend marks as DISCONNECTED
3. Server starts up → Wakeup ping sent automatically
4. Backend marks as CONNECTED
5. Heartbeats resume

### Network Interruption
1. Heartbeats stop reaching backend
2. After 90 seconds: failedHeartbeatCount = 1
3. After 180 seconds: failedHeartbeatCount = 2
4. After 270 seconds: failedHeartbeatCount = 3 → DISCONNECTED
5. When network restored: Wakeup ping sent → CONNECTED

### Lost API Key
1. Admin runs `/rcconnect <api-key>` with wrong key
2. Backend returns 401 Unauthorized
3. Plugin displays error message
4. Admin must get new API key from website
5. Admin runs `/rcconnect <new-api-key>`

## Testing

### Plugin Tests
- `ServerContextTest` - State management
- Integration tests for connection flow

### Backend Tests (Move to Backend Project)
- `ApiKeyGeneratorTest` - Key generation and validation
- `RateLimiterTest` - Rate limiting
- `AuthenticationServiceImplTest` - Authentication logic
- `HeartbeatServiceImplTest` - Heartbeat processing
- `ServerAuthenticationIntegrationTest` - End-to-end flows

## Troubleshooting

### Server Won't Connect
- Check API key is correct (copy from website)
- Verify owner UUID matches (must be same Minecraft account)
- Check rate limit (wait 1 minute if exceeded)
- Verify backend URL in config
- Check network connectivity

### Server Shows as Offline
- Check heartbeat is running (`/rc status` or similar)
- Verify network connectivity
- Check backend logs for heartbeat processing
- Verify server wasn't rate limited

### Wakeup Ping Fails
- API key may have been revoked
- Server registration may have been deleted
- Run `/rcconnect <api-key>` to re-authenticate

## Next Steps

1. **Move backend files** to website project (see BACKEND_MIGRATION_TASKS.md)
2. **Implement backend API endpoints** using your web framework
3. **Set up disconnect detection** scheduled task on backend
4. **Test end-to-end flow** from registration to heartbeat
5. **Deploy backend** with new endpoints
6. **Update plugin config** with backend URL
7. **Test shutdown/wakeup** notifications
