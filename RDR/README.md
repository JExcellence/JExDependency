# RDR (Raindrop Distributed Resources)

RDR is the Raindrop storage module: a customizable player storage plugin with GUI workflows, numeric hotkeys (`/rr <hotkey>`), trust-based shared access, and lease-safe persistence.

This README reflects source state as of **March 6, 2026**.

## Module layout

| Module | Role | Main class |
|---|---|---|
| `rdr-common` | Shared runtime, persistence, commands, listeners, services, and views | `com.raindropcentral.rdr.RDR` |
| `rdr-free` | Free edition bootstrap and limits | `com.raindropcentral.rdr.RDRFree` |
| `rdr-premium` | Premium edition bootstrap and behavior | `com.raindropcentral.rdr.RDRPremium` |

## Verified project facts

- Group: `com.raindropcentral.rdr`
- Version: `5.0.0`
- Java toolchain/source/target: `21`
- Plugin `api-version`: `1.19`
- `folia-supported: true` in paper descriptors
- Non-test class count (excluding `package-info.java`):
  - `rdr-common`: `49`
  - `rdr-free`: `3`
  - `rdr-premium`: `3`

## Runtime lifecycle (shared core)

`RDR` is the shared runtime used by both editions:

1. `onLoad()`
   - Creates `RPlatform`
   - Allocates async executor
2. `onEnable()`
   - Initializes platform + scheduler access
   - Ensures default config exists and loads config
   - Initializes requirement adapter setup (`RDRRequirementSetup`)
   - Loads/creates per-server UUID for lease identity
   - Initializes Hibernate repositories
   - Registers services, commands/listeners, and all GUI views
   - Starts recurring services:
     - `StorageSidebarScoreboardService`
     - `StorageFilledTaxScheduler`
3. `onDisable()`
   - Stops scheduler services
   - Shuts down executor
   - Closes `EntityManagerFactory`
   - Shuts down requirement setup

## Capability map from class review

- Persistent per-player storages with max/starting caps and first-join provisioning.
- Numeric storage hotkeys with direct command opens (`/rr <hotkey>`).
- GUI-first flow (overview, list, settings, trusted access, store, tax, admin/config/integrations).
- Trust model per storage:
  - `ASSOCIATE`: deposit access
  - `TRUSTED`: deposit + withdraw access
- Lease-safe storage editing (`RRStorage` lease acquire/renew/save-release) for single active session semantics.
- Global item blacklist enforcement at inventory interaction time.
- Protection bridge checks for restricted storages and open-taxed storages.
- Recurring filled-storage taxes with debt accumulation and frozen-storage repayment flow.
- Optional town tax-bank ledger and mayor transfer support (`vault` transfer path).
- Sidebar scoreboard toggle with persisted per-player preference.
- RPlatform requirement-driven storage store purchases with progress banking.

## Edition behavior

| Behavior | Free (`FreeStorageService`) | Premium (`PremiumStorageService`) |
|---|---|---|
| Player storage limit | `min(config.max_storages, 3)` | Uses `config.max_storages` directly |
| Initial provisioning | Clamped to effective max | Clamped to effective max |
| Storage settings edits | Disabled (`canChangeStorageSettings = false`) | Enabled (`canChangeStorageSettings = true`) |
| `isPremium()` | `false` | `true` |

## Setup

### Requirements

- Java `21+`
- Minecraft server with Bukkit API `1.19+` (Bukkit/Spigot/Paper/Folia)
- RPlatform is shaded into RDR jars (no separate server-side installation required)
- Optional economy plugin: `Vault`, `JExEconomy`, or `TheNewEconomy`
- Optional protection plugin for restricted/taxed storage flows: `RDT`, `Towny`, or `HuskTowns`

### Installation

1. Build RDR artifacts:
   ```bash
   ./gradlew :RDR:buildAll
   ```
2. Copy one edition jar to your server `plugins/` folder:
   - Free: `RDR/rdr-free/build/libs/RDR-5.0.0-Free.jar`
   - Premium: `RDR/rdr-premium/build/libs/RDR-5.0.0-Premium.jar`
3. No separate RPlatform plugin install is required (already shaded in RDR jars).
4. Start/restart the server and configure files in `plugins/RDR/`.

## Command reference

Primary command handler: `PRR` (`PlayerCommand`), so all subcommands are player-only.

- Primary label: `/prr`
- Alias: `/rr`

