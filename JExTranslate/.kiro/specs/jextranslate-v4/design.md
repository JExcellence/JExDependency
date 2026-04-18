# Design Document

## Overview

JExTranslate v4.0 is a major enhancement that consolidates the API, adds performance optimizations through caching, enables player locale persistence with database support, and introduces several developer-friendly features like JSON support, type-safe keys, and hot reload.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        R18nManager                               │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐ │
│  │ MessageBuilder│  │TranslationLoader│  │    LocaleStorage     │ │
│  └──────┬──────┘  └───────┬──────┘  │ ┌─────────┐ ┌─────────┐ │ │
│         │                 │         │ │InMemory │ │Database │ │ │
│         ▼                 ▼         │ └─────────┘ └─────────┘ │ │
│  ┌─────────────┐  ┌──────────────┐  └─────────────────────────┘ │
│  │MessageProvider│  │  FileWatcher │                             │
│  │  + Cache    │  └──────────────┘                             │
│  └─────────────┘                                                │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐ │
│  │   Metrics   │  │  KeyGenerator │  │     ExportService      │ │
│  └─────────────┘  └──────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. LocaleStorage Interface

```java
public interface LocaleStorage {
    Optional<String> getLocale(UUID playerId);
    void setLocale(UUID playerId, String locale);
    void removeLocale(UUID playerId);
    void clearAll();
}
```

### 2. InMemoryLocaleStorage

```java
public class InMemoryLocaleStorage implements LocaleStorage {
    private final Map<UUID, String> locales = new ConcurrentHashMap<>();
    // Simple in-memory implementation
}
```

### 3. DatabaseLocaleStorage with JEHibernate

```java
@Entity
@Table(name = "r18n_player_locale")
public class PlayerLocale extends BaseEntity {
    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;
    
    @Column(name = "locale", nullable = false, length = 10)
    private String locale;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}

public class PlayerLocaleRepository extends GenericCachedRepository<PlayerLocale, Long, UUID> {
    // Standard JEHibernate repository pattern
}

public class DatabaseLocaleStorage implements LocaleStorage {
    private final PlayerLocaleRepository repository;
    // Delegates to repository with caching
}
```

### 4. Enhanced R18nConfiguration

```java
public record R18nConfiguration(
    String defaultLocale,
    Set<String> supportedLocales,
    String translationDirectory,
    boolean keyValidationEnabled,
    boolean placeholderAPIEnabled,
    boolean legacyColorSupport,
    boolean debugMode,
    // New fields
    boolean cacheEnabled,
    int cacheMaxSize,
    int cacheExpireMinutes,
    boolean watchFiles,
    boolean metricsEnabled,
    boolean generateKeys,
    MissingKeyHandler missingKeyHandler
) {
    @FunctionalInterface
    public interface MissingKeyHandler {
        @Nullable String handle(String key, String locale, Map<String, Object> placeholders);
    }
}
```

### 5. Translation Cache in MessageProvider

```java
public class MessageProvider {
    private final Cache<CacheKey, Component> componentCache;
    
    private record CacheKey(String key, String locale, int placeholderHash) {}
    
    public MessageProvider(R18nConfiguration config, VersionDetector detector) {
        if (config.cacheEnabled()) {
            this.componentCache = Caffeine.newBuilder()
                .maximumSize(config.cacheMaxSize())
                .expireAfterAccess(config.cacheExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();
        }
    }
    
    public CacheStats getCacheStats() {
        return componentCache != null ? componentCache.stats() : null;
    }
}
```

### 6. Enhanced TranslationLoader with JSON Support

```java
public class TranslationLoader {
    private final ObjectMapper jsonMapper;
    private final Yaml yamlParser;
    
    private void loadTranslationFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        if (fileName.endsWith(".json")) {
            loadJsonFile(filePath);
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            loadYamlFile(filePath);
        }
    }
    
    private void loadJsonFile(Path filePath) {
        Map<String, Object> data = jsonMapper.readValue(
            Files.newInputStream(filePath),
            new TypeReference<>() {}
        );
        // Same flattening logic as YAML
    }
}
```

### 7. FileWatcher for Hot Reload

```java
public class TranslationFileWatcher implements Runnable {
    private final WatchService watchService;
    private final Path translationsDir;
    private final Runnable onReload;
    private volatile boolean running = true;
    
    @Override
    public void run() {
        while (running) {
            WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (isTranslationFile(event)) {
                        onReload.run();
                        break;
                    }
                }
                key.reset();
            }
        }
    }
    
    public void stop() {
        running = false;
    }
}
```

### 8. Translation Metrics

