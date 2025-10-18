# RaindropCentral Plugin Suite

RaindropCentral is a multi-module Minecraft server platform produced by Antimatter Zone LLC. The repository ships a family of Bukkit/Paper plugins that share infrastructure for dependency loading, asynchronous services, and command discovery. This document introduces each module, how it boots, and which components are responsible for their runtime lifecycle.

## Repository Layout

| Module | Purpose |
| --- | --- |
| [`RCore`](RCore/) | Core services shared by the ecosystem including database repositories, metrics, and the `RCoreService` API. |
| [`RDQ`](RDQ/) | RaindropQuests gameplay plugin with free and premium editions layered on a shared quest/bounty engine. |
| [`RPlatform`](RPlatform/) | Platform bootstrapper that provides logging, metrics, placeholders, database access, and scheduler adapters for every plugin. |
| [`JExDependency`](JExDependency/) | Runtime dependency loader that injects shaded libraries and remaps classes when Paper's plugin loader is active. |
| [`JExCommand`](JExCommand/) | Annotation-driven command and listener discovery used across RCore and RDQ. |
| [`JExEconomy`](JExEconomy/) | Multi-currency economy services and developer APIs (utility module consumed by other products). |
| [`JExTranslate`](JExTranslate/) | Internationalization tooling and MiniMessage-friendly translation manager. |

All Gradle modules are wired so the root build generates three distributable jars: `RCore.jar`, `RDQ-Free.jar`, and `RDQ-Premium.jar`.

## Common Bootstrap Pieces

### Runtime dependency loader (`JEDependency`)

Every plugin entrypoint calls `JEDependency.initializeWithRemapping(...)` during `onLoad`. The loader detects the server type, optionally injects libraries that Paper pre-downloads, and prefers the remapping manager when available. When remapping cannot be used, it falls back to the standard `DependencyManager` before logging completion.【F:JExDependency/src/main/java/de/jexcellence/dependency/JEDependency.java†L22-L110】

### Shared platform (`RPlatform`)

`RPlatform` wraps environment detection, async scheduling, logging, translations, and database bootstrap logic. `initialize()` runs asynchronously, creates the plugin data folders, copies `database/hibernate.properties`, and opens a JPA `EntityManagerFactory`. Additional opt-in hooks configure bStats metrics, PlaceholderAPI integration, and premium-resource detection, while `shutdown()` tears down placeholder hooks and closes resources.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java†L16-L143】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java†L145-L193】

### Command discovery (`CommandFactory`)

`CommandFactory` scans the plugin classloader for classes inside `command` and `listener` packages, instantiates annotated commands, and injects either the plugin instance or an edition-specific context object. Command metadata is loaded from YAML sections (e.g., `commands/<Command>.yml`), and listeners are registered automatically when they implement `Listener`. This factory underpins both RCore and RDQ command registrations.【F:JExCommand/src/main/java/com/raindropcentral/commands/CommandFactory.java†L18-L131】

## Module Lifecycle Summaries

### RCore (Free & Premium)

Both `RCoreFree` and `RCorePremium` extend `JavaPlugin`. On load they initialize runtime dependencies, reflectively construct their `RCore*Impl`, and delegate lifecycle callbacks. Failed loads disable the plugin during `onEnable` to avoid inconsistent state.【F:RCore/rcore-free/src/main/java/com/raindropcentral/core/RCoreFree.java†L1-L41】【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremium.java†L1-L41】

`RCoreFreeImpl` registers the shared `RCoreService` API during `onLoad`, then orchestrates asynchronous startup when enabled: it waits for `RPlatform.initialize()`, wires commands, repositories, supported plugin detection, and server registration before emitting the ASCII-art banner. Metrics service ID `25809` is used for bStats, and lifecycle failures disable the plugin safely.【F:RCore/rcore-free/src/main/java/com/raindropcentral/core/RCoreFreeImpl.java†L1-L126】

`RCorePremiumImpl` follows a similar pattern but defers service registration until after repositories and integrations are ready. It builds repositories from the platform's `EntityManagerFactory`, inspects optional plugin hooks, registers `RCoreService` with higher priority, and records the current server in the database. Shutdown un-registers Bukkit services and closes executors.【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremiumImpl.java†L1-L166】【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremiumImpl.java†L200-L249】

### RDQ (Free & Premium)

`RDQFree` and `RDQPremium` perform the same dependency bootstrap and delegate to their edition implementations. If the implementation fails to load, `onEnable` logs an error and disables the plugin to protect servers from partial initialization.【F:RDQ/rdq-free/src/main/java/com/raindropcentral/rdq/RDQFree.java†L1-L46】【F:RDQ/rdq-premium/src/main/java/com/raindropcentral/rdq/RDQPremium.java†L1-L47】

Both variants wrap a shared `RDQ` engine. The abstract base initializes the `RPlatform`, command framework, Inventory Framework views, bounty/rank repositories, and metrics before registering services. Enable sequences run asynchronously to avoid blocking the main thread and cancel safely during shutdown.【F:RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java†L1-L157】【F:RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java†L159-L234】

`RDQFreeImpl` provisions a lightweight `RDQManager`, limits bounty functionality, and keeps startup metrics under service ID `25810`. `RDQPremiumImpl` wires premium persistence providers, exposes a full-featured `PremiumBountyService`, and registers it in Bukkit's `ServicesManager` with `ServicePriority.High`. Both editions log ASCII banners reflecting available systems.【F:RDQ/rdq-free/src/main/java/com/raindropcentral/rdq/RDQFreeImpl.java†L1-L104】【F:RDQ/rdq-premium/src/main/java/com/raindropcentral/rdq/RDQPremiumImpl.java†L1-L132】

### Supporting Libraries

- **JExEconomy** – Provides an extendable economy service; consult module-specific docs when integrating with RDQ rewards or RCore stats.
- **JExTranslate** – Ships translation managers and MiniMessage utilities, used by `RPlatform` and example plugins.

## Building & Testing

The project uses Gradle with the Wrapper committed. Typical commands:

```bash
./gradlew clean build    # Compile all modules, run tests, and produce plugin jars
./gradlew :RCore:build   # Build only the RCore module
./gradlew :RDQ:build     # Build RDQ variants
```

Run these commands from the repository root after making changes. For full verification, follow up with integration tests or Windows-specific packaging via `build-all.bat` if required by your workflow.

## Next Steps

- Review module-specific READMEs (where present) for configuration details.
- Explore `commands/` and `database/` folders inside each plugin jar to understand runtime resources packaged alongside the code.
- Consult maintainers before altering dependency rules defined by `JEDependency` to avoid breaking runtime shading.
