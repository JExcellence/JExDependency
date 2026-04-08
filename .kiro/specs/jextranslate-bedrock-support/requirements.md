# Requirements Document

## Introduction

This document specifies the requirements for adding Bedrock Edition player support to JExTranslate. Bedrock Edition clients have limited text formatting capabilities compared to Java Edition - they only support legacy color codes (§ codes) and plain strings, not MiniMessage or Adventure Components. The system shall automatically detect Bedrock players via the GeyserService from RPlatform and convert messages to a Bedrock-compatible format before sending.

## Glossary

- **JExTranslate**: The translation and internationalization library for Minecraft plugins in the RaindropCentral ecosystem
- **Bedrock Edition**: The version of Minecraft that runs on mobile devices, consoles, and Windows 10/11 (via Bedrock)
- **Java Edition**: The original version of Minecraft that runs on desktop computers
- **Floodgate**: A plugin that allows Bedrock players to join Java Edition servers through Geyser
- **GeyserService**: The service in RPlatform that detects Bedrock players via Floodgate
- **MiniMessage**: A modern text formatting syntax used by Adventure for rich text
- **Adventure Component**: A rich text object that supports colors, formatting, click events, and hover events
- **Legacy Color Codes**: The original Minecraft color formatting using § or & followed by a color/format code
- **MessageBuilder**: The fluent API class in JExTranslate for building and sending messages
- **VersionedMessageSender**: The class responsible for sending messages with version-appropriate formatting

## Requirements

### Requirement 1: Bedrock Player Detection Integration

**User Story:** As a plugin developer, I want JExTranslate to automatically detect Bedrock players, so that messages are formatted appropriately without manual checks.

#### Acceptance Criteria

1. WHEN the JExTranslate system initializes, THE JExTranslate system SHALL attempt to obtain a GeyserService instance from the RPlatform ServiceRegistry.
2. WHILE the GeyserService is available, THE JExTranslate system SHALL use it to detect Bedrock players when sending messages.
3. IF the GeyserService is unavailable, THEN THE JExTranslate system SHALL treat all players as Java Edition players and continue normal operation.

### Requirement 2: Automatic Message Format Conversion for Bedrock

**User Story:** As a plugin developer, I want messages sent to Bedrock players to be automatically converted to a compatible format, so that Bedrock players see properly formatted text.

#### Acceptance Criteria

1. WHEN a message is sent to a Bedrock player, THE VersionedMessageSender SHALL convert the Adventure Component to a legacy string using section (§) color codes.
2. WHEN converting messages for Bedrock players, THE JExTranslate system SHALL strip unsupported formatting such as click events, hover events, and gradients.
3. WHEN converting messages for Bedrock players, THE JExTranslate system SHALL preserve basic color codes (§0-§9, §a-§f) and format codes (§l, §m, §n, §o, §r).
4. WHILE a player is detected as Bedrock Edition, THE MessageBuilder SHALL use the Bedrock-compatible message path when calling send methods.

### Requirement 3: Bedrock-Specific String Methods

**User Story:** As a plugin developer, I want dedicated methods to get Bedrock-formatted strings, so that I can use them in Bedrock forms and other Bedrock-specific contexts.

#### Acceptance Criteria

1. THE MessageBuilder SHALL provide a `toBedrockString(Player)` method that returns a legacy-formatted string suitable for Bedrock clients.
2. THE MessageBuilder SHALL provide a `toBedrockStrings(Player)` method that returns a list of legacy-formatted strings for multi-line messages.
3. WHEN `toBedrockString` is called, THE JExTranslate system SHALL convert MiniMessage formatting to legacy color codes.
4. WHEN `toBedrockString` is called, THE JExTranslate system SHALL strip hex colors and convert them to the nearest legacy color equivalent.

### Requirement 4: Bedrock Detection Caching

**User Story:** As a server administrator, I want Bedrock player detection to be efficient, so that message sending does not cause performance issues.

#### Acceptance Criteria

1. WHEN a player's Bedrock status is first checked, THE JExTranslate system SHALL cache the result for the duration of the player's session.
2. WHEN a player disconnects, THE JExTranslate system SHALL remove the cached Bedrock status for that player.
3. THE JExTranslate system SHALL provide a method to manually clear the Bedrock status cache if needed.

### Requirement 5: Configuration Options

**User Story:** As a server administrator, I want to configure Bedrock message handling behavior, so that I can customize how messages appear for Bedrock players.

#### Acceptance Criteria

1. THE R18nConfiguration SHALL include a `bedrockSupportEnabled` option that defaults to true.
2. IF `bedrockSupportEnabled` is false, THEN THE JExTranslate system SHALL treat all players as Java Edition players regardless of actual client type.
3. THE R18nConfiguration SHALL include a `bedrockHexColorFallback` option to specify how hex colors are handled (strip, nearest-legacy, or grayscale).
