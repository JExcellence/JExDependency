# Requirements Document

## Introduction

This document specifies the requirements for adding Bedrock Edition player support to JExHome's GUI system. Bedrock players connecting via Geyser/Floodgate have different UI capabilities and limitations compared to Java Edition players. The system shall detect Bedrock players and provide an optimized user experience using Bedrock Forms instead of chest-based GUIs.

## Glossary

- **Bedrock_Player**: A player connecting to the server using Minecraft Bedrock Edition via Geyser/Floodgate
- **Java_Player**: A player connecting to the server using Minecraft Java Edition
- **Floodgate**: A plugin that allows Bedrock players to join Java servers without Java accounts
- **Geyser**: A proxy that translates between Bedrock and Java Edition protocols
- **Bedrock_Form**: Native Bedrock UI elements (SimpleForm, ModalForm, CustomForm) that provide better UX for Bedrock players
- **Chest_GUI**: Traditional inventory-based GUI used for Java Edition players
- **JExHome_System**: The home management plugin system

## Requirements

### Requirement 1: Bedrock Player Detection

**User Story:** As a server administrator, I want the system to automatically detect Bedrock players, so that they receive an optimized UI experience.

#### Acceptance Criteria

1. WHEN a player executes a home command, THE JExHome_System SHALL detect whether the player is a Bedrock_Player within 50 milliseconds.
2. THE JExHome_System SHALL use the Floodgate API to determine if a player is a Bedrock_Player.
3. IF Floodgate is not installed, THEN THE JExHome_System SHALL treat all players as Java_Players.
4. THE JExHome_System SHALL cache the Bedrock_Player detection result for the duration of the player session.

### Requirement 2: Bedrock Form Integration

**User Story:** As a Bedrock player, I want to see native Bedrock forms instead of chest GUIs, so that I can navigate the home system more easily.

#### Acceptance Criteria

1. WHEN a Bedrock_Player opens the home overview, THE JExHome_System SHALL display a SimpleForm listing all homes.
2. WHEN a Bedrock_Player creates a new home, THE JExHome_System SHALL display a CustomForm with a text input field for the home name.
3. WHEN a Bedrock_Player confirms a home deletion, THE JExHome_System SHALL display a ModalForm with confirm/cancel buttons.
4. THE JExHome_System SHALL display home information (world, coordinates, visit count) in the form button descriptions.

### Requirement 3: Java Player Compatibility

**User Story:** As a Java player, I want to continue using the existing chest-based GUI, so that my experience remains unchanged.

#### Acceptance Criteria

1. WHEN a Java_Player executes a home command, THE JExHome_System SHALL display the existing Chest_GUI.
2. THE JExHome_System SHALL not modify the existing Java_Player GUI behavior.
3. THE JExHome_System SHALL maintain feature parity between Bedrock_Form and Chest_GUI interfaces.

### Requirement 4: Graceful Degradation

**User Story:** As a server administrator, I want the system to work even if Floodgate/Geyser is not installed, so that the plugin remains functional on Java-only servers.

#### Acceptance Criteria

1. IF Floodgate is not available, THEN THE JExHome_System SHALL log an informational message at startup.
2. IF Floodgate is not available, THEN THE JExHome_System SHALL use Chest_GUI for all players.
3. THE JExHome_System SHALL not throw exceptions when Floodgate classes are not present.

### Requirement 5: Configuration Options

**User Story:** As a server administrator, I want to configure Bedrock GUI behavior, so that I can customize the experience for my server.

#### Acceptance Criteria

1. THE JExHome_System SHALL provide a configuration option to enable or disable Bedrock_Form support.
2. THE JExHome_System SHALL provide a configuration option to force all players to use Chest_GUI.
3. WHEN configuration is changed, THE JExHome_System SHALL apply changes without requiring a server restart.
