# RPlatform

RPlatform is the shared platform/runtime module used by RaindropCentral plugins.
It provides cross-platform abstractions, requirement/reward systems, integrations, UI helpers, serialization, and runtime utilities for Spigot/Paper/Folia environments.

This README was refreshed from source on **March 6, 2026** to correct stale references and document current behavior.

## Module Facts (Verified)

- Module: `:RPlatform`
- Group: `com.raindropcentral.platform`
- Version: `2.0.0`
- Java target: `21`
- Core server API dependency: Paper `1.21.10-R0.1-SNAPSHOT` (compileOnly)
- Platform detection order: `FOLIA -> PAPER -> SPIGOT`
- Class inventory snapshot: `208` Java types (excluding `package-info.java`) across `62` packages

## What Exists In This Module

- Runtime orchestrator: `RPlatform`
- Platform abstraction: `PlatformAPI`, `PlatformAPIFactory`, `PlatformType`, impls for Spigot/Paper/Folia
- Scheduler abstraction: `ISchedulerAdapter` with Bukkit and Folia adapters
- Requirements system: types, registry/provider model, parser/factory/builder, lifecycle/events/metrics/validators, plugin integrations
- Rewards system: types, registry/provider model, parser/factory/builder, lifecycle/events/metrics/validators
- Logging system: centralized plugin logging with formatting, filtered console output, rotating files, recursion safeguards
- Localization: `TranslationManager` wrapper over JExTranslate (`R18nManager`)
- Integrations: skill/job/protection/geyser/luckperms/economy bridges
- UI abstractions: inventory-framework based views plus custom anvil input feature/NMS bridge
- Utilities: item/head builders, serializers/converters, custom head catalog support, workload executor

## Lifecycle And Usage

`RPlatform` construction selects platform/scheduler adapters immediately, but most systems are prepared in `initialize()` asynchronously.

Initialization flow:

1. Register built-in requirement types (`BuiltInRequirementProvider.initialize()`).
2. Attempt database resource bootstrap (`database/hibernate.properties`) and create `EntityManagerFactory`.
3. Build `TranslationManager` and `CommandUpdater`.
4. Mark platform initialized.
5. Initialize translations (`translationManager.initialize()`).

Recommended usage:

```java
public final class MyPlugin extends JavaPlugin {

    private RPlatform platform;

    @Override
    public void onEnable() {
        platform = new RPlatform(this);
        platform.initialize().thenRun(() -> {
            platform.initializeMetrics(12345);
            platform.initializeGeyser();
            platform.initializePlaceholders("myplugin");
        }).exceptionally(ex -> {
            getLogger().severe("RPlatform init failed: " + ex.getMessage());
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (platform != null) {
            if (platform.isInitialized()) {
                platform.getTranslationManager().shutdown();
            }
            platform.shutdown();
        }
    }
}
```

## Requirements System

Primary packages:

- `requirement`
- `requirement.impl`
- `requirement.config`
- `requirement.json`
- `requirement.plugin`
- `requirement.validation`
- `requirement.lifecycle`
- `requirement.event`
- `requirement.metrics`
- `requirement.async`

Built-in type ids registered by `BuiltInRequirementProvider`:

- `ITEM`
- `CURRENCY`
- `EXPERIENCE_LEVEL`
- `PERMISSION`
- `LOCATION`
- `PLAYTIME`
- `COMPOSITE`
- `CHOICE`
- `TIME_BASED`
- `PLUGIN`

Plugin requirements (`type: PLUGIN`) are routed through `PluginIntegrationRegistry` and support explicit integration ids plus category auto-detection (`SKILLS`, `JOBS`, `TOWNS`).

Built-in `PLUGIN` integrations currently include:

- skills: `ecoskills`, `auraskills`, `mcmmo`
- jobs: `ecojobs`, `jobsreborn`
- towns: `rdt`, `towny`, `husktowns`

Town integrations share the following requirement key:

- `town_level`

`town_level` resolves each plugin's native town level concept:

- `rdt`: composite town level
- `towny`: Towny level number
- `husktowns`: HuskTowns town level

RDT additionally exposes the following requirement keys:

- `nexus_level`
- `chunk_level`
- `security_chunk_level`
- `bank_chunk_level`
- `farm_chunk_level`
- `outpost_chunk_level`
- `medic_chunk_level`
- `armory_chunk_level`

## Rewards System

Primary packages:

- `reward`
- `reward.impl`
- `reward.config`
- `reward.json`
- `reward.validation`
- `reward.lifecycle`
- `reward.event`
- `reward.metrics`
- `reward.async`

Reward implementation classes currently present:

- `ITEM`
- `CURRENCY`
- `EXPERIENCE`
- `COMMAND`
- `COMPOSITE`
- `CHOICE`
- `PERMISSION`
- `SOUND`
- `PARTICLE`
- `TELEPORT`
- `VANISHING_CHEST`

Important wiring detail:

- `RewardFactory` default converters currently cover: `ITEM`, `CURRENCY`, `EXPERIENCE`, `COMMAND`, `COMPOSITE`, `CHOICE`, `PERMISSION`.
- If you need additional reward config parsing paths, register converters/adapters explicitly.

## Integration Matrix

Built-in bridge families:

- Skills: EcoSkills, AuraSkills, mcMMO
- Jobs: EcoJobs, JobsReborn
- Protection: Towny, RDT, HuskTowns
- Bedrock detection: Floodgate/Geyser via `GeyserService`
- Economy bridge: JExEconomy reflection bridge
- Permission bridge utilities: LuckPerms service helpers

Related config/examples:

- `src/main/resources/plugin-integrations.yml`
- `src/main/resources/examples/plugin-requirement-examples.yml`

## Logging

The active logging classes in this module are:

- `CentralLogger`
- `PluginLogger`
- `RLogFormatter`
- `FilteredConsoleHandler`
- `RotatingFileHandler`
- `SafeLoggingPrintStream`

There is no `PlatformLogFormatter` class in this module.

## PlaceholderAPI Notes

Two placeholder abstractions exist:

- `AbstractPlaceholderExpansion` + `PlaceholderRegistry`
- `PlaceholderManager` (reflection-based manager)

`PlaceholderManager` currently reflects `com.raindropcentral.rplatform.placeholder.PAPIHook`.
That class is not present in this module, so if you rely on `initializePlaceholders(...)`, provide the expected hook class in your runtime artifact or adapt the integration path.

## Package Map (Developer + AI Friendly)

Use this as the navigation index when changing behavior:

- `api`: platform abstractions and implementations
- `scheduler`: sync/async execution adapters
- `service`: service discovery/registration with retries
- `localization`: translation bootstrap wrapper
- `logging`: centralized plugin logging stack
- `metrics`: bStats wrapper and manager
- `placeholder`: PlaceholderAPI abstractions and registration
- `requirement`: requirement domain, parsing, lifecycle, events, validation
- `reward`: reward domain, parsing, lifecycle, events, validation
- `integration.geyser`: Floodgate/Geyser detection and adapter
- `skill`, `job`, `protection`: optional plugin bridge layers
- `view` and `view.anvil`: inventory-framework views and custom anvil feature
- `utility`: builders, heads, small helpers
- `serializer`, `json`, `database.converter`: serialization and persistence conversion helpers
- `workload`: per-tick budgeted task execution
- `type`: statistics enum (`EStatisticType`)
- `version`: runtime environment detection

To regenerate the class inventory quickly:

```powershell
Get-ChildItem src/main/java/com/raindropcentral/rplatform -Recurse -Filter *.java |
  Where-Object { $_.Name -ne 'package-info.java' } |
  Group-Object DirectoryName
```

## Known Caveats

- `RPlatform.initializeDatabaseResources()` expects `database/hibernate.properties` in plugin resources; this module does not currently include that resource under `src/main/resources`.
- `RPlatform.shutdown()` handles placeholders/platform API/logger, but does not call `TranslationManager.shutdown()` automatically.
- README previously referenced `PLATFORM_GUIDE.md` and `PlatformLogFormatter`; those references were stale and removed.

## Build And Verification

From repository root:

```bash
./gradlew :RPlatform:build
./gradlew :RPlatform:javadoc
```

Windows:

```powershell
.\gradlew.bat :RPlatform:build
.\gradlew.bat :RPlatform:javadoc
```

## For AI Contributors

When making edits in this module:

1. Start from `RPlatform` for lifecycle impact.
2. Check package-level docs (`package-info.java`) before changing public behavior.
3. For new requirement/reward types, update all relevant wiring:
   - implementation class
   - type/provider registration
   - parser/factory conversion path
   - validation/lifecycle/event hooks as needed
4. Do not assume reflective integrations are available at runtime; preserve graceful fallback behavior.
5. Verify docs and code agree after edits.
