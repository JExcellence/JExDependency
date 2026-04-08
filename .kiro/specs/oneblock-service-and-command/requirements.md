# Requirements Document

## Introduction

This document specifies the requirements for implementing a working `IInfrastructureService` implementation, a new `PIsland` player command, and enhanced biome management for the JExOneblock plugin. The infrastructure service will bridge the existing `InfrastructureManager` with the service layer, while the `PIsland` command will provide players with access to island management, evolution overview, biome settings, upgrades, and related GUI views.

## Glossary

- **JExOneblock**: The Minecraft OneBlock plugin that manages player islands, evolutions, and infrastructure systems
- **IInfrastructureService**: Service interface for infrastructure operations including energy, storage, automation, and crafting
- **InfrastructureManager**: Core manager class handling island automation and infrastructure logic
- **InfrastructureTickProcessor**: Background processor for energy generation, automation, and passive rewards
- **PIsland**: Player command for island management and GUI access
- **Evolution**: Progression stages in the OneBlock system with different blocks, items, and entities
- **ViewFrame**: InventoryFramework component for managing GUI views
- **IslandInfrastructure**: Entity representing an island's infrastructure state (energy, storage, processors, etc.)
- **OneblockIsland**: Entity representing a player's OneBlock island
- **DistributedBiomeChanger**: High-performance utility for changing biomes across island regions
- **IslandRegion**: Embeddable component defining the 3D boundaries of an island
- **OneblockCore**: Embeddable component tracking evolution progression, experience, and prestige
- **StorageTier**: Enum defining storage capacity levels with associated bonuses
- **Prestige**: System allowing players to reset progress for permanent bonuses

## Requirements

### Requirement 1: Infrastructure Service Implementation

**User Story:** As a plugin developer, I want a working `IInfrastructureService` implementation, so that the infrastructure system integrates properly with the plugin lifecycle and other services.

#### Acceptance Criteria

1. WHEN the plugin enables, THE InfrastructureService SHALL initialize with the InfrastructureManager and InfrastructureTickProcessor instances.
2. WHEN `getInfrastructure(islandId, playerId)` is called, THE InfrastructureService SHALL return the IslandInfrastructure from the InfrastructureManager cache or create a new one.
3. WHEN `getInfrastructureAsync(islandId)` is called, THE InfrastructureService SHALL return a CompletableFuture containing the infrastructure loaded from the repository.
4. WHEN `getManager()` is called, THE InfrastructureService SHALL return the InfrastructureManager instance.
5. WHEN `getTickProcessor()` is called, THE InfrastructureService SHALL return the InfrastructureTickProcessor instance.

### Requirement 2: Infrastructure Service Integration

**User Story:** As a plugin developer, I want the infrastructure service to be properly integrated into the plugin lifecycle, so that infrastructure systems start and stop correctly.

#### Acceptance Criteria

1. WHEN the plugin enables, THE JExOneblock class SHALL create and set the InfrastructureService implementation.
2. WHEN the plugin enables, THE InfrastructureTickProcessor SHALL start background processing tasks.
3. WHEN the plugin disables, THE InfrastructureTickProcessor SHALL stop all background tasks and save pending data.
4. WHEN a player joins with an existing island, THE InfrastructureTickProcessor SHALL register their infrastructure for processing.
5. WHEN a player quits, THE InfrastructureTickProcessor SHALL unregister their infrastructure and save its state.

### Requirement 3: PIsland Command Structure

**User Story:** As a player, I want a `/island` command to manage my OneBlock island, so that I can access all island-related features from one command.

#### Acceptance Criteria

1. WHEN a player executes `/island` without arguments, THE PIsland command SHALL open the main island overview GUI.
2. WHEN a player executes `/island` without having an island, THE PIsland command SHALL display a localized message indicating no island exists.
3. WHEN a player executes `/island help`, THE PIsland command SHALL display all available subcommands with descriptions.
4. THE PIsland command SHALL support tab completion for all valid subcommands.
5. THE PIsland command SHALL check permissions before executing each subcommand action.

### Requirement 4: Island Information Subcommands

