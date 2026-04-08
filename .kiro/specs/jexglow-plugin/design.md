# Design Document

## Overview

JExGlow is a lightweight Paper 1.21.10 plugin that provides persistent glowing effects for players. The plugin follows the established RCore/JExHome architectural patterns, utilizing RPlatform for infrastructure, JExCommand for command handling, and JExTranslate for localization. The design emphasizes simplicity, performance, and consistency with existing JExcellence plugins.

The core functionality revolves around managing player glow states through a database-backed service layer, applying the Minecraft glowing effect through Bukkit APIs, and exposing glow status through PlaceholderAPI integration.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        JExGlow Plugin                        │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Commands   │  │   Listeners  │  │ Placeholders │      │
│  │  (GlowCmd)   │  │ (GlowEvents) │  │ (GlowPAPI)   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │  GlowService    │                        │
│                   │  (IGlowService) │                        │
│                   └────────┬────────┘                        │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │ GlowRepository  │                        │
│                   └────────┬────────┘                        │
│                            │                                 │
├────────────────────────────┼─────────────────────────────────┤
│                   ┌────────▼────────┐                        │
│                   │   RPlatform     │                        │
│                   │ (EntityManager, │                        │
│                   │  Translation,   │                        │
│                   │   Scheduler)    │                        │
│                   └─────────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

Following the JExHome pattern, the plugin will have a simple structure:

```
JExGlow/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── java/
│       │   └── de/jexcellence/glow/
│       │       ├── JExGlow.java (Main plugin class)
│       │       ├── command/
│       │       │   ├── GlowCommand.java
│       │       │   └── GlowCommandSection.java
│       │       ├── listener/
│       │       │   └── GlowEventListener.java
│       │       ├── service/
│       │       │   ├── IGlowService.java
│       │       │   └── GlowService.java
│       │       ├── database/
│       │       │   ├── entity/
│       │       │   │   └── PlayerGlow.java
│       │       │   └── repository/
│       │       │       └── GlowRepository.java
│       │       ├── placeholder/
│       │       │   └── GlowPlaceholder.java
│       │       └── factory/
│       │           └── GlowFactory.java
│       └── resources/
│           ├── plugin.yml
│           ├── database/
│           │   └── hibernate.properties
│           ├── commands/
│           │   └── glow.yml
│           └── translations/
│               ├── en_US.yml
│               └── de_DE.yml
```

## Components and Interfaces

### 1. Main Plugin Class (JExGlow)

**Responsibility:** Plugin lifecycle management, dependency initialization, and component wiring.

**Key Methods:**
- `onLoad()`: Initialize JEDependency with remapping
- `onEnable()`: Initialize RPlatform, repositories, services, commands, and listeners
- `onDisable()`: Cleanup resources and shutdown executors

**Dependencies:**
- RPlatform (database, translation, scheduler)
- RepositoryManager (JEHibernate)
- CommandFactory (JExCommand)
- GlowService

**Pattern:** Follows JExHome initialization pattern with CompletableFuture-based async initialization.

### 2. Database Layer

#### PlayerGlow Entity

**Responsibility:** JPA entity representing player glow state.

**Fields:**
- `id` (Long): Primary key, auto-generated
- `playerUuid` (UUID): Player's unique identifier (indexed)
- `glowEnabled` (boolean): Whether glow is currently enabled
- `enabledAt` (LocalDateTime): When glow was last enabled
- `enabledBy` (UUID): Admin who enabled the glow (nullable)
- `updatedAt` (LocalDateTime): Last update timestamp

**Annotations:**
- `@Entity`
- `@Table(name = "jexglow_player_glow")`
- `@Index` on playerUuid for fast lookups

#### GlowRepository

**Responsibility:** Data access layer for PlayerGlow entities.

**Extends:** `CachedRepository<PlayerGlow, Long, UUID>`

**Key Methods:**
- `findByPlayerUuid(UUID)`: CompletableFuture<Optional<PlayerGlow>>
- `saveGlowState(PlayerGlow)`: CompletableFuture<PlayerGlow>
- `deleteByPlayerUuid(UUID)`: CompletableFuture<Boolean>
- `findAllEnabled()`: CompletableFuture<List<PlayerGlow>>

**Caching Strategy:** Uses UUID as cache key for fast in-memory lookups.

### 3. Service Layer

#### IGlowService Interface

**Responsibility:** Define contract for glow operations.

**Key Methods:**
```java
CompletableFuture<Boolean> enableGlow(UUID playerId, UUID adminId);
CompletableFuture<Boolean> disableGlow(UUID playerId);
CompletableFuture<Boolean> isGlowEnabled(UUID playerId);
CompletableFuture<Void> applyGlowEffect(Player player);
CompletableFuture<Void> removeGlowEffect(Player player);
```

#### GlowService Implementation

**Responsibility:** Business logic for glow management.

