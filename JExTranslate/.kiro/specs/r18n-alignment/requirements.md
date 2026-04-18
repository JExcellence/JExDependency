# Requirements Document

## Introduction

This document specifies the requirements for aligning the current JExTranslate implementation with the original R18n project architecture. The goal is to restore the proven patterns, simplicity, and reliability of the original R18n system while maintaining Java 17+ modern features. The alignment focuses on matching the original class structure, API patterns, version-aware messaging, and enhanced command UI with MiniMessage gradients.

## Glossary

- **R18n**: The original translation library providing the reference implementation patterns
- **R18nManager**: Modern manager class with builder pattern for configuring the translation system
- **I18n**: Fluent builder entry point for creating and sending localized messages
- **VersionDetector**: Component that detects server type (Paper/Spigot/Bukkit) and Minecraft version
- **VersionedMessageSender**: Version-aware message sender supporting both legacy (1.8-1.12) and modern (1.13+) servers
- **II18nVersionWrapper**: Interface for version-specific message handling implementations
- **UniversalI18nWrapper**: Universal wrapper using Adventure API for all modern versions
- **MiniMessage**: Adventure API's text formatting system with gradient and tag support
- **BukkitAudiences**: Adventure platform adapter for Bukkit/Spigot servers
- **Locale**: Language/region identifier using full codes (e.g., `en_GB`, `de_DE`, `ja_JP`)

## Requirements

### Requirement 1: R18n Legacy Compatibility Class

**User Story:** As a plugin developer, I want a backward-compatible R18n class so that existing plugins using the original API continue to work without modification.

#### Acceptance Criteria

1. THE R18n class SHALL provide a static `r18n` instance accessible globally after initialization.
2. WHEN R18n is constructed, THE R18n class SHALL initialize BukkitAudiences for Adventure platform support.
3. THE R18n class SHALL store translations in a static `Map<String, Map<String, List<String>>>` structure where the outer key is the translation key, inner key is locale code, and value is message lines.
4. THE R18n class SHALL provide `getTranslations()` returning the static translations map.
5. THE R18n class SHALL provide `getDefaultLocale()` returning the configured default locale string.
6. THE R18n class SHALL provide `getAudiences()` returning the BukkitAudiences instance.
7. WHEN `loadTranslations()` is called, THE R18n class SHALL load YAML files from the translations directory and merge them into the static map.
8. THE R18n class SHALL extract bundled translation files from the plugin JAR if they do not exist in the data folder.

### Requirement 2: R18nManager Modern Builder API

**User Story:** As a plugin developer, I want a modern R18nManager with builder pattern so that I can configure the translation system with fluent, type-safe API.

#### Acceptance Criteria

1. THE R18nManager class SHALL provide a static `builder(JavaPlugin)` method returning a Builder instance.
2. THE Builder SHALL provide `defaultLocale(String)` for setting the default locale.
3. THE Builder SHALL provide `supportedLocales(String...)` for configuring supported locale codes.
4. THE Builder SHALL provide `enableKeyValidation(boolean)` for toggling key validation.
5. THE Builder SHALL provide `enablePlaceholderAPI(boolean)` for toggling PlaceholderAPI integration.
6. THE Builder SHALL provide `translationDirectory(String)` for setting the translations folder name.
7. WHEN `build()` is called, THE Builder SHALL return a configured R18nManager instance.
8. THE R18nManager SHALL provide `initialize()` returning `CompletableFuture<Void>` for async initialization.
9. THE R18nManager SHALL provide `message(String key)` returning a MessageBuilder for fluent message creation.
10. THE R18nManager SHALL provide `reload()` returning `CompletableFuture<Void>` for hot-reloading translations.
11. THE R18nManager SHALL provide `shutdown()` for releasing Adventure platform resources.

### Requirement 3: I18n Fluent Builder Entry Point

**User Story:** As a plugin developer, I want a simple I18n builder class so that I can create and send localized messages with minimal boilerplate.

#### Acceptance Criteria

