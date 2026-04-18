# Requirements Document

## Introduction

This document specifies the requirements for refactoring JExTranslate, a modern i18n (internationalization) API for Spigot/Bukkit/Paper Minecraft servers. The refactoring aims to restore the simplicity and reliability of the original JE18n project while preserving and enhancing the modern features of JExTranslate, including MiniMessage support, multi-locale handling (16+ languages), and Adventure API integration.

The core issue identified is that the current JExTranslate implementation shows raw translation keys instead of resolved translations, indicating problems in the translation lookup and retrieval mechanism. The refactoring will simplify the architecture while maintaining extensibility.

## Glossary

- **JExTranslate**: The current translation library being refactored, providing i18n support for Minecraft plugins
- **JE18n**: The original working translation library that serves as the reference implementation
- **TranslationService**: The main facade class for resolving and sending translated messages to players
- **TranslationRepository**: The data source abstraction for loading and storing translation key-value pairs
- **LocaleResolver**: Component responsible for determining a player's preferred locale
- **MessageFormatter**: Component that processes templates with placeholders and MiniMessage markup
- **MiniMessage**: Adventure API's text formatting system using XML-like tags (e.g., `<gold>`, `<bold>`)
- **Placeholder**: A token in translation templates that gets replaced with dynamic values (e.g., `{player}`, `%coins%`)
- **Locale**: A language/region identifier (e.g., `en_US`, `de_DE`, `ja_JP`)
- **Translation Key**: A dot-separated identifier for a translation entry (e.g., `welcome.message`, `error.not_found`)

## Requirements

### Requirement 1: Simplified Translation Storage and Retrieval

**User Story:** As a plugin developer, I want translations to be stored in a simple, predictable format so that I can easily add and modify translations without complex configuration.

#### Acceptance Criteria

1. WHEN a translation file is loaded, THE TranslationRepository SHALL parse YAML files with nested keys into a flat map structure using dot notation (e.g., `welcome.message` from `welcome: message:`).
2. WHEN a translation key is requested, THE TranslationRepository SHALL return the translation value within 5 milliseconds for cached entries.
3. WHILE translations are being loaded, THE TranslationRepository SHALL support both single-string values and list values (for multi-line messages).
4. IF a translation key is not found for the requested locale, THEN THE TranslationRepository SHALL fall back to the default locale before returning an empty result.
5. THE TranslationRepository SHALL store translations in a structure of `Map<String, Map<String, List<String>>>` where the outer key is the translation key, the inner key is the locale code, and the value is a list of message lines.

### Requirement 2: Reliable Locale Detection and Resolution

**User Story:** As a server administrator, I want player locales to be automatically detected from their Minecraft client settings so that players see messages in their preferred language without manual configuration.

#### Acceptance Criteria

1. WHEN a player joins the server, THE LocaleResolver SHALL detect the player's client locale using the Adventure API `Player#locale()` method when available.
2. WHERE the modern Adventure API is unavailable, THE LocaleResolver SHALL fall back to the legacy `Player#getLocale()` method.
3. IF locale detection fails, THEN THE LocaleResolver SHALL use the configured default locale.
4. THE LocaleResolver SHALL support manual locale overrides that persist for the player's session.
5. WHEN a locale override is set, THE LocaleResolver SHALL prioritize the override over client-detected locales.

### Requirement 3: MiniMessage and Legacy Color Support

**User Story:** As a plugin developer, I want to use MiniMessage formatting in my translations while maintaining backward compatibility with legacy color codes so that I can create rich, styled messages.

#### Acceptance Criteria

1. WHEN formatting a message, THE MessageFormatter SHALL parse MiniMessage tags (e.g., `<gold>`, `<bold>`, `<gradient:red:blue>`).
2. THE MessageFormatter SHALL convert legacy color codes (`&a`, `§b`) to MiniMessage format before parsing.
3. WHEN placeholders are present, THE MessageFormatter SHALL replace both `{placeholder}` and `%placeholder%` formats with their values.
4. THE MessageFormatter SHALL escape user-provided placeholder values to prevent MiniMessage injection.
5. IF MiniMessage parsing fails, THEN THE MessageFormatter SHALL return a plain text fallback containing the original template.

### Requirement 4: Fluent Translation API

**User Story:** As a plugin developer, I want a fluent, builder-style API for creating and sending translations so that my code is readable and maintainable.