**Key Operations:**
1. **Enable Glow:**
   - Create/update PlayerGlow entity with glowEnabled=true
   - Persist to database asynchronously
   - Apply visual glow effect if player is online
   - Return success/failure

2. **Disable Glow:**
   - Update PlayerGlow entity with glowEnabled=false
   - Persist to database asynchronously
   - Remove visual glow effect if player is online
   - Return success/failure

3. **Apply Effect:**
   - Query database for player's glow state
   - If enabled, call `player.setGlowing(true)` on main thread
   - Handle world/entity state edge cases

4. **Remove Effect:**
   - Call `player.setGlowing(false)` on main thread
   - No database operation needed

**Thread Safety:** All database operations run asynchronously; Bukkit API calls scheduled on main thread via RPlatform scheduler.

### 4. Command Layer

#### GlowCommand

**Responsibility:** Handle `/glow on <player>` and `/glow off <player>` commands.

**Constructor:** `GlowCommand(GlowCommandSection section, JExGlow plugin)`

**Execution Flow:**
1. Validate permission (`jexglow.admin`)
2. Parse arguments (on/off, player name)
3. Resolve target player (must be online)
4. Call appropriate service method
5. Send translated feedback message

**Error Handling:**
- Invalid arguments → usage message
- Player not found → error message
- Service failure → error message with logging

#### GlowCommandSection

**Responsibility:** Configuration section for command metadata.

**Fields:**
- `name`: "glow"
- `aliases`: []
- `description`: Translation key
- `usage`: "/glow <on|off> <player>"
- `permission`: "jexglow.admin"

**File:** `commands/glow.yml`

### 5. Event Listener

#### GlowEventListener

**Responsibility:** Reapply glow effects on player join and respawn.

**Events:**
1. **PlayerJoinEvent:**
   - Query glow state asynchronously
   - If enabled, schedule glow application on main thread (300ms delay)
   - Log any errors

2. **PlayerRespawnEvent:**
   - Query glow state asynchronously
   - If enabled, schedule glow application on main thread (100ms delay)
   - Log any errors

**Timing:** Delays ensure player entity is fully loaded before applying effect.

### 6. PlaceholderAPI Integration

#### GlowPlaceholder

**Responsibility:** Provide `%jexglow_status%` placeholder.

**Implementation:**
- Extends `PlaceholderExpansion`
- Identifier: "jexglow"
- Placeholder: "status"
- Returns: "true" or "false" based on database state

**Caching:** Leverages repository cache for fast lookups (50ms target).

**Registration:** Conditional on PlaceholderAPI presence, registered during plugin initialization.

### 7. Factory Pattern

#### GlowFactory

**Responsibility:** Centralized access to service and repository instances.

**Pattern:** Static singleton following HomeFactory pattern.

**Methods:**
```java
static void initialize(GlowService service, GlowRepository repository);
static GlowService getService();
static GlowRepository getRepository();
static void reset();
```

**Usage:** Provides global access point for commands, listeners, and placeholders.

## Data Models

### PlayerGlow Entity Schema

```sql
CREATE TABLE jexglow_player_glow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    glow_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    enabled_at TIMESTAMP NULL,
    enabled_by VARCHAR(36) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_uuid (player_uuid),
    UNIQUE KEY uk_player_uuid (player_uuid)
);
```

### Configuration Models

#### Command Configuration (glow.yml)

```yaml
commands:
  glow:
    name: "glow"
    aliases: []
    description: "glow.command.description"
    usage: "/glow <on|off> <player>"
    permissions:
      default: "jexglow.admin"
```

#### Translation Keys (en_US.yml)

```yaml
glow:
  command:
    description: "Manage player glow effects"
    enabled: "&aGlow effect enabled for {player}"
    disabled: "&cGlow effect disabled for {player}"
    player-not-found: "&cPlayer {player} not found"
    usage: "&cUsage: /glow <on|off> <player>"
  error:
    database: "&cDatabase error occurred. Please contact an administrator."
    general: "&cAn error occurred while processing your request."
```

## Error Handling

### Error Categories

1. **Command Errors:**
   - Invalid arguments → Usage message
   - Missing permissions → Permission denied message
   - Player not found → Player not found message

2. **Database Errors:**
   - Connection failure → Log error, disable plugin gracefully
   - Query timeout → Log warning, return user-friendly error
   - Constraint violation → Log error, return error message

3. **Bukkit API Errors:**
   - Player offline during effect application → Silently skip
   - World not loaded → Log warning, skip effect
   - Entity state invalid → Log warning, retry once

### Error Handling Strategy

- **Logging:** All errors logged via CentralLogger with appropriate levels
- **User Feedback:** Translated error messages for command failures
- **Graceful Degradation:** Plugin continues running even if individual operations fail
- **Async Safety:** All exceptions caught in CompletableFuture chains

### Exception Flow

