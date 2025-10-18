# Changelog

All notable changes to RPlatform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2025-01-11

### 🎉 Major Release - Complete Modernization

This release represents a complete rewrite and modernization of RPlatform with breaking changes.

### Added

#### Workload System
- `Workload.of(Runnable)` - Factory method for creating workloads from runnables
- `Workload.andThen(Workload)` - Method chaining for workloads
- `WorkloadExecutor.submitAsync(Workload)` - Async workload submission with CompletableFuture
- `WorkloadExecutor.getPendingCount()` - Get number of pending workloads
- `WorkloadExecutor.clear()` - Clear all pending workloads
- Thread-safe implementation with proper synchronization

#### Placeholder System
- `AbstractPlaceholderExpansion` - New base class for placeholder expansions
- `PlaceholderRegistry` - New registry for managing placeholder lifecycle
- `PlaceholderRegistry.isAvailable()` - Check if PlaceholderAPI is available
- `PlaceholderRegistry.isRegistered()` - Check if expansion is registered
- Automatic availability checking

#### Custom Head System
- `CustomHead` - New abstract base class for custom heads
- `HeadCategory` - New enum for head categorization
- Multiple constructor overloads for convenience
- Abstract `createItem()` method for flexible implementation

#### Statistics System
- `StatisticType.getByKey(String)` - Find statistic by key
- `StatisticType.getByDataType(DataType)` - Filter statistics by data type
- `StatisticType.getByCategory(Category)` - Filter statistics by category
- `StatisticType.getDefaultValuesForCategory(Category)` - Get default values for category
- `StatisticType.getPerkActivationCountKey(String)` - Generate dynamic perk activation key
- `StatisticType.getPerkLastUsedKey(String)` - Generate dynamic perk last used key
- `StatisticType.getPerkUsageTimeKey(String)` - Generate dynamic perk usage time key

#### Metrics Integration
- `BStatsMetrics` - New modernized metrics class
- `BStatsMetrics.SimplePie` - Simple pie chart
- `BStatsMetrics.SingleLineChart` - Single line chart
- `BStatsMetrics.AdvancedPie` - Advanced pie chart
- `BStatsMetrics.MultiLineChart` - Multi-line chart
- `BStatsMetrics.DrillDownPie` - Drill-down pie chart
- `BStatsMetrics.JsonBuilder` - Simplified JSON builder

#### Logger Utility
- `CentralLogger` - New centralized logger utility
- `CentralLogger.getLogger(Class)` - Get logger by class
- `CentralLogger.getLogger(String)` - Get logger by name
- `CentralLogger.info(String, String)` - Static info logging
- `CentralLogger.warning(String, String)` - Static warning logging
- `CentralLogger.severe(String, String)` - Static severe logging
- `CentralLogger.log(String, Level, String)` - Static logging with level
- `CentralLogger.log(String, Level, String, Throwable)` - Static logging with exception
- Logger caching for performance

#### Documentation
- `PLATFORM_GUIDE.md` - Comprehensive platform guide
- `MODERNIZATION_SUMMARY.md` - Complete modernization summary
- `QUICK_REFERENCE.md` - Quick reference card
- `CHANGELOG.md` - This changelog

### Changed

#### Workload System
- **BREAKING:** Renamed `WorkloadRunnable` to `WorkloadExecutor`
- **BREAKING:** Renamed `Workload.compute()` to `Workload.execute()`
- **BREAKING:** Renamed `addWorkload()` to `submit()`
- Improved time budget enforcement (2.5ms per tick)
- Better thread safety with synchronized blocks

#### Placeholder System
- **BREAKING:** Renamed `APlaceholder` to `AbstractPlaceholderExpansion`
- **BREAKING:** Renamed `setPlaceholder()` to `definePlaceholders()`
- **BREAKING:** Renamed `onPlaceholder()` to `resolvePlaceholder()`
- **BREAKING:** Removed `isPAPIEnabled()` (use `PlaceholderRegistry.isAvailable()`)
- Simplified API with clearer method names
- Better lifecycle management

#### Custom Head System
- **BREAKING:** Renamed `RHead` to `CustomHead`
- **BREAKING:** Renamed `EHeadFilter` to `HeadCategory`
- **BREAKING:** Changed `getHead()` to abstract `createItem()`
- Decoupled from translation system
- Immutable design with final fields

#### Statistics System
- **BREAKING:** Renamed `EStatisticType` to `StatisticType`
- **BREAKING:** Renamed `StatisticDataType` to `DataType`
- **BREAKING:** Renamed `StatisticCategory` to `Category`
- Enhanced filtering capabilities
- Better default value handling

