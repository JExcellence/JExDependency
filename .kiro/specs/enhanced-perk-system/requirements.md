# Requirements Document

## Introduction

This document defines the requirements for an enhanced perk system in the RDQ plugin. The perk system allows players to unlock, manage, and activate various gameplay perks through a requirement-based progression system. Perks provide passive effects, event-triggered abilities, and special gameplay modifications that enhance the player experience.

The system integrates with the existing rank system, requirement framework, reward system, database infrastructure (Hibernate), and i18n translation system using MiniMessage/Kyori components.

## Glossary

- **Perk System**: The complete feature that manages perk definitions, player perk ownership, activation states, and effects
- **Perk**: A gameplay enhancement or ability that can be unlocked and activated by players
- **Player Perk**: The association between a player and a perk, tracking ownership, enabled state, and usage statistics
- **Perk State**: The current status of a perk for a player (LOCKED, AVAILABLE, ACTIVE, COOLDOWN, DISABLED)
- **Perk Type**: The activation behavior category of a perk (PASSIVE, EVENT_TRIGGERED, COOLDOWN_BASED, PERCENTAGE_BASED)
- **Perk Category**: The thematic grouping of perks (COMBAT, MOVEMENT, UTILITY, SURVIVAL, ECONOMY, SOCIAL, COSMETIC, SPECIAL)
- **Requirement System**: The existing framework for defining and checking unlock conditions
- **Activation**: The process of enabling a perk's effects for a player
- **Deactivation**: The process of disabling a perk's effects for a player
- **Cooldown**: A time period during which a perk cannot be activated after use
- **Trigger Chance**: The probability that a percentage-based perk will activate when its event occurs

## Requirements

### Requirement 1

**User Story:** As a server administrator, I want to define perks with unlock requirements, so that players must progress through the game to access powerful abilities

#### Acceptance Criteria

1. WHEN the Perk System loads configuration files, THE Perk System SHALL parse perk definitions from YAML files in the perks directory
2. WHEN a perk definition includes requirements, THE Perk System SHALL convert requirement configurations to AbstractRequirement instances using the existing requirement framework
3. WHEN a perk definition includes rewards, THE Perk System SHALL convert reward configurations to AbstractReward instances using the existing reward framework
4. WHEN a perk definition is invalid, THE Perk System SHALL log a warning and skip that perk without crashing
5. WHEN all perk configurations are loaded, THE Perk System SHALL persist perk definitions to the database using Hibernate

### Requirement 2

**User Story:** As a player, I want to unlock perks by meeting requirements, so that I can earn new abilities through gameplay

#### Acceptance Criteria

1. WHEN a player meets all requirements for a perk, THE Perk System SHALL mark the perk as unlockable for that player
2. WHEN a player unlocks a perk, THE Perk System SHALL create a PlayerPerk association in the database
3. WHEN a player unlocks a perk, THE Perk System SHALL grant configured unlock rewards to the player
4. WHEN a player unlocks a perk, THE Perk System SHALL send a notification message to the player using i18n translations
5. WHEN a player views locked perks, THE Perk System SHALL display requirement progress using the existing requirement progress system

### Requirement 3

**User Story:** As a player, I want to enable and disable my unlocked perks, so that I can customize my active abilities

#### Acceptance Criteria

1. WHEN a player enables a perk, THE Perk System SHALL verify the player has not exceeded the maximum enabled perk limit
2. WHEN a player enables a perk, THE Perk System SHALL update the PlayerPerk enabled state in the database
3. WHEN a player disables a perk, THE Perk System SHALL update the PlayerPerk enabled state in the database
4. WHEN a player toggles a perk, THE Perk System SHALL activate or deactivate the perk effects accordingly
5. WHEN a player reaches the enabled perk limit, THE Perk System SHALL prevent enabling additional perks and display an error message

### Requirement 4

**User Story:** As a player, I want passive perks to automatically apply effects, so that I benefit from them without manual activation

#### Acceptance Criteria

