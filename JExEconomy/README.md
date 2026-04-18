# JExEconomy

Multi-currency economy plugin for Paper servers with GUI management, internationalization, transaction logging, and a developer API.

- Multiple currencies with identifiers, symbols, prefixes/suffixes, and icons
- Interactive GUIs powered by InventoryFramework for all currency operations
- Administrative currency logs with filters, pagination, and file export
- 20 languages out of the box, MiniMessage gradient styling
- Console deposit/withdraw commands for server operators
- Developer API via `CurrencyAdapter` with async `CompletableFuture` design
- Vault economy provider — other plugins see JExEconomy through the standard Vault API
- Vault migration — import balances from Essentials, CMI, TNE, iConomy, BOSEconomy, or any Vault-compatible economy


## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Commands](#commands)
- [Permissions](#permissions)
- [Vault Integration](#vault-integration)
- [Developer API](#developer-api)
- [Events](#events)
- [Building from Source](#building-from-source)
- [Credits](#credits)


## Requirements

- Java 21+
- Paper 1.19+ (developed against Paper API 1.21.x)
- JExDependency runtime loader (handles library injection)
- Optional: Vault (soft dependency for third-party economy integration)


## Installation

1. Place the JExEconomy `.jar` into the server's `plugins/` folder.
2. Start the server. Configuration and translation files are generated automatically.
3. Create your first currency with `/currencies` in-game.


## Configuration

### Translations

Translation files live in `plugins/JExEconomy/translations/`. Shipped languages:

`da_DK`, `de`, `de_DE`, `en`, `en_GB`, `en_US`, `es_ES`, `fr_FR`, `it_IT`, `ja_JP`, `ko_KR`, `nl_NL`, `no_NO`, `pl_PL`, `pt_BR`, `ru_RU`, `sv_SE`, `tr_TR`, `zh_CN`, `zh_TW`

Add a new language by creating `<locale>.yml` and registering it in `translation.yml` under `supportedLanguages`. Reload in-game with `/r18n reload`.

### Database

Hibernate properties are in `plugins/JExEconomy/database/hibernate.properties`. Supported backends: H2 (default), MySQL, PostgreSQL, Oracle, SQL Server. Review the generated file for connection and schema settings.


## Commands

### Player Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/pcurrency` | `/currency`, `/balance`, `/bal`, `/money` | View currency balances |
| `/pcurrencies` | `/currencies` | Open currency management GUI |
| `/pcurrencylog` | `/plog`, `/currencylog`, `/economylog` | View and filter transaction logs |
| `/ppay` | `/pay` | Pay another player |

**`/pcurrency`** usage:
- `/currency` -- all your balances
- `/currency <currency>` -- your balance for a specific currency
- `/currency <player>` -- all balances of another player
- `/currency <currency> <player>` -- another player's specific balance

**`/pcurrencies`** opens a GUI with:
- Create currency (anvil-based wizard with validation)
- Edit currency properties (symbol, icon, prefix, suffix)
- Delete currency (impact assessment + confirmation)
- Browse currencies with pagination and leaderboards
- Reset all balances for a currency (admin)

**`/pcurrencylog`** subcommands:
- `view [page]` -- paginated transaction logs with hover details
- `filter <player|currency|type|level|operation> <value>` -- apply filter
- `clear` -- clear all active filters
- `stats` -- log statistics and analytics
- `export` -- export logs to file (admin)
- `details <log_id>` -- inspect a specific log entry
- `help` -- show command usage

**`/ppay`** usage:
- `/pay <currency> <player> <amount>` -- send currency to another player

### Console Commands

| Command | Usage | Description |
|---------|-------|-------------|
| `cdeposit` | `cdeposit <player> <currency> <amount>` | Deposit to a player's account |
| `cwithdraw` | `cwithdraw <player> <currency> <amount>` | Withdraw from a player's account |
| `cmigrate` | `cmigrate <start\|status\|supported\|info>` | Migrate economy data from Vault providers |

**`cmigrate`** subcommands:
- `start [--backup] [--no-backup] [--replace-vault] [currency-id]` — run migration from the detected Vault provider into JExEconomy
- `status` — check the result of the last migration
- `supported` — list economy plugins with dedicated migration support
- `info` — general information about the migration system

### Utility Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/r18n reload` | `/i18n reload` | Reload translation files |
| `/r18n missing` | `/i18n missing` | Browse missing translation keys per locale |


## Permissions

### Player

| Permission | Description |
|------------|-------------|
| `currency.command` | Use `/currency` for own balances |
| `currency.command.other` | View other players' balances |
| `currencies.command` | Access `/currencies` GUI |
| `currencies.command.create` | Create currencies |
| `currencies.command.update` | Edit currencies |
| `currencies.command.delete` | Delete currencies |
| `currencies.command.overview` | View currency overview and details |
| `pcurrencylog.command` | Access `/pcurrencylog` |
| `pay.command` | Use `/pay` |

### Admin

| Permission | Description |
|------------|-------------|
| `jexeconomy.admin.reset` | Reset all balances for a currency |
| `jexeconomy.admin.delete` | Delete a currency |
| `jexeconomy.admin.export` | Export currency logs to file |
| `r18n.reload` | Reload translation files |
| `r18n.missing` | Browse missing translation keys |


## Vault Integration

When Vault is present on the server, JExEconomy automatically registers itself as the Vault economy provider with `ServicePriority.Highest`. Any plugin that uses Vault's `Economy` API (shop plugins, minigame rewards, job plugins, etc.) will use JExEconomy as the backend without any extra configuration.

JExEconomy's multi-currency system is bridged to Vault's single-currency API by mapping operations to the server's default currency.

### Migrating from Another Economy

If your server already uses an economy plugin (Essentials, CMI, TheNewEconomy, iConomy, BOSEconomy, etc.), you can import all player balances into JExEconomy from the server console:

```
cmigrate supported          # see which plugins have dedicated migration support
cmigrate start --backup     # migrate with a backup (default)
cmigrate status             # check how the migration went
```

Options for `cmigrate start`:

| Flag | Description |
|------|-------------|
| `--backup` | Create a backup before migrating (default) |
| `--no-backup` | Skip backup creation |
| `--replace-vault` | Replace the active Vault provider with JExEconomy after migration |
| `[currency-id]` | Target currency to import into (auto-selects the default if omitted) |

Plugins without dedicated support are migrated through the generic Vault API bridge, so any Vault-compatible economy can be imported.


## Developer API

JExEconomy registers `CurrencyAdapter` with Bukkit's `ServicesManager`.

```java
// Obtain the adapter
CurrencyAdapter adapter = Bukkit.getServicesManager().load(CurrencyAdapter.class);

// Query balance (async)
adapter.getBalance(player, currency).thenAccept(balance -> {
    // use balance
});

// Deposit
adapter.deposit(player, currency, 100.0).thenAccept(response -> {
    if (response.isSuccess()) {
        // success -- response.getNewBalance()
    }
});

// Withdraw
adapter.withdraw(player, currency, 50.0).thenAccept(response -> {
    if (response.isSuccess()) {
        // success
    }
});

// Check currency existence
adapter.hasGivenCurrency("coins").thenAccept(exists -> { /* ... */ });

// Create / delete currency (admin operations)
adapter.createCurrency(newCurrency, initiatorPlayer);
adapter.deleteCurrency("coins", initiatorPlayer);
```

All API methods return `CompletableFuture`. Perform heavy work off the main thread; schedule Bukkit API calls back onto the primary thread.


## Events

JExEconomy fires Bukkit events for balance changes and currency lifecycle operations:

| Event | Cancellable | Description |
|-------|:-----------:|-------------|
| `BalanceChangeEvent` | Yes | Fired before a balance modification |
| `BalanceChangedEvent` | No | Fired after a successful balance modification |
| `CurrencyCreateEvent` | Yes | Fired before a currency is created |
| `CurrencyCreatedEvent` | No | Fired after a currency is created |
| `CurrencyDeleteEvent` | Yes | Fired before a currency is deleted |
| `CurrencyDeletedEvent` | No | Fired after a currency is deleted |

Pre-events (cancellable) allow plugins to prevent or modify operations. Post-events are for notification only.


## Building from Source

```bash
# Publish local dependencies first
./gradlew :JExCommand:publishToMavenLocal
./gradlew :JExTranslate:publishToMavenLocal
./gradlew :JExDependency:publishToMavenLocal

# Build both editions
./gradlew :JExEconomy:buildAll

# Or individually
./gradlew :JExEconomy:jexeconomy-free:shadowJar
./gradlew :JExEconomy:jexeconomy-premium:shadowJar

# Publish to local Maven
./gradlew :JExEconomy:publishLocal
```

Artifacts:
- `jexeconomy-free/build/libs/JExEconomy-<version>-Free.jar`
- `jexeconomy-premium/build/libs/JExEconomy-<version>-Premium.jar`


## Credits

- Author: JExcellence -- https://jexcellence.de
- Runtime dependency injection: JExDependency
- GUI framework: InventoryFramework
- Economy bridge: Vault (optional)