```java
service.enableGlow(uuid, adminUuid)
    .thenAccept(success -> {
        if (success) {
            sendMessage(player, "glow.command.enabled");
        } else {
            sendMessage(player, "glow.error.general");
        }
    })
    .exceptionally(throwable -> {
        logger.log(Level.SEVERE, "Failed to enable glow", throwable);
        sendMessage(player, "glow.error.database");
        return null;
    });
```

## Testing Strategy

### Unit Testing

**Target:** Service layer business logic

**Framework:** JUnit 5

**Test Cases:**
1. Enable glow for player → verify database update
2. Disable glow for player → verify database update
3. Query glow state → verify correct boolean returned
4. Handle database failure → verify exception handling

**Mocking:** Mock GlowRepository using Mockito

### Integration Testing

**Target:** Database operations and repository layer

**Framework:** JUnit 5 + H2 in-memory database

**Test Cases:**
1. Create PlayerGlow entity → verify persistence
2. Query by UUID → verify retrieval
3. Update glow state → verify changes persisted
4. Delete entity → verify removal

### Manual Testing

**Target:** End-to-end functionality

**Test Scenarios:**
1. Enable glow via command → verify visual effect
2. Disable glow via command → verify effect removed
3. Player dies → verify glow persists after respawn
4. Player rejoins → verify glow persists after login
5. PlaceholderAPI → verify placeholder returns correct value

**Environment:** Local Paper 1.21.10 test server

## Performance Considerations

### Database Optimization

- **Indexing:** UUID column indexed for O(log n) lookups
- **Caching:** Repository cache reduces database queries
- **Batch Operations:** Not needed (single-player operations)
- **Connection Pooling:** Handled by Hibernate/JEHibernate

### Async Operations

- **Database Queries:** All queries run on async executor
- **Effect Application:** Scheduled on main thread via RPlatform scheduler
- **Event Handling:** Async queries with sync effect application

### Memory Footprint

- **Entity Size:** ~100 bytes per PlayerGlow entity
- **Cache Size:** Unbounded (acceptable for player count scale)
- **Expected Load:** 1 entity per player, ~1000 players = ~100KB

### Latency Targets

- **Command Execution:** < 50ms (async database write)
- **Placeholder Query:** < 50ms (cached lookup)
- **Effect Application:** < 100ms (main thread scheduling)
- **Join/Respawn Handling:** < 300ms (async query + sync application)

## Security Considerations

### Permission System

- **Admin Command:** `jexglow.admin` permission required
- **No Player Access:** Players cannot toggle their own glow
- **Permission Checks:** Validated before command execution

### Input Validation

- **Player Names:** Validated against online players
- **Command Arguments:** Strict parsing (on/off only)
- **UUID Validation:** Handled by Bukkit API

### Database Security

- **SQL Injection:** Prevented by JPA/Hibernate parameterized queries
- **Data Integrity:** UUID uniqueness enforced by database constraint
- **Access Control:** Database credentials in hibernate.properties (not version controlled)

## Dependencies

### Required Dependencies

1. **Paper API 1.21.10:** Core server API
2. **RPlatform:** Database, translation, scheduler infrastructure
3. **JExCommand:** Command framework
4. **JExTranslate:** Translation system
5. **JEHibernate:** ORM and repository layer
6. **Hibernate 6.x:** JPA implementation

### Optional Dependencies

1. **PlaceholderAPI:** Placeholder integration (soft dependency)

### Build Configuration

```kotlin
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)
    
    implementation(project(":RPlatform"))
    implementation(project(":JExCommand"))
    implementation(project(":JExTranslate"))
    
    compileOnly(libs.jehibernate)
    compileOnly(libs.hibernate.core)
}
```

## Deployment

### Build Process

1. Gradle build with shadow plugin
2. Relocate dependencies (if needed)
3. Generate plugin.yml with version
4. Output: `JExGlow-<version>.jar`

### Installation

1. Place JAR in `plugins/` folder
2. Ensure RCore is installed (provides RPlatform)
3. Start server
4. Plugin creates `plugins/JExGlow/` directory
5. Database tables auto-created by Hibernate

### Configuration Files

- `database/hibernate.properties`: Database connection settings
- `commands/glow.yml`: Command configuration
- `translations/en_US.yml`: English translations
- `translations/de_DE.yml`: German translations

### Migration Path

Not applicable (new plugin, no migration needed).

## Future Enhancements

### Potential Features (Out of Scope)

1. **Glow Colors:** Custom glow colors per player (requires client-side mods)
2. **Timed Glow:** Auto-disable after duration
3. **Glow Groups:** Apply glow to permission groups
4. **Glow Particles:** Additional particle effects
5. **GUI Management:** Admin GUI for bulk glow management

### Extensibility Points

- Service interface allows alternative implementations
- Factory pattern enables easy service swapping
- Repository pattern supports different storage backends
- Translation system supports additional languages
