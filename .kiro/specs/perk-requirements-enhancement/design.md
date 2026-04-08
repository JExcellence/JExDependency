# Design: Perk Requirements Enhancement

## Overview

This design enhances the perk requirement system to properly detect currency balances and display detailed requirement information similar to the rank system. The solution involves fixing currency detection logic, adding display name support, and creating a comprehensive requirement rendering system.

## Architecture

### Current Issues

1. **Currency Detection Problem**: `CurrencyRequirement.getCurrentBalance()` returns 0 because it cannot find the currency
   - The currency lookup is case-sensitive
   - No logging when currency is not found
   - No fallback mechanism

2. **Basic Requirement Cards**: Perk requirement cards only show type and progress percentage
   - No detailed task previews
   - No specific information about what's needed
   - No I18n support for descriptions

### Proposed Solution

```
┌─────────────────────────────────────────────────────────────┐
│                    CurrencyRequirement                       │
│  - Fix currency lookup (case-insensitive)                   │
│  - Add display name retrieval                               │
│  - Add detailed logging                                     │
│  - Cache currency lookups                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              PerkRequirementCardRenderer                     │
│  - Generate task previews per requirement type              │
│  - Use I18n for translatable text                          │
│  - Use MiniMessage for formatting                          │
│  - Display progress bars                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    PerkDetailView                            │
│  - Use enhanced requirement cards                           │
│  - Display detailed requirement information                 │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. CurrencyRequirement Enhancements

**New Methods:**
```java
public class CurrencyRequirement extends AbstractRequirement {
    
    /**
     * Gets the display name of the currency.
     * Uses JExEconomy currency display name or falls back to identifier.
     */
    @NotNull
    public String getCurrencyDisplayName();
    
    /**
     * Gets detailed description with current/required amounts.
     */
    @NotNull
    public String getDetailedDescription(@NotNull Player player);
    
    /**
     * Finds currency with case-insensitive matching and logging.
     */
    @Nullable
    private Object findJExCurrencyImproved(Object adapter, String identifier);
}
```

**Key Changes:**
- Make currency lookup case-insensitive
- Add logging when currency not found
- Cache currency lookups to avoid repeated reflection calls
- Add display name retrieval from JExEconomy

### 2. PerkRequirementCardRenderer

**New Class:**
```java
public class PerkRequirementCardRenderer {
    
    /**
     * Generates task previews for a requirement.
     */
    @NotNull
    public List<TaskPreview> generateTaskPreviews(
        @NotNull PerkRequirement requirement,
        @NotNull Player player
    );
    
    /**
     * Creates an enhanced requirement card with task previews.
     */
    @NotNull
    public ItemStack createEnhancedRequirementCard(
        @NotNull Player player,
        @NotNull PerkRequirement requirement,
        @NotNull PerkRequirementService requirementService
    );
    
    /**
     * Task preview record.
     */
    public record TaskPreview(String name, boolean completed) {}
}
```

**Requirement-Specific Rendering:**
- **CURRENCY**: "Need {amount} {currency_name}" or "Have {current}/{required} {currency_name}"
- **ITEM**: "Need {quantity}x {item_name}"
- **EXPERIENCE_LEVEL**: "Reach level {level}"
- **PLAYTIME**: "Play for {time}"
- **PERMISSION**: "Requires permission: {permission}"

### 3. Translation Keys

**New I18n Keys (en_US.yml):**
```yaml
# Currency requirement descriptions
requirement:
  currency:
    coins: "{amount} Coins"
    gems: "{amount} Gems"
    tokens: "{amount} Tokens"
    
# Task preview templates
requirement:
  task:
    currency:
      need: "Need {amount} {currency}"
      have: "Have {current}/{required} {currency}"
    item:
      need: "Need {quantity}x {item}"
    experience:
      need: "Reach level {level}"
    playtime:
      need: "Play for {time}"
```

## Data Models

### TaskPreview Record
```java
public record TaskPreview(
    String name,        // Display name of the task
    boolean completed   // Whether task is completed
) {}
```

### Currency Cache
```java
private static final Map<String, Object> CURRENCY_CACHE = new ConcurrentHashMap<>();
```

## Error Handling

### Currency Not Found
```java
if (currency == null) {
    LOGGER.log(Level.WARNING, 
        "Currency ''{0}'' not found in JExEconomy. Available currencies: {1}",
        new Object[]{currencyId, getAvailableCurrencies(adapter)});
}
```

### Reflection Failures
```java
try {
    // Reflection code
} catch (ClassNotFoundException e) {
    LOGGER.log(Level.FINE, "JExEconomy not available, trying Vault");
} catch (Exception e) {
    LOGGER.log(Level.WARNING, "Error accessing economy API", e);
}
```

## Testing Strategy

### Unit Tests
- Test currency lookup with various identifiers (case variations)
- Test display name retrieval
- Test task preview generation for each requirement type
- Test I18n key resolution with missing translations

### Integration Tests
- Test with JExEconomy present
- Test with Vault only
- Test with neither economy plugin
- Test requirement card rendering in UI

### Manual Testing
- Verify currency balance displays correctly
- Verify requirement cards show detailed information
- Verify translations work for different locales
- Verify progress bars display accurately

## Implementation Notes

### Currency Lookup Improvement
```java
private Object findJExCurrencyImproved(Object adapter, String identifier) {
    // Check cache first
    String cacheKey = identifier.toLowerCase();
    if (CURRENCY_CACHE.containsKey(cacheKey)) {
        return CURRENCY_CACHE.get(cacheKey);
    }
    
    try {
        Method getAllCurrenciesMethod = adapter.getClass().getMethod("getAllCurrencies");
        Map<Long, ?> currencies = (Map<Long, ?>) getAllCurrenciesMethod.invoke(adapter);
        
        // Case-insensitive search
        for (Object currency : currencies.values()) {
            Method getIdentifierMethod = currency.getClass().getMethod("getIdentifier");
            String currencyIdentifier = (String) getIdentifierMethod.invoke(currency);
            
            if (currencyIdentifier.equalsIgnoreCase(identifier)) {
                CURRENCY_CACHE.put(cacheKey, currency);
                return currency;
            }
        }
        
        // Log available currencies if not found
        List<String> available = currencies.values().stream()
            .map(c -> {
                try {
                    Method m = c.getClass().getMethod("getIdentifier");
                    return (String) m.invoke(c);
                } catch (Exception e) {
                    return "unknown";
                }
            })
            .collect(Collectors.toList());
            
        LOGGER.log(Level.WARNING, 
            "Currency ''{0}'' not found. Available: {1}", 
            new Object[]{identifier, available});
            
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error finding currency: " + identifier, e);
    }
    
    return null;
}
```

### Task Preview Generation
```java
private List<TaskPreview> generateCurrencyTaskPreviews(
        CurrencyRequirement req, Player player) {
    double current = req.getCurrentBalance(player);
    double required = req.getAmount();
    String currencyName = req.getCurrencyDisplayName();
    
    boolean completed = current >= required;
    String taskName = completed ?
        String.format("Have %.0f/%0.f %s", current, required, currencyName) :
        String.format("Need %.0f %s", required, currencyName);
    
    return List.of(new TaskPreview(taskName, completed));
}
```

## Benefits

1. **Accurate Progress Display**: Players see their actual currency balance
2. **Better UX**: Detailed requirement information helps players understand what they need
3. **Consistency**: Perk requirements match rank requirements in detail level
4. **Localization**: Full I18n support for all requirement text
5. **Debugging**: Better logging helps diagnose configuration issues
6. **Performance**: Currency caching reduces reflection overhead