**User Story:** As a player, I want to view information about my island, so that I can track my progress and statistics.

#### Acceptance Criteria

1. WHEN a player executes `/island info`, THE PIsland command SHALL display island statistics including level, experience, blocks broken, and member count.
2. WHEN a player executes `/island level`, THE PIsland command SHALL display the current island level and experience progress.
3. WHEN a player executes `/island stats`, THE PIsland command SHALL open a statistics GUI showing detailed island metrics.
4. WHEN a player executes `/island top`, THE PIsland command SHALL display a leaderboard of top islands by level.

### Requirement 5: Evolution Overview

**User Story:** As a player, I want to view my evolution progress and available evolutions, so that I can understand my progression path.

#### Acceptance Criteria

1. WHEN a player executes `/island evolution`, THE PIsland command SHALL open the evolution overview GUI.
2. THE evolution overview GUI SHALL display the current evolution name, level, and experience progress.
3. THE evolution overview GUI SHALL show blocks, items, and entities available in the current evolution.
4. THE evolution overview GUI SHALL provide navigation to view other unlocked evolutions.
5. WHEN viewing an evolution, THE GUI SHALL display the experience required to advance to the next evolution.

### Requirement 6: Island Teleportation

**User Story:** As a player, I want to teleport to my island and manage teleportation settings, so that I can easily access my island.

#### Acceptance Criteria

1. WHEN a player executes `/island home` or `/island tp`, THE PIsland command SHALL teleport the player to their island spawn location.
2. WHEN a player executes `/island sethome`, THE PIsland command SHALL set the island spawn location to the player's current position.
3. IF the player is not within their island region, THEN THE PIsland command SHALL reject the sethome request with a localized error message.
4. WHEN teleportation completes, THE PIsland command SHALL display a localized success message.

### Requirement 7: Island Member Management

**User Story:** As an island owner, I want to manage island members, so that I can control who has access to my island.

#### Acceptance Criteria

1. WHEN a player executes `/island members`, THE PIsland command SHALL open a GUI showing all island members and their roles.
2. WHEN a player executes `/island invite <player>`, THE PIsland command SHALL send an island invitation to the target player.
3. WHEN a player executes `/island kick <player>`, THE PIsland command SHALL remove the target player from the island if the executor has permission.
4. WHEN a player executes `/island ban <player>`, THE PIsland command SHALL ban the target player from the island.
5. WHEN a player executes `/island unban <player>`, THE PIsland command SHALL remove the ban for the target player.

### Requirement 8: Island Settings

**User Story:** As an island owner, I want to configure island settings, so that I can customize my island's behavior and access controls.

#### Acceptance Criteria

1. WHEN a player executes `/island settings`, THE PIsland command SHALL open the island settings GUI.
2. THE settings GUI SHALL allow toggling visitor permissions for building, container access, and entity interaction.
3. THE settings GUI SHALL allow setting the island as public or private.
4. WHEN a setting is changed, THE PIsland command SHALL persist the change to the database immediately.

### Requirement 9: Island Creation and Deletion

**User Story:** As a player, I want to create and manage my island lifecycle, so that I can start fresh or reset my progress.

#### Acceptance Criteria

1. WHEN a player without an island executes `/island create`, THE PIsland command SHALL initiate island creation through the OneblockService.
2. WHEN a player executes `/island delete`, THE PIsland command SHALL require confirmation before deleting the island.
3. WHEN island deletion is confirmed, THE PIsland command SHALL delete the island through the OneblockService and notify the player.
4. IF island creation fails, THEN THE PIsland command SHALL display a localized error message explaining the failure.

### Requirement 10: Permission System

**User Story:** As a server administrator, I want granular permissions for island commands, so that I can control feature access.

#### Acceptance Criteria

1. THE PIsland command SHALL define a base permission `jexoneblock.island.command` for command access.
2. THE PIsland command SHALL define individual permissions for each subcommand following the pattern `jexoneblock.island.<subcommand>`.
3. WHEN a player lacks permission for a subcommand, THE PIsland command SHALL display a localized no-permission message.
4. THE permission enum SHALL be defined in a separate `EIslandPermission` class following the existing pattern.

