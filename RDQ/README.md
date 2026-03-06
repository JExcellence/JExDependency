# RDQ - RaindropQuests

A modern Minecraft plugin providing rank progression, bounty hunting, and perk systems for Bukkit/Spigot/Paper servers (1.13 - 1.21.10).

## Architecture

```
RDQ/
├── rdq-common/     # Shared library - domain models, services, repositories
├── rdq-free/       # Free edition entry point
└── rdq-premium/    # Premium edition entry point
```

### Module Overview

- **rdq-common**: Contains all shared logic including domain models (records), service interfaces (sealed), repositories, views, and commands
- **rdq-free**: Plugin entry point for free edition with limited features
- **rdq-premium**: Plugin entry point for premium edition with full features

### Package Structure

```
com.raindropcentral.rdq
├── api/            # Public service interfaces (RankService, BountyService, PerkService)
├── rank/           # Rank progression system
├── bounty/         # Bounty hunting system
├── perk/           # Perk/ability system
├── player/         # Player data management
├── command/        # Command handlers
└── shared/         # Cross-cutting concerns (caching, async, translations, errors)
```

### Key Design Patterns

- **Records** for all DTOs and value objects
- **Sealed interfaces** for type-safe service hierarchies and error handling
- **Pattern matching** with switch expressions for clean control flow
- **CompletableFuture** for all async operations
- **Virtual threads** (Java 21+) with fallback to thread pools

## Features

### Rank System
- Multiple rank trees (warrior, cleric, mage, rogue, merchant, ranger)
- Configurable requirements (statistics, permissions, currency, items)
- LuckPerms integration for permission groups
- GUI-based progression views

### Bounty System
- Player-placed bounties with configurable limits
- Multiple distribution modes (instant, chest, drop, virtual)
- Hunter statistics and leaderboards
- Vault economy integration

### Perk System
- Toggleable and event-based perks
- 15 built-in perk types (speed, strength, flight, etc.)
- Cooldown and duration management
- Requirement-based unlocking

## Setup

### Requirements

- Java 21+
- Minecraft Server 1.13 - 1.21.10 (Bukkit/Spigot/Paper/Folia)
- RPlatform is shaded into RDQ jars (no separate server-side installation required)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) (optional, for economy)
- [LuckPerms](https://luckperms.net/) (optional, for permissions)

### Installation

1. Build the plugin:
   ```bash
   ./gradlew :RDQ:buildAll
   ```

2. Copy the appropriate JAR to your server's `plugins/` folder:
   - Free: `rdq-free/build/libs/RDQ-Free-6.0.0-all.jar`
   - Premium: `rdq-premium/build/libs/RDQ-Premium-6.0.0-all.jar`

3. No separate RPlatform plugin install is required (already shaded in RDQ jars)

4. Start the server - default configurations will be generated

5. Configure the plugin in `plugins/RDQ/`

### Building from Source

```bash
# Build all modules
./gradlew :RDQ:buildAll

# Run tests
./gradlew :RDQ:testAll

# Publish to local Maven
./gradlew :RDQ:publishLocal
```

## Configuration

Configuration files are located in `plugins/RDQ/`:


### Database (`database/hibernate.properties`)

Supports H2 (default), MySQL, MariaDB, and PostgreSQL:

```properties
# Select database type: H2, MYSQL, MARIADB, POSTGRESQL
database.type=H2

# MySQL example
mysql.url=jdbc:mysql://localhost:3306/rdq
mysql.username=rdq_user
mysql.password=your_password

# Connection pool settings
hikari.maximumPoolSize=10
hikari.minimumIdle=2
```

### Bounty System (`bounty/bounty.yml`)

```yaml
enabled: true
amounts:
  minimum: 100
  maximum: 1000000
expiration:
  enabled: true
  hours: 168
distribution: INSTANT  # INSTANT, CHEST, DROP, VIRTUAL
announcements:
  onCreate:
    enabled: true
    scope: SERVER  # SERVER, NEARBY, TARGET_ONLY
```

### Rank System (`rank/rank-system.yml`)

```yaml
enabled: true
progression:
  linearProgression: true
  allowSkipping: false
trees:
  maxActiveTrees: 1  # Premium: unlimited
  crossTreeSwitching: false
luckperms:
  enabled: true
  assignGroups: true
```

### Perks (`perks/*.yml`)

```yaml
id: speed
type: TOGGLEABLE
cooldownSeconds: 300
durationSeconds: 60
effect:
  type: POTION_EFFECT
  potionType: SPEED
  amplifier: 1
requirements:
  - type: RANK
    value: warrior_3
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rdq` | Main plugin command | `rdq.use` |
| `/rank` | View rank progression | `rdq.rank.use` |
| `/rank view [tree]` | View specific rank tree | `rdq.rank.view` |
| `/bounty` | Bounty system commands | `rdq.bounty.use` |
| `/bounty create <player> <amount>` | Place a bounty | `rdq.bounty.create` |
| `/bounty list` | View active bounties | `rdq.bounty.list` |
| `/perk` | Perk system commands | `rdq.perk.use` |
| `/perk list` | View available perks | `rdq.perk.list` |
| `/perk activate <perk>` | Activate a perk | `rdq.perk.activate` |

## Developer Guidelines

### Code Style

- Use `var` for local variables with obvious types
- Prefer records over classes for data objects
- Use sealed interfaces for type hierarchies
- Pattern matching with switch expressions for control flow
- No inline comments - code should be self-documenting
- All service methods return `CompletableFuture<T>`

### Adding a New Perk Type

1. Add effect type to `PerkEffect` sealed interface
2. Implement effect application in `PerkRuntime.applyEffect()`
3. Create YAML configuration in `perks/`
4. Add translations for display name and description

### Adding a New Rank Tree

1. Create YAML file in `rank/paths/`
2. Define ranks with requirements
3. Add translations for rank names
4. Configure LuckPerms groups (optional)

### Testing

```bash
# Run all tests
./gradlew :RDQ:rdq-common:test

# Run specific test class
./gradlew :RDQ:rdq-common:test --tests "BountyServiceTest"
```

## Edition Differences

| Feature | Free | Premium |
|---------|------|---------|
| Rank Trees | 1 active | Unlimited |
| Cross-tree Switching | ❌ | ✅ |
| Active Perks | 1 | Unlimited |
| Premium Perk Types | ❌ | ✅ |
| Distribution Modes | INSTANT only | All modes |

## Dependencies

- [RPlatform](../RPlatform) - Internal platform layer (shaded into RDQ runtime jars)
- [JExTranslate](../JExTranslate) - Internationalization (18+ locales)
- [JExCommand](../JExCommand) - YAML-based command registration
- [inventory-framework](https://github.com/DevNatan/inventory-framework) - GUI system
- [JExHibernate](https://github.com/JExcellence/JEHibernate) - Database operations
- [Caffeine](https://github.com/ben-manes/caffeine) - High-performance caching

## License

Proprietary - RaindropCentral
