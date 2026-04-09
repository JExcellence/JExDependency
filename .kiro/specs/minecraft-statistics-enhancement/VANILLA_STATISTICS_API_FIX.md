# Vanilla Statistics API Integration Fix

## Problem

The vanilla statistics collection system is experiencing authentication failures when submitting data to the backend:

```
WARN: Missing API key for server-data request from IP: 127.0.0.1
```

## Root Cause

The plugin is currently using the generic `/api/statistics/deliver` endpoint, but the backend expects vanilla statistics at the dedicated `/api/v1/statistics/vanilla` endpoint with specific authentication headers.

### Backend Requirements

According to the backend API (`VanillaStatisticsController.java`), the vanilla statistics endpoint requires:

1. **Endpoint**: `/api/v1/statistics/vanilla`
2. **Method**: POST
3. **Headers**:
   - `Authorization: Bearer <API_KEY>` or just `<API_KEY>`
   - `X-Server-Id: <SERVER_UUID>`
   - `X-Signature` (optional): HMAC signature for payload verification
4. **Content-Type**: `application/json`

## Solution Implemented

### 1. Added New API Method

Added `submitVanillaStatistics()` method to `RCentralApiClient`:

```java
/**
 * Submits vanilla Minecraft statistics to the backend API.
 * <p>
 * This method sends statistics to the {@code /api/v1/statistics/vanilla} endpoint
 * which is specifically designed for vanilla Minecraft statistics collection.
 * </p>
 *
 * @param apiKey       the API key for authentication
 * @param serverUuid   the unique server identifier
 * @param payload      the batch payload containing vanilla statistics
 * @return a future containing the delivery receipt
 */
public CompletableFuture<DeliveryReceipt> submitVanillaStatistics(
        final @NotNull String apiKey,
        final @NotNull UUID serverUuid,
        final @NotNull BatchPayload payload
) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            String json = gson.toJson(payload);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/statistics/vanilla"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-Server-Id", serverUuid.toString())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), DeliveryReceipt.class);
            } else {
                logger.warning("Vanilla statistics submission failed with status " + 
                    response.statusCode() + ": " + response.body());
                throw new RuntimeException("Vanilla statistics submission failed with status " + 
                    response.statusCode() + ": " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof ConnectException) {
                logger.log(Level.INFO, "Backend not reachable at " + baseUrl + 
                    ". If you think this is an issue contact the administrator of the plugin.");
                return null;
            }

            logger.log(Level.WARNING, "Vanilla statistics submission failed", e);
            throw new RuntimeException("Vanilla statistics submission failed: " + e.getMessage(), e);
        }
    });
}
```

### 2. Next Steps Required

The following changes still need to be made:

1. **Update Statistics Delivery Service**: Modify the statistics delivery service to use `submitVanillaStatistics()` instead of `deliverStatistics()` for vanilla statistics.

2. **Configure Server UUID**: Ensure the server UUID is properly configured and passed to the submission method.

3. **Update Configuration**: Add configuration options for:
   ```yaml
   vanilla-statistics:
     enabled: true
     api-endpoint: "http://localhost:5000"  # Backend URL
     server-uuid: "YOUR-SERVER-UUID-HERE"   # Generate with UUID.randomUUID()
     api-key: "YOUR-API-KEY-HERE"           # Secure random string
   ```

4. **Database Setup**: Create a server entry in the backend database:
   ```sql
   INSERT INTO rcentral_servers (
       id,
       server_uuid,
       server_name,
       api_key,
       is_disabled,
       created_at,
       updated_at,
       version
   ) VALUES (
       1,  -- or next available ID
       'YOUR-SERVER-UUID-HERE',
       'My Minecraft Server',
       'YOUR-SECURE-API-KEY-HERE',
       false,
       NOW(),
       NOW(),
       0
   );
   ```

## Configuration Example

### Plugin Configuration (config.yml)

```yaml
rcentral:
  api:
    url: "http://localhost:5000"
    key: "your-secure-api-key-here"
    server-uuid: "550e8400-e29b-41d4-a716-446655440000"
  
  vanilla-statistics:
    enabled: true
    collection-frequency: 300  # seconds
    enable-cross-server-sync: true
```

### Generating Credentials

```java
// Generate Server UUID
UUID serverUuid = UUID.randomUUID();
System.out.println("Server UUID: " + serverUuid);

// Generate API Key (use a secure random generator)
String apiKey = UUID.randomUUID().toString().replace("-", "") + 
                UUID.randomUUID().toString().replace("-", "");
System.out.println("API Key: " + apiKey);
```

## Testing

1. **Verify Configuration**: Ensure server UUID and API key are set in the plugin configuration
2. **Check Database**: Verify the server entry exists in `rcentral_servers` table
3. **Monitor Logs**: Watch for successful submissions:
   ```
   [INFO] Vanilla statistics submitted successfully for 5 players
   ```
4. **Backend Logs**: Verify no more "Missing API key" warnings

## Benefits

1. **Proper Authentication**: Uses the correct authentication headers expected by the backend
2. **Dedicated Endpoint**: Uses the vanilla-specific endpoint designed for this purpose
3. **Better Error Handling**: Provides clear error messages for debugging
4. **Type Safety**: Strongly typed method signature prevents misuse

## Related Files

- `RCore/src/main/java/com/raindropcentral/core/service/central/RCentralApiClient.java` - Added new method
- `RCore/src/main/java/com/raindropcentral/core/service/statistics/vanilla/VanillaStatisticCollectionService.java` - Needs update
- Backend: `backend/raindropcentral-api/src/main/java/com/raindropcentral/api/controller/VanillaStatisticsController.java`

## Backend API Reference

### Endpoint: POST /api/v1/statistics/vanilla

**Request Headers:**
- `Authorization`: Bearer token or API key
- `X-Server-Id`: Server UUID
- `X-Signature` (optional): HMAC-SHA256 signature

**Request Body:**
```json
{
  "batchId": "batch-uuid",
  "timestamp": 1234567890,
  "statistics": [
    {
      "playerUuid": "player-uuid",
      "statisticKey": "minecraft.blocks.mined.stone",
      "value": 100,
      "timestamp": 1234567890
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Statistics processed successfully",
  "data": {
    "batchId": "batch-uuid",
    "processedCount": 1,
    "errors": []
  }
}
```

## Security Considerations

1. **API Key Storage**: Store API keys securely, never commit to version control
2. **HTTPS**: Use HTTPS in production for encrypted communication
3. **Key Rotation**: Implement periodic API key rotation
4. **Rate Limiting**: Backend implements rate limiting per server
5. **Signature Verification**: Optional HMAC signature for payload integrity
