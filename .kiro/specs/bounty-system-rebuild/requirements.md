# Requirements Document

## Introduction

This document specifies the requirements for rebuilding the RDQ bounty system using the inventory-framework library. The bounty system allows players to place bounties on other players, offering rewards (items and currencies) for eliminating the target. The system includes bounty creation, management, claiming, leaderboards, and hunter statistics tracking. The rebuild will modernize the inventory UI implementation while maintaining all existing functionality and database persistence.

## Glossary

- **Bounty System**: The complete feature set allowing players to create, manage, and claim bounties on other players
- **RBounty**: The database entity representing a bounty with target, commissioner, rewards, and status
- **Bounty Hunter**: A player who claims bounties by eliminating bounty targets
- **BountyHunterStats**: The database entity tracking cumulative statistics for bounty hunters
- **Commissioner**: The player who creates and funds a bounty
- **Target**: The player who has a bounty placed on them
- **Reward Items**: Physical Minecraft items offered as bounty rewards
- **Reward Currencies**: Virtual currency amounts offered as bounty rewards
- **inventory-framework**: The third-party library used for creating interactive inventory UIs
- **BaseView**: The abstract base class for inventory views in the RDQ system
- **BountyService**: The service interface providing bounty operations with async database access
- **Claim Mode**: The method by which bounty kills are attributed (LAST_HIT, MOST_DAMAGE, DAMAGE_SPLIT)
- **Distribution Mode**: The method by which rewards are delivered (INSTANT, VIRTUAL, DROP, CHEST)
- **JExTranslate**: The internationalization system used for all user-facing messages
- **Static Bounty**: A pre-configured bounty loaded from configuration files (free edition feature)

## Requirements

### Requirement 1

**User Story:** As a player, I want to open the bounty main menu, so that I can access all bounty-related features from a central hub.

#### Acceptance Criteria

1. WHEN a player executes the bounty command THEN the Bounty System SHALL display the main menu inventory
2. WHEN the main menu renders THEN the Bounty System SHALL display navigation buttons for create, list, leaderboard, and my-bounties features
3. WHEN the main menu renders THEN the Bounty System SHALL use decorative glass panes to frame the interface
4. WHEN a player clicks a navigation button THEN the Bounty System SHALL open the corresponding view
5. WHEN the main menu is open THEN the Bounty System SHALL use the inventory-framework's layout system for slot positioning

### Requirement 2

**User Story:** As a player, I want to create a bounty on another player, so that I can incentivize others to eliminate my target.

#### Acceptance Criteria

1. WHEN a player opens the bounty creation view THEN the Bounty System SHALL display slots for target selection, item rewards, currency rewards, and confirmation
2. WHEN a player has not selected a target THEN the Bounty System SHALL disable the item and currency selection buttons
3. WHEN a player selects a target THEN the Bounty System SHALL enable the reward selection buttons
4. WHEN a player adds reward items THEN the Bounty System SHALL store them in the creation context state
5. WHEN a player confirms bounty creation with valid target and rewards THEN the Bounty System SHALL create the bounty asynchronously via BountyService
6. WHEN bounty creation succeeds THEN the Bounty System SHALL clear the player's inserted items from temporary storage
7. WHEN a player closes the creation view without confirming THEN the Bounty System SHALL refund all inserted items to the player's inventory
8. IF the player's inventory is full during refund THEN the Bounty System SHALL drop excess items at the player's location

### Requirement 3

**User Story:** As a player, I want to select a target player for my bounty, so that I can specify who should be eliminated.

#### Acceptance Criteria

1. WHEN a player clicks the target selection button THEN the Bounty System SHALL open a player selection view
2. WHEN the player selection view renders THEN the Bounty System SHALL display all online players as clickable heads
3. WHEN a player selects a target THEN the Bounty System SHALL update the creation view's target state
4. WHEN a target is selected THEN the Bounty System SHALL display the target's head and name in the creation view
5. WHEN a player attempts to select themselves as a target THEN the Bounty System SHALL prevent the selection and display an error message

### Requirement 4

**User Story:** As a player, I want to add item rewards to my bounty, so that I can offer physical items as incentives.

#### Acceptance Criteria

1. WHEN a player clicks the item reward button with a target selected THEN the Bounty System SHALL open the reward item selection view
2. WHEN the reward item view renders THEN the Bounty System SHALL display empty slots for item insertion
3. WHEN a player inserts items into reward slots THEN the Bounty System SHALL store them in the creation context
4. WHEN a player removes items from reward slots THEN the Bounty System SHALL update the stored reward items
5. WHEN the player returns to the creation view THEN the Bounty System SHALL preserve the inserted reward items
6. WHEN reward items are merged THEN the Bounty System SHALL combine similar items by stacking amounts

