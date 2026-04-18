# Requirements Document

## Introduction

The Machine Fabrication System introduces an advanced automation framework for RDQ that allows players to construct and operate automated crafting machines. The system features permission-based machine unlocking, multi-block structure construction, automated resource processing, and upgradeable machine capabilities. This system integrates with the existing perk system and focuses on the Fabricator machine type, which provides automated crafting functionality with fuel consumption, storage management, and upgrade paths.

## Glossary

- **Fabricator**: A multi-block automated crafting machine that processes recipes using stored resources
- **Machine System**: The overarching framework managing all machine types and their interactions
- **Multi-Block Structure**: A specific arrangement of blocks required to construct a functional machine
- **Blueprint**: A configuration defining machine construction requirements and capabilities
- **Machine Fuel**: Consumable resource required to power machine operations
- **Recipe Lock**: State where a crafting recipe is set and the machine begins automated production
- **Storage Block**: Attached block providing input/output inventory for machines
- **Machine Upgrade**: Enhancement items that improve machine performance metrics
- **Trust System**: Permission mechanism allowing specific players to interact with machines
- **Virtual Storage**: Internal machine inventory not represented by physical blocks

## Requirements

### Requirement 1: Machine Permission System

**User Story:** As a server administrator, I want machines to be gated behind permissions and perks, so that players progress through machine tiers systematically.

#### Acceptance Criteria

1. WHEN a player attempts to craft or place a machine, THE System SHALL verify the player possesses the required permission node
2. THE System SHALL support permission nodes in format "rdq.machine.{machine_type}" for each machine type
3. THE System SHALL integrate with the existing perk configuration system to define machine unlock requirements
4. THE System SHALL load machine definitions from YAML configuration files in the perks directory
5. WHERE a player lacks required permissions, THE System SHALL display a translated error message indicating missing requirements

### Requirement 2: Machine Configuration System

**User Story:** As a server administrator, I want to configure machine properties through YAML files, so that I can customize machine behavior without code changes.

#### Acceptance Criteria

1. THE System SHALL load machine configurations from "plugins/RDQ/machines/{machine_type}.yml" files
2. THE System SHALL support configuration of multi-block structure patterns for each machine type
3. THE System SHALL define blueprint requirements including currency costs and item requirements
4. THE System SHALL specify fuel types and consumption rates per machine operation
5. THE System SHALL configure upgrade paths with requirement definitions and performance modifiers
6. THE System SHALL validate configuration on server startup and log any errors with specific line references
7. THE System SHALL support hot-reloading of machine configurations via admin command

### Requirement 3: Multi-Block Structure Construction

**User Story:** As a player, I want to build machines by placing blocks in specific patterns, so that I can create functional automated systems.

#### Acceptance Criteria

1. WHEN a player places the final block of a valid machine structure, THE System SHALL detect the completed pattern
2. THE System SHALL validate that all required blocks are present in correct positions relative to the core block
3. THE System SHALL verify the player has permission to construct the machine type
4. THE System SHALL consume blueprint requirements from player inventory upon successful construction
5. THE System SHALL register the machine in the database with owner UUID and location data
6. THE System SHALL display a translated success message with machine coordinates
7. IF construction fails validation, THEN THE System SHALL display specific error indicating which requirement failed

### Requirement 4: Fabricator Core Functionality

**User Story:** As a player, I want to operate a Fabricator machine that automates crafting, so that I can produce items without manual interaction.

#### Acceptance Criteria

1. THE Fabricator SHALL provide a GUI with machine status, fuel level, and recipe configuration interface
2. THE Fabricator SHALL support 3x3 crafting grid for recipe definition
3. WHEN a valid recipe is set, THE Fabricator SHALL lock the recipe and display it as the active pattern
4. THE Fabricator SHALL accept items through attached storage blocks or manual deposit
5. THE Fabricator SHALL track internal virtual storage for all deposited materials
6. WHEN sufficient materials and fuel exist, THE Fabricator SHALL automatically craft items at configured intervals
7. THE Fabricator SHALL output crafted items to attached output storage or virtual output inventory
8. THE Fabricator SHALL consume fuel per crafting operation based on configuration

### Requirement 5: Machine Trust and Security System

**User Story:** As a machine owner, I want to control who can interact with my machines, so that my resources remain secure.

#### Acceptance Criteria

1. THE System SHALL designate the machine placer as the owner with full permissions
2. THE System SHALL provide a trust management GUI accessible only to the owner
3. THE System SHALL allow owners to add trusted players by username or UUID
4. THE System SHALL allow owners to remove trusted players from the trust list
5. WHEN an untrusted player attempts interaction, THE System SHALL cancel the event and display a permission error
6. THE System SHALL prevent untrusted players from breaking, placing blocks adjacent to, or opening machine GUIs
7. THE System SHALL persist trust lists in the database across server restarts

### Requirement 6: Machine Storage System

**User Story:** As a player, I want to manage machine input and output through storage interfaces, so that I can efficiently supply and collect materials.

#### Acceptance Criteria

1. THE System SHALL support attaching Hopper blocks as input storage to machines
2. THE System SHALL support attaching Chest blocks as output storage to machines
3. THE System SHALL provide a storage management GUI showing all stored items with quantities
4. THE System SHALL allow manual deposit of items from player inventory to machine storage
5. THE System SHALL allow manual withdrawal of items from machine storage to player inventory
6. THE System SHALL support unlimited virtual storage capacity per machine
7. WHEN a machine is broken, THE System SHALL either drop all stored items or retain them in virtual storage based on configuration

### Requirement 7: Recipe Management System

**User Story:** As a player, I want to configure crafting recipes in my Fabricator, so that I can automate production of specific items.

#### Acceptance Criteria

1. THE System SHALL provide a recipe configuration GUI with a 3x3 crafting grid
2. THE System SHALL validate recipes against Minecraft's crafting system
3. WHEN a valid recipe is detected, THE System SHALL display a "Set Recipe" button
4. WHEN recipe is set, THE System SHALL lock the grid and change item displays to indicate locked state
5. THE System SHALL require sufficient fuel or currency balance before allowing recipe activation
6. THE System SHALL display recipe preview after successful validation
7. THE System SHALL allow recipe clearing only when machine is in OFF state

### Requirement 8: Machine Fuel System

**User Story:** As a player, I want to fuel my machines using consumable resources, so that automation has an operational cost.

#### Acceptance Criteria

1. THE System SHALL define fuel types in machine configuration with energy values
2. THE System SHALL accept fuel items through deposit interface or attached storage
3. THE System SHALL track current fuel level as an integer value in the database
4. WHEN a crafting operation occurs, THE System SHALL consume fuel based on recipe complexity
5. WHEN fuel reaches zero, THE System SHALL automatically disable the machine
6. THE System SHALL display current fuel level and maximum capacity in machine GUI
7. THE System SHALL support multiple fuel types with different energy conversion rates

### Requirement 9: Machine Upgrade System

**User Story:** As a player, I want to upgrade my machines with enhancement items, so that I can improve their performance.

#### Acceptance Criteria

1. THE System SHALL define upgrade types: Speed, Efficiency, Bonus Output, and Fuel Reduction
2. THE System SHALL validate upgrade requirements through the existing requirement system
3. WHEN an upgrade is applied, THE System SHALL consume required items and currency
4. THE System SHALL persist upgrade levels in the database per machine instance
5. THE System SHALL apply Speed upgrades as percentage reduction to crafting cooldown
6. THE System SHALL apply Efficiency upgrades as percentage chance to avoid fuel consumption
7. THE System SHALL apply Bonus Output upgrades as percentage chance to produce extra items
8. THE System SHALL apply Fuel Reduction upgrades as percentage reduction to fuel cost per operation
9. THE System SHALL display current upgrade levels in machine GUI

### Requirement 10: Machine State Management

**User Story:** As a player, I want to toggle my machines on and off, so that I can control when automation occurs.

#### Acceptance Criteria

1. THE System SHALL provide an ON/OFF toggle button in machine GUI
2. WHEN toggled to ON, THE System SHALL verify sufficient fuel and valid recipe exist
3. WHEN toggled to ON, THE System SHALL begin automated crafting cycles
4. WHEN toggled to OFF, THE System SHALL halt all crafting operations immediately
5. THE System SHALL persist machine state in database across server restarts
6. THE System SHALL display current state prominently in machine GUI
7. IF fuel depletes during operation, THEN THE System SHALL automatically toggle machine to OFF state

### Requirement 11: Automated Crafting Cycle

**User Story:** As a player, I want my Fabricator to automatically craft items when enabled, so that I can produce resources passively.

#### Acceptance Criteria

1. WHEN machine is ON and has valid recipe, THE System SHALL check for sufficient materials every configured interval
2. THE System SHALL search attached storage blocks and virtual storage for recipe ingredients
3. WHEN all ingredients are available, THE System SHALL consume them from storage
4. THE System SHALL consume configured fuel amount for the crafting operation
5. THE System SHALL apply upgrade modifiers to fuel consumption and output quantity
6. THE System SHALL add crafted items to output storage or virtual output inventory
7. THE System SHALL apply configured cooldown before next crafting cycle
8. THE System SHALL apply Speed upgrade modifiers to cooldown duration

### Requirement 12: Machine Breaking and Persistence

**User Story:** As a player, I want my machine data to persist when broken, so that I don't lose progress and resources.

#### Acceptance Criteria

1. WHEN a machine is broken by owner or trusted player, THE System SHALL validate break permission
2. THE System SHALL provide configuration option to drop stored items or retain in virtual storage
3. WHERE items are retained, THE System SHALL save complete machine state to database
4. THE System SHALL drop a machine item with NBT data containing machine ID
5. WHEN machine item is placed, THE System SHALL restore machine state from database
6. THE System SHALL preserve fuel levels, upgrade levels, recipe configuration, and trust list
7. THE System SHALL remove machine from world database but retain in archive table

### Requirement 13: Machine Item System

**User Story:** As a player, I want to obtain machine items through commands or crafting, so that I can deploy machines in my base.

#### Acceptance Criteria

1. THE System SHALL provide admin command "/rq machine give {player} {machine_type}" to grant machine items
2. THE System SHALL support crafting recipes for machine items defined in configuration
3. THE System SHALL create ItemStack with custom NBT data identifying machine type
4. THE System SHALL apply custom display name and lore from translation system
5. THE System SHALL validate player has permission before allowing machine item usage
6. THE System SHALL support purchasing machine items through economy integration
7. THE System SHALL track machine item acquisition in player statistics

### Requirement 14: Machine GUI System

**User Story:** As a player, I want intuitive GUIs for machine interaction, so that I can easily manage my automation.

#### Acceptance Criteria

1. THE System SHALL use BaseView pattern for all machine GUIs
2. THE System SHALL provide main machine GUI with status display, toggle button, and navigation
3. THE System SHALL provide storage management GUI with deposit/withdraw functionality
4. THE System SHALL provide trust management GUI with player list and add/remove controls
5. THE System SHALL provide upgrade GUI showing available upgrades and requirements
6. THE System SHALL provide recipe configuration GUI with crafting grid and validation
7. THE System SHALL use R18n translation system for all GUI text with player locale support
8. THE System SHALL display real-time updates for fuel level, storage contents, and machine state

### Requirement 15: Database Schema for Machines

**User Story:** As a system, I want to persist machine data in the database, so that machines survive server restarts and can be queried efficiently.

#### Acceptance Criteria

1. THE System SHALL create "rdq_machines" table with columns: id, owner_uuid, machine_type, world, x, y, z, state, fuel_level, recipe_data, created_at, updated_at
2. THE System SHALL create "rdq_machine_storage" table with columns: id, machine_id, item_data, quantity, storage_type
3. THE System SHALL create "rdq_machine_upgrades" table with columns: id, machine_id, upgrade_type, level
4. THE System SHALL create "rdq_machine_trust" table with columns: id, machine_id, trusted_uuid, granted_at
5. THE System SHALL use JEHibernate ORM for all database operations
6. THE System SHALL implement CachedRepository pattern for active machines
7. THE System SHALL load machine data asynchronously on chunk load events

### Requirement 16: Integration with JExWorkbench

**User Story:** As a developer, I want the machine system to integrate with JExWorkbench, so that larger crafting grids can be supported in the future.

#### Acceptance Criteria

1. THE System SHALL design recipe configuration interface to support variable grid sizes
2. THE System SHALL abstract crafting grid size from core machine logic
3. THE System SHALL provide API methods for registering custom grid sizes
4. THE System SHALL validate recipes against appropriate crafting system based on grid size
5. THE System SHALL support 6x6 grid size when JExWorkbench integration is implemented

### Requirement 17: Machine Commands and Administration

**User Story:** As a server administrator, I want commands to manage machines, so that I can troubleshoot and maintain the system.

#### Acceptance Criteria

1. THE System SHALL provide "/rq machine give {player} {type}" command to grant machine items
2. THE System SHALL provide "/rq machine list {player}" command to show player's machines
3. THE System SHALL provide "/rq machine remove {machine_id}" command to delete machines
4. THE System SHALL provide "/rq machine reload" command to reload machine configurations
5. THE System SHALL provide "/rq machine info {machine_id}" command to display machine details
6. THE System SHALL provide "/rq machine teleport {machine_id}" command to teleport to machine location
7. THE System SHALL require "rdq.admin.machine" permission for all admin commands
