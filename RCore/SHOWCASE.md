# RCore Showcase

RCore is the **free core dependency plugin** for the [RaindropCentral](<https://raindropcentral.com>) stack.  
It powers shared systems for **RDQ (RaindropQuests)**, **RDR (RaindropReserve)**, and **RDS (RaindropShops)**.

If you are searching for a **Minecraft core dependency plugin**, **shared player data plugin**, **cross-plugin statistics backend**, or **Paper/Spigot plugin API foundation**, RCore is built for that exact role.

## Why RCore

RCore is the backbone layer that keeps the Raindrop ecosystem consistent, fast, and scalable.

- Free unified core plugin.
- Shared player and statistics persistence for multiple Raindrop modules.
- Async-first runtime with virtual-thread execution for non-blocking operations.
- Database-backed architecture for production servers and growing networks.
- Built-in integration bridge to [RaindropCentral](<https://raindropcentral.com>) services.
- Designed for Paper, Spigot, and Folia environments.

## International Language Support

- RCore ships with `24` included languages (including English).
- Language rendering is player-specific and follows each player's Minecraft client language setting.
- Server owners do not need to set or force a server-wide language to support multilingual communities.
- To add or customize translations, place language `.yml` files in `plugins/RCore/translations/`.
- This enables per-player localization automatically without extra setup complexity.

## Built for RDQ, RDR, and RDS

RCore is not just optional glue. It is a central runtime foundation for the major [RaindropCentral](<https://raindropcentral.com>) plugins.

| Module | RCore Relationship |
|---|---|
| `RDQ` | Declares RCore as a required server dependency in Paper descriptors |
| `RDR` | Declares RCore as a plugin soft dependency |
| `RDS` | Declares RCore as a plugin soft dependency |

For server owners, this means one central core service can support multiple premium gameplay modules without duplicated backend logic.

## Core Capability Stack

### Shared Data Layer

- Persistent player profiles and player statistics repositories.
- Typed statistic entities (number/string/date/boolean models).
- Repository-based architecture for clean, maintainable data access.
- API surface designed for async usage in dependent plugins.

### Performance and Stability

- Virtual-thread executor strategy for async workloads.
- Safe lifecycle boot flow with controlled startup sequencing.
- Graceful shutdown behavior for service cleanup and queue flushing.
- Metrics support with bStats and [RaindropCentral](<https://raindropcentral.com>).

### RaindropCentral Integration

- Backend URL configuration and environment-aware backend selection.
- Server connect/disconnect workflow with API key validation.
- Wakeup ping, heartbeat scheduling, and shutdown notifications.
- Delivery pipeline for statistics with batching, retries, and rate limiting.
- Security controls for payload signing and sensitive-data encryption.

### Database Flexibility

RCore supports embedded and external database backends:

- H2 (default embedded mode)
- MySQL
- MariaDB
- PostgreSQL
- Oracle
- Microsoft SQL Server
- SQLite
- HSQLDB

### Ecosystem Awareness

RCore can detect and work alongside supported server plugins and utilities, helping Raindrop modules adjust cleanly in real server environments.

## Setup

### Requirements

- Java `21+`
- Bukkit/Spigot/Paper/Folia server (API `1.19+`)
- Recommended disk headroom for dependencies, database, and queue persistence

### Installation

1. Build or obtain the RCore jar.
2. Place `RCore-<version>.jar` in your server `plugins/` directory.
3. Start the server and let RCore generate its files.
4. Configure database and central integration files in `plugins/RCore/`.
5. Install RDQ, RDR, and/or RDS after RCore is present.

### Key Configuration Files

- `plugins/RCore/database/hibernate.properties`
- `plugins/RCore/rcentral/rcentral.yml`
- `plugins/RCore/statistics-delivery-config.yml`

## Commands

RCore is mostly infrastructure, but it includes connection commands for RaindropCentral integration:

| Command | Description | Permission |
|---|---|---|
| `/rcconnect <api-key>` | Connect this server to RaindropCentral | `rcore.central.connect` |
| `/rcdisconnect` | Disconnect this server from RaindropCentral | `rcore.central.disconnect` |

Aliases:

- `/rclink` for `/rcconnect`
- `/rcunlink` for `/rcdisconnect`

## Permissions

| Node | Purpose |
|---|---|
| `rcore.central.connect` | Allows connecting server to RaindropCentral |
| `rcore.central.disconnect` | Allows disconnecting server from RaindropCentral |

## SEO Summary

RCore is optimized for high-intent search terms such as:

- Minecraft core dependency plugin
- Minecraft shared plugin API
- Minecraft player data backend plugin
- Minecraft statistics persistence plugin
- Paper plugin dependency framework
- Spigot async data plugin
- Folia compatible core plugin
- Minecraft plugin stack foundation
- RDQ dependency plugin
- RDR dependency plugin
- RDS dependency plugin

## Conclusion

RCore is the free foundation layer that lets your Raindrop modules scale cleanly.  
Install once, power RDQ/RDR/RDS together, and run a consistent, data-driven server stack from one core plugin.
