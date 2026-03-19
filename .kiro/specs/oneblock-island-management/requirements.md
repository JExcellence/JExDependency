# Requirements Document

## Introduction

This document specifies the requirements for a comprehensive island management system for JExOneblock. The system includes a working implementation of IInfrastructureService, a new PIsland/POneblock command for island-centric operations, evolution overview views, visitor settings management, member management (kick, ban, invite), permission management for members/trusted players, and overall island settings configuration.

## Glossary

- **OneblockIsland**: The core island entity representing a player's oneblock island with associated data, members, and settings
- **IInfrastructureService**: Service interface providing infrastructure operations for islands including energy, storage, and automation
- **PIsland**: Player command for island management operations (alias: POneblock)
- **VisitorSettings**: Configuration entity controlling what visitors can do on an island
- **MemberRole**: Enumeration defining permission levels (VISITOR, MEMBER, TRUSTED, MODERATOR, CO_OWNER)
- **Evolution**: Progression stages that unlock new content and abilities on the island
- **ViewFrame**: Inventory-based GUI framework for displaying interactive menus

## Requirements

### Requirement 1: IInfrastructureService Implementation

**User Story:** As a plugin developer, I want a fully functional IInfrastructureService implementation, so that infrastructure features work correctly with the island system.

#### Acceptance Criteria

1. WHEN a player requests infrastructure data, THE IInfrastructureService SHALL return the IslandInfrastructure entity for the specified island within 100 milliseconds.
2. WHEN infrastructure data is requested asynchronously, THE IInfrastructureService SHALL return a CompletableFuture containing the infrastructure wrapped in an Optional.
3. WHEN the infrastructure manager is requested, THE IInfrastructureService SHALL return a non-null InfrastructureManager instance.
4. WHEN the tick processor is requested, THE IInfrastructureService SHALL return a non-null InfrastructureTickProcessor instance.
5. WHEN infrastructure is modified, THE IInfrastructureService SHALL persist changes to the database within 5 seconds.

### Requirement 2: PIsland Command Structure

**User Story:** As a player, I want a /island (or /oneblock) command, so that I can manage all aspects of my island from a single command.

#### Acceptance Criteria

1. WHEN a player executes /island without arguments, THE PIsland command SHALL open the main island management GUI.
2. WHEN a player executes /island with a valid subcommand, THE PIsland command SHALL execute the corresponding action.
3. WHEN a player without an island executes /island, THE PIsland command SHALL display a localized message indicating no island exists.
4. WHEN a player lacks permission for a subcommand, THE PIsland command SHALL display a localized permission denied message.
5. THE PIsland command SHALL support tab completion for all valid subcommands based on player permissions.

### Requirement 3: Island Main View

**User Story:** As a player, I want a main island GUI, so that I can see my island overview and access all management features.

#### Acceptance Criteria

1. WHEN the main island view opens, THE IslandMainView SHALL display the island name, level, and current evolution.
2. WHEN the main island view opens, THE IslandMainView SHALL display navigation buttons for members, settings, evolution, and visitor settings.
3. WHEN a player clicks a navigation button, THE IslandMainView SHALL open the corresponding sub-view.
4. THE IslandMainView SHALL display the island owner's head as a visual identifier.
5. THE IslandMainView SHALL update dynamically when island data changes.

### Requirement 4: Evolution Overview View

**User Story:** As a player, I want to see my evolution progress, so that I can track my advancement and see upcoming unlocks.

#### Acceptance Criteria

1. WHEN the evolution view opens, THE EvolutionOverviewView SHALL display the current evolution name and progress percentage.
2. WHEN the evolution view opens, THE EvolutionOverviewView SHALL display total blocks broken and blocks until next evolution.
3. THE EvolutionOverviewView SHALL display a visual progress bar representing evolution completion.
4. WHEN a player views evolution details, THE EvolutionOverviewView SHALL show unlockable content for the next evolution stage.
5. THE EvolutionOverviewView SHALL support pagination for viewing all available evolutions.

### Requirement 5: Visitor Settings Management

**User Story:** As an island owner, I want to configure visitor permissions, so that I can control what non-members can do on my island.

#### Acceptance Criteria

1. WHEN the visitor settings view opens, THE VisitorSettingsView SHALL display all configurable visitor permissions with their current state.
2. WHEN a player toggles a permission, THE VisitorSettingsView SHALL update the OneblockVisitorSettings entity immediately.
3. THE VisitorSettingsView SHALL provide preset buttons for "Allow All", "Deny All", "Basic", and "Trusted" permission sets.
4. WHILE a player lacks MODERATOR or higher role, THE VisitorSettingsView SHALL deny access to visitor settings modification.
5. WHEN visitor settings are modified, THE VisitorSettingsView SHALL persist changes to the database within 2 seconds.

### Requirement 6: Kick Non-Members

**User Story:** As an island member with appropriate permissions, I want to kick visitors from my island, so that I can remove unwanted players.

#### Acceptance Criteria

1. WHEN a player with MODERATOR or higher role executes kick, THE PIsland command SHALL teleport the target player to spawn.
2. WHEN a player attempts to kick an island member, THE PIsland command SHALL deny the action with a localized error message.
3. WHEN a player attempts to kick the island owner, THE PIsland command SHALL deny the action with a localized error message.
4. WHEN a player is kicked, THE PIsland command SHALL send localized notifications to both the kicker and the kicked player.
5. IF the target player is not on the island, THEN THE PIsland command SHALL display a localized message indicating the player is not present.

