# RDS (RaindropShops)

RDS is the RaindropShops module that provides player-owned shops and server-owned (admin) shops with a rich GUI-first flow based on Inventory Framework views and anvil inputs.

This README reflects source state as of **March 6, 2026**.

## Module layout

| Module | Role | Main class |
|---|---|---|
| `rds-common` | Shared runtime, persistence, commands, listeners, services, and views | `com.raindropcentral.rds.RDS` |
| `rds-free` | Free edition bootstrap and limits | `com.raindropcentral.rds.RDSFree` |
| `rds-premium` | Premium edition bootstrap and behavior | `com.raindropcentral.rds.RDSPremium` |

## Verified project facts

- Group: `com.raindropcentral.rds`
- Version: `1.0.0`
- Java toolchain/source/target: `21`
- Plugin `api-version`: `1.19`
- `folia-supported: true` in paper descriptors
- Non-test class count (excluding `package-info.java`):
  - `rds-common`: `92`
  - `rds-free`: `3`
  - `rds-premium`: `3`

## Runtime lifecycle (shared core)

`RDS` is the shared runtime used by both editions:

1. `onLoad()`
   - Creates `RPlatform`
   - Allocates async executor
2. `onEnable()`
   - Initializes platform + scheduler access
   - Loads config and initializes Hibernate repositories
   - Registers commands/listeners via command factory
   - Registers all views and anvil views through `ViewFrame`
   - Starts schedulers/services:
     - `ShopTaxScheduler`
     - `AdminShopRestockScheduler`
     - `AdminShopServerBankScheduler`
     - `ShopBossBarService`
     - `ShopSidebarScoreboardService`
3. `onDisable()`
   - Shuts down services/schedulers
   - Stops executor
   - Closes `EntityManagerFactory`

## Capability map from class review

- Shop ownership and placement rules: `service/shop`, `listeners/BlockListener`, `ShopOwnershipSupport`
- Admin shops and stock controls: `service/shop/AdminShopStockSupport`, `AdminShopRestockScheduler`
- Economy and banking: `database/entity/*Bank*`, `service/bank/ServerBankTransferSupport`, `AdminShopServerBankScheduler`
- Tax engine and debt tracking: `service/tax/*`, `configs/TaxSection`
- GUI/view stack:
  - Main views: `view/shop/*View`
  - Anvil editors: `view/shop/anvil/*AnvilView`
- Configurable requirement store tiers: `configs/StoreRequirementSection`, `view/shop/ShopStorePricingSupport`
- Integrations:
  - Economy providers via Vault/JExEconomy bridge
  - Protection bridge integration support
  - PlaceholderAPI integration/admin tooling views

## Edition behavior

| Behavior | Free (`FreeShopService`) | Premium (`PremiumShopService`) |
|---|---|---|
| Player shop limit | `min(config.max_shops, 3)` (`3` if configured unlimited) | Uses `config.max_shops` directly |
| Admin shop limit | `2` | Unlimited (`-1`) |
| Config editing | Disabled (`canChangeConfigs = false`) | Enabled (`canChangeConfigs = true`) |
| `isPremium()` | `false` | `true` |

## Setup

### Requirements

- Java `21+`
- Minecraft server with Bukkit API `1.19+` (Bukkit/Spigot/Paper/Folia)
- RPlatform is shaded into RDS jars (no separate server-side installation required)
- Optional economy plugin: `Vault`, `JExEconomy`, or `TheNewEconomy`
- Optional integrations when available: `PlaceholderAPI`, `LuckPerms`, `CMI`, and supported protection plugins

### Installation

1. Build RDS artifacts:
   ```bash
   ./gradlew :RDS:buildAll
   ```
2. Copy one edition jar to your server `plugins/` folder:
   - Free: `RDS/rds-free/build/libs/RDS-1.0.0-Free.jar`
   - Premium: `RDS/rds-premium/build/libs/RDS-1.0.0-Premium.jar`
3. No separate RPlatform plugin install is required (already shaded in RDS jars).
4. Start/restart the server and configure files in `plugins/RDS/`.

## Command reference

Primary command handler: `PRS` (`PlayerCommand`), so all subcommands are player-only.

- Primary label: `/prs`
- Alias: `/rs`

