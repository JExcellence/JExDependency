# RDR (RaindropReserve)

RDR (RaindropReserve) is a high-performance Minecraft storage plugin for Paper, Spigot, and Folia servers offering modern player vaults, player bound hotkey access controls, and economy-aware progression.

If you are searching for a Minecraft storage plugin, player vault plugin, shared chest plugin, or Paper/Folia storage system, RDR is built for that exact use case.

## Why RDR

- GUI-first storage workflow for fast player onboarding and intuitive daily use.
- Hotkey access with `/rr <hotkey>` for instant storage opens.
- Trusted sharing model with role-based access:
  - `ASSOCIATE` can deposit items.
  - `TRUSTED` can deposit and withdraw items.
- Lease-safe storage sessions to prevent conflicting edits across server instances.
- Global blacklist controls to block prohibited items from entering storages.
- Economy-integrated open taxes and recurring filled-storage taxes.
- Frozen-storage debt flow with repayment handling when taxes are unpaid.
- Optional town-bank tax ledger transfer support for town-based economies.
- Optional sidebar scoreboard players can toggle with `/rr scoreboard`.
- Admin integration views for currencies, skills/jobs requirement plugins, and PlaceholderAPI tooling.

## International Language Support

- RDR ships with `24` included languages (including English).
- Language messages are player-specific and driven by the Minecraft client language setting.
- Server owners do not need to configure a single language to support everyone.
- To add or customize translations, place language `.yml` files in `plugins/RDR/translations/`.
- Players automatically see supported content in their preferred language when available.

## Free vs Premium

Both editions share the same core runtime, commands, tax system, GUI framework, and integrations. The primary differences are storage limits and settings control.

| Capability | Free Edition | Premium Edition |
|---|---|---|
| Maximum storages per player | `min(config.max_storages, 3)` | Uses `config.max_storages` directly |
| First-join starting storages | Clamped to free effective max | Clamped to configured max |
| Storage settings editing | Disabled | Enabled |
| Hotkey/trusted settings UI | Locked for owners | Fully available for owners |
| Runtime premium flag | `isPremium() = false` | `isPremium() = true` |

## Setup

### Requirements

- Java `21+`
- Minecraft server API `1.19+` (Paper, Spigot, Bukkit, Folia)
- Optional economy plugins: `Vault`, `JExEconomy`, `TheNewEconomy`
- Optional protection plugins for restricted/taxed storage rules: `RDT`, `Towny`, `HuskTowns`

RDR jars shade required runtime components, so no separate `RPlatform` install is required.

### Installation

1. Build or obtain the edition jar you want to run.
   - Free jar: `RDR/rdr-free/build/libs/RDR-5.0.0-Free.jar`
   - Premium jar: `RDR/rdr-premium/build/libs/RDR-5.0.0-Premium.jar`
2. Place exactly one edition jar in your server `plugins/` folder.
3. Start or restart the server.
4. Configure `plugins/RDR/config/config.yml` for limits, taxes, blacklists, and requirement tiers.

## Commands

Primary command: `/prr`  
Alias: `/rr`  
All command actions are player-only.

| Command | Description | Permission |
|---|---|---|
| `/rr` | Default route (`info`) then opens storage overview when allowed | `raindroprdr.command.info` + `raindroprdr.command.storage` |
| `/rr info` | Same as default route | `raindroprdr.command.info` + `raindroprdr.command.storage` |
| `/rr storage` | Opens storage overview | `raindroprdr.command.storage` |
| `/rr admin` | Opens admin interface | `raindroprdr.command.admin` |
| `/rr scoreboard` | Toggles sidebar scoreboard | `raindroprdr.command.scoreboard` |
| `/rr taxes` | Opens storage tax view | `raindroprdr.command.storage` |
| `/rr <hotkey>` | Opens storage bound to numeric hotkey | `raindroprdr.command.storage` |

## Permissions

| Node | Purpose |
|---|---|
| `raindroprdr.command` | Root node used for first-argument tab-completion access |
| `raindroprdr.command.admin` | Admin view and admin command access |
| `raindroprdr.command.info` | Info/default route access |
| `raindroprdr.command.scoreboard` | Scoreboard toggle access |
| `raindroprdr.command.storage` | Storage open flows, taxes view, and hotkeys |

Notes:

- Several admin views also accept operator status (`op`) as admin access.
- Requirement-based purchases can include dynamic permission checks from configured requirement tiers.

## PlaceholderAPI Placeholders

| Placeholder | Description |
|---|---|
| `%rdr_storages_max%` | Maximum storages allowed. |
| `%rdr_storages_players%` | Number of storages unlocked by the player. |
| `%rdr_storages_items%` | Number of items currently stored across the player's storages. |
| `%rdr_storages_tax%` | Comma-separated list of tax debt by currency. |

## SEO Summary

RDR targets these high-intent server-owner searches:

- Minecraft storage plugin
- Minecraft player vault plugin
- Paper storage plugin
- Spigot storage plugin
- Folia compatible storage plugin
- Shared storage plugin Minecraft
- Storage tax plugin Minecraft
- GUI storage plugin Minecraft

RDR combines secure storage access, economy progression, and admin-grade integration controls in one deployable plugin.