1. THE I18n class SHALL provide a nested Builder class for fluent message construction.
2. THE Builder SHALL accept a translation key and optional Player in its constructor.
3. THE Builder SHALL provide `withPlaceholder(String key, Object value)` for adding single placeholders.
4. THE Builder SHALL provide `withPlaceholders(Map<String, Object>)` for adding multiple placeholders.
5. THE Builder SHALL provide `includePrefix()` for prepending the configured prefix.
6. THE Builder SHALL provide `sendMessage()` for sending to the player's chat.
7. THE Builder SHALL provide `sendMultiple()` for sending multi-line messages.
8. THE Builder SHALL provide `component()` returning the formatted Component.
9. THE Builder SHALL provide `children()` returning `List<Component>` for multi-line messages.
10. THE Builder SHALL delegate to the appropriate II18nVersionWrapper based on server version.

### Requirement 4: Version Detection System

**User Story:** As a plugin developer, I want automatic server version detection so that the translation system works correctly across all Minecraft versions from 1.8 to 1.21+.

#### Acceptance Criteria

1. THE VersionDetector class SHALL detect Paper servers by checking for Paper-specific classes.
2. THE VersionDetector class SHALL detect Purpur servers by checking for Purpur configuration classes.
3. THE VersionDetector class SHALL detect Folia servers by checking for regionized server classes.
4. THE VersionDetector class SHALL detect Spigot servers by checking for SpigotConfig class.
5. THE VersionDetector class SHALL fall back to Bukkit detection when no other server type is found.
6. THE VersionDetector class SHALL parse the Minecraft version from server package name.
7. THE VersionDetector class SHALL provide `isModern()` returning true for 1.13+ servers.
8. THE VersionDetector class SHALL provide `hasNativeAdventure()` returning true for Paper/Purpur/Folia.
9. THE VersionDetector class SHALL provide `getEnvironmentSummary()` returning a formatted string with server details.

### Requirement 5: Version-Aware Message Sending

**User Story:** As a plugin developer, I want messages to be sent correctly on both legacy and modern servers so that my plugin works across all supported Minecraft versions.

#### Acceptance Criteria

1. THE VersionedMessageSender class SHALL accept VersionDetector and BukkitAudiences in its constructor.
2. WHEN sending to modern servers (1.13+), THE VersionedMessageSender SHALL use Adventure Audiences.
3. WHEN sending to legacy servers (1.8-1.12), THE VersionedMessageSender SHALL convert Components to legacy strings.
4. THE VersionedMessageSender SHALL provide `sendMessage(Player, Component)` for player messages.
5. THE VersionedMessageSender SHALL provide `sendMessage(CommandSender, Component)` for generic senders.
6. THE VersionedMessageSender SHALL provide `broadcast(Component)` for server-wide messages.
7. THE VersionedMessageSender SHALL provide `console(Component)` for console output.
8. THE VersionedMessageSender SHALL use LegacyComponentSerializer for converting Components on legacy servers.

### Requirement 6: I18n Version Wrapper Interface

**User Story:** As a plugin developer, I want a clean interface for version-specific message handling so that the system can be extended for future Minecraft versions.

#### Acceptance Criteria

1. THE II18nVersionWrapper interface SHALL define `sendMessage()` for sending single messages.
2. THE II18nVersionWrapper interface SHALL define `sendMessages()` for sending multiple messages.
3. THE II18nVersionWrapper interface SHALL define `displayMessage()` returning the formatted message type.
4. THE II18nVersionWrapper interface SHALL define `displayMessages()` returning a list of formatted messages.
5. THE II18nVersionWrapper interface SHALL define `getPrefix()` returning the prefix component.
6. THE II18nVersionWrapper interface SHALL define `getFormattedMessage()` returning the fully formatted message.
7. THE II18nVersionWrapper interface SHALL define `asPlaceholder()` returning the message as a plain string.
8. THE II18nVersionWrapper interface SHALL use a generic type parameter for the message type.

