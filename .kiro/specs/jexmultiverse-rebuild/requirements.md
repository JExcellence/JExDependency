# Requirements Document

## Introduction

JExMultiverse is a Minecraft plugin for managing multiple worlds with features like world creation, deletion, teleportation, spawn management, and custom world generators (void, plot). The plugin follows the RDQ/RPlatform architecture patterns with a common/free/premium module structure, using Hibernate for database persistence, I18n for translations, and InventoryFramework for GUIs.

## Glossary

- **JExMultiverse**: The main plugin system for multiverse world management
- **MVWorld**: A database entity representing a managed world with spawn location, type, and settings
- **WorldManager**: A utility class handling world creation, deletion, loading, and teleportation
- **IMultiverseService**: Interface defining multiverse operations for free/premium implementations
- **IMultiverseAdapter**: API interface for external plugins to interact with JExMultiverse
- **MVWorldType**: Enum defining world generation types (DEFAULT, VOID, PLOT)
- **TeleportFactory**: RPlatform utility for handling player teleportation with warmup support

## Requirements

### Requirement 1: Plugin Architecture

**User Story:** As a developer, I want JExMultiverse to follow the RDQ module structure, so that I can maintain consistency across the plugin ecosystem.

#### Acceptance Criteria

1. THE JExMultiverse SHALL use a three-module Gradle structure: jexmultiverse-common, jexmultiverse-free, jexmultiverse-premium
2. THE JExMultiverse common module SHALL contain all shared code including entities, repositories, views, commands, and services
3. THE JExMultiverse free module SHALL extend the common module with limited functionality
4. THE JExMultiverse premium module SHALL extend the common module with full functionality
5. THE JExMultiverse SHALL use RPlatform for platform services, logging, and teleportation

### Requirement 2: Database Layer

**User Story:** As a server administrator, I want world configurations to persist across server restarts, so that I don't lose my world settings.

#### Acceptance Criteria

1. THE JExMultiverse SHALL store MVWorld entities using Hibernate JPA with the table name "mv_world"
2. THE MVWorld entity SHALL include fields: identifier (unique), type, environment, spawnLocation, isGlobalizedSpawn, isPvPEnabled, enterPermission
3. THE MVWorldRepository SHALL extend AbstractCRUDRepository and implement Caffeine caching with 30-minute expiration
4. THE MVWorldRepository SHALL provide async methods: findByIdentifierAsync, findByGlobalSpawnAsync, findAllAsync
5. THE JExMultiverse SHALL use a LocationConverter for serializing Bukkit Location objects to database

### Requirement 3: World Management

**User Story:** As a server administrator, I want to create, delete, and manage worlds through commands, so that I can customize my server's world setup.

#### Acceptance Criteria

1. WHEN a player executes the create command with valid parameters, THE JExMultiverse SHALL create a new world with the specified type and environment
2. WHEN a player executes the delete command, THE JExMultiverse SHALL unload the world, remove database entry, and delete world files only if no players are present
3. THE WorldManager SHALL support three world types: DEFAULT (vanilla), VOID (empty), PLOT (grid-based plots)
4. THE JExMultiverse SHALL load all persisted worlds from the database on plugin enable
5. IF world creation fails, THEN THE JExMultiverse SHALL notify the player with the error message and clean up partial resources

### Requirement 4: Spawn Management

**User Story:** As a server administrator, I want to configure spawn points per world and a global spawn, so that players respawn at appropriate locations.

#### Acceptance Criteria

1. THE JExMultiverse SHALL allow setting a spawn location for each managed world
2. THE JExMultiverse SHALL support designating one world as the global spawn location
3. WHEN a player respawns, THE JExMultiverse SHALL teleport them to the global spawn if set, otherwise to the current world's spawn
4. WHEN a player executes the spawn command, THE JExMultiverse SHALL teleport them to the appropriate spawn location
5. THE JExMultiverse SHALL provide a GUI for editing world spawn settings

### Requirement 5: Commands

**User Story:** As a player, I want intuitive commands to interact with the multiverse system, so that I can navigate and manage worlds easily.

#### Acceptance Criteria

1. THE JExMultiverse SHALL provide a /multiverse command with subcommands: create, delete, edit, teleport, load, help
2. THE JExMultiverse SHALL provide a /spawn command for teleporting to spawn
3. THE JExMultiverse SHALL use the RPlatform CommandFactory for command registration with YAML configuration
4. THE JExMultiverse SHALL implement permission checks using IPermissionNode pattern for each command action
5. THE JExMultiverse SHALL provide tab completion for world names, environments, and world types

### Requirement 6: User Interface

**User Story:** As a server administrator, I want a GUI to edit world settings, so that I can configure worlds without memorizing commands.

#### Acceptance Criteria

1. THE JExMultiverse SHALL provide a MultiverseEditorView extending BaseView for editing world settings
2. THE MultiverseEditorView SHALL display options for: spawn location, global spawn toggle, PvP toggle
3. THE JExMultiverse SHALL use I18n for all GUI text with placeholder support
4. THE JExMultiverse SHALL register views using ViewFrame with standard click cancellation settings

### Requirement 7: World Generators

**User Story:** As a server administrator, I want custom world generators, so that I can create specialized worlds like void or plot worlds.

#### Acceptance Criteria

1. THE JExMultiverse SHALL provide a VoidChunkGenerator that generates empty worlds with THE_VOID biome
2. THE JExMultiverse SHALL provide a PlotChunkGenerator that generates grid-based plot worlds with configurable plot size, road width, and materials
3. THE JExMultiverse SHALL provide corresponding BiomeProviders for each generator type
4. THE JExMultiverse SHALL set a fixed spawn location appropriate for each world type

### Requirement 8: API and Integration

**User Story:** As a plugin developer, I want an API to interact with JExMultiverse, so that I can integrate world management into my plugins.

#### Acceptance Criteria

1. THE JExMultiverse SHALL provide an IMultiverseAdapter interface with methods: getGlobalMVWorld, getMVWorld, hasMultiverseSpawn, spawn
2. THE JExMultiverse SHALL register the MultiverseAdapter as a Bukkit service for external access
3. THE JExMultiverse SHALL return CompletableFuture for all async API operations

### Requirement 9: Translations

**User Story:** As a server administrator, I want localized messages, so that players see messages in their preferred language.

#### Acceptance Criteria

1. THE JExMultiverse SHALL use I18n.Builder pattern for all player-facing messages
2. THE JExMultiverse SHALL provide translation files in YAML format under resources/translations/
3. THE JExMultiverse SHALL support placeholder substitution using %placeholder% syntax
4. THE JExMultiverse SHALL include prefix support for consistent message formatting

### Requirement 10: Event Handling

**User Story:** As a server administrator, I want spawn events to be handled automatically, so that players spawn at configured locations.

#### Acceptance Criteria

1. WHEN PlayerSpawnLocationEvent fires, THE JExMultiverse SHALL set the spawn location based on global or world spawn configuration
2. WHEN PlayerRespawnEvent fires, THE JExMultiverse SHALL set the respawn location based on global or world spawn configuration
3. THE JExMultiverse SHALL handle spawn events with HIGHEST priority to override other plugins