| Command | Arguments | Behavior | Permission | Notes |
|---|---|---|---|---|
| `/rs` | none | Falls through to info summary output. | `raindropshops.command.info` (configured) | Unknown/invalid first argument also resolves to `INFO`. |
| `/rs info` | none | Shows owned shops, tracked bank currencies, tax totals, protection plugin, and next tax schedule. | `raindropshops.command.info` (configured) | No explicit `hasNoPermission` check in the `INFO` branch. |
| `/rs admin` | none | Opens `ShopAdminView`. | `raindropshops.command.admin` | Explicit permission gate in handler. |
| `/rs bar` | none | Toggles shop boss bar for the player. | `raindropshops.command.bar` | Explicit permission gate in handler. |
| `/rs give <player> <amount>` | `player` must be online, `amount` must be positive integer | Increments target shop count and gives `ShopBlock` items. | `raindropshops.command.give` | Syntax/help shown on invalid args. |
| `/rs scoreboard <ledger\|stock>` | `ledger` or `stock` | Enables/switches sidebar type, or disables when same type is already active. | `raindropshops.command.scoreboard` | Persists selection on player profile. |
| `/rs search` | none | Opens `ShopSearchView`. | `raindropshops.command.search` (configured) | No explicit `hasNoPermission` check in branch. |
| `/rs store` | none | Opens `ShopStoreView`. | `raindropshops.command.store` (configured) | No explicit `hasNoPermission` check in branch. |
| `/rs taxes` | none | Opens `ShopTaxView`. | `raindropshops.command.taxes` | Explicit permission gate in handler. |

## Permission reference

| Permission node | Description |
|---|---|
| `raindropshops.command` | Root command node; explicitly used for first-argument tab completion access. |
| `raindropshops.command.admin` | Required for `/rs admin`; reused by multiple admin views (config/bank/jobs/skills/integrations). |
| `raindropshops.command.bar` | Required for `/rs bar`. |
| `raindropshops.command.info` | Configured info node for `/rs info` and base `/rs` fallback. |
| `raindropshops.command.give` | Required for `/rs give`. |
| `raindropshops.command.scoreboard` | Required for `/rs scoreboard <ledger|stock>`. |
| `raindropshops.command.search` | Configured node for `/rs search`. |
| `raindropshops.command.store` | Configured node for `/rs store`. |
| `raindropshops.command.taxes` | Required for `/rs taxes`. |
| `raindropshops.admin.shops` | Required in `ShopOverviewView` to toggle a shop between player/admin mode. |
| `raindropshops.admin.bypass.town` | Allows ShopBlock placement outside the player's own town even when `protection.only_player_shops` is enabled. |

## PlaceholderAPI Placeholders

| Placeholder | Description |
|---|---|
| `%rds_shops_owned%` | Number of shops owned by the player (non-admin shops). |
| `%rds_shops_admin%` | Number of admin shops in the database. |
| `%rds_shops_items%` | Number of items the player is selling across owned shops. |
| `%rds_shops_tax%` | Comma-separated list of tax debt by currency for the player's owned shops. |

## Configuration hotspots

Main config: `rds-common/src/main/resources/config/config.yml`

- `max_shops`
- `requirements`
- `admin_shops`
- `server_bank`
- `protection`
- `taxes`
- `boss_bar`
- `default_currency_type`
- `blacklisted_currencies`

## Developer and AI navigation

- Bootstrap and service wiring: `rds-common/src/main/java/com/raindropcentral/rds/RDS.java`
- Command surface: `rds-common/src/main/java/com/raindropcentral/rds/commands`
- Listener entry points: `rds-common/src/main/java/com/raindropcentral/rds/listeners`
- Config sections/parsers: `rds-common/src/main/java/com/raindropcentral/rds/configs`
- Persistence model/repositories: `rds-common/src/main/java/com/raindropcentral/rds/database`
- Runtime services/schedulers: `rds-common/src/main/java/com/raindropcentral/rds/service`
- GUI layer: `rds-common/src/main/java/com/raindropcentral/rds/view/shop`
- Anvil UI editors: `rds-common/src/main/java/com/raindropcentral/rds/view/shop/anvil`
- Edition deltas:
  - `rds-free/src/main/java/com/raindropcentral/rds/service/FreeShopService.java`
  - `rds-premium/src/main/java/com/raindropcentral/rds/service/PremiumShopService.java`

## Known mismatches to track

- `EPRSAction` includes `BAR`, but `commands/prs.yml` usage text omits `bar`.
- `commands/prs.yml` defines `commandMain`, but no corresponding `EPRSPermission` enum entry exists.
- `INFO`, `SEARCH`, and `STORE` have configured permission nodes, but execution branches do not call explicit permission checks.

## Building from source

From repository root:

```bash
# Build distributable Free/Premium jars
./gradlew :RDS:buildAll

# Run all RDS tests
./gradlew :RDS:testAll

# Publish RDS modules to local Maven
./gradlew :RDS:publishLocal

# Module-level validation (compile + javadocs)
./gradlew :RDS:rds-common:build :RDS:rds-free:build :RDS:rds-premium:build
./gradlew :RDS:rds-common:javadoc :RDS:rds-free:javadoc :RDS:rds-premium:javadoc
```

Windows:

```powershell
.\gradlew.bat :RDS:buildAll
.\gradlew.bat :RDS:testAll
.\gradlew.bat :RDS:publishLocal
.\gradlew.bat :RDS:rds-common:build :RDS:rds-free:build :RDS:rds-premium:build
.\gradlew.bat :RDS:rds-common:javadoc :RDS:rds-free:javadoc :RDS:rds-premium:javadoc
```