### Requirement 7: Universal I18n Wrapper Implementation

**User Story:** As a plugin developer, I want a single universal wrapper that works across all modern Minecraft versions so that I don't need version-specific code.

#### Acceptance Criteria

1. THE UniversalI18nWrapper class SHALL implement II18nVersionWrapper with Component type.
2. THE UniversalI18nWrapper SHALL accept Player, key, placeholders map, and includePrefix flag.
3. THE UniversalI18nWrapper SHALL resolve translations from the static R18n.getTranslations() map.
4. THE UniversalI18nWrapper SHALL support both `{placeholder}` and `%placeholder%` formats.
5. THE UniversalI18nWrapper SHALL use MiniMessage for parsing formatted strings.
6. THE UniversalI18nWrapper SHALL fall back to default locale when player locale is unavailable.
7. THE UniversalI18nWrapper SHALL convert legacy color codes to MiniMessage format before parsing.
8. THE UniversalI18nWrapper SHALL send messages via R18n.getAudiences().player(player).

### Requirement 8: Console I18n Wrapper

**User Story:** As a plugin developer, I want console messages to use the default locale so that server administrators see properly formatted messages.

#### Acceptance Criteria

1. THE I18nConsoleWrapper class SHALL implement II18nVersionWrapper with Component type.
2. THE I18nConsoleWrapper SHALL use the default locale from R18n.getDefaultLocale().
3. THE I18nConsoleWrapper SHALL send messages via R18n.getAudiences().console().
4. THE I18nConsoleWrapper SHALL throw UnsupportedOperationException for displayMessage() and displayMessages().
5. THE I18nConsoleWrapper SHALL support placeholder replacement identical to UniversalI18nWrapper.

### Requirement 9: Enhanced Command UI with MiniMessage Gradients

**User Story:** As a server administrator, I want a visually appealing command interface with modern MiniMessage gradients so that translation management is intuitive and professional.

#### Acceptance Criteria

1. THE PR18n command class SHALL use MiniMessage gradients for header/footer decorative lines.
2. THE PR18n command SHALL display locale selection with color-coded status indicators (green=complete, yellow=minor issues, red=needs attention).
3. THE PR18n command SHALL provide interactive locale buttons with hover events showing locale information.
4. THE PR18n command SHALL provide click events for navigating to missing keys pages.
5. THE PR18n command SHALL display missing keys with pagination (12 keys per page).
6. THE PR18n command SHALL provide Previous/Next navigation buttons with hover effects.
7. THE PR18n command SHALL provide a "Back to Locales" button for returning to locale selection.
8. THE PR18n command SHALL display key details on hover including status and placeholder information.
9. THE PR18n command SHALL support click-to-copy for translation keys.
10. THE PR18n command SHALL use VersionedMessageSender for cross-version message delivery.

### Requirement 10: R18n Configuration Record

**User Story:** As a plugin developer, I want an immutable configuration record so that translation settings are type-safe and easily modifiable.

#### Acceptance Criteria

1. THE R18nConfiguration record SHALL contain defaultLocale, supportedLocales, translationDirectory fields.
2. THE R18nConfiguration record SHALL contain keyValidationEnabled, placeholderAPIEnabled, legacyColorSupport, debugMode flags.
3. THE R18nConfiguration record SHALL provide `withDefaultLocale(String)` returning a new instance.
4. THE R18nConfiguration record SHALL provide `withSupportedLocales(String...)` returning a new instance.
5. THE R18nConfiguration record SHALL provide `withKeyValidationEnabled(boolean)` returning a new instance.
6. THE R18nConfiguration record SHALL provide `isLocaleSupported(String)` for checking locale availability.
7. THE R18nConfiguration record SHALL provide `getBestMatchingLocale(String)` for fallback resolution.
8. THE R18nConfiguration record SHALL provide a static `defaultConfiguration()` factory method.

### Requirement 11: Full Locale Code Support

**User Story:** As a server administrator, I want to use full locale codes like en_GB and de_DE so that regional language variants are properly supported.