#### Metrics Integration
- **BREAKING:** Renamed `Metrics` to `BStatsMetrics`
- **BREAKING:** Renamed `JsonObjectBuilder` to `JsonBuilder`
- Simplified chart classes
- Better error handling
- Modern Java features (switch expressions)

### Removed

#### Documentation
- Removed `EXAMPLES.md` (merged into PLATFORM_GUIDE.md)
- Removed `UTILITIES.md` (merged into PLATFORM_GUIDE.md)
- Removed `ARCHITECTURE.md` (merged into PLATFORM_GUIDE.md)
- Removed `PROJECT_SUMMARY.md` (replaced by MODERNIZATION_SUMMARY.md)

#### Code
- Removed all inline comments (self-documenting code)
- Removed `PAPIHook` class (replaced by `PlaceholderRegistry`)
- Removed Hungarian notation from enum names

### Fixed

- Thread safety issues in workload execution
- Logger creation overhead with caching
- Placeholder registration lifecycle
- Metrics configuration loading
- JSON escaping in metrics data

### Improved

#### Code Quality
- Consistent naming conventions
- Modern Java 21+ features
- Comprehensive @NotNull/@Nullable annotations
- Immutable designs with final fields
- Better separation of concerns

#### Performance
- Logger caching reduces overhead
- Concurrent collections for better multi-threading
- Async-first design for non-blocking operations
- Time-budgeted execution prevents lag

#### Documentation
- Single comprehensive guide
- Clear API reference
- Extensive examples
- Migration guide
- Quick reference card

---

## [1.0.0] - 2024-XX-XX

### Initial Release

- Basic workload management
- PlaceholderAPI integration
- Custom head system
- Statistics framework
- Metrics integration
- Platform detection
- Scheduler abstraction

---

## Migration Guide

### From 1.x to 2.0

#### Update Dependencies

```kotlin
// Old
implementation("com.raindropcentral:rplatform:1.0.0")

// New
implementation("com.raindropcentral:rplatform:2.0.0")
```

#### Update Imports

```java
// Old
import com.raindropcentral.rplatform.utility.workload.WorkloadRunnable;
import com.raindropcentral.rplatform.placeholder.APlaceholder;

import com.raindropcentral.rplatform.enumeration.EStatisticType;

// New
import com.raindropcentral.rplatform.workload.WorkloadExecutor;
import com.raindropcentral.rplatform.placeholder.AbstractPlaceholderExpansion;
import com.raindropcentral.rplatform.head.CustomHead;
import com.raindropcentral.rplatform.statistic.StatisticType;
```

#### Update Code

**Workload System:**
```java
// Old
WorkloadRunnable runnable = new WorkloadRunnable();
runnable.addWorkload(() -> task());

// New
WorkloadExecutor executor = new WorkloadExecutor();
executor.submit(() -> task());
```

**Placeholder System:**
```java
// Old
public class MyExpansion extends APlaceholder {
    @Override
    public List<String> setPlaceholder() { ... }
    
    @Override
    public String onPlaceholder(Player player, String params) { ... }
}

// New
public class MyExpansion extends AbstractPlaceholderExpansion {
    @Override
    protected List<String> definePlaceholders() { ... }
    
    @Override
    protected String resolvePlaceholder(Player player, String params) { ... }
}
```

**Custom Heads:**
```java
// Old
public class MyHead extends RHead {
    public ItemStack getHead(Player player) { ... }
}

// New
public class MyHead extends CustomHead {
    @Override
    public ItemStack createItem() { ... }
}
```

**Statistics:**
```java
// Old
EStatisticType stat = EStatisticType.TOTAL_KILLS;

// New
StatisticType stat = StatisticType.TOTAL_KILLS;
```

**Metrics:**
```java
// Old
Metrics metrics = new Metrics(plugin, serviceId, isFolia);

// New
BStatsMetrics metrics = new BStatsMetrics(plugin, serviceId, isFolia);
```

---

## Versioning

RPlatform follows [Semantic Versioning](https://semver.org/):

- **MAJOR** version for incompatible API changes
- **MINOR** version for backwards-compatible functionality additions
- **PATCH** version for backwards-compatible bug fixes

---

## Support

For questions, issues, or feature requests:

- 📖 [Documentation](PLATFORM_GUIDE.md)
- 🐛 [Issue Tracker](https://github.com/raindropcentral/rplatform/issues)
- 💬 [Discord](https://discord.gg/raindropcentral)

---

**© 2025 RaindropCentral - All Rights Reserved**
