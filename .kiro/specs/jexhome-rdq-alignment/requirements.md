# Requirements Document

## Introduction

This document specifies the requirements for refactoring JExHome to align with RDQ patterns exactly, fixing current command issues, ensuring all i18n keys exist, implementing a service pattern, and adding enhanced features with a custom blue-orange gradient color scheme. The refactor will ensure consistent usage of configs, commands, listeners, entities, and repositories matching the RDQ codebase.

## Glossary

- **JExHome**: The home teleportation plugin being refactored
- **Home**: A saved location entity that players can teleport to
- **IHomeService**: Service interface for home operations (following IBountyService pattern)
- **HomeFactory**: Factory class for business logic (following BountyFactory pattern)
- **RDQ**: Reference project for patterns and structure alignment
- **APaginatedView**: Abstract paginated view base class from RPlatform
- **LocationConverter**: JPA converter for Bukkit Location objects
- **Gradient Colors**: Blue (#1e3a8a to #60a5fa) and Orange (#ea580c to #fb923c) color scheme

## Requirements

### Requirement 1: Service Pattern Implementation

**User Story:** As a developer, I want JExHome to use the same service pattern as RDQ's IBountyService, so that business logic is properly abstracted and testable.

#### Acceptance Criteria

1. THE JExHome project SHALL define an IHomeService interface in jexhome-common with async CompletableFuture methods for all home operations.
2. THE jexhome-free module SHALL implement FreeHomeService with limited functionality (max 3 homes per player).
3. THE jexhome-premium module SHALL implement PremiumHomeService with full functionality and configurable limits.
4. WHEN a command or view needs home operations, THE system SHALL use IHomeService instead of directly accessing HomeRepository.
5. THE IHomeService interface SHALL include isPremium(), canCreateHome(), getMaxHomesForPlayer(), and getHomeCount() methods matching IBountyService patterns.

### Requirement 2: Fix Command Registration and Execution

**User Story:** As a player, I want all home commands (/home, /sethome, /delhome) to work correctly, so that I can manage my home locations.

#### Acceptance Criteria

1. WHEN a player executes /sethome with a valid name, THE system SHALL create a new home at the player's current location within 100ms.
2. WHEN a player executes /sethome with an existing home name, THE system SHALL update the home location and send a confirmation message.
3. WHEN a player executes /home with a valid home name, THE system SHALL teleport the player to that location.
4. WHEN a player executes /home without arguments, THE system SHALL open the HomeOverviewView GUI.
5. WHEN a player executes /delhome with a valid home name, THE system SHALL delete the home and send a confirmation message.
6. IF a player executes any home command without the required permission, THEN THE system SHALL send a permission denied message.
7. IF a database error occurs during command execution, THEN THE system SHALL log the error and send a user-friendly error message.

### Requirement 3: Complete i18n Key Coverage

**User Story:** As a server administrator, I want all messages to have proper translation keys, so that the plugin supports multiple languages without missing text.

#### Acceptance Criteria

1. THE translation files SHALL include all keys used by commands: home.teleported, home.does_not_exist, home.world_not_loaded, home.error.internal.
2. THE translation files SHALL include all keys used by sethome: sethome.created, sethome.home_overwritten, sethome.home_limit_reached, sethome.usage.
3. THE translation files SHALL include all keys used by delhome: delhome.deleted, delhome.does_not_exist, delhome.usage.
4. THE translation files SHALL include GUI keys: home_overview_ui.title, home.name, home.lore, home.options_coming_soon.
5. THE translation files SHALL use gradient color syntax for the blue-orange theme: `<gradient:#1e3a8a:#60a5fa>` for primary and `<gradient:#ea580c:#fb923c>` for secondary.
6. WHEN a translation key is missing, THE system SHALL log a warning and display the key name as fallback.

### Requirement 4: Enhanced Home Entity with Metadata

**User Story:** As a player, I want my homes to track additional information like visit count and last visited time, so that I can see usage statistics.

#### Acceptance Criteria

1. THE Home entity SHALL include fields: category (String), favorite (boolean), description (String), icon (String material name).
2. THE Home entity SHALL include metadata fields: visitCount (int), lastVisited (LocalDateTime), createdAt (LocalDateTime).
3. THE Home entity SHALL use LocationConverter from RPlatform for storing Location as JSON.
4. WHEN a player teleports to a home, THE system SHALL increment visitCount and update lastVisited timestamp.
5. THE HomeRepository SHALL provide methods: findByCategory(), findFavorites(), countByPlayerUuid().

### Requirement 5: GUI Enhancement with Blue-Orange Theme

**User Story:** As a player, I want the home GUI to have a visually appealing blue-orange gradient theme with sorting and filtering options.

#### Acceptance Criteria

1. THE HomeOverviewView SHALL display homes in a paginated grid layout matching RDQ view patterns.
2. THE view title SHALL use gradient colors: `<gradient:#1e3a8a:#60a5fa>Your Homes</gradient>`.
3. THE view SHALL include filter buttons for: All homes, Favorites only, By category.
4. THE view SHALL include sort options: By name, By creation date, By last visited.
5. WHEN a player left-clicks a home item, THE system SHALL teleport them to that home.
6. WHEN a player right-clicks a home item, THE system SHALL open a HomeManagementView for that home.
7. THE home items SHALL display: name with gradient, world name, coordinates, visit count, and last visited date.

### Requirement 6: Configuration Alignment with RDQ Patterns

**User Story:** As a developer, I want JExHome configuration to follow the same patterns as RDQ's BountySection, so that configuration is consistent across projects.

#### Acceptance Criteria

1. THE HomeSystemConfig SHALL extend AConfigSection with @CSAlways annotation matching BountySection pattern.
2. THE configuration SHALL include sections: homeLimits (Map), teleport (TeleportSection), gui (GuiSection), colors (ColorSchemeSection).
3. THE ColorSchemeSection SHALL define: primaryGradient, secondaryGradient, successGradient, errorGradient, warningGradient.
4. THE TeleportSection SHALL define: delay, cancelOnMove, cancelOnDamage, showCountdown, playSounds, showParticles.
5. WHEN a configuration value is missing, THE system SHALL use sensible defaults without throwing exceptions.

### Requirement 7: HomeFactory for Business Logic

**User Story:** As a developer, I want a HomeFactory class to centralize home business logic, so that commands and views don't contain duplicate logic.

#### Acceptance Criteria

1. THE HomeFactory SHALL be a singleton accessible via getInstance() following BountyFactory pattern.
2. THE HomeFactory SHALL provide methods: createHome(), deleteHome(), teleportToHome(), getPlayerHomes().
3. THE HomeFactory SHALL handle permission checks, limit validation, and error handling.
4. THE HomeFactory SHALL cache active homes for performance optimization.
5. WHEN teleporting a player, THE HomeFactory SHALL handle warmup delay, movement cancellation, and damage cancellation based on config.

### Requirement 8: Async Repository Operations

**User Story:** As a developer, I want all database operations to be asynchronous, so that the main server thread is never blocked.

#### Acceptance Criteria

1. THE HomeRepository SHALL extend CachedRepository<Home, Long, Long> matching RDQ repository patterns.
2. ALL repository methods SHALL return CompletableFuture for async execution.
3. THE repository SHALL use findAllByAttributesAsync() for multi-attribute queries.
4. WHEN a repository operation completes, THE system SHALL execute callbacks on the main thread for Bukkit API calls.
5. IF a repository operation fails, THEN THE system SHALL log the exception and return a failed CompletableFuture.

### Requirement 9: Premium vs Free Feature Differentiation

**User Story:** As a server administrator, I want clear differentiation between free and premium features, so that players understand the value of upgrading.

#### Acceptance Criteria

1. THE FreeHomeService SHALL limit players to maximum 3 homes.
2. THE FreeHomeService SHALL disable advanced features: categories, favorites, sharing, statistics.
3. THE PremiumHomeService SHALL support configurable home limits via permissions.
4. THE PremiumHomeService SHALL enable all advanced features.
5. WHEN a free user attempts a premium feature, THE system SHALL display an upgrade message.

### Requirement 10: Error Handling and Logging

**User Story:** As a server administrator, I want comprehensive error handling and logging, so that I can diagnose issues quickly.

#### Acceptance Criteria

1. THE system SHALL use CentralLogger for all logging with appropriate log levels.
2. WHEN a database operation fails, THE system SHALL log the full stack trace at SEVERE level.
3. WHEN a player action fails, THE system SHALL send a user-friendly message without technical details.
4. THE system SHALL log command executions at FINE level for debugging.
5. IF the plugin fails to initialize, THEN THE system SHALL disable itself gracefully and log the reason.