#### Acceptance Criteria

1. THE translation system SHALL support locale codes with underscore format (e.g., `en_GB`, `de_DE`, `ja_JP`).
2. THE translation system SHALL support locale codes with hyphen format (e.g., `en-GB`, `de-DE`).
3. WHEN a specific regional locale is unavailable, THE system SHALL fall back to language-only locale (e.g., `en_GB` → `en`).
4. THE translation files SHALL be named with full locale codes (e.g., `en_GB.yml`, `de_DE.yml`).
5. THE player locale detection SHALL use the full locale from player.getLocale().

### Requirement 12: Key Validation System

**User Story:** As a plugin developer, I want comprehensive key validation so that I can identify missing translations and formatting issues.

#### Acceptance Criteria

1. THE KeyValidator class SHALL validate all translation keys on startup when enabled.
2. THE KeyValidator class SHALL detect missing keys across locales.
3. THE KeyValidator class SHALL detect placeholder inconsistencies between locales.
4. THE KeyValidator class SHALL detect MiniMessage format errors.
5. THE KeyValidator class SHALL return a ValidationReport with categorized issues.
6. THE ValidationReport record SHALL provide `hasIssues()` returning true if any issues exist.
7. THE ValidationReport record SHALL provide `getValidationScore()` returning a percentage (0-100).
8. THE ValidationReport record SHALL provide `getSummary()` returning a formatted summary string.

### Requirement 13: Translation Loader with Async Support

**User Story:** As a plugin developer, I want async translation loading so that server startup is not blocked by file I/O.

#### Acceptance Criteria

1. THE TranslationLoader class SHALL provide `loadTranslations()` returning `CompletableFuture<Void>`.
2. THE TranslationLoader class SHALL load YAML files from the configured translations directory.
3. THE TranslationLoader class SHALL flatten nested YAML keys using dot notation.
4. THE TranslationLoader class SHALL preserve YAML list values as `List<String>`.
5. THE TranslationLoader class SHALL extract bundled resources from plugin JAR.
6. THE TranslationLoader class SHALL add programmatic translations for internal R18n messages.
7. THE TranslationLoader class SHALL provide `getRawTranslation(String key, String locale)` returning `Optional<List<String>>`.
8. THE TranslationLoader class SHALL provide `getMissingKeys(String locale)` returning missing key set.

### Requirement 14: Message Provider with MiniMessage Support

**User Story:** As a plugin developer, I want full MiniMessage support with legacy fallback so that I can use modern formatting while maintaining backward compatibility.

#### Acceptance Criteria

1. THE MessageProvider class SHALL parse MiniMessage tags including gradients, colors, and decorations.
2. THE MessageProvider class SHALL convert legacy color codes (`&` and `§`) to MiniMessage format.
3. THE MessageProvider class SHALL replace both `{placeholder}` and `%placeholder%` formats.
4. THE MessageProvider class SHALL escape user-provided values to prevent MiniMessage injection.
5. IF MiniMessage parsing fails, THEN THE MessageProvider SHALL return plain text fallback.
6. THE MessageProvider class SHALL support PlaceholderAPI integration when enabled and available.
7. THE MessageProvider class SHALL provide `getComponent(String key, String locale, Map placeholders, boolean includePrefix)`.
8. THE MessageProvider class SHALL provide `getComponents(String key, String locale, Map placeholders, boolean includePrefix)` for multi-line.

### Requirement 15: Permission System Integration

**User Story:** As a server administrator, I want granular permissions for translation commands so that I can control access to administrative functions.

#### Acceptance Criteria

1. THE ER18nPermission enum SHALL implement IPermissionNode interface.
2. THE ER18nPermission enum SHALL define COMMAND, HELP, RELOAD, MISSING permission nodes.
3. EACH permission node SHALL have an internalName and fallbackNode string.
4. THE PR18n command SHALL check permissions before executing subcommands.
5. THE permission nodes SHALL follow the pattern `r18n.command.*` for consistency.
