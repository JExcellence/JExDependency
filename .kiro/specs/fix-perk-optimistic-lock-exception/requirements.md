# Requirements Document

## Introduction

This specification addresses the `OptimisticLockException` occurring in the RDQ perk system when multiple concurrent transactions attempt to update the same `PlayerPerk` entity. The system currently fails when a player toggles a perk (activate/deactivate) and another transaction has modified the entity since it was loaded. This results in failed perk state changes and poor user experience.

## Glossary

- **Perk System**: The RDQ module that manages player perks including activation, deactivation, and state persistence
- **PlayerPerk Entity**: A JPA entity representing a player's association with a perk, including state (active, enabled, unlocked) and statistics
- **OptimisticLockException**: A JPA exception thrown when an entity's version has changed since it was loaded, indicating a concurrent modification conflict
- **PerkActivationService**: The service class responsible for activating and deactivating perks
- **PlayerPerkRepository**: The repository interface for database operations on PlayerPerk entities
- **Retry Strategy**: A mechanism to automatically retry failed operations with exponential backoff

## Requirements

### Requirement 1

**User Story:** As a player, I want my perk toggle actions to succeed reliably even when the server is under load, so that I can enable or disable perks without errors

#### Acceptance Criteria

1. WHEN a player toggles a perk AND another transaction has modified the PlayerPerk entity, THE Perk System SHALL retry the operation with fresh entity data
2. WHEN the Perk System retries a failed update operation, THE Perk System SHALL reload the PlayerPerk entity from the database before attempting the update
3. WHEN the Perk System performs a retry, THE Perk System SHALL apply exponential backoff with a maximum of 3 retry attempts
4. WHEN all retry attempts are exhausted, THE Perk System SHALL log the failure with complete context and notify the player with a user-friendly error message
5. WHEN a perk toggle operation succeeds after retry, THE Perk System SHALL log the retry count for monitoring purposes

### Requirement 2

**User Story:** As a system administrator, I want detailed logging of concurrency conflicts, so that I can monitor system health and identify patterns

#### Acceptance Criteria

1. WHEN an OptimisticLockException occurs, THE Perk System SHALL log the exception with player name, perk identifier, and entity version information
2. WHEN a retry attempt is made, THE Perk System SHALL log the retry attempt number and the reason for retry
3. WHEN a perk operation fails after all retries, THE Perk System SHALL log the complete failure with stack trace at SEVERE level
4. WHEN a perk operation succeeds after retry, THE Perk System SHALL log the success at INFO level with retry count
5. THE Perk System SHALL use the centralized logging system for all perk-related log messages

### Requirement 3

**User Story:** As a developer, I want the retry logic to be reusable across different repository operations, so that other parts of the system can benefit from the same concurrency handling

#### Acceptance Criteria

1. THE Perk System SHALL implement retry logic in the BaseRepository or a dedicated utility class
2. THE Perk System SHALL provide a generic retry method that accepts a database operation as a parameter
3. THE Perk System SHALL allow configuration of retry parameters including maximum attempts and backoff multiplier
4. THE Perk System SHALL handle both OptimisticLockException and StaleObjectStateException in the retry logic
5. THE Perk System SHALL ensure the retry mechanism is thread-safe for concurrent operations

### Requirement 4

**User Story:** As a player, I want to receive clear feedback when a perk operation fails, so that I understand what happened and can take appropriate action

#### Acceptance Criteria

1. WHEN a perk toggle operation fails after all retries, THE Perk System SHALL send a localized error message to the player
2. WHEN a perk toggle operation is being retried, THE Perk System SHALL NOT send intermediate messages to the player
3. WHEN a perk toggle operation succeeds, THE Perk System SHALL send a success confirmation message to the player
4. THE Perk System SHALL ensure error messages do not expose technical details to players
5. THE Perk System SHALL provide different error messages for temporary failures versus permanent failures

### Requirement 5

**User Story:** As a system architect, I want the transaction handling to be correct and consistent, so that data integrity is maintained under concurrent load

#### Acceptance Criteria

1. WHEN the Perk System retries an operation, THE Perk System SHALL ensure the previous transaction is properly closed before starting a new one
2. WHEN reloading an entity for retry, THE Perk System SHALL use a fresh transaction to fetch the latest data
3. WHEN applying state changes during retry, THE Perk System SHALL reapply the intended changes to the freshly loaded entity
4. THE Perk System SHALL ensure that entity state changes (recordActivation, recordDeactivation) are idempotent
5. THE Perk System SHALL prevent cascading failures by isolating each retry attempt in its own transaction context