| Command | Arguments | Behavior | Permission | Notes |
|---|---|---|---|---|
| `/rr` | none | Routes to INFO behavior and opens overview when permitted. | `raindroprdr.command.info` + `raindroprdr.command.storage` | INFO branch checks `INFO`; overview open checks `STORAGE`. |
| `/rr info` | none | Same as `/rr` default route. | `raindroprdr.command.info` + `raindroprdr.command.storage` | `info` is valid action even though not listed in `usage`. |
| `/rr storage` | none | Opens `StorageOverviewView`. | `raindroprdr.command.storage` | Explicit storage gate in handler. |
| `/rr admin` | none | Opens `StorageAdminView`. | `raindroprdr.command.admin` | Explicit admin gate in handler. |
| `/rr scoreboard` | none | Toggles sidebar scoreboard on/off and persists preference. | `raindroprdr.command.scoreboard` | Any extra argument triggers scoreboard syntax message. |
| `/rr taxes` | none | Opens `StorageTaxView`. | `raindroprdr.command.storage` | No dedicated taxes permission node; taxes action reuses storage permission. |
| `/rr <hotkey>` | integer `1..max_hotkeys` | Opens storage bound to that hotkey. | `raindroprdr.command.storage` | Numeric first arg is interpreted as hotkey; invalid/out-of-range/unassigned sends feedback. |

## Permission reference

| Permission node | Description |
|---|---|
| `raindroprdr.command` | Root command node used for first-argument tab completion gate. |
| `raindroprdr.command.admin` | Required for `/rr admin`; reused by admin/config/integration/currency/skills/jobs/placeholder views. |
| `raindroprdr.command.info` | INFO action node for `/rr` and `/rr info` routing. |
| `raindroprdr.command.scoreboard` | Required for `/rr scoreboard`. |
| `raindroprdr.command.storage` | Required for storage opens, hotkeys, and `/rr taxes` view access. |

Additional permission behavior:
- Several admin views also treat `op` status as admin access in addition to `raindroprdr.command.admin`.
- Store requirements can include dynamic permission requirements via configured RPlatform requirement definitions.

## Configuration hotspots

Main config: `rdr-common/src/main/resources/config/config.yml`

- `starting_storages`
- `max_storages`
- `max_hotkeys`
- `global_blacklist`
- `warn_missing_requirements`
- `requirements`
- `protection.restricted_storages`
- `protection.taxed_storages`
- `protection.open_storage_taxes`
- `protection.filled_storage_taxes.interval_ticks`
- `protection.filled_storage_taxes.maximum_freeze`
- `protection.filled_storage_taxes.maximum_debt`
- `protection.filled_storage_taxes.currencies`

## Developer and AI navigation

- Bootstrap and service wiring: `rdr-common/src/main/java/com/raindropcentral/rdr/RDR.java`
- Command surface: `rdr-common/src/main/java/com/raindropcentral/rdr/commands`
- Listener entry points: `rdr-common/src/main/java/com/raindropcentral/rdr/listeners`
- Config sections/parsers: `rdr-common/src/main/java/com/raindropcentral/rdr/configs`
- Requirement adapter: `rdr-common/src/main/java/com/raindropcentral/rdr/requirement/RDRRequirementSetup.java`
- Persistence model/repositories: `rdr-common/src/main/java/com/raindropcentral/rdr/database`
- Runtime services/schedulers: `rdr-common/src/main/java/com/raindropcentral/rdr/service`
- Scoreboard service: `rdr-common/src/main/java/com/raindropcentral/rdr/service/scoreboard/StorageSidebarScoreboardService.java`
- GUI layer: `rdr-common/src/main/java/com/raindropcentral/rdr/view`
- Lease/open path: `rdr-common/src/main/java/com/raindropcentral/rdr/view/StorageViewLauncher.java`
- Edition deltas:
  - `rdr-free/src/main/java/com/raindropcentral/rdr/service/FreeStorageService.java`
  - `rdr-premium/src/main/java/com/raindropcentral/rdr/service/PremiumStorageService.java`

## Known notes and caveats

- `commands/prr.yml` usage text does not list `info`, but `INFO` is a valid action (`/rr info`) and the default fallback.
- `TAXES` action exists, but there is no dedicated `commandTaxes` permission node; handler uses storage permission.
- Numeric first arguments are always treated as hotkeys before action resolution.

## Building from source

From repository root:

```bash
# Build distributable Free/Premium jars
./gradlew :RDR:buildAll

# Run all RDR tests
./gradlew :RDR:testAll

# Publish RDR modules to local Maven
./gradlew :RDR:publishLocal

# Module-level validation (compile + javadocs)
./gradlew :RDR:rdr-common:build :RDR:rdr-free:build :RDR:rdr-premium:build
./gradlew :RDR:rdr-common:javadoc :RDR:rdr-free:javadoc :RDR:rdr-premium:javadoc
```

Windows:

```powershell
.\gradlew.bat :RDR:buildAll
.\gradlew.bat :RDR:testAll
.\gradlew.bat :RDR:publishLocal
.\gradlew.bat :RDR:rdr-common:build :RDR:rdr-free:build :RDR:rdr-premium:build
.\gradlew.bat :RDR:rdr-common:javadoc :RDR:rdr-free:javadoc :RDR:rdr-premium:javadoc
```