1. WHEN a player enables a passive perk, THE Perk System SHALL immediately apply the perk effects to the player
2. WHILE a passive perk is enabled, THE Perk System SHALL maintain the perk effects continuously
3. WHEN a player disables a passive perk, THE Perk System SHALL remove the perk effects from the player
4. WHEN a player logs in with enabled passive perks, THE Perk System SHALL reapply all passive perk effects
5. WHEN a player logs out, THE Perk System SHALL persist the enabled state of passive perks

### Requirement 5

**User Story:** As a player, I want event-triggered perks to activate on specific actions, so that I can gain situational advantages

#### Acceptance Criteria

1. WHEN a configured event occurs for a player with an enabled event-triggered perk, THE Perk System SHALL check if the perk is on cooldown
2. WHEN an event-triggered perk is not on cooldown, THE Perk System SHALL activate the perk effect
3. WHEN an event-triggered perk activates, THE Perk System SHALL start the configured cooldown timer
4. WHEN an event-triggered perk is on cooldown, THE Perk System SHALL prevent activation and optionally notify the player
5. WHEN a cooldown expires, THE Perk System SHALL mark the perk as available for activation

### Requirement 6

**User Story:** As a player, I want percentage-based perks to have a chance to trigger, so that I experience varied and exciting gameplay

#### Acceptance Criteria

1. WHEN a configured event occurs for a player with an enabled percentage-based perk, THE Perk System SHALL generate a random number between 0 and 100
2. WHEN the random number is less than or equal to the configured trigger chance, THE Perk System SHALL activate the perk effect
3. WHEN the random number exceeds the trigger chance, THE Perk System SHALL not activate the perk effect
4. WHEN a percentage-based perk activates, THE Perk System SHALL apply the configured cooldown if specified
5. WHEN a percentage-based perk activates, THE Perk System SHALL optionally notify the player based on configuration

### Requirement 7

**User Story:** As a player, I want to view all available perks in a GUI similar to the rank system views, so that I can see what abilities I can unlock

#### Acceptance Criteria

1. WHEN a player opens the perk overview GUI, THE Perk System SHALL display all perks grouped by category using a paginated view similar to RankPathOverview
2. WHEN a player views a perk, THE Perk System SHALL display the perk icon, name, description, and state using i18n translations and card rendering similar to RequirementCardRenderer
3. WHEN a player views a locked perk, THE Perk System SHALL display requirement progress using the existing requirement progress system similar to RankRequirementsJourneyView
4. WHEN a player views an unlocked perk, THE Perk System SHALL display enable/disable toggle options with visual state indicators
5. WHEN a player clicks a perk, THE Perk System SHALL open a detailed view with full information, requirements, and actions similar to RankRewardsDetailView

### Requirement 8

**User Story:** As a player, I want to toggle perks from the GUI, so that I can easily manage my active abilities

#### Acceptance Criteria

1. WHEN a player clicks the enable button for an unlocked perk, THE Perk System SHALL enable the perk if the limit is not exceeded
2. WHEN a player clicks the disable button for an enabled perk, THE Perk System SHALL disable the perk
3. WHEN a player enables a perk, THE Perk System SHALL update the GUI to reflect the new state
4. WHEN a player disables a perk, THE Perk System SHALL update the GUI to reflect the new state
5. WHEN a perk state changes, THE Perk System SHALL play a sound effect and display visual feedback

### Requirement 9

**User Story:** As a server administrator, I want to configure perk limits and settings, so that I can balance the perk system for my server

#### Acceptance Criteria

1. WHEN the Perk System loads, THE Perk System SHALL read the perk-system.yml configuration file
2. WHEN the configuration specifies maximum enabled perks, THE Perk System SHALL enforce that limit per player
3. WHEN the configuration specifies global perk enable/disable, THE Perk System SHALL respect those settings
4. WHEN the configuration specifies cooldown multipliers, THE Perk System SHALL apply those multipliers to all cooldowns
5. WHEN the configuration is invalid, THE Perk System SHALL use default values and log a warning

### Requirement 10

