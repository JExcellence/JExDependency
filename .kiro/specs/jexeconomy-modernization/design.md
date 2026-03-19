# Design Document

## Overview

This design document outlines the comprehensive modernization of the JExEconomy plugin to address critical architectural flaws, improve code maintainability, and enhance user experience. The modernization focuses on five key areas:

1. **Icon and Emoji System Redesign**: Replace hardcoded Unicode emojis with a flexible MiniMessage-based icon registry
2. **Translation System Enhancement**: Restructure translation keys for better organization and eliminate static content
3. **View Functionality Restoration**: Fix broken currency creation and editing views
4. **Code Quality Improvements**: Implement consistent naming conventions and efficient patterns
5. **Performance Optimization**: Enhance view rendering and state management

The design maintains backward compatibility where possible while introducing modern patterns that improve extensibility and maintainability.

## Architecture

### High-Level Component Structure

```
JExEconomy Plugin
├── Icon Registry System (NEW)
│   ├── IconDefinition (semantic name → MiniMessage)
│   ├── IconRegistry (centralized icon management)
│   └── IconResolver (runtime resolution)
├── Translation System (ENHANCED)
│   ├── TranslationKeyRegistry (hierarchical organization)
│   ├── PlaceholderResolver (dynamic content injection)
│   └── TranslationValidator (key validation)
├── View Layer (FIXED)
│   ├── Currency Creation Views
│   ├── Currency Editing Views
│   ├── Currency Overview Views
│   └── View State Manager (NEW)
├── Service Layer (EXISTING)
│   ├── CurrencyAdapter
│   ├── CurrencyRepository
│   └── UserCurrencyRepository
└── Data Layer (EXISTING)
    ├── Currency Entity
    ├── UserCurrency Entity
    └── Database Configuration
```

### Component Relationships

- Icon Registry System provides icons to Translation System and View Layer
- Translation System consumes Icon Registry for dynamic icon resolution
- View Layer uses Translation System for all user-facing text
- View State Manager coordinates state across view transitions
- Service Layer remains unchanged but benefits from improved error handling

## Components and Interfaces

### 1. Icon Registry System

#### IconDefinition

Represents a single icon definition with semantic naming and MiniMessage formatting.

```java
public class IconDefinition {
    private final String semanticName;      // e.g., "success", "error", "currency"
    private final String miniMessageFormat; // e.g., "<green>✓</green>", "<red>✖</red>"
    private final String category;          // e.g., "status", "action", "decoration"
    
    // Validation ensures MiniMessage format is valid
    public IconDefinition(String semanticName, String miniMessageFormat, String category) {
        this.semanticName = validateSemanticName(semanticName);
        this.miniMessageFormat = validateMiniMessage(miniMessageFormat);
        this.category = category;
    }
    
    public String resolve() {
        return miniMessageFormat;
    }
}
```

#### IconRegistry

Centralized registry for all icon definitions with category-based organization.

```java
public class IconRegistry {
    private final Map<String, IconDefinition> icons = new ConcurrentHashMap<>();
    private final Map<String, List<IconDefinition>> categorizedIcons = new ConcurrentHashMap<>();
    
    public void registerIcon(IconDefinition icon) {
        icons.put(icon.getSemanticName(), icon);
        categorizedIcons.computeIfAbsent(icon.getCategory(), k -> new ArrayList<>()).add(icon);
    }
    
    public Optional<IconDefinition> getIcon(String semanticName) {
        return Optional.ofNullable(icons.get(semanticName));
    }
    
    public List<IconDefinition> getIconsByCategory(String category) {
        return categorizedIcons.getOrDefault(category, Collections.emptyList());
    }
    
    // Load icons from configuration file
    public void loadFromConfig(File configFile) {
        // YAML parsing logic
    }
}
```

#### IconResolver

Runtime resolution of icon references in translation strings.

```java
public class IconResolver {
    private final IconRegistry registry;
    
    public String resolveIcons(String text) {
        // Replace {icon:semantic_name} with actual MiniMessage format
        Pattern pattern = Pattern.compile("\\{icon:([a-z_]+)\\}");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String semanticName = matcher.group(1);
            String replacement = registry.getIcon(semanticName)
                .map(IconDefinition::resolve)
                .orElse("[MISSING_ICON:" + semanticName + "]");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
```

### 2. Enhanced Translation System

#### TranslationKeyRegistry

