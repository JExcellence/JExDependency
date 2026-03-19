# Design Document

## Overview

This design addresses the Map serialization bug in JEConfig's ConfigMapper where dotted keys in Map fields are incorrectly interpreted as nested YAML paths, causing configuration file corruption through duplicate entries.

## Architecture

The fix involves three main areas:
1. **Key Serialization** - How map keys are written to YAML
2. **Value Replacement** - How existing values are handled during save
3. **Default Value Handling** - How null fields with default getters are processed

```
┌─────────────────────────────────────────────────────────────┐
│                    ConfigMapper                              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │ FieldSerializer │───▶│ MapSerializer   │                 │
│  └─────────────────┘    └────────┬────────┘                 │
│                                  │                           │
│                    ┌─────────────▼─────────────┐            │
│                    │    KeyStrategy            │            │
│                    │  ┌─────────┐ ┌─────────┐  │            │
│                    │  │ Nested  │ │  Flat   │  │            │
│                    │  │ (dots=  │ │ (dots=  │  │            │
│                    │  │  paths) │ │ literal)│  │            │
│                    │  └─────────┘ └─────────┘  │            │
│                    └───────────────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. @FlatKeys Annotation

```java
package de.jexcellence.configmapper.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a Map field should treat keys as literal strings,
 * not as nested YAML paths. Dots in keys will be preserved as-is.
 *
 * <p>Example usage:</p>
 * <pre>
 * @FlatKeys
 * private Map<String, Integer> homeLimits;
 * </pre>
 *
 * <p>This will serialize as:</p>
 * <pre>
 * homeLimits:
 *   "jexhome.limit.basic": 3
 *   "jexhome.limit.vip": 10
 * </pre>
 *
 * <p>Instead of the nested interpretation:</p>
 * <pre>
 * homeLimits:
 *   jexhome:
 *     limit:
 *       basic: 3
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface FlatKeys {
}
```

### 2. MapSerializer Enhancement

```java
public class MapSerializer {
    
    /**
     * Serializes a Map to YAML, respecting the @FlatKeys annotation.
     *
     * @param map the map to serialize
     * @param field the field being serialized (for annotation checking)
     * @param writer the YAML writer
     */
    public void serialize(Map<?, ?> map, Field field, YamlWriter writer) {
        boolean flatKeys = field.isAnnotationPresent(FlatKeys.class);
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            
            if (flatKeys || !key.contains(".")) {
                // Write as literal key (quote if contains special chars)
                writer.writeQuotedKey(key, value);
            } else {
                // Legacy behavior: interpret dots as path separators
                writer.writeNestedPath(key, value);
            }
        }
    }
}
```

### 3. ConfigSection Save Logic

```java
public abstract class AConfigSection {
    
    /**
     * Saves the configuration, replacing existing values completely.
     * Does not serialize fields that are null (unless they have explicit defaults).
     */
    public void save() {
        // Clear existing section before writing
        clearSection();
        
        for (Field field : getSerializableFields()) {
            Object value = getFieldValue(field);
            
            // Skip null fields - don't serialize default getter values
            if (value == null) {
                continue;
            }
            
            // Skip if value equals the annotated default
            if (isDefaultValue(field, value)) {
                continue;
            }
            
            serializeField(field, value);
        }
    }
    
    /**
     * Gets the actual field value, NOT the getter return value.
     * This prevents default values from getters being serialized.
     */
    private Object getFieldValue(Field field) {
        field.setAccessible(true);
        return field.get(this);
    }
}
```

## Data Models

### Configuration State Tracking

```java
/**
 * Tracks whether a field value was explicitly set or is using defaults.
 */
public class FieldState {
    private final Object value;
    private final boolean explicitlySet;
    private final Object defaultValue;
    
    public boolean shouldSerialize() {
        // Only serialize if explicitly set and different from default
        return explicitlySet && !Objects.equals(value, defaultValue);
    }
}
```

## Error Handling

| Scenario | Handling |
|----------|----------|
| Map key contains YAML special characters | Quote the key in output |
| Existing config has corrupted nested structure | Log warning, attempt recovery by flattening |
| Field is null but getter returns default | Do not serialize (field value takes precedence) |
| @FlatKeys on non-Map field | Log warning, ignore annotation |

## Testing Strategy

### Unit Tests

1. **FlatKeys Serialization Test**
   - Map with dotted keys and @FlatKeys annotation
   - Verify keys are written as quoted literals
   - Verify no nested structure is created

2. **Idempotency Test**
   - Load config, save immediately
   - Verify file is identical
   - Repeat 10 times, verify no growth

3. **Default Value Test**
   - Field is null, getter returns default
   - Verify default is NOT serialized
   - Verify loading works with missing key

4. **Migration Test**
   - Load corrupted config with nested duplicates
   - Save with @FlatKeys
   - Verify clean output

### Integration Tests

1. **Server Restart Simulation**
   - Create config with Map<String, Integer> homeLimits
   - Simulate 100 restart cycles
   - Verify file size remains constant
   - Verify no duplicate entries

## Migration Path

For existing configurations with corrupted nested structures:

```java
/**
 * Utility to clean corrupted configs during migration.
 */
public class ConfigMigrator {
    
    /**
     * Flattens a corrupted nested map structure back to flat keys.
     */
    public Map<String, Object> flattenCorruptedMap(Map<String, Object> nested, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : nested.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            
            if (entry.getValue() instanceof Map) {
                // Recursively flatten nested maps
                result.putAll(flattenCorruptedMap((Map) entry.getValue(), key));
            } else {
                result.put(key, entry.getValue());
            }
        }
        
        return result;
    }
}
```
