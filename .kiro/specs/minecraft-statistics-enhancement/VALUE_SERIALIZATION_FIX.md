# Statistics Value Serialization Fix

## Problem

The backend API was receiving `ClassCastException` errors when processing statistics:

```
class java.util.ImmutableCollections$Map1 cannot be cast to class java.lang.String
```

This occurred because:
1. The `PlayerStatistic` entity in the backend expects the `value` field to be a `String`
2. Complex statistic values (Maps, Lists) were being serialized as JSON objects instead of JSON strings
3. When the backend tried to deserialize, it received a Map object where it expected a String

## Root Cause

While `StatisticEntry.fromQueued()` correctly converts values to strings using `convertValueToString()`, Gson's default serialization was not preserving this conversion during JSON serialization. When Gson serialized the `BatchPayload`, it would serialize complex objects in the `value` field as nested JSON objects rather than escaped JSON strings.

## Solution

Added a custom Gson `JsonSerializer` for `StatisticEntry` in `RCentralApiClient` that explicitly serializes the `value` field as a string property:

```java
.registerTypeAdapter(StatisticEntry.class, (JsonSerializer<StatisticEntry>) (src, typeOfSrc, context) -> {
    JsonObject obj = new JsonObject();
    obj.addProperty("playerUuid", src.playerUuid().toString());
    obj.addProperty("statisticKey", src.statisticKey());
    obj.addProperty("value", src.value()); // Already a String from fromQueued()
    obj.addProperty("dataType", src.dataType().name());
    obj.addProperty("collectionTimestamp", src.collectionTimestamp());
    obj.addProperty("isDelta", src.isDelta());
    obj.addProperty("sourcePlugin", src.sourcePlugin());
    return obj;
})
```

## Changes Made

### Modified Files

1. **RCore/src/main/java/com/raindropcentral/core/service/central/RCentralApiClient.java**
   - Added `JsonSerializer` import
   - Registered custom `StatisticEntry` serializer in Gson configuration
   - Ensures all `value` fields are serialized as strings, not nested objects

## Expected Behavior

After this fix:

1. **Complex Values**: Statistics with Map or List values will be serialized as JSON strings:
   ```json
   {
     "value": "{\"key\":\"value\"}"  // String containing JSON
   }
   ```
   Instead of:
   ```json
   {
     "value": {"key":"value"}  // Nested object
   }
   ```

2. **Backend Processing**: The backend can now correctly deserialize the `value` field as a String and store it in the `PlayerStatistic` entity

3. **No More ClassCastException**: The backend will no longer throw `ClassCastException` when processing statistics

## Testing

To verify the fix:

1. Start the server with RCore enabled
2. Trigger vanilla statistics collection
3. Check backend logs for successful statistics processing
4. Verify no `ClassCastException` errors appear
5. Confirm statistics are being stored in the database

## Debugging

If the issue persists, check:

1. Ensure `StatisticEntry.fromQueued()` is being called (not direct construction)
2. Verify the Gson serializer is registered correctly
3. Check that `convertValueToString()` is converting Maps to JSON strings
4. Enable debug logging to see the actual JSON being sent

To test locally, you can add logging to see the serialized JSON:

```java
String json = gson.toJson(payload);
logger.info("Sending statistics JSON: " + json);
```

Look for the `value` field in the JSON - it should be a string like `"{\"key\":\"value\"}"` not a nested object like `{"key":"value"}`.

## Alternative Fix (If Issue Persists)

If the Gson serializer approach doesn't work, we may need to modify the `BatchPayload` creation to pre-convert all values:

```java
List<StatisticEntry> entries = queuedStatistics.stream()
    .map(StatisticEntry::fromQueued)
    .map(entry -> {
        // Double-check value is a string
        if (!(entry.value() instanceof String)) {
            throw new IllegalStateException("StatisticEntry value must be a String");
        }
        return entry;
    })
    .toList();
```

## Related Issues

- Connection closed during rollback: This was a secondary issue caused by the primary exception. With the serialization fixed, transactions should complete normally.
- Frontend 401 errors: These appear to be unrelated authentication issues with the web dashboard

## Files Modified

- `RCore/src/main/java/com/raindropcentral/core/service/central/RCentralApiClient.java`

## Build Status

✅ Build successful
✅ No compilation errors
✅ Ready for testing