Hierarchical organization of translation keys with validation.

```java
public class TranslationKeyRegistry {
    private final Map<String, TranslationKeyMetadata> keyMetadata = new HashMap<>();
    
    public void registerKey(String key, TranslationKeyMetadata metadata) {
        validateKeyFormat(key);
        keyMetadata.put(key, metadata);
    }
    
    private void validateKeyFormat(String key) {
        // Enforce naming conventions: view_name.component.property
        if (!key.matches("^[a-z_]+\\.[a-z_]+\\.[a-z_]+$")) {
            throw new IllegalArgumentException("Invalid key format: " + key);
        }
    }
}
```

#### PlaceholderResolver

Dynamic content injection with type safety.

```java
public class PlaceholderResolver {
    public String resolvePlaceholders(String template, Map<String, Object> placeholders) {
        String result = template;
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            String value = formatValue(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    private String formatValue(Object value) {
        if (value instanceof Number) {
            return NumberFormat.getInstance().format(value);
        }
        return String.valueOf(value);
    }
}
```

### 3. View State Manager

Coordinates state management across view transitions and ensures consistency.

```java
public class ViewStateManager {
    private final Map<UUID, ViewState> playerStates = new ConcurrentHashMap<>();
    
    public void saveState(UUID playerId, String viewKey, Object state) {
        playerStates.computeIfAbsent(playerId, k -> new ViewState()).put(viewKey, state);
    }
    
    public <T> Optional<T> loadState(UUID playerId, String viewKey, Class<T> type) {
        return Optional.ofNullable(playerStates.get(playerId))
            .map(state -> state.get(viewKey, type));
    }
    
    public void clearState(UUID playerId) {
        playerStates.remove(playerId);
    }
    
    // Automatic cleanup for offline players
    public void cleanupStaleStates() {
        playerStates.entrySet().removeIf(entry -> 
            Bukkit.getPlayer(entry.getKey()) == null
        );
    }
}
```

### 4. Fixed View Components

#### CurrencyCreationViewState

Maintains creation state across anvil view transitions.

```java
public class CurrencyCreationViewState {
    private String identifier;
    private String symbol;
    private Material icon = Material.GOLD_INGOT; // default
    private String prefix = "";
    private String suffix = "";
    
    public boolean isValid() {
        return identifier != null && !identifier.isEmpty() 
            && symbol != null && !symbol.isEmpty();
    }
    
    public Currency toCurrency() {
        return new Currency(prefix, suffix, identifier, symbol, icon);
    }
}
```

#### Enhanced Anvil View Pattern

Improved anvil view with proper state persistence.

```java
public abstract class BaseAnvilInputView extends AnvilView {
    protected final ViewStateManager stateManager;
    
    @Override
    public void onClose(Context context) {
        // Persist state before closing
        UUID playerId = context.getPlayer().getUniqueId();
        Object state = extractState(context);
        stateManager.saveState(playerId, getStateKey(), state);
    }
    
    @Override
    public void onResume(Context originContext, Context targetContext) {
        // Restore state when resuming
        UUID playerId = targetContext.getPlayer().getUniqueId();
        stateManager.loadState(playerId, getStateKey(), getStateType())
            .ifPresent(state -> applyState(targetContext, state));
    }
    
    protected abstract String getStateKey();
    protected abstract Class<?> getStateType();
    protected abstract Object extractState(Context context);
    protected abstract void applyState(Context context, Object state);
}
```

## Data Models

### Icon Configuration Format

```yaml
# icons.yml
icons:
  status:
    success:
      format: "<green>✓</green>"
      fallback: "[OK]"
    error:
      format: "<red>✖</red>"
      fallback: "[ERROR]"
    warning:
      format: "<yellow>⚠</yellow>"
      fallback: "[WARN]"
  
  action:
    create:
      format: "<gradient:#00FF00:#32CD32>✚</gradient>"
      fallback: "[+]"
    edit:
      format: "<gradient:#FFD700:#FFA500>🔧</gradient>"
      fallback: "[EDIT]"
    delete:
      format: "<gradient:#FF6B6B:#FF8E8E>🗑</gradient>"
      fallback: "[DEL]"
  
  decoration:
    currency:
      format: "<gradient:#FFD700:#FFA500>💰</gradient>"
      fallback: "[$]"
    leaderboard:
      format: "<gradient:#FFD700:#FFA500>🏆</gradient>"
      fallback: "[#1]"
```

