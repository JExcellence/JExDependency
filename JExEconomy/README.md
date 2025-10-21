# JExEconomy

Advanced multi-currency economy plugin for Paper servers with a modern UI, internationalization, detailed logging, console administration utilities, and a developer API.

JExEconomy provides:
- Multiple currencies with identifiers, symbols, prefixes/suffixes, and icons
- Interactive GUIs powered by InventoryFramework with comprehensive currency management
- Streamlined GUI-only command interface for intuitive user experience
- Complete currency lifecycle management (create, edit, delete, view)
- Advanced leaderboard system with ranking visualization and balance reset functionality
- Administrative actions and pagination-based currency logs with filters and export
- Internationalization (any language via R18n) with MiniMessage formatting
- Console deposit/withdraw tools for server management
- Developer API via CurrencyAdapter for integrations


## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
  - [Translations](#translations)
  - [Database](#database)
- [Commands](#commands)
  - [Player Commands](#player-commands)
    - [/pcurrency](#pcurrency)
    - [/pcurrencies](#pcurrencies)
    - [/pcurrencylog](#pcurrencylog)
  - [Console Commands](#console-commands)
    - [/cdeposit](#cdeposit)
    - [/cwithdraw](#cwithdraw)
  - [Utility Commands](#utility-commands)
    - [/r18n](#r18n)
- [Permissions](#permissions)
- [Developer API](#developer-api)
- [Building from Source](#building-from-source)
- [Runtime Files & Folders](#runtime-files--folders)
- [Status and Roadmap](#status-and-roadmap)
- [Live Example (RaindropPlugins)](#live-example-raindropplugins)
- [Credits](#credits)


## Overview
JExEconomy is a modular, extensible currency system for Paper servers. It supports creating and managing multiple currencies, exposing user-friendly commands and rich, gradient-styled messages and GUIs. It includes a robust logging utility for transaction and management events with filters and export options.

- Plugin name: JExEconomy
- Authors: JExcellence
- Main: `de.jexcellence.currency.JExEconomyImpl`
- API version: 1.19 (builds against Paper API 1.21.x)
- Dependency: InventoryFramework (already injected at runtime)
- Soft dependency: Vault (optional)


## Features
- **Multi-currency support**
  - Identifier, symbol, prefix, suffix, and icon per currency
  - Automatic account provisioning for players on currency creation
  - Complete currency lifecycle management (create, edit, delete)
- **Modern GUIs (InventoryFramework)**
  - **Streamlined Command Interface**: `/currencies` opens comprehensive GUI management
  - **Currency Creation**: Interactive anvil-based creation wizard with real-time validation
  - **Currency Editing**: Full property editing with anvil input views for all currency attributes
  - **Currency Deletion**: Safe deletion with impact assessment and confirmation dialogs
  - **Currency Overview**: Paginated display of all currencies with detailed information
  - **Advanced Leaderboards**: Ranking system with visual hierarchy (gold/silver/bronze) and formatted displays
  - **Admin Operations**: Reset all player balances for specific currencies with confirmation
- **Comprehensive Currency Management**
  - **Create**: Through intuitive anvil GUI with validation for identifiers, symbols, icons, prefixes, and suffixes
  - **Edit**: Modify existing currency properties (symbol, icon, prefix, suffix) with real-time updates
  - **Delete**: Safe removal with impact assessment showing affected players and total balances
  - **View**: Detailed currency information with leaderboard access and administrative functions
  - **Reset**: Administrative function to reset all player balances to zero with confirmation
- **Detailed currency logs for auditing**
  - View with pages, hover tooltips, and clickable navigation
  - Filters by player, currency, log type, log level, and operation (deposit/withdraw)
  - Export logs to file
- **Internationalization (R18n)**
  - Any language is supported: create a new language file and fill translations
  - Reload translations in-game and analyze missing translation keys
  - MiniMessage gradient styling across all messages and UIs
- **Console utilities for economy administration**
  - Deposit/withdraw to player accounts directly from console
- **Developer API**
  - Use `CurrencyAdapter` for balance queries, deposits, withdrawals, and currency management


## Requirements
- Java: 21+
- Server: Paper 1.19+ (developed with Paper API 1.21.x)
- InventoryFramework: Already injected by the platform; no separate installation required
- Optional: Vault (softdepend)


## Installation
1. Ensure your server runs the required Paper version and Java runtime.
2. Place the JExEconomy jar into the server's `plugins` folder.
3. Start the server to generate configuration and translation files.
4. Optionally configure translations and database settings (see below).


## Configuration
JExEconomy ships with structured configuration resources.

### Translations
- Location (resources): `src/main/resources/translations/`
  - `translation.yml`
    - `defaultLanguage: en`
    - `supportedLanguages: [en, de]`
  - `en.yml`, `de.yml` contain comprehensive MiniMessage-styled messages.

Any language is supported: add your own `<lang>.yml` file alongside the existing ones, then include the language code in `supportedLanguages` within `translation.yml`. After saving files, reload translations in-game:
- `/r18n reload` — reloads all translation files
- `/r18n missing` — opens a locale browser; inspect missing keys per locale with pagination

The missing-keys browser highlights absent entries and shows detected placeholders to help you complete translations.

### Database
A sample Hibernate properties file is included:
- Location (resources): `src/main/resources/database/hibernate.properties`
- Contains commented guidance for configuring storage engines (H2, MySQL, PostgreSQL, Oracle, SQL Server) and schema modes.

If you plan to back JExEconomy with a database, review and adapt the Hibernate properties to your environment, then ensure your chosen JDBC driver is present (if not shaded) and any plugin-side configuration aligns with your server setup.


## Commands
JExEconomy declares commands via YAML under `src/main/resources/commands`.

### Player Commands

#### /pcurrency
- Aliases: `/currency`, `/balance`, `/bal`, `/money`
- Permissions: `currency.command` (self), `currency.command.other` (view others)
- Usage examples:
  - `/currency` — view all your balances
  - `/currency <currency>` — view your balance for the specified currency
  - `/currency <player>` — view all balances of another player (requires permission)
  - `/currency <currency> <player>` — view a player's balance for a specific currency (requires permission)

#### /pcurrencies
- Alias: `/currencies`
- Base permission: `currencies.command`
- **GUI-Only Interface**: Opens comprehensive currency management GUI
- **Usage**: Simply execute `/currencies` to access all currency management features
- **Features Available in GUI**:
  - **Create Currency**: Interactive anvil-based creation wizard with validation
  - **Edit Currency**: Select and modify existing currency properties
  - **Delete Currency**: Safe deletion with impact assessment (admin-only)
  - **View Currencies**: Browse all currencies with pagination and leaderboards
  - **Administrative Functions**: Reset balances, detailed currency information
- **Permission-Based Access**: Different GUI elements require appropriate permissions
  - `currencies.command.create` — access currency creation interface
  - `currencies.command.delete` — access currency deletion interface (admin)
  - `currencies.command.update` — access currency editing interface
  - `currencies.command.overview` — access currency overview and details

#### /pcurrencylog
- Aliases: `/plog`, `/currencylog`, `/economylog`, `/ecolog`
- Permission: `pcurrencylog.command`
- Admin export requires: `jexeconomy.admin.export`
- Subcommands:
  - `view [page]` — paginated transaction logs
  - `filter` — open interactive filter menu
  - `filter <player|currency|type|level|operation> <value>` — apply specific filter
  - `clear` — clear all active filters
  - `stats` — show log statistics and analytics
  - `export` — export logs to file (see Runtime Files)
  - `details <log_id>` — view details for a specific log

### Console Commands

#### /cdeposit
- Usage: `cdeposit <player_name> <currency_name> <amount>`
- Description: Deposit an amount of a currency to a player.
- Console-only (error is shown if run as a player).

#### /cwithdraw
- Usage: `cwithdraw <player_name> <currency_name> <amount>`
- Description: Withdraw an amount of a currency from a player.
- Console-only (error is shown if run as a player).

### Utility Commands

#### /r18n
- Aliases: `/i18n`, `/language`
- Permissions: `r18n.reload` (reload), `r18n.missing` (missing keys)
- Subcommands:
  - `reload` — reload translation files
  - `missing [<locale> <page>]` — open the locale selection or inspect missing keys for a given locale with pagination


## Permissions
- Player currency permissions
  - `currency.command` — use `/currency` for own balances
  - `currency.command.other` — view other players' balances
- Currency management permissions
  - `currencies.command` — access `/currencies`
  - `currencies.command.create` — create currency
  - `currencies.command.delete` — delete currency
  - `currencies.command.update` — edit/update currency
  - `currencies.command.overview` — view overview
- Currency log permissions
  - `pcurrencylog.command` — access `/pcurrencylog`
  - `jexeconomy.admin.export` — export currency logs
- Administrative (used by GUIs and actions)
  - `jexeconomy.admin.reset` — reset balances for a currency
  - `jexeconomy.admin.delete` — delete a currency
  - `jexeconomy.admin.*` — all administrative permissions (by convention)
- Translation administration
  - `r18n.reload` — use `/r18n reload`
  - `r18n.missing` — use `/r18n missing`


## Developer API
JExEconomy registers a public service for integrations.

- Service class: `de.jexcellence.currency.adapter.CurrencyAdapter`
- Registered with Bukkit’s ServicesManager during plugin load
- Also accessible from the JExEconomy plugin instance
- Asynchronous by design (returns `CompletableFuture` for most methods)

Access the adapter:
```java
// Using ServicesManager
CurrencyAdapter adapter = Bukkit.getServicesManager().load(CurrencyAdapter.class);

// Or via the plugin instance
JExEconomy plugin = JavaPlugin.getPlugin(JExEconomy.class);
CurrencyAdapter adapter2 = plugin.getCurrencyAdapter();
```

Common operations (examples):
```java
OfflinePlayer player = ...;
Currency currency = ...; // obtain from repository/cache in your integration

// Query balance (async)
adapter.getBalance(player, currency).thenAccept(balance -> {
    // use balance
});

// Deposit/withdraw (async)
adapter.deposit(player, currency, 100.0).thenAccept(response -> {
    if (response.isSuccess()) {
        // success
    }
});

adapter.withdraw(player, currency, 25.0).thenAccept(response -> { /* ... */ });

// Check currency existence
adapter.hasGivenCurrency("coins").thenAccept(exists -> { /* ... */ });

// Create/delete currency (admin logic)
// adapter.createCurrency(newCurrency, initiatorPlayer);
// adapter.deleteCurrency("coins", initiatorPlayer);
```

Events fired (listen via Bukkit):
- `BalanceChangeEvent` and `BalanceChangedEvent`
- `CurrencyCreateEvent` and `CurrencyCreatedEvent`
- `CurrencyDeleteEvent` and `CurrencyDeletedEvent`

Notes:
- Perform heavy tasks on async threads; UI updates must be scheduled back on the main thread
- The adapter writes transaction/management logs (see CurrencyLog entities)


## Building from Source
Prerequisites:
- JDK 21+
- Gradle wrapper included

Build targets:
- Free build: `./gradlew shadowFree`
- Premium build: `./gradlew shadowPremium`
- Full build: `./gradlew build` (depends on both)

Artifacts (by default):
- `build/libs/JExEconomy-Free-<version>.jar`
- `build/libs/JExEconomy-Premium-<version>.jar`

Notes:
- The project uses multiple source sets (`main`, `free`, `premium`).
- Paper API dependency is `1.21.7-R0.1-SNAPSHOT` for compilation.


## Runtime Files & Folders
- Logs export location: `plugins/JExEconomy/logs/`
  - File name pattern: `currency-logs-export_<player>_<yyyy-MM-dd_HH-mm-ss>.txt`
- Translations: generated and/or read from your plugin data folder; sources in `src/main/resources/translations/`
- Database configuration template: `src/main/resources/database/hibernate.properties`


## Status and Roadmap
- **Currency Management**: ✅ **Complete** - Full CRUD operations via intuitive GUI interface
  - ✅ Currency Creation with anvil-based wizard and validation
  - ✅ Currency Editing with real-time property modification
  - ✅ Currency Deletion with safety measures and impact assessment
  - ✅ Currency Overview with pagination and detailed information
  - ✅ Advanced Leaderboards with ranking visualization
  - ✅ Administrative Functions (balance reset, detailed views)
- **Command Interface**: ✅ **Complete** - Streamlined GUI-only `/currencies` command
- **Safety Features**: ✅ **Complete** - Confirmation dialogs, impact assessment, permission-based access
- Vault migration tool: Work in progress (WIP). Compatibility/provider replacement scaffolding exists.
- InventoryFramework: dependency already injected; no extra setup required.

### Recent Updates
- **v2.0**: Complete GUI overhaul with comprehensive currency management
- **Enhanced UX**: Simplified command interface - just `/currencies` opens everything
- **Safety First**: Multiple confirmation dialogs and impact assessment for destructive operations
- **Admin Tools**: Advanced administrative functions with proper permission controls
- **Visual Polish**: Ranking system with gold/silver/bronze hierarchy and formatted displays


## Live Example (RaindropPlugins)
A live, actively developed example of JExEconomy in the broader RaindropPlugins ecosystem:

- RaindropPlugins: https://github.com/Antimatter-Zone/RaindropPlugins_

This repository showcases integration patterns with JExEconomy, command usage, translations, and UI flows. Use it as a reference implementation when building your own integrations.

## Credits
- Author: JExcellence — https://jexcellence.de
- Dependencies: InventoryFramework (already injected); Vault (optional/softdepend)

If you find issues or want to propose improvements, please open an issue or submit a PR.