# Requirements Document

## Introduction

This document specifies the requirements for a complete rebuild of the RDQ (RaindropQuests) plugin system. The rebuild aims to create a modern, performant, and maintainable Minecraft plugin that provides rank progression, bounty hunting, and perk systems. The new implementation will leverage existing ecosystem libraries (RPlatform, JExTranslate, JExCommand) while following modern Java practices without inline comments and verbose patterns. The system must support Bukkit/Spigot and Paper servers from version 1.13 through 1.21.10.

## Glossary

- **RDQ_System**: The complete RaindropQuests plugin ecosystem consisting of rdq-common, rdq-free, and rdq-premium modules
- **RPlatform**: The shared platform library providing cross-version compatibility, database converters, logging, scheduling, and utility classes
- **JExTranslate**: The internationalization library providing multi-language support with MiniMessage formatting
- **JExCommand**: The command framework library providing YAML-based command registration and execution
- **Rank_System**: The progression system allowing players to advance through rank trees (warrior, cleric, mage, rogue, merchant, ranger)
- **Bounty_System**: The player-vs-player hunting system where players can place and claim bounties on other players
- **Perk_System**: The ability system providing toggleable and event-based perks with cooldowns and requirements
- **Inventory_Framework**: The devnatan/inventory-framework library for creating GUI views
- **JExHibernate**: The JExcellence Hibernate wrapper library (de.jexcellence.hibernate:JEHibernate) providing simplified database operations and repository patterns
- **Multi_Version_Support**: Compatibility layer ensuring functionality across Minecraft versions 1.13 to 1.21.10
- **Edition**: The plugin variant (Free or Premium) determining available features

## Requirements

### Requirement 1: Project Structure and Module Architecture

**User Story:** As a developer, I want a clean modular project structure, so that code is organized, maintainable, and supports both free and premium editions.

#### Acceptance Criteria

1. THE RDQ_System SHALL organize code into three Gradle submodules: rdq-common (shared logic), rdq-free (free edition entry point), and rdq-premium (premium edition entry point)
2. THE RDQ_System SHALL use Gradle Kotlin DSL for build configuration with version catalogs for dependency management
3. THE RDQ_System SHALL depend on RPlatform for cross-cutting concerns including logging, scheduling, database converters, and version detection
4. THE RDQ_System SHALL integrate JExCommand for all command registration and handling via YAML configuration
5. THE RDQ_System SHALL integrate JExTranslate for all player-facing messages with support for 18+ locales
6. THE RDQ_System SHALL use devnatan/inventory-framework for all GUI implementations
7. THE RDQ_System SHALL follow package structure: `com.raindropcentral.rdq.[feature].[layer]` where layer includes api, service, repository, entity, config, view, and command

### Requirement 2: Multi-Version Server Compatibility

**User Story:** As a server administrator, I want the plugin to work on any Minecraft version from 1.13 to 1.21.10, so that I can use it regardless of my server version.

#### Acceptance Criteria

1. THE RDQ_System SHALL detect server type (Bukkit, Spigot, Paper, Folia) at runtime using RPlatform's ServerEnvironment
2. THE RDQ_System SHALL provide version-specific implementations for ItemStack building using RPlatform's UnifiedBuilderFactory
3. THE RDQ_System SHALL handle Material enum differences between versions using XSeries library
4. THE RDQ_System SHALL support both plugin.yml (legacy) and paper-plugin.yml (modern Paper) descriptors
5. WHEN the server version is below 1.16, THE RDQ_System SHALL use legacy color codes instead of MiniMessage hex colors
6. THE RDQ_System SHALL use RPlatform's ISchedulerAdapter for Folia-compatible task scheduling

### Requirement 3: Rank Progression System

**User Story:** As a player, I want to progress through rank trees by completing requirements, so that I can unlock new abilities and show my achievements.

#### Acceptance Criteria

1. THE Rank_System SHALL support multiple rank trees (paths) including warrior, cleric, mage, rogue, merchant, and ranger
2. THE Rank_System SHALL load rank configurations from YAML files in the `rank/paths/` resource directory
3. THE Rank_System SHALL persist player rank progress using Hibernate entities with async repository operations
4. THE Rank_System SHALL enforce linear progression within a rank tree unless configuration allows rank skipping
5. THE Rank_System SHALL support cross-tree switching at configurable tier thresholds with cooldown periods
6. THE Rank_System SHALL integrate with LuckPerms for permission group assignment upon rank changes
7. WHEN a player completes all requirements for a rank, THE Rank_System SHALL automatically unlock the next rank and send configurable notifications
8. THE Rank_System SHALL provide GUI views for rank tree overview, rank details, and requirement progress using Inventory_Framework
9. THE Rank_System SHALL support final rank rules requiring completion of multiple rank trees

### Requirement 4: Bounty Hunting System

**User Story:** As a player, I want to place bounties on other players and hunt bounties for rewards, so that I can engage in competitive PvP gameplay.

#### Acceptance Criteria

1. THE Bounty_System SHALL allow players to create bounties on other players with configurable minimum and maximum values
2. THE Bounty_System SHALL support multiple reward distribution modes: instant, chest, drop, and virtual
3. THE Bounty_System SHALL track bounty hunter statistics including kills, deaths, bounties placed, and bounties claimed
4. THE Bounty_System SHALL persist bounty data using Hibernate entities with async repository operations
5. THE Bounty_System SHALL support bounty expiration with configurable duration and automatic cleanup tasks
6. IF a player attempts to place a bounty on themselves, THEN THE Bounty_System SHALL reject the request with an appropriate error message
7. THE Bounty_System SHALL provide GUI views for bounty creation, active bounty list, bounty details, and leaderboards
8. THE Bounty_System SHALL announce bounty placements and claims to configured audiences (server-wide, nearby players, or target only)
9. THE Bounty_System SHALL integrate with Vault economy for bounty payments and rewards
10. THE Bounty_System SHALL support damage tracking to attribute bounty kills to the correct hunter

### Requirement 5: Perk System

**User Story:** As a player, I want to unlock and use perks that provide gameplay advantages, so that I can customize my playstyle.

#### Acceptance Criteria

1. THE Perk_System SHALL support two perk categories: toggleable perks (persistent effects) and event-based perks (triggered on specific events)
2. THE Perk_System SHALL load perk configurations from YAML files in the `perks/` resource directory
3. THE Perk_System SHALL enforce perk requirements including rank requirements, permission requirements, and currency costs
4. THE Perk_System SHALL implement cooldown tracking per player per perk with configurable durations
5. THE Perk_System SHALL persist player perk unlocks and activation states using Hibernate entities
6. THE Perk_System SHALL provide a PerkRegistry for runtime perk management and a PerkTypeRegistry for perk type definitions
7. THE Perk_System SHALL emit audit events for perk activations, deactivations, and trigger attempts
8. THE Perk_System SHALL provide GUI views for perk browsing, perk details, and perk management
9. WHILE a toggleable perk is active, THE Perk_System SHALL apply the configured effects to the player
10. THE Perk_System SHALL support built-in perk types: speed, strength, resistance, regeneration, night_vision, fire_resistance, water_breathing, jump_boost, haste, luck, fly, double_experience, prevent_death, treasure_hunter, and vampire

### Requirement 6: Configuration System

**User Story:** As a server administrator, I want flexible YAML-based configuration, so that I can customize all aspects of the plugin without code changes.

#### Acceptance Criteria

1. THE RDQ_System SHALL use JeConfig library for type-safe YAML configuration loading and validation
2. THE RDQ_System SHALL support hot-reloading of configuration files without server restart
3. THE RDQ_System SHALL provide default configuration files that are copied to the plugin data folder on first run
4. THE RDQ_System SHALL validate configuration values and log warnings for invalid entries
5. THE RDQ_System SHALL support environment-specific configuration overrides (development, staging, production)
6. THE RDQ_System SHALL organize configurations by feature: bounty/, rank/, perks/, commands/, translations/, database/

### Requirement 7: Database and Persistence

**User Story:** As a server administrator, I want reliable data persistence with support for multiple database backends, so that player progress is never lost.

#### Acceptance Criteria

1. THE RDQ_System SHALL use JExHibernate library for all database operations with JPA entity annotations
2. THE RDQ_System SHALL support H2 (embedded), MySQL, MariaDB, and PostgreSQL database backends
3. THE RDQ_System SHALL perform all database operations asynchronously using virtual threads when available
4. THE RDQ_System SHALL leverage JExHibernate's repository pattern with generic CRUD operations for all entities
5. THE RDQ_System SHALL use RPlatform's database converters for Bukkit types (Location, ItemStack, BoundingBox)
6. THE RDQ_System SHALL implement connection pooling with configurable pool size and timeout settings via JExHibernate
7. THE RDQ_System SHALL handle database migration and schema updates automatically on startup using JExHibernate's schema management

### Requirement 8: Command System Integration

**User Story:** As a player, I want intuitive commands with tab completion, so that I can easily interact with all plugin features.

#### Acceptance Criteria

1. THE RDQ_System SHALL register all commands using JExCommand's YAML-based command definitions
2. THE RDQ_System SHALL provide tab completion for all command arguments including player names, rank names, and perk names
3. THE RDQ_System SHALL enforce permission checks before command execution using the configured permission nodes
4. THE RDQ_System SHALL support both player and console command execution where appropriate
5. THE RDQ_System SHALL provide admin commands for rank management, bounty management, and perk management
6. THE RDQ_System SHALL provide player commands for viewing ranks, placing bounties, and managing perks

### Requirement 9: Internationalization

**User Story:** As a player, I want to see messages in my preferred language, so that I can understand all plugin interactions.

#### Acceptance Criteria

1. THE RDQ_System SHALL detect player locale using JExTranslate's LocaleResolver
2. THE RDQ_System SHALL support MiniMessage formatting for all player-facing messages
3. THE RDQ_System SHALL provide translation files for at least 18 locales including en_US, de_DE, fr_FR, es_ES, pt_BR, ja_JP, ko_KR, zh_CN, ru_RU, pl_PL, nl_NL, it_IT, tr_TR, sv_SE, no_NO, da_DK, en_GB
4. THE RDQ_System SHALL support placeholder substitution in translated messages using JExTranslate's Placeholder API
5. THE RDQ_System SHALL track missing translation keys and log them for developer review
6. THE RDQ_System SHALL fall back to en_US locale when a translation key is missing in the player's locale

### Requirement 10: GUI System

**User Story:** As a player, I want intuitive inventory-based menus, so that I can navigate plugin features without memorizing commands.

#### Acceptance Criteria

1. THE RDQ_System SHALL use devnatan/inventory-framework for all GUI implementations
2. THE RDQ_System SHALL implement paginated views for lists exceeding inventory size
3. THE RDQ_System SHALL prevent item duplication and inventory manipulation exploits
4. THE RDQ_System SHALL support dynamic item updates without closing and reopening the inventory
5. THE RDQ_System SHALL provide consistent navigation patterns across all views (back button, close button, pagination)
6. THE RDQ_System SHALL apply interaction delays to prevent click spam exploits

### Requirement 11: Performance and Optimization

**User Story:** As a server administrator, I want the plugin to have minimal performance impact, so that my server runs smoothly with many players.

#### Acceptance Criteria

1. THE RDQ_System SHALL use Caffeine cache for frequently accessed data with configurable TTL
2. THE RDQ_System SHALL batch database operations where possible to reduce connection overhead
3. THE RDQ_System SHALL use virtual threads (Java 21+) for async operations with fallback to thread pools
4. THE RDQ_System SHALL implement circuit breakers for external service calls (LuckPerms, Vault)
5. THE RDQ_System SHALL throttle log output for repeated errors to prevent log flooding
6. THE RDQ_System SHALL clean up player data from memory caches on disconnect

### Requirement 12: Testing and Quality

**User Story:** As a developer, I want comprehensive test coverage, so that I can refactor with confidence and catch regressions early.

#### Acceptance Criteria

1. THE RDQ_System SHALL include unit tests for all service classes using JUnit 5 and Mockito
2. THE RDQ_System SHALL include integration tests for repository operations using MockBukkit
3. THE RDQ_System SHALL achieve minimum 70% code coverage for rdq-common module
4. THE RDQ_System SHALL include property-based tests using jqwik for configuration parsing
5. THE RDQ_System SHALL provide test fixtures for common test scenarios

### Requirement 13: Documentation

**User Story:** As a developer or administrator, I want comprehensive documentation, so that I can understand and configure the plugin effectively.

#### Acceptance Criteria

1. THE RDQ_System SHALL include a README.md with architecture overview, setup instructions, and developer guidelines
2. THE RDQ_System SHALL include Javadoc comments on all public API classes and methods
3. THE RDQ_System SHALL include configuration documentation with examples for all YAML files
4. THE RDQ_System SHALL include a CHANGELOG.md tracking version history and breaking changes
5. THE RDQ_System SHALL include package-info.java files describing the purpose of each package

### Requirement 14: Edition Differentiation

**User Story:** As a server administrator, I want clear feature differentiation between free and premium editions, so that I can choose the right edition for my needs.

#### Acceptance Criteria

1. THE RDQ_System SHALL provide core rank, bounty, and perk functionality in the free edition
2. THE RDQ_System SHALL provide advanced features in the premium edition including: multiple active rank trees, cross-tree switching, advanced bounty distribution modes, and premium perk types
3. THE RDQ_System SHALL gracefully handle premium feature access attempts in the free edition with appropriate messaging
4. THE RDQ_System SHALL share all common code in rdq-common to avoid duplication between editions
5. THE RDQ_System SHALL use service interfaces with edition-specific implementations for feature differentiation