### Enhanced Translation Format

```yaml
# en.yml
currencies_creating_ui:
  title: "{icon:create} <gradient:#4ECDC4:#45B7B8>Create Currency</gradient>"
  
  identifier:
    name: "{icon:label} <gradient:#FFD700:#FFA500>Currency Identifier</gradient>"
    lore:
      - ""
      - "<gray>Set the unique name that identifies"
      - "<gray>this currency in the system."
      - ""
      - "<gradient:#FFD700:#FFA500>Requirements:</gradient>"
      - "<white>• 2-16 characters</white>"
      - "<white>• Letters, numbers, _, - only</white>"
      - ""
      - "{icon:action_click} <gradient:#00FF00:#32CD32>Click to set identifier</gradient>"
  
  create:
    processing: "{icon:loading} <gradient:#FFD700:#FFA500>Processing:</gradient> <white>Creating currency <gradient:#FFD700:#FFA500>%identifier%</gradient>...</white>"
    success: "{icon:success} <gradient:#00FF00:#32CD32>Currency Created:</gradient> <white>Successfully created currency <gradient:#FFD700:#FFA500>%identifier%</gradient>!</white>"
    error: "{icon:error} <gradient:#FF6B6B:#FF8E8E>Creation Failed:</gradient> <white>%error_message%</white>"
```

## Error Handling

### Error Categories

1. **Validation Errors**: User input doesn't meet requirements
2. **Database Errors**: Persistence layer failures
3. **State Errors**: View state inconsistencies
4. **Icon Resolution Errors**: Missing or invalid icon references

### Error Handling Strategy

```java
public class ErrorHandler {
    private final Logger logger;
    private final IconRegistry iconRegistry;
    
    public void handleError(ErrorContext context) {
        // Log detailed error for administrators
        logger.error("Error in {}: {}", context.getComponent(), context.getMessage(), context.getException());
        
        // Send user-friendly message to player
        String userMessage = formatUserMessage(context);
        context.getPlayer().sendMessage(userMessage);
        
        // Track error metrics
        ErrorMetrics.increment(context.getCategory());
    }
    
    private String formatUserMessage(ErrorContext context) {
        String icon = iconRegistry.getIcon("error")
            .map(IconDefinition::resolve)
            .orElse("[ERROR]");
        
        return icon + " " + context.getUserFriendlyMessage();
    }
}
```

## Testing Strategy

### Unit Tests

1. **IconRegistry Tests**
   - Icon registration and retrieval
   - Category-based filtering
   - Configuration loading
   - Invalid icon handling

2. **IconResolver Tests**
   - Pattern matching accuracy
   - Missing icon fallback
   - Nested icon references
   - Performance with large texts

3. **ViewStateManager Tests**
   - State persistence across transitions
   - Concurrent access handling
   - Stale state cleanup
   - Memory leak prevention

### Integration Tests

1. **View Flow Tests**
   - Complete currency creation workflow
   - Currency editing with state persistence
   - Navigation between views
   - Error recovery scenarios

2. **Translation System Tests**
   - Icon resolution in translations
   - Placeholder replacement
   - Multi-language support
   - Missing key handling

### Performance Tests

1. **View Rendering Performance**
   - Measure render time for paginated views
   - Test with 100+ currencies
   - Concurrent player access
   - Memory usage profiling

2. **Icon Resolution Performance**
   - Benchmark icon resolution speed
   - Test with complex translation files
   - Cache effectiveness measurement

## Implementation Notes

### Migration Strategy

1. **Phase 1**: Implement icon registry system without breaking existing functionality
2. **Phase 2**: Update translation files to use icon placeholders
3. **Phase 3**: Fix view state management and broken functionality
4. **Phase 4**: Refactor naming conventions and code quality
5. **Phase 5**: Performance optimization and cleanup

### Backward Compatibility

- Existing translation files continue to work during migration
- Icon placeholders are optional; literal text still supported
- View behavior remains consistent for end users
- Database schema unchanged

### Configuration Files

New configuration files to be created:
- `icons.yml`: Icon definitions
- `view-config.yml`: View-specific settings
- `performance.yml`: Performance tuning parameters

### Dependencies

No new external dependencies required. All enhancements use existing:
- InventoryFramework for view management
- R18n for translation system
- Bukkit/Paper API for core functionality
