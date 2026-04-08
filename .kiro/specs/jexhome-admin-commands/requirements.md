# Requirements Document

## Introduction

This feature adds administrative commands to JExHome that allow server administrators to create and delete homes for other players, including offline players. This enables server staff to manage player homes without requiring the target player to be online, useful for support scenarios, server maintenance, and administrative tasks.

## Glossary

- **JExHome**: The home management plugin that allows players to set, teleport to, and manage personal home locations
- **Admin**: A server operator or staff member with elevated permissions to manage other players' homes
- **Target Player**: The player whose home is being created or deleted by an admin
- **Offline Player**: A player who has previously joined the server but is not currently online
- **HomeFactory**: The central factory class that manages home creation, deletion, and caching
- **IHomeService**: The service interface that handles home CRUD operations and business logic

## Requirements

### Requirement 1: Admin Set Home Command

**User Story:** As a server administrator, I want to create homes for other players (online or offline), so that I can assist players with home management without requiring them to be present.

#### Acceptance Criteria

1. WHEN an admin executes `/admin sethome <player> <name>`, THE JExHome System SHALL create a home with the specified name at the admin's current location for the target player.
2. WHEN the target player does not exist in the database, THE JExHome System SHALL display an error message indicating the player was not found.
3. WHEN the home name already exists for the target player, THE JExHome System SHALL overwrite the existing home location and display a confirmation message.
4. WHEN the admin lacks the required permission, THE JExHome System SHALL deny the command execution and display a permission denied message.
5. THE JExHome System SHALL support both online and offline players as valid targets for the admin sethome command.

### Requirement 2: Admin Delete Home Command

**User Story:** As a server administrator, I want to delete homes belonging to other players (online or offline), so that I can clean up or correct home data without requiring the player to be present.

#### Acceptance Criteria

1. WHEN an admin executes `/admin delhome <player> <name>`, THE JExHome System SHALL delete the specified home belonging to the target player.
2. WHEN the specified home does not exist for the target player, THE JExHome System SHALL display an error message indicating the home was not found.
3. WHEN the target player does not exist in the database, THE JExHome System SHALL display an error message indicating the player was not found.
4. WHEN the admin lacks the required permission, THE JExHome System SHALL deny the command execution and display a permission denied message.
5. THE JExHome System SHALL support both online and offline players as valid targets for the admin delhome command.

### Requirement 3: Tab Completion Support

**User Story:** As a server administrator, I want tab completion for admin home commands, so that I can quickly find player names and home names without typing them manually.

#### Acceptance Criteria

1. WHEN an admin types `/admin sethome ` and presses tab, THE JExHome System SHALL suggest online player names matching the partial input.
2. WHEN an admin types `/admin delhome ` and presses tab, THE JExHome System SHALL suggest online player names matching the partial input.
3. WHEN an admin types `/admin delhome <player> ` and presses tab, THE JExHome System SHALL suggest home names belonging to the specified player.
4. THE JExHome System SHALL filter tab completion suggestions based on the current partial input.

### Requirement 4: Permission System Integration

**User Story:** As a server administrator, I want granular permissions for admin home commands, so that I can control which staff members can manage player homes.

#### Acceptance Criteria

1. THE JExHome System SHALL require the permission `jexhome.admin.sethome` for executing the admin sethome command.
2. THE JExHome System SHALL require the permission `jexhome.admin.delhome` for executing the admin delhome command.
3. WHEN a user without the required permission attempts to execute an admin command, THE JExHome System SHALL display a localized permission denied message.