### Requirement 5

**User Story:** As a player, I want to add currency rewards to my bounty, so that I can offer virtual currency as incentives.

#### Acceptance Criteria

1. WHEN a player clicks the currency reward button with a target selected THEN the Bounty System SHALL open the currency selection view
2. WHEN the currency selection view renders THEN the Bounty System SHALL display available currencies from the economy system
3. WHEN a player selects a currency and amount THEN the Bounty System SHALL validate the player has sufficient balance
4. WHEN currency validation succeeds THEN the Bounty System SHALL add the currency to the bounty's reward currencies map
5. WHEN a player adds multiple currencies THEN the Bounty System SHALL accumulate them in the rewards map

### Requirement 6

**User Story:** As a player, I want to view all active bounties, so that I can see available targets and rewards.

#### Acceptance Criteria

1. WHEN a player opens the bounty list view THEN the Bounty System SHALL fetch active bounties asynchronously via BountyService
2. WHEN bounties are loaded THEN the Bounty System SHALL display them as paginated player heads
3. WHEN a bounty is displayed THEN the Bounty System SHALL show the target's head, name, reward summary, and expiration time
4. WHEN a player clicks a bounty entry THEN the Bounty System SHALL open the detailed bounty information view
5. WHEN there are more bounties than fit on one page THEN the Bounty System SHALL provide pagination controls
6. WHEN a player navigates pages THEN the Bounty System SHALL load and display the next page of bounties

### Requirement 7

**User Story:** As a player, I want to view detailed information about a specific bounty, so that I can see all rewards and bounty details.

#### Acceptance Criteria

1. WHEN a player opens a bounty detail view THEN the Bounty System SHALL display the target's information, commissioner, and all rewards
2. WHEN reward items are displayed THEN the Bounty System SHALL show each item with its quantity and type
3. WHEN reward currencies are displayed THEN the Bounty System SHALL show each currency name and amount
4. WHEN the bounty has an expiration time THEN the Bounty System SHALL display the remaining time
5. WHEN the bounty is claimed THEN the Bounty System SHALL display the claimer's name and claim timestamp
6. WHEN the bounty is expired THEN the Bounty System SHALL display an expired status indicator

### Requirement 8

**User Story:** As a player, I want to view the bounty hunter leaderboard, so that I can see top hunters and their statistics.

#### Acceptance Criteria

1. WHEN a player opens the leaderboard view THEN the Bounty System SHALL fetch top hunters asynchronously via BountyService
2. WHEN hunters are loaded THEN the Bounty System SHALL display them ranked by total bounties claimed
3. WHEN a hunter entry is displayed THEN the Bounty System SHALL show their rank, name, bounties claimed, and total reward value
4. WHEN the leaderboard updates THEN the Bounty System SHALL refresh the display with current statistics
5. WHEN a player views their own rank THEN the Bounty System SHALL highlight their entry in the leaderboard

### Requirement 9

**User Story:** As a player, I want to view my created bounties, so that I can track bounties I have commissioned.

#### Acceptance Criteria

1. WHEN a player opens the my-bounties view THEN the Bounty System SHALL fetch bounties created by the player via BountyService
2. WHEN the player's bounties are loaded THEN the Bounty System SHALL display them with status indicators
3. WHEN a bounty is active THEN the Bounty System SHALL show it with an active status indicator
4. WHEN a bounty is claimed THEN the Bounty System SHALL show it with a claimed status and claimer information
5. WHEN a bounty is expired THEN the Bounty System SHALL show it with an expired status indicator
6. WHEN a player clicks their bounty THEN the Bounty System SHALL open the detailed bounty information view

### Requirement 10

**User Story:** As a bounty hunter, I want bounties to be automatically claimed when I eliminate a target, so that I receive rewards without manual claiming.

#### Acceptance Criteria

