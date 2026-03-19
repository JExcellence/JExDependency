# Requirements Document

## Introduction

This specification addresses the need for a player perk caching system to reduce database traffic and eliminate transaction conflicts. Currently, every perk operation queries the database directly, causing performance issues and `RollbackException` errors under concurrent load. The solution is to load all player perks into memory when a player joins, operate on the cached data during gameplay, and persist changes back to the database when the player disconnects.

## Glossary

- **Perk Cache**: An in-memory storage system that holds PlayerPerk entities for online players
- **PlayerPerk Entity**: A JPA entity representing a player's association with a perk, including state (active, enabled, unlocked) and statistics
- **Perk System**: The RDQ module that manages player perks including activation, deactivation, and state persistence
- **PerkManagementService**: The service class responsible for managing perk ownership and state
- **PerkActivationService**: The service class responsible for activating and deactivating perks
- **Cache Invalidation**: The process of removing stale data from the cache
- **Dirty Tracking**: Mechanism to track which cached entities have been modified and need database persistence

## Requirements

### Requirement 1

**User Story:** As a player, I want my perk operations to be fast and reliable, so that I can toggle perks without delays or errors

#### Acceptance Criteria

1. WHEN a player joins the server, THE Perk System SHALL load all PlayerPerk entities for that player into memory
2. WHEN a player performs perk operations, THE Perk System SHALL read from and write to the in-memory cache
3. WHEN a player disconnects from the server, THE Perk System SHALL persist all modified PlayerPerk entities to the database
4. THE Perk System SHALL complete cache loading before the player can interact with the perk system
5. THE Perk System SHALL handle cache loading failures gracefully without blocking player login

### Requirement 2

**User Story:** As a system administrator, I want the perk cache to track modifications efficiently, so that only changed data is written to the database

#### Acceptance Criteria

1. THE Perk System SHALL track which PlayerPerk entities have been modified since loading
2. WHEN a player disconnects, THE Perk System SHALL persist only the modified PlayerPerk entities
3. THE Perk System SHALL clear the dirty flag after successful database persistence
4. WHEN a persistence operation fails, THE Perk System SHALL retry the operation with exponential backoff
5. THE Perk System SHALL log all cache persistence operations with player context

### Requirement 3

**User Story:** As a developer, I want the cache to be thread-safe, so that concurrent perk operations don't cause data corruption

#### Acceptance Criteria

1. THE Perk System SHALL use thread-safe data structures for the perk cache
2. WHEN multiple threads access the same player's perk cache, THE Perk System SHALL synchronize access to prevent race conditions
3. THE Perk System SHALL ensure atomic read-modify-write operations on cached entities
4. THE Perk System SHALL prevent concurrent modification exceptions during cache operations
5. THE Perk System SHALL use appropriate locking mechanisms with minimal contention

### Requirement 4

**User Story:** As a system administrator, I want the cache to handle edge cases properly, so that data integrity is maintained

#### Acceptance Criteria

1. WHEN a player's cache fails to load on join, THE Perk System SHALL allow the player to join but disable perk operations
2. WHEN a player's cache fails to persist on disconnect, THE Perk System SHALL log the failure and attempt to save critical data
3. WHEN the server shuts down, THE Perk System SHALL persist all cached data for all online players
4. WHEN a player reconnects quickly after disconnect, THE Perk System SHALL handle the cache state transition correctly
5. THE Perk System SHALL provide administrative commands to manually flush the cache for specific players

### Requirement 5

**User Story:** As a developer, I want the cache implementation to be transparent to existing code, so that minimal refactoring is required

#### Acceptance Criteria

1. THE Perk System SHALL maintain the existing public API of PerkManagementService
2. THE Perk System SHALL maintain the existing public API of PerkActivationService
3. THE Perk System SHALL implement caching internally without requiring changes to UI code
4. THE Perk System SHALL provide a cache abstraction layer that can be easily tested
5. THE Perk System SHALL allow configuration to enable or disable caching via config file

### Requirement 6

**User Story:** As a system administrator, I want visibility into cache performance, so that I can monitor and optimize the system

#### Acceptance Criteria

1. THE Perk System SHALL log cache hit and miss statistics
2. THE Perk System SHALL log cache load times for each player
3. THE Perk System SHALL log cache persistence times and success rates
4. THE Perk System SHALL provide metrics for cache size and memory usage
5. THE Perk System SHALL log warnings when cache operations exceed performance thresholds