### Requirement 11: Biome Management

**User Story:** As a player, I want to change my island's biome, so that I can customize the visual appearance and mob spawning of my island.

#### Acceptance Criteria

1. WHEN a player executes `/island biome`, THE PIsland command SHALL open a biome selection GUI.
2. THE biome selection GUI SHALL display all available biomes organized by category (plains, forest, desert, ocean, nether, end, etc.).
3. WHEN a player selects a biome, THE system SHALL use the DistributedBiomeChanger to change the island region's biome asynchronously.
4. WHILE the biome change is in progress, THE system SHALL display progress feedback to the player.
5. WHEN the biome change completes, THE system SHALL notify the player with a localized success message.
6. IF the player lacks sufficient resources or permissions for a biome, THEN THE GUI SHALL display the biome as locked with requirements shown.

### Requirement 12: Island Upgrades System

**User Story:** As a player, I want to upgrade my island's capabilities, so that I can expand my island size, unlock features, and improve efficiency.

#### Acceptance Criteria

1. WHEN a player executes `/island upgrades`, THE PIsland command SHALL open the island upgrades GUI.
2. THE upgrades GUI SHALL display available upgrades including: island size expansion, member slots, storage capacity, and biome unlock tiers.
3. THE upgrades GUI SHALL show the current level, next level benefits, and resource requirements for each upgrade.
4. WHEN a player clicks an upgrade, THE system SHALL check resource requirements and apply the upgrade if met.
5. IF the player lacks required resources, THEN THE system SHALL display a localized message listing missing requirements.
6. WHEN an upgrade is applied, THE system SHALL persist the change and update the island entity immediately.

### Requirement 13: OneBlock Core View

**User Story:** As a player, I want to view detailed information about my OneBlock progression, so that I can track my evolution progress and statistics.

#### Acceptance Criteria

1. WHEN a player executes `/island oneblock` or clicks the OneBlock item in the main GUI, THE system SHALL open the OneBlock core view.
2. THE OneBlock core view SHALL display: current evolution name, evolution level, experience progress bar, blocks broken count, and prestige level.
3. THE OneBlock core view SHALL show the current block drop rates by rarity tier (common, uncommon, rare, epic, legendary).
4. THE OneBlock core view SHALL display entity spawn chances for the current evolution.
5. THE OneBlock core view SHALL show item drop chances for the current evolution.
6. WHEN the player clicks the evolution showcase item, THE system SHALL navigate to the full evolution browser.

### Requirement 14: Evolution Browser

**User Story:** As a player, I want to browse all evolutions, so that I can see what content awaits me at each progression stage.

#### Acceptance Criteria

1. WHEN a player opens the evolution browser, THE system SHALL display all evolutions in a paginated GUI.
2. THE evolution browser SHALL show locked evolutions with their unlock requirements (level, prestige, etc.).
3. THE evolution browser SHALL highlight the player's current evolution.
4. WHEN a player clicks an unlocked evolution, THE system SHALL display detailed information including all blocks, items, and entities.
5. THE evolution detail view SHALL organize content by rarity tier with visual indicators.
6. THE evolution browser SHALL support filtering by evolution category or search by name.

### Requirement 15: Prestige System Integration

**User Story:** As a player who has completed all evolutions, I want to prestige my island, so that I can gain permanent bonuses and start fresh with enhanced capabilities.

#### Acceptance Criteria

1. WHEN a player executes `/island prestige`, THE PIsland command SHALL check if prestige requirements are met.
2. IF prestige requirements are not met, THEN THE system SHALL display current progress and remaining requirements.
3. WHEN prestige requirements are met, THE system SHALL display a confirmation GUI showing prestige rewards and what will be reset.
4. WHEN the player confirms prestige, THE system SHALL reset evolution progress, grant prestige points, and apply permanent bonuses.
5. THE prestige confirmation GUI SHALL clearly list: experience multiplier bonus, drop rate bonus, new unlocked features, and items that will be preserved.
6. WHEN prestige completes, THE system SHALL update the OneblockCore prestige level and notify the player.