1. WHEN a player with an active bounty is killed THEN the Bounty System SHALL determine the killer based on the configured claim mode
2. WHEN the claim mode is LAST_HIT THEN the Bounty System SHALL attribute the kill to the player who dealt the final blow
3. WHEN the claim mode is MOST_DAMAGE THEN the Bounty System SHALL attribute the kill to the player who dealt the most damage within the tracking window
4. WHEN the claim mode is DAMAGE_SPLIT THEN the Bounty System SHALL distribute rewards proportionally among all damage dealers
5. WHEN a bounty is claimed THEN the Bounty System SHALL mark it as claimed with the claimer's UUID and timestamp
6. WHEN a bounty is claimed THEN the Bounty System SHALL update the hunter's statistics via BountyService
7. WHEN a bounty is claimed THEN the Bounty System SHALL distribute rewards according to the configured distribution mode

### Requirement 11

**User Story:** As a bounty hunter, I want to receive rewards when I claim a bounty, so that I am compensated for eliminating the target.

#### Acceptance Criteria

1. WHEN the distribution mode is INSTANT THEN the Bounty System SHALL add reward items directly to the hunter's inventory
2. WHEN the distribution mode is VIRTUAL THEN the Bounty System SHALL credit rewards to the hunter's virtual storage
3. WHEN the distribution mode is DROP THEN the Bounty System SHALL drop reward items at the target's death location
4. WHEN the distribution mode is CHEST THEN the Bounty System SHALL place rewards in a chest at the death location
5. WHEN reward currencies are distributed THEN the Bounty System SHALL credit them to the hunter's economy balance
6. WHEN the hunter's inventory is full during INSTANT distribution THEN the Bounty System SHALL drop excess items at the hunter's location

### Requirement 12

**User Story:** As a system administrator, I want bounties to expire after a configured duration, so that stale bounties are automatically removed.

#### Acceptance Criteria

1. WHEN a bounty is created THEN the Bounty System SHALL set the expiration time based on the configured expiry-days setting
2. WHEN the system checks bounty status THEN the Bounty System SHALL mark expired bounties as inactive
3. WHEN a bounty expires THEN the Bounty System SHALL refund rewards to the commissioner if configured
4. WHEN an expired bounty is viewed THEN the Bounty System SHALL display it with an expired status
5. WHEN a player attempts to claim an expired bounty THEN the Bounty System SHALL prevent the claim

### Requirement 13

**User Story:** As a system administrator, I want bounty events to be announced, so that players are aware of bounty activity.

#### Acceptance Criteria

1. WHEN a bounty is created and announcements are enabled THEN the Bounty System SHALL broadcast a creation message using JExTranslate
2. WHEN a bounty is claimed and announcements are enabled THEN the Bounty System SHALL broadcast a claim message using JExTranslate
3. WHEN the broadcast radius is -1 THEN the Bounty System SHALL send announcements globally to all players
4. WHEN the broadcast radius is positive THEN the Bounty System SHALL send announcements only to players within the radius
5. WHEN announcements are disabled THEN the Bounty System SHALL not broadcast any bounty events
6. WHEN messages are displayed THEN the Bounty System SHALL use JExTranslate for all user-facing text with proper placeholder support

### Requirement 14

**User Story:** As a player, I want visual indicators on bounty targets, so that I can easily identify them in-game.

#### Acceptance Criteria

1. WHEN visual indicators are enabled and a player has an active bounty THEN the Bounty System SHALL apply the configured tab prefix to their tab list name
2. WHEN visual indicators are enabled and a player has an active bounty THEN the Bounty System SHALL apply the configured name color to their display name
3. WHEN particles are enabled and a player has an active bounty THEN the Bounty System SHALL spawn particles around them at the configured interval
4. WHEN a bounty is claimed or expires THEN the Bounty System SHALL remove all visual indicators from the target
5. WHEN a player logs out with an active bounty THEN the Bounty System SHALL preserve the bounty for when they return

### Requirement 15

**User Story:** As a developer, I want all bounty views to use the inventory-framework, so that the UI is consistent and maintainable.

#### Acceptance Criteria

1. WHEN any bounty view is created THEN the Bounty System SHALL extend the inventory-framework's view classes
2. WHEN views use state management THEN the Bounty System SHALL use inventory-framework's State and MutableState
3. WHEN views render items THEN the Bounty System SHALL use inventory-framework's slot and layout APIs
4. WHEN views handle clicks THEN the Bounty System SHALL use inventory-framework's onClick handlers
5. WHEN views navigate between each other THEN the Bounty System SHALL use inventory-framework's openForPlayer method
6. WHEN views need to update dynamically THEN the Bounty System SHALL use inventory-framework's computed states and watchers

### Requirement 16

**User Story:** As a developer, I want bounty operations to be asynchronous, so that database operations do not block the main server thread.

#### Acceptance Criteria