### Requirement 7: Ban Non-Members

**User Story:** As an island member with appropriate permissions, I want to ban players from my island, so that they cannot return.

#### Acceptance Criteria

1. WHEN a player with MODERATOR or higher role executes ban, THE PIsland command SHALL create a OneblockIslandBan entity for the target player.
2. WHEN a banned player attempts to enter the island, THE OneblockIsland SHALL deny entry and display a localized ban message.
3. WHEN a player is banned, THE PIsland command SHALL remove them from the island if currently present.
4. THE PIsland command SHALL support optional ban duration and reason parameters.
5. WHEN a player attempts to ban an island member or owner, THE PIsland command SHALL deny the action with a localized error message.

### Requirement 8: Unban Players

**User Story:** As an island member with appropriate permissions, I want to unban players, so that they can visit my island again.

#### Acceptance Criteria

1. WHEN a player with MODERATOR or higher role executes unban, THE PIsland command SHALL deactivate the OneblockIslandBan entity.
2. WHEN a player is unbanned, THE PIsland command SHALL record the unbanning player and timestamp.
3. THE PIsland command SHALL provide a view listing all banned players with ban details.
4. IF the target player is not banned, THEN THE PIsland command SHALL display a localized message indicating no active ban exists.

### Requirement 9: Invite Players

**User Story:** As an island member with appropriate permissions, I want to invite players to my island, so that they can become members.

#### Acceptance Criteria

1. WHEN a player with TRUSTED or higher role executes invite, THE PIsland command SHALL create a pending OneblockIslandMember entity.
2. WHEN an invitation is sent, THE PIsland command SHALL notify the target player with a clickable accept/decline message.
3. WHEN a player accepts an invitation, THE OneblockIslandMember entity SHALL update with joined timestamp and active status.
4. WHEN a player declines an invitation, THE OneblockIslandMember entity SHALL be removed.
5. IF the target player is already a member or banned, THEN THE PIsland command SHALL deny the invitation with a localized error message.
6. THE PIsland command SHALL support invitation expiration after a configurable duration.

### Requirement 10: Member Permission View

**User Story:** As an island owner or co-owner, I want to manage member permissions, so that I can control what each member can do.

#### Acceptance Criteria

1. WHEN the member permission view opens, THE MemberPermissionView SHALL display all island members with their current roles.
2. WHEN a player clicks on a member, THE MemberPermissionView SHALL open a role selection interface.
3. WHEN a role is changed, THE MemberPermissionView SHALL update the OneblockIslandMember entity immediately.
4. WHILE a player has lower role than the target member, THE MemberPermissionView SHALL deny role modification.
5. THE MemberPermissionView SHALL prevent demoting players to a role higher than the modifier's role.
6. WHEN viewing member details, THE MemberPermissionView SHALL display join date, invited by, and activity status.

### Requirement 11: Remove Members

**User Story:** As an island owner or co-owner, I want to remove members from my island, so that I can manage my island team.

#### Acceptance Criteria

1. WHEN a player with CO_OWNER or higher role executes remove, THE PIsland command SHALL deactivate the OneblockIslandMember entity.
2. WHEN a member is removed, THE PIsland command SHALL teleport them off the island if currently present.
3. WHEN a member is removed, THE PIsland command SHALL send localized notifications to both parties.
4. WHILE a player attempts to remove someone with equal or higher role, THE PIsland command SHALL deny the action.
5. THE PIsland command SHALL prevent the owner from being removed.

### Requirement 12: Island Settings View

**User Story:** As an island owner, I want to configure island settings, so that I can customize my island behavior.

#### Acceptance Criteria

1. WHEN the settings view opens, THE IslandSettingsView SHALL display configurable island options including name, description, and privacy.
2. WHEN a player modifies the island name, THE IslandSettingsView SHALL validate the name length and content.
3. WHEN privacy is toggled, THE IslandSettingsView SHALL update the OneblockIsland privacy field immediately.
4. WHILE a player lacks CO_OWNER or higher role, THE IslandSettingsView SHALL deny access to settings modification.
5. THE IslandSettingsView SHALL provide an option to reset the island with confirmation dialog.

### Requirement 13: Banned Players View

**User Story:** As an island moderator, I want to see all banned players, so that I can manage island bans effectively.

#### Acceptance Criteria

1. WHEN the banned players view opens, THE BannedPlayersView SHALL display all active bans with player name, reason, and date.
2. THE BannedPlayersView SHALL support pagination for islands with many bans.
3. WHEN a player clicks on a ban entry, THE BannedPlayersView SHALL show options to unban or view details.
4. THE BannedPlayersView SHALL display ban expiration time for temporary bans.
5. THE BannedPlayersView SHALL visually distinguish between permanent and temporary bans.

### Requirement 14: Members List View

**User Story:** As an island member, I want to see all island members, so that I can know who has access to the island.

#### Acceptance Criteria

1. WHEN the members view opens, THE MembersListView SHALL display all active members with their roles and online status.
2. THE MembersListView SHALL display the island owner prominently at the top.
3. THE MembersListView SHALL support pagination for islands with many members.
4. WHEN a player with management permissions clicks a member, THE MembersListView SHALL show management options.
5. THE MembersListView SHALL display member join date and last activity.