```java
public class TranslationMetrics {
    private final Map<String, AtomicLong> keyUsage = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> missingKeys = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localeUsage = new ConcurrentHashMap<>();
    
    public void recordKeyUsage(String key, String locale) {
        keyUsage.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        localeUsage.computeIfAbsent(locale, k -> new AtomicLong()).incrementAndGet();
    }
    
    public void recordMissingKey(String key, String locale) {
        missingKeys.computeIfAbsent(key + ":" + locale, k -> new AtomicLong()).incrementAndGet();
    }
    
    public List<Map.Entry<String, Long>> getMostUsedKeys(int limit) {
        return keyUsage.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
            .limit(limit)
            .map(e -> Map.entry(e.getKey(), e.getValue().get()))
            .toList();
    }
}
```

### 9. Plural Support

```java
// Translation file format:
// items.count.one: "You have {count} item"
// items.count.other: "You have {count} items"

public class MessageBuilder {
    private final Map<String, Integer> countPlaceholders = new HashMap<>();
    
    public MessageBuilder count(String placeholder, int value) {
        this.countPlaceholders.put(placeholder, value);
        this.placeholders.put(placeholder, String.valueOf(value));
        return this;
    }
    
    private String resolvePluralKey(String baseKey, String locale) {
        if (countPlaceholders.isEmpty()) return baseKey;
        
        // Get first count placeholder for plural selection
        var entry = countPlaceholders.entrySet().iterator().next();
        int count = entry.getValue();
        
        String pluralForm = PluralRules.select(locale, count);
        String pluralKey = baseKey + "." + pluralForm;
        
        if (loader.hasKey(pluralKey, locale)) {
            return pluralKey;
        }
        // Fallback to .other or base key
        String otherKey = baseKey + ".other";
        return loader.hasKey(otherKey, locale) ? otherKey : baseKey;
    }
}

public class PluralRules {
    public static String select(String locale, int count) {
        // Simplified ICU rules
        String lang = locale.split("[_-]")[0];
        return switch (lang) {
            case "en", "de", "es", "it", "pt" -> count == 1 ? "one" : "other";
            case "fr" -> count <= 1 ? "one" : "other";
            case "ru", "uk" -> selectSlavic(count);
            case "ar" -> selectArabic(count);
            case "ja", "ko", "zh" -> "other"; // No plural forms
            default -> count == 1 ? "one" : "other";
        };
    }
}
```

### 10. Export Service

```java
public class TranslationExportService {
    public enum ExportFormat { CSV, JSON, YAML }
    
    public void export(Path outputPath, ExportFormat format, 
                       Map<String, Map<String, List<String>>> translations) {
        switch (format) {
            case CSV -> exportCsv(outputPath, translations);
            case JSON -> exportJson(outputPath, translations);
            case YAML -> exportYaml(outputPath, translations);
        }
    }
    
    private void exportCsv(Path path, Map<String, Map<String, List<String>>> translations) {
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("key,locale,value\n");
            for (var keyEntry : translations.entrySet()) {
                for (var localeEntry : keyEntry.getValue().entrySet()) {
                    String value = String.join("\\n", localeEntry.getValue());
                    writer.write(String.format("\"%s\",\"%s\",\"%s\"\n",
                        escape(keyEntry.getKey()),
                        localeEntry.getKey(),
                        escape(value)));
                }
            }
        }
    }
}
```

### 11. Type-Safe Key Generator (Gradle Task)

```java
// Generated output example:
public final class TranslationKeys {
    private TranslationKeys() {}
    
    public static final class Welcome {
        public static final String MESSAGE = "welcome.message";
        public static final String FIRST_JOIN = "welcome.first-join";
    }
    
    public static final class Error {
        public static final String PERMISSION = "error.permission";
        public static final String NOT_FOUND = "error.not-found";
    }
}
```

## Data Models

### PlayerLocale Entity

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key (auto-generated) |
| uniqueId | UUID | Player UUID (unique) |
| locale | String | Locale code (e.g., "en_US") |
| updatedAt | Instant | Last update timestamp |

### CacheKey Record

| Field | Type | Description |
|-------|------|-------------|
| key | String | Translation key |
| locale | String | Locale code |
| placeholderHash | int | Hash of placeholder values |

## Error Handling

1. **Missing translations**: Use configurable MissingKeyHandler
2. **JSON parse errors**: Log warning, skip file, continue loading
3. **Database errors**: Fall back to in-memory storage with warning
4. **File watcher errors**: Log error, disable watcher, continue operation
5. **Cache errors**: Disable caching, continue with direct parsing

## Testing Strategy

1. **Unit tests** for LocaleStorage implementations
2. **Unit tests** for PluralRules with various locales
3. **Unit tests** for TranslationExportService formats
4. **Integration tests** for file watcher (with temp directories)
5. **Performance tests** for cache hit/miss scenarios