1. WHEN any bounty database operation is performed THEN the Bounty System SHALL execute it asynchronously via CompletableFuture
2. WHEN async operations complete THEN the Bounty System SHALL handle results on the appropriate thread
3. WHEN async operations fail THEN the Bounty System SHALL log errors and notify the user appropriately
4. WHEN views need database data THEN the Bounty System SHALL fetch it asynchronously and update the view when loaded
5. WHEN multiple async operations are chained THEN the Bounty System SHALL use CompletableFuture composition methods

### Requirement 17

**User Story:** As a developer, I want bounty hunter statistics to be tracked persistently, so that leaderboards and rankings are accurate.

#### Acceptance Criteria

1. WHEN a bounty is claimed THEN the Bounty System SHALL increment the hunter's bounties claimed counter
2. WHEN a bounty is claimed THEN the Bounty System SHALL add the reward value to the hunter's total reward value
3. WHEN a bounty is claimed THEN the Bounty System SHALL update the hunter's highest bounty value if the current bounty exceeds it
4. WHEN a bounty is claimed THEN the Bounty System SHALL update the hunter's last claim timestamp
5. WHEN hunter statistics are queried THEN the Bounty System SHALL fetch them asynchronously from the database
6. WHEN a new hunter claims their first bounty THEN the Bounty System SHALL create a new BountyHunterStats entity

### Requirement 18

**User Story:** As a developer, I want the bounty system to support both free and premium editions, so that features can be edition-gated appropriately.

#### Acceptance Criteria

1. WHEN the premium edition is active THEN the Bounty System SHALL use PremiumBountyService with full database persistence and unlimited bounties
2. WHEN the free edition is active THEN the Bounty System SHALL use FreeBountyService with in-memory storage and a global limit of 3 total active bounties
3. WHEN the free edition is active THEN the Bounty System SHALL support static pre-configured bounties loaded from configuration
4. WHEN edition-specific limits are checked THEN the Bounty System SHALL query the service's isPremium method
5. WHEN the maximum bounties is checked THEN the Bounty System SHALL return 3 for free edition and unlimited for premium
6. WHEN the maximum reward items is checked THEN the Bounty System SHALL return edition-specific limits

### Requirement 19

**User Story:** As a free edition user, I want to use static pre-configured bounties, so that I can experience bounty functionality without full dynamic creation.

#### Acceptance Criteria

1. WHEN the free edition loads THEN the Bounty System SHALL read static bounty configurations from the bounty.yml file
2. WHEN a static bounty is configured THEN the Bounty System SHALL include target UUID, reward items, and reward currencies
3. WHEN a player creates a bounty in free edition THEN the Bounty System SHALL only allow a maximum of 3 total active bounties globally
4. WHEN a free edition bounty is claimed or expires THEN the Bounty System SHALL allow creating a new bounty if under the limit
5. WHEN static bounties are used THEN the Bounty System SHALL validate that configured targets exist

### Requirement 20

**User Story:** As a developer, I want the bounty codebase to use modern Java patterns, so that the code is maintainable and efficient.

#### Acceptance Criteria

1. WHEN bounty entities are designed THEN the Bounty System SHALL use records for immutable data transfer objects
2. WHEN bounty services are implemented THEN the Bounty System SHALL use sealed interfaces to restrict implementation hierarchy
3. WHEN optional values are handled THEN the Bounty System SHALL use Optional consistently instead of null checks
4. WHEN collections are used THEN the Bounty System SHALL prefer immutable collections where appropriate
5. WHEN validation is performed THEN the Bounty System SHALL use pattern matching and switch expressions where applicable
6. WHEN async operations are composed THEN the Bounty System SHALL use CompletableFuture fluent API methods

### Requirement 21

**User Story:** As a developer, I want comprehensive unit tests for bounty functionality, so that the system is reliable and regressions are prevented.

#### Acceptance Criteria

1. WHEN bounty entities are modified THEN the Bounty System SHALL have unit tests validating entity behavior
2. WHEN bounty services are implemented THEN the Bounty System SHALL have unit tests for all service methods
3. WHEN bounty claim logic is implemented THEN the Bounty System SHALL have unit tests for each claim mode
4. WHEN reward distribution is implemented THEN the Bounty System SHALL have unit tests for each distribution mode
5. WHEN bounty expiration is implemented THEN the Bounty System SHALL have unit tests validating expiration logic
6. WHEN hunter statistics are updated THEN the Bounty System SHALL have unit tests validating stat calculations
