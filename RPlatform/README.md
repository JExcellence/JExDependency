# RPlatform

**Modern Spigot/Paper/Folia Plugin Development Framework**

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/raindropcentral/rplatform)
[![Java](https://img.shields.io/badge/java-21+-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

---

## Overview

RPlatform is a comprehensive utility framework designed for modern Minecraft plugin development. It provides essential tools and abstractions for building robust, performant plugins across Spigot, Paper, and Folia platforms.

### Key Features

- ⚡ **Workload Management** - Time-budgeted task execution preventing server lag
- 🕒 **Scheduler Delegation** - Folia-aware scheduler adapter selection through `ISchedulerAdapter`
- 🔌 **Placeholder Integration** - Simplified PlaceholderAPI expansion creation backed by `PlaceholderManager`
- 🎭 **Custom Head System** - Type-safe custom player head management
- 📊 **Statistics Framework** - Comprehensive player statistics tracking
- 📈 **Metrics Integration** - bStats integration with custom charts
- 🧭 **Premium Detection** - Classpath resource probes via `detectPremiumVersion(...)`
- 🗄️ **Database Safeguards** - Hibernate property bootstrapping with automatic resource copying
- 🔄 **Async-First Design** - CompletableFuture-based operations
- 🛡️ **Type Safety** - Extensive @NotNull/@Nullable annotations
- ☕ **Modern Java** - Java 21+ features and best practices

### Security & logging

- `CentralLogger` centralizes all plugin output, writing to rotating log files while optionally mirroring to console. Call `CentralLogger.initialize(plugin)` during `onLoad` to enable the managed handlers.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/logging/CentralLogger.java†L34-L347】
- `PlatformLogFormatter` renders structured JSON lines that include timestamp, plugin name, thread, and sanitized context, enabling ingestion into modern observability pipelines.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/logging/PlatformLogFormatter.java†L17-L166】
- Logging APIs expose console toggles, flush hooks, and shutdown routines; always invoke `CentralLogger.shutdown()` during plugin disable to avoid truncated records.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/logging/CentralLogger.java†L351-L458】

---

## Quick Start

### Installation

**Gradle (Kotlin DSL)**
```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation("com.raindropcentral:rplatform:2.0.0")
}
```

### Basic Usage

```java
public class MyPlugin extends JavaPlugin {

    private RPlatform platform;

    @Override
    public void onEnable() {
        platform = new RPlatform(this);

        platform.initialize()
            .thenRun(() -> {
                platform.initializeMetrics(12345);
                platform.initializePlaceholders("myplugin");
                getLogger().info("Plugin initialized!");
            });
    }

    @Override
    public void onDisable() {
        platform.shutdown();
    }
}
```

---

## Core Components

### 1. Workload System

Execute tasks within a strict time budget to prevent server lag:

```java
WorkloadExecutor executor = new WorkloadExecutor();

for (Chunk chunk : chunks) {
    executor.submit(() -> processChunk(chunk));
}

Bukkit.getScheduler().runTaskTimer(plugin, executor, 0L, 1L);
```

### 2. Placeholder System

Create PlaceholderAPI expansions with ease:

```java
public class MyExpansion extends AbstractPlaceholderExpansion {

    @Override
    protected List<String> definePlaceholders() {
        return List.of("balance", "level", "rank");
    }

    @Override
    protected String resolvePlaceholder(Player player, String params) {
        return switch (params) {
            case "balance" -> getBalance(player);
            case "level" -> getLevel(player);
            case "rank" -> getRank(player);
            default -> null;
        };
    }
}
```

> ℹ️ Call `platform.initializePlaceholders("identifier")` inside the `initialize()` completion callback so the asynchronous bootstrap has populated translation, command, and database services before the PlaceholderAPI bridge registers. The `PlaceholderManager` will validate PlaceholderAPI availability and ignore duplicate registrations.

### 3. Statistics System

Type-safe player statistics tracking:

```java
// Get statistic by key
StatisticType stat = StatisticType.getByKey("total_kills");

// Filter by category
List<StatisticType> gameplayStats = StatisticType.getByCategory(
    StatisticType.Category.GAMEPLAY
);

// Get default values
Map<String, Object> defaults = StatisticType.getDefaultValuesForCategory(
    StatisticType.Category.CORE
);
```

### 4. Custom Heads

Manage custom player heads with type safety:

```java
public class MyHead extends CustomHead {

    public MyHead() {
        super(
            "diamond_sword",
            "8d7f3e3c-3f4a-4c5d-9e2f-1a2b3c4d5e6f",
            "eyJ0ZXh0dXJlcyI6...",
            HeadCategory.INVENTORY
        );
    }

    @Override
    public ItemStack createItem() {
        return UnifiedBuilderFactory.head()
            .setCustomTexture(getTextureUuid(), getTextureValue())
            .setName(Component.text("Diamond Sword Head"))
            .build();
    }
}
```

### 5. Metrics Integration

bStats integration with custom charts:

```java
platform.initialize()
    .thenRun(() -> {
        platform.initializeMetrics(12345);

        BStatsMetrics metrics = new BStatsMetrics(
                this,
                12345,
                platform.getPlatformType() == PlatformType.FOLIA
        );

        metrics.addCustomChart(new BStatsMetrics.SimplePie("server_type", () -> {
            return platform.getPlatformType().name();
        }));

        metrics.addCustomChart(new BStatsMetrics.AdvancedPie("player_ranks", () -> {
            Map<String, Integer> ranks = new HashMap<>();
            // Populate ranks
            return ranks;
        }));
    });
```

> ✅ The metrics manager should be initialized only after `initialize()` completes; the guard in `initializeMetrics(int)` prevents duplicate registrations and ignores non-positive service identifiers.
> 💡 Import `com.raindropcentral.rplatform.api.PlatformType` when using the enum comparison shown above.

### 6. Localization Glue

JExTranslate is configured by `com.raindropcentral.rplatform.localization.TranslationManager`,
which RPlatform initializes asynchronously. The manager wires together the YAML repository,
MiniMessage formatter, and auto-detecting locale resolver. Locale selection follows:

1. Explicit overrides passed to `TranslationService.create(key, player, locale)` or `createFresh(...)`
2. The player's cached locale from the active resolver
3. The service default configured during bootstrap (defaults to `Locale.ENGLISH`)

Locale caches are cleared whenever `TranslationManager.reload()` completes or when
`setPlayerLocale(...)` is called. If you hot-swap repository files, formatter implementations,
or resolver strategies, ensure `TranslationService.clearLocaleCache()` is triggered as part of
your reload routine.

Always route player-facing messages through the fluent `TranslationService` API so that
MiniMessage formatting, prefixes, and placeholder chaining remain consistent. Translation YAML
files live under `<plugin data folder>/translations`; the manager copies bundled defaults and
reloads them on demand.

---

## Documentation

For complete documentation, examples, and API reference, see:

📖 **[PLATFORM_GUIDE.md](PLATFORM_GUIDE.md)** - Complete platform guide with detailed examples

---

## Project Structure

```
RPlatform/
├── src/main/java/com/raindropcentral/rplatform/
│   ├── api/               # Public-facing platform abstractions
│   ├── config/            # Configuration and permission descriptors
│   ├── database/          # Hibernate bootstrap and converters
│   ├── localization/      # Translation manager and locale support
│   ├── metrics/           # bStats integration helpers
│   ├── placeholder/       # PlaceholderAPI expansions and managers
│   ├── scheduler/         # Adaptive scheduler adapters and implementations
│   ├── service/           # Service registry and default bindings
│   ├── translation/       # YAML repository utilities and services
│   ├── logging/           # Centralized logging utilities
│   ├── utility/           # Builders, serializers, and shared helpers
│   ├── view/              # Inventory and anvil view abstractions
│   ├── workload/          # Workload management system
│   └── RPlatform.java     # Main platform class
└── PLATFORM_GUIDE.md      # Complete documentation
```

---

## Requirements

- **Java:** 21 or higher
- **Server:** Spigot/Paper/Folia 1.21+
- **Gradle:** 8.0+

---

## Building

```bash
# Build project
./gradlew build

# Publish to local Maven
./gradlew publishToMavenLocal

# Run tests
./gradlew test
```

---

## Best Practices

### 1. Always Use Async for I/O

```java
platform.getScheduler().runAsync(() -> {
    // Database operations
    // File operations
    // Network requests
});
```

### 2. Use Workload System for Heavy Tasks

```java
WorkloadExecutor executor = new WorkloadExecutor();
for (Task task : heavyTasks) {
    executor.submit(() -> processTask(task));
}
```

### 3. Proper Resource Cleanup

```java
@Override
public void onDisable() {
    if (platform != null) {
        platform.shutdown();
    }
}
```

---

## Migration from Old RPlatform

**Old Code:**
```java
RPlatform platform = new RPlatform(plugin);
platform.onEnable("Starting...");
```

**New Code:**
```java
RPlatform platform = new RPlatform(plugin);
platform.initialize().thenRun(() -> {
    platform.getLogger().info("Starting...");
});
```

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch
3. Follow existing code style
4. Add tests for new features
5. Submit a pull request

---

## Support

- **Issues:** [GitHub Issues](https://github.com/raindropcentral/rplatform/issues)
- **Documentation:** [Platform Guide](PLATFORM_GUIDE.md)
- **Discord:** [RaindropCentral Discord](https://discord.gg/raindropcentral)

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Credits

**Author:** JExcellence
**Organization:** RaindropCentral
**Version:** 2.0.0

---

**© 2025 RaindropCentral - All Rights Reserved**