#### Acceptance Criteria

1. THE TranslationService SHALL provide a static factory method `create(String key, Player player)` that returns a builder instance.
2. THE TranslationService builder SHALL provide a `with(String key, Object value)` method for adding placeholders.
3. THE TranslationService builder SHALL provide a `withPrefix()` method to prepend a configurable prefix to messages.
4. THE TranslationService builder SHALL provide `send()`, `sendActionBar()`, and `sendTitle()` methods for message delivery.
5. THE TranslationService builder SHALL provide a `display()` method that returns the formatted Component without sending.
6. THE TranslationService builder SHALL provide a `displayList()` method that returns a list of Components for multi-line messages.

### Requirement 5: Multi-Language Support

**User Story:** As a server administrator, I want to support at least 16 different languages including CJK (Chinese, Japanese, Korean) languages so that my international player base can enjoy the server in their native language.

#### Acceptance Criteria

1. THE TranslationRepository SHALL load translation files named with locale codes (e.g., `en_US.yml`, `de_DE.yml`, `ja_JP.yml`, `zh_CN.yml`, `ko_KR.yml`).
2. THE TranslationRepository SHALL support locale codes with both underscore (`en_US`) and hyphen (`en-US`) separators.
3. WHEN a specific regional locale is unavailable, THE TranslationRepository SHALL fall back to the language-only locale (e.g., `en_US` → `en`).
4. THE TranslationRepository SHALL correctly handle UTF-8 encoded translation files for CJK character support.
5. THE TranslationRepository SHALL provide a method to list all available locales.

### Requirement 6: Bundled Translation Synchronization

**User Story:** As a plugin developer, I want default translations bundled in my plugin JAR to be automatically copied to the server's data folder so that server administrators have working translations out of the box.

#### Acceptance Criteria

1. WHEN the plugin starts, THE TranslationRepository SHALL copy bundled translation files from the JAR to the translations directory if they do not exist.
2. WHEN bundled translations contain new keys not present in existing files, THE TranslationRepository SHALL append the missing keys to the existing files.
3. THE TranslationRepository SHALL create a backup of existing translation files before modifying them.
4. THE TranslationRepository SHALL log the number of files copied and keys synchronized during startup.

### Requirement 7: Missing Translation Handling

**User Story:** As a plugin developer, I want clear feedback when translations are missing so that I can identify and fix gaps in my translation coverage.

#### Acceptance Criteria

1. IF a translation key is not found in any locale, THEN THE TranslationService SHALL return a formatted error message containing the missing key.
2. THE MissingKeyTracker SHALL record all missing translation keys with their requested locales.
3. THE MissingKeyTracker SHALL provide statistics including total missing keys, affected locales, and most frequently missing keys.
4. WHEN a missing key is later added, THE MissingKeyTracker SHALL allow marking it as resolved.

### Requirement 8: Hot Reload Support

**User Story:** As a server administrator, I want to reload translations without restarting the server so that I can make changes to translations while the server is running.

#### Acceptance Criteria

1. THE TranslationRepository SHALL provide a `reload()` method that refreshes all cached translations from disk.
2. WHEN translations are reloaded, THE TranslationService SHALL clear its locale cache to ensure fresh lookups.
3. THE TranslationRepository SHALL notify registered listeners when a reload completes.
4. THE reload operation SHALL complete within 2 seconds for repositories with up to 1000 translation keys per locale.

### Requirement 9: Administrative Commands

**User Story:** As a server administrator, I want commands to manage translations, view statistics, and troubleshoot issues so that I can maintain the translation system effectively.

#### Acceptance Criteria

1. THE TranslationCommand SHALL provide a `/translate reload` subcommand to trigger translation reloading.
2. THE TranslationCommand SHALL provide a `/translate stats` subcommand to display missing key statistics.
3. THE TranslationCommand SHALL provide a `/translate info` subcommand to display repository metadata.
4. THE TranslationCommand SHALL provide a `/translate missing [locale]` subcommand to list missing keys for a locale.
5. THE TranslationCommand SHALL require appropriate permissions for each subcommand.

### Requirement 10: Simple Initialization

**User Story:** As a plugin developer, I want to initialize the translation system with minimal boilerplate code so that I can quickly integrate translations into my plugin.

#### Acceptance Criteria