**User Story:** As a server administrator, I want perks to support all potion effects, so that I can create diverse passive abilities

#### Acceptance Criteria

1. WHEN a perk is configured with a potion effect type, THE Perk System SHALL apply that potion effect when the perk is active
2. WHEN a perk specifies amplifier and duration, THE Perk System SHALL apply the potion effect with those parameters
3. WHEN a passive potion perk is enabled, THE Perk System SHALL continuously refresh the potion effect
4. WHEN a passive potion perk is disabled, THE Perk System SHALL remove the potion effect from the player
5. WHEN a player logs in with enabled potion perks, THE Perk System SHALL reapply all potion effects

### Requirement 11

**User Story:** As a server administrator, I want perks to support special abilities like fly, glow, and no fall damage, so that I can offer unique gameplay modifications

#### Acceptance Criteria

1. WHEN a fly perk is enabled, THE Perk System SHALL grant the player flight capability
2. WHEN a glow perk is enabled, THE Perk System SHALL apply the glowing effect to the player
3. WHEN a no fall damage perk is enabled, THE Perk System SHALL cancel fall damage events for the player
4. WHEN a keep inventory perk is enabled and the player dies, THE Perk System SHALL preserve the player inventory
5. WHEN a keep experience perk is enabled and the player dies, THE Perk System SHALL preserve the player experience

### Requirement 12

**User Story:** As a server administrator, I want to configure perks as rank rewards using a PERK reward type, so that players unlock perks through rank progression

#### Acceptance Criteria

1. WHEN the Reward System initializes, THE Perk System SHALL register a PERK reward type with the reward registry
2. WHEN a rank reward is configured with type PERK, THE Reward System SHALL parse the perk identifier from the configuration
3. WHEN a player achieves a rank with PERK rewards, THE Perk System SHALL grant the specified perks to the player
4. WHEN a perk is granted through a PERK reward, THE Perk System SHALL send an unlock notification to the player using i18n
5. WHEN a player already owns a perk from a PERK reward, THE Perk System SHALL skip granting that perk and log an info message

### Requirement 13

**User Story:** As a developer, I want the perk system to use Hibernate for persistence, so that it integrates with the existing database infrastructure

#### Acceptance Criteria

1. WHEN the Perk System initializes, THE Perk System SHALL create or update database tables using Hibernate entity mappings
2. WHEN a perk is created or updated, THE Perk System SHALL persist changes using Hibernate repositories
3. WHEN a PlayerPerk association is created, THE Perk System SHALL persist the relationship using Hibernate
4. WHEN perk data is queried, THE Perk System SHALL use Hibernate query methods with proper eager/lazy loading
5. WHEN the plugin shuts down, THE Perk System SHALL ensure all pending database operations complete

### Requirement 14

**User Story:** As a player, I want perk messages to use my language, so that I can understand perk information in my preferred language

#### Acceptance Criteria

1. WHEN the Perk System displays perk names, THE Perk System SHALL use i18n translation keys with MiniMessage formatting
2. WHEN the Perk System displays perk descriptions, THE Perk System SHALL use i18n translation keys with MiniMessage formatting
3. WHEN the Perk System sends notifications, THE Perk System SHALL use i18n translation keys with player-specific locale
4. WHEN the Perk System displays requirement text, THE Perk System SHALL use i18n translation keys from the requirement system
5. WHEN translation keys are missing, THE Perk System SHALL display the key name and log a warning

### Requirement 15

**User Story:** As a server administrator, I want to reload perk configurations without restarting, so that I can test changes quickly

#### Acceptance Criteria

1. WHEN an administrator executes the reload command, THE Perk System SHALL reload all perk configuration files
2. WHEN perk configurations are reloaded, THE Perk System SHALL update existing perk definitions in the database
3. WHEN perk configurations are reloaded, THE Perk System SHALL preserve player perk ownership and states
4. WHEN perk configurations are reloaded, THE Perk System SHALL reactivate enabled perks with new settings
5. WHEN a reload fails, THE Perk System SHALL maintain the previous configuration and log detailed error information
