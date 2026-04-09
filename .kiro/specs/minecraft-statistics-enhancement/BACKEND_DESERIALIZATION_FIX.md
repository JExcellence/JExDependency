# Backend Deserialization Fix for StatisticEntry.value

## Problem Statement

The backend API is failing to deserialize the `StatisticEntry.value` field with the following error:

```
class java.util.ImmutableCollections$Map1 cannot be cast to class java.lang.String
```

This indicates that the backend is receiving a nested JSON object for the `value` field but expects it to be a String.

## Root Cause

The `StatisticEntry` model has a `value` field of type `Map<String, Object>`, but the backend's deserialization logic expects this field to be stored as a JSON string in the database/API contract, not as a nested object.

## Current Data Structure

The frontend is sending data in this format:

```json
{
  "entries": [
    {
      "key": "minecraft:custom:minecraft.play_time",
      "category": "GENERAL",
      "value": {
        "material": null,
        "entity": null,
        "value": 12345
      }
    }
  ]
}
```

## Expected Behavior

The backend should be able to handle BOTH formats:
1. **Nested object format** (current): `"value": { "material": null, "entity": null, "value": 12345 }`
2. **JSON string format** (legacy): `"value": "{\"material\":null,\"entity\":null,\"value\":12345}"`

## Solution: Update Backend Deserialization

### Step 1: Locate the StatisticEntry Model

Find the backend model class for `StatisticEntry`. It's likely in a package like:
- `com.raindropcentral.backend.model.statistics.StatisticEntry`
- `com.raindropcentral.api.dto.StatisticEntry`
- Or similar

### Step 2: Add Custom Deserializer

Add a custom Jackson deserializer that handles both formats:

```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.Map;

public class StatisticEntry {
    private String key;
    private String category;
    
    @JsonDeserialize(using = ValueDeserializer.class)
    private Map<String, Object> value;
    
    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Map<String, Object> getValue() { return value; }
    public void setValue(Map<String, Object> value) { this.value = value; }
    
    /**
     * Custom deserializer that handles both JSON string and nested object formats.
     */
    public static class ValueDeserializer extends JsonDeserializer<Map<String, Object>> {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final TypeReference<Map<String, Object>> MAP_TYPE = 
            new TypeReference<Map<String, Object>>() {};
        
        @Override
        public Map<String, Object> deserialize(JsonParser parser, DeserializationContext context) 
                throws IOException, JsonProcessingException {
            JsonNode node = parser.getCodec().readTree(parser);
            
            if (node.isTextual()) {
                // Handle JSON string format: "value": "{\"key\":\"value\"}"
                String jsonString = node.asText();
                try {
                    return MAPPER.readValue(jsonString, MAP_TYPE);
                } catch (JsonProcessingException e) {
                    context.reportInputMismatch(Map.class, 
                        "Failed to parse value as JSON string: " + e.getMessage());
                    return Map.of();
                }
            } else if (node.isObject()) {
                // Handle nested object format: "value": {"key": "value"}
                return MAPPER.convertValue(node, MAP_TYPE);
            } else if (node.isNull()) {
                // Handle null values
                return Map.of();
            } else {
                context.reportInputMismatch(Map.class, 
                    "Expected JSON object or string, got: " + node.getNodeType());
                return Map.of();
            }
        }
    }
}
```

### Step 3: Alternative Solution (If Using Gson)

If the backend uses Gson instead of Jackson, use this implementation:

```java
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class StatisticEntry {
    private String key;
    private String category;
    
    @JsonAdapter(ValueDeserializer.class)
    private Map<String, Object> value;
    
    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Map<String, Object> getValue() { return value; }
    public void setValue(Map<String, Object> value) { this.value = value; }
    
    /**
     * Custom Gson deserializer that handles both JSON string and nested object formats.
     */
    public static class ValueDeserializer implements JsonDeserializer<Map<String, Object>> {
        private static final Gson GSON = new Gson();
        private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();
        
        @Override
        public Map<String, Object> deserialize(JsonElement json, Type typeOfT, 
                JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                // Handle JSON string format: "value": "{\"key\":\"value\"}"
                String jsonString = json.getAsString();
                try {
                    return GSON.fromJson(jsonString, MAP_TYPE);
                } catch (JsonSyntaxException e) {
                    throw new JsonParseException("Failed to parse value as JSON string", e);
                }
            } else if (json.isJsonObject()) {
                // Handle nested object format: "value": {"key": "value"}
                return GSON.fromJson(json, MAP_TYPE);
            } else if (json.isJsonNull()) {
                // Handle null values
                return Map.of();
            } else {
                throw new JsonParseException("Expected JSON object or string, got: " + json);
            }
        }
    }
}
```

### Step 4: Update Database Schema (If Needed)

If the `value` field is stored as a `TEXT` or `VARCHAR` column in the database, you may need to update the JPA/Hibernate mapping:

```java
import jakarta.persistence.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "statistic_entries")
public class StatisticEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String key;
    
    @Column(nullable = false)
    private String category;
    
    // Store as JSON string in database
    @Column(columnDefinition = "TEXT")
    private String valueJson;
    
    // Transient field for application use
    @Transient
    private Map<String, Object> value;
    
    // Convert Map to JSON string before persisting
    @PrePersist
    @PreUpdate
    private void serializeValue() {
        if (value != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.valueJson = mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize value", e);
            }
        }
    }
    
    // Convert JSON string to Map after loading
    @PostLoad
    private void deserializeValue() {
        if (valueJson != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.value = mapper.readValue(valueJson, 
                    new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize value", e);
            }
        }
    }
    
    // Getters and setters
    public Map<String, Object> getValue() { return value; }
    public void setValue(Map<String, Object> value) { this.value = value; }
}
```

## Testing the Fix

### Test Case 1: Nested Object Format

```json
POST /api/statistics/batch
{
  "entries": [
    {
      "key": "minecraft:custom:minecraft.play_time",
      "category": "GENERAL",
      "value": {
        "material": null,
        "entity": null,
        "value": 12345
      }
    }
  ]
}
```

**Expected**: Should deserialize successfully without errors.

### Test Case 2: JSON String Format (Legacy)

```json
POST /api/statistics/batch
{
  "entries": [
    {
      "key": "minecraft:custom:minecraft.play_time",
      "category": "GENERAL",
      "value": "{\"material\":null,\"entity\":null,\"value\":12345}"
    }
  ]
}
```

**Expected**: Should deserialize successfully without errors.

### Test Case 3: Null Value

```json
POST /api/statistics/batch
{
  "entries": [
    {
      "key": "minecraft:custom:minecraft.play_time",
      "category": "GENERAL",
      "value": null
    }
  ]
}
```

**Expected**: Should handle gracefully, storing an empty map.

## Verification Steps

1. **Apply the custom deserializer** to the `StatisticEntry` model
2. **Rebuild the backend** application
3. **Run unit tests** to verify deserialization works for both formats
4. **Test the API endpoint** with sample payloads (see test cases above)
5. **Check logs** for any deserialization errors
6. **Verify database storage** to ensure values are stored correctly

## Additional Recommendations

### 1. Add Logging

Add debug logging to track deserialization:

```java
@Override
public Map<String, Object> deserialize(JsonParser parser, DeserializationContext context) 
        throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);
    
    logger.debug("Deserializing value node type: {}", node.getNodeType());
    
    if (node.isTextual()) {
        logger.debug("Parsing as JSON string");
        // ... rest of implementation
    } else if (node.isObject()) {
        logger.debug("Parsing as nested object");
        // ... rest of implementation
    }
}
```

### 2. Add Validation

Validate the deserialized map structure:

```java
private void validateValue(Map<String, Object> value) {
    if (value == null) return;
    
    // Ensure required fields exist
    if (!value.containsKey("value")) {
        throw new IllegalArgumentException("Value map must contain 'value' field");
    }
    
    // Validate value is numeric
    Object val = value.get("value");
    if (!(val instanceof Number)) {
        throw new IllegalArgumentException("Value field must be numeric");
    }
}
```

### 3. Add Unit Tests

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatisticEntryDeserializationTest {
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    void testDeserializeNestedObject() throws Exception {
        String json = """
            {
              "key": "test.key",
              "category": "GENERAL",
              "value": {"material": null, "entity": null, "value": 100}
            }
            """;
        
        StatisticEntry entry = mapper.readValue(json, StatisticEntry.class);
        
        assertNotNull(entry.getValue());
        assertEquals(100, entry.getValue().get("value"));
    }
    
    @Test
    void testDeserializeJsonString() throws Exception {
        String json = """
            {
              "key": "test.key",
              "category": "GENERAL",
              "value": "{\\"material\\":null,\\"entity\\":null,\\"value\\":100}"
            }
            """;
        
        StatisticEntry entry = mapper.readValue(json, StatisticEntry.class);
        
        assertNotNull(entry.getValue());
        assertEquals(100, entry.getValue().get("value"));
    }
    
    @Test
    void testDeserializeNullValue() throws Exception {
        String json = """
            {
              "key": "test.key",
              "category": "GENERAL",
              "value": null
            }
            """;
        
        StatisticEntry entry = mapper.readValue(json, StatisticEntry.class);
        
        assertNotNull(entry.getValue());
        assertTrue(entry.getValue().isEmpty());
    }
}
```

## Summary

The fix requires adding a custom deserializer to the backend's `StatisticEntry` model that can handle both nested object and JSON string formats for the `value` field. This ensures backward compatibility while supporting the current frontend implementation.

**Key Changes:**
1. Add `@JsonDeserialize(using = ValueDeserializer.class)` annotation to the `value` field
2. Implement `ValueDeserializer` class that handles both formats
3. Add unit tests to verify both formats work correctly
4. Update database mapping if needed (for JPA/Hibernate entities)

**Priority:** HIGH - This is blocking the statistics collection feature from working correctly.