1. THE TranslationService SHALL be configurable with a single `configure()` call accepting repository, formatter, and resolver.
2. THE YamlTranslationRepository SHALL provide a static factory method `create(Path directory, Locale defaultLocale)` for simple instantiation.
3. THE LocaleResolverProvider SHALL provide a static factory method `createAutoDetecting(Locale defaultLocale)` that selects the best available API.
4. IF the TranslationService is used before configuration, THEN THE TranslationService SHALL throw an IllegalStateException with a clear error message.

### Requirement 11: List and Lore Support

**User Story:** As a plugin developer, I want to easily create item lores, descriptions, and multi-line messages from translations so that I can build rich UI elements without manual list handling.

#### Acceptance Criteria

1. THE TranslationService SHALL provide a `displayList()` method that returns `List<Component>` for multi-line translations.
2. THE TranslationService SHALL provide a `displayStringList()` method that returns `List<String>` for legacy item lore compatibility.
3. WHEN a translation value is a YAML list, THE TranslationRepository SHALL preserve each list item as a separate line.
4. THE TranslationService SHALL provide a `displayLore()` method optimized for ItemStack lore with automatic line wrapping.
5. WHEN building lore, THE MessageFormatter SHALL support a configurable maximum line width for automatic text wrapping.

### Requirement 12: Performance Optimization

**User Story:** As a server administrator, I want the translation system to have minimal performance impact so that it does not cause lag even with many players and frequent message sends.

#### Acceptance Criteria

1. THE TranslationRepository SHALL cache all translations in memory after initial load.
2. THE TranslationService SHALL cache resolved player locales to avoid repeated API calls.
3. THE MessageFormatter SHALL cache compiled MiniMessage templates for frequently used translations.
4. WHEN sending messages, THE TranslationService SHALL complete placeholder replacement and formatting within 1 millisecond for typical messages.
5. THE TranslationRepository SHALL use ConcurrentHashMap for thread-safe access without synchronization overhead.

### Requirement 13: Consistency and Ease of Use

**User Story:** As a plugin developer, I want a consistent API with predictable behavior so that I can write translation code confidently without unexpected edge cases.

#### Acceptance Criteria

1. THE TranslationService SHALL use consistent method naming following the pattern: `with*()` for builders, `send*()` for delivery, `display*()` for retrieval.
2. THE TranslationService SHALL never return null; empty translations SHALL return empty Components or empty strings.
3. THE Placeholder API SHALL support all common types: String, Number, Component, LocalDateTime, and custom formatters.
4. THE TranslationService SHALL provide clear, actionable error messages when misconfigured or when translations are missing.
5. THE API SHALL follow immutable builder pattern where each `with*()` call returns a new instance.

### Requirement 14: Translation Key Discovery and Validation

**User Story:** As a plugin developer, I want tools to discover unconfigured translation keys and validate translation completeness across all languages so that I can ensure full translation coverage.

#### Acceptance Criteria

1. THE TranslationCommand SHALL provide a `/translate scan` subcommand that lists all keys missing translations in any supported locale.
2. THE TranslationCommand SHALL provide a `/translate compare <locale1> <locale2>` subcommand to show keys present in one locale but missing in another.
3. THE MissingKeyTracker SHALL automatically track keys requested at runtime that are not found.
4. THE TranslationCommand SHALL provide a `/translate export-missing [locale]` subcommand that generates a YAML file with missing keys and placeholder values.
5. WHEN displaying missing keys, THE TranslationCommand SHALL group them by namespace prefix for easier navigation.

### Requirement 15: Advanced Placeholder Features

**User Story:** As a plugin developer, I want advanced placeholder capabilities including conditional text, pluralization, and nested placeholders so that I can create dynamic, context-aware messages.

#### Acceptance Criteria

1. THE MessageFormatter SHALL support plural forms using ICU MessageFormat syntax (e.g., `{count, plural, one {# item} other {# items}}`).
2. THE MessageFormatter SHALL support choice format for conditional text (e.g., `{value, choice, 0#none|1#one|1<many}`).
3. THE Placeholder API SHALL support number formatting with locale-specific thousands separators and decimal points.
4. THE Placeholder API SHALL support date/time formatting with configurable patterns and locale-aware output.
5. THE MessageFormatter SHALL support nested placeholder resolution where placeholder values can contain other placeholders.
