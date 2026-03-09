# RDS Showcase

RDS (RaindropShops) is a feature-rich Minecraft shop plugin for Paper, Spigot, and Folia servers that offers scalable player shops, server-admin shops, smart stock behavior, and economy-aware taxation.

If you are looking for a Minecraft shop plugin, player shop plugin, admin shop system, or tax-integrated server economy plugin, RDS is built for that exact stack.

## Why RDS

- Player-owned shops with placement rules, ownership protections, and trusted access controls.
- Server-owned admin shops for controlled economy sinks/sources and curated item catalogs.
- Rotating inventory options for dynamic storefronts:
  - Item availability modes: `ALWAYS`, `ROTATE`, `NEVER`
  - Configurable rotation windows (minutes) per item
  - Admin restock modes: `GRADUAL` or `FULL_AT_TIME`
- Scheduled tax engine with configurable start time, duration, growth, and currency handling.
- Bankruptcy/debt tracking for unpaid taxes, including capped or unlimited debt modes.
- Protection tax support with town-bank-first charging and optional fallback to player balance.
- Town tax transfer workflow for mayors via in-game tax UI.
- Protection integration support for `RDT`, `Towny`, and `HuskTowns`.
- GUI-first UX across search, store, admin, tax, ledger, stock, and configuration flows.

## Advanced Commerce Features

- In-game GUI editor with anvil-driven inputs for pricing, currency type, stock limits, rotation windows, and admin purchase actions.
- Command items with multi-command support per listing; choose server or player execution and configure delayed execution for timed rewards.
- Custom item workflows: sell standard materials or advanced ItemStack payloads with metadata/NBT, including custom items, custom enchantments, and custom spawner items.
- Limited-stock controls per item with configurable restock intervals and server-managed replenishment behavior.
- Per-item currencies so a single shop can mix default and custom economy currency types.
- Level-based economy support through experience/level requirements in the shop store flow for progression-driven servers.
- Placeholder-ready UI text and command pipelines for dynamic names, lore, and transaction messaging.
- Database flexibility with embedded `H2` by default, plus `MySQL`, `MariaDB`, `PostgreSQL`, and other Hibernate-compatible backends for multi-server sync.
- Built-in discovery tools with `/rs search` and material-based lookup to help players find items fast.

## International Language Support

- RDS ships with `24` included languages (including English).
- Language delivery is player-specific and follows each player's Minecraft client language setting.
- Server owners do not need to define a single server-wide language to support multilingual players.
- To add or customize translations, place language `.yml` files in `plugins/RDS/translations/`.
- Servers can support everyone's preferred language automatically with no additional setup burden.

## Free vs Premium Comparison

Both editions share the same core runtime, GUI framework, tax engine, integrations, and command surface. The differences are edition limits and config control.

| Capability | Free Edition | Premium Edition |
|---|---|---|
| Player shop limit | `min(config.max_shops, 3)` (`3` when config is unlimited) | Uses `config.max_shops` directly |
| Admin shop limit | `2` | Unlimited (`-1`) |
| Plugin config editing in UI | Disabled | Enabled |
| Runtime premium flag | `isPremium() = false` | `isPremium() = true` |

## Setup

### Requirements

- Java `21+`
- Minecraft server API `1.19+` (Bukkit, Spigot, Paper, Folia)
- Optional economy plugins: `Vault`, `JExEconomy`, `TheNewEconomy`
- Optional integrations: `PlaceholderAPI`, `LuckPerms`, `CMI`
- Optional protection integration: `RDT`, `Towny`, `HuskTowns`

RDS jars shade required runtime components, so no separate `RPlatform` plugin install is required.

### Installation

1. Build or obtain the edition jar you want to run.
   - Free jar: `RDS/rds-free/build/libs/RDS-1.0.0-Free.jar`
   - Premium jar: `RDS/rds-premium/build/libs/RDS-1.0.0-Premium.jar`
2. Place exactly one edition jar in your server `plugins/` folder.
3. Start or restart the server.
4. Configure files under `plugins/RDS/` (especially taxes, protection, admin shops, and store requirements).

## Commands

Primary command: `/prs`  
Alias: `/rs`  
All actions are player command routes.

| Command | Description | Permission |
|---|---|---|
| `/rs` | Default info route with owned shops, currencies, taxes, protection, and schedule summary | `raindropshops.command.info` (configured) |
| `/rs info` | Explicit info summary route | `raindropshops.command.info` (configured) |
| `/rs admin` | Opens admin view | `raindropshops.command.admin` |
| `/rs bar` | Toggles boss bar | `raindropshops.command.bar` |
| `/rs give <player> <amount>` | Grants shop blocks and increments target shop count | `raindropshops.command.give` |
| `/rs scoreboard <ledger|stock>` | Enables/switches/disables sidebar scoreboard mode | `raindropshops.command.scoreboard` |
| `/rs search` | Opens shop search view | `raindropshops.command.search` (configured) |
| `/rs store` | Opens shop store view | `raindropshops.command.store` (configured) |
| `/rs taxes` | Opens town tax transfer view | `raindropshops.command.taxes` |

## Permissions

| Node | Purpose |
|---|---|
| `raindropshops.command` | Root command node (used for first-argument tab completion) |
| `raindropshops.command.admin` | Admin command and admin-oriented view access |
| `raindropshops.command.bar` | Boss bar toggle access |
| `raindropshops.command.info` | Info summary route access |
| `raindropshops.command.give` | Shop block grant command access |
| `raindropshops.command.scoreboard` | Scoreboard command access |
| `raindropshops.command.search` | Search route access |
| `raindropshops.command.store` | Store route access |
| `raindropshops.command.taxes` | Tax view access |
| `raindropshops.admin.shops` | Permission used for toggling player/admin shop mode in overview tools |
| `raindropshops.admin.bypass.town` | Allows admins to place ShopBlocks outside their own town while `protection.only_player_shops` is enabled |

## PlaceholderAPI Placeholders

| Placeholder | Description |
|---|---|
| `%rds_shops_owned%` | Number of shops owned by the player (non-admin shops). |
| `%rds_shops_admin%` | Number of admin shops in the database. |
| `%rds_shops_items%` | Number of items the player is selling across owned shops. |
| `%rds_shops_tax%` | Comma-separated list of tax debt by currency for the player's owned shops. |

## SEO Summary

RDS is optimized for these high-intent searches:

- Minecraft shop plugin
- Minecraft player shops plugin
- Minecraft admin shops plugin
- Minecraft shop plugin with GUI editor
- Paper shop plugin
- Spigot shop plugin
- Folia compatible shop plugin
- Minecraft custom items shop plugin
- Minecraft custom enchantments shop plugin
- Minecraft limited stock shop plugin
- Minecraft per-item currency shop plugin
- Minecraft shop search GUI plugin
- Minecraft rotating shop inventory
- Minecraft shop taxes plugin
- Towny shop tax integration

RDS delivers a modern shop economy stack with configurable stock behavior, tax automation, and production-grade protection integration.
