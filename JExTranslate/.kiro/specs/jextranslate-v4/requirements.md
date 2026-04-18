# Requirements Document

## Introduction

This document defines the requirements for JExTranslate v4.0 - a major enhancement to the internationalization library. The update focuses on removing legacy APIs, adding caching, player locale persistence with database support, JSON translation files, type-safe keys, plural support, console commands, file watching, export functionality, metrics, and thread safety improvements.

## Glossary

- **R18nManager**: The modern translation manager providing fluent API for message handling
- **TranslationLoader**: Component responsible for loading and parsing translation files
- **MessageProvider**: Component that formats messages with placeholders and MiniMessage
- **LocaleStorage**: Interface for persisting player language preferences
- **JEHibernate**: JExcellence's Hibernate wrapper providing GenericCachedRepository pattern
- **MiniMessage**: Adventure API's text formatting system with gradient and tag support
- **Translation Key**: Dot-notation string identifying a translation (e.g., `welcome.message`)
- **Plural Rules**: ICU-style rules for handling singular/plural forms based on count

## Requirements

### Requirement 1: Remove Legacy TranslationService API

**User Story:** As a developer, I want a single unified translation API so that I don't have confusion between old and new systems.

#### Acceptance Criteria

1. THE JExTranslate system SHALL remove the `TranslationService` class from the `api` package.
2. THE JExTranslate system SHALL remove the `TranslationRepository` interface and implementations.
3. THE JExTranslate system SHALL remove the `LocaleResolver` interface from the old API.
4. THE JExTranslate system SHALL remove the `MessageFormatter` interface from the old API.
5. THE JExTranslate system SHALL remove the `MissingKeyTracker` interface and implementations.
6. THE JExTranslate system SHALL remove the `Placeholder` class from the old API.
7. THE JExTranslate system SHALL remove the `TranslatedMessage` class.
8. THE JExTranslate system SHALL remove the `TranslationKey` record.
9. THE JExTranslate system SHALL update any remaining code that references removed classes.

### Requirement 2: Translation Caching

**User Story:** As a server administrator, I want parsed translations to be cached so that message sending is faster and uses less CPU.

#### Acceptance Criteria

1. THE MessageProvider SHALL cache parsed MiniMessage Components using Caffeine cache.
2. THE cache SHALL use a composite key of translation key, locale, and placeholder hash.
3. THE R18nConfiguration SHALL include a `cacheEnabled` boolean flag defaulting to true.
4. THE R18nConfiguration SHALL include a `cacheMaxSize` integer defaulting to 1000.
5. THE R18nConfiguration SHALL include a `cacheExpireMinutes` integer defaulting to 30.
6. WHEN translations are reloaded, THE cache SHALL be invalidated completely.
7. THE R18nManager SHALL expose cache statistics via `getCacheStats()` method.
8. WHEN caching is disabled, THE MessageProvider SHALL parse messages on every request.

### Requirement 3: Player Locale Storage

**User Story:** As a player, I want my language preference to be saved so that I don't have to change it every time I join.

#### Acceptance Criteria

1. THE JExTranslate system SHALL define a `LocaleStorage` interface with `getLocale(UUID)` and `setLocale(UUID, String)` methods.
2. THE JExTranslate system SHALL provide an `InMemoryLocaleStorage` implementation for default behavior.
3. THE JExTranslate system SHALL provide a `DatabaseLocaleStorage` implementation using JEHibernate.
4. THE DatabaseLocaleStorage SHALL use a `PlayerLocale` entity with `uniqueId` and `locale` fields.
5. THE DatabaseLocaleStorage SHALL use a `PlayerLocaleRepository` extending `GenericCachedRepository`.
6. THE R18nManager.Builder SHALL accept an optional `LocaleStorage` instance.
7. THE R18nManager.Builder SHALL accept an optional `EntityManagerFactory` for database storage.
8. WHEN no LocaleStorage is provided, THE system SHALL use InMemoryLocaleStorage.
9. THE MessageBuilder SHALL check LocaleStorage before falling back to player.getLocale().

### Requirement 4: JSON Translation Support

**User Story:** As a developer, I want to use JSON files for translations so that I can use my preferred format.

#### Acceptance Criteria

1. THE TranslationLoader SHALL detect and load `.json` files from the translations directory.
2. THE JSON format SHALL support nested objects for key hierarchy (same as YAML).
3. THE JSON format SHALL support arrays for multi-line messages.
4. THE TranslationLoader SHALL use Jackson for JSON parsing.
5. WHEN both YAML and JSON files exist for the same locale, THE YAML file SHALL take precedence.
6. THE resource extraction SHALL support bundled JSON files from plugin JAR.

### Requirement 5: Type-Safe Translation Keys

**User Story:** As a developer, I want compile-time safety for translation keys so that typos are caught early.

#### Acceptance Criteria

1. THE JExTranslate system SHALL provide a Gradle task to generate key constants from translation files.
2. THE generated class SHALL be named `TranslationKeys` in a configurable package.
3. THE generated class SHALL contain nested classes matching the key hierarchy.
4. EACH translation key SHALL generate a `public static final String` constant.
5. THE generator SHALL run automatically before compilation when enabled.
6. THE R18nConfiguration SHALL include a `generateKeys` boolean flag.

### Requirement 6: Plural Support

**User Story:** As a translator, I want to define different messages for singular and plural forms so that translations are grammatically correct.

#### Acceptance Criteria

1. THE translation format SHALL support plural variants using suffixes: `.zero`, `.one`, `.two`, `.few`, `.many`, `.other`.
2. THE MessageBuilder SHALL accept a `count(String placeholder, int value)` method.
3. WHEN a count placeholder is provided, THE system SHALL select the appropriate plural form.
4. THE plural selection SHALL follow ICU plural rules for the target locale.
5. IF a specific plural form is missing, THE system SHALL fall back to `.other`.

### Requirement 7: Console Command Support

**User Story:** As a server administrator, I want to use translation commands from console so that I can manage translations without being in-game.

#### Acceptance Criteria

1. THE PR18nCommand SHALL support CommandSender (not just Player) for reload subcommand.
2. THE PR18nCommand SHALL support CommandSender for missing subcommand with text output.
3. WHEN executed from console, THE missing command SHALL output plain text without MiniMessage formatting.
4. THE console output SHALL include pagination information in text format.

### Requirement 8: File Watcher for Hot Reload

**User Story:** As a developer, I want translations to auto-reload when files change so that I can iterate faster during development.

#### Acceptance Criteria

1. THE R18nConfiguration SHALL include a `watchFiles` boolean flag defaulting to false.
2. WHEN watchFiles is enabled, THE TranslationLoader SHALL register a WatchService for the translations directory.
3. WHEN a translation file is modified, THE system SHALL reload translations within 2 seconds.
4. WHEN a translation file is added, THE system SHALL load the new file.
5. WHEN a translation file is deleted, THE system SHALL remove those translations.
6. THE file watcher SHALL run on a separate daemon thread.
7. THE R18nManager.shutdown() SHALL stop the file watcher.

### Requirement 9: Translation Export

**User Story:** As a project manager, I want to export translations so that I can send them to external translation services.

#### Acceptance Criteria

1. THE R18nManager SHALL provide an `exportTranslations(Path, ExportFormat)` method.
2. THE ExportFormat enum SHALL include CSV, JSON, and YAML options.
3. THE CSV export SHALL include columns: key, locale, value.
4. THE JSON export SHALL produce a flat key-value structure per locale.
5. THE export SHALL include all loaded translations across all locales.
6. THE PR18nCommand SHALL include an `export <format>` subcommand.

### Requirement 10: Missing Key Fallback Customization

**User Story:** As a developer, I want to customize what happens when a translation key is missing so that I can control the user experience.

#### Acceptance Criteria

1. THE R18nConfiguration SHALL include a `missingKeyHandler` functional interface.
2. THE default handler SHALL return `<gold>Missing: <red>{key}</red></gold>`.
3. THE handler SHALL receive the key, locale, and placeholders as parameters.
4. THE R18nManager.Builder SHALL accept a custom `onMissingKey(MissingKeyHandler)` method.
5. THE handler MAY return null to suppress the message entirely.

### Requirement 11: Translation Metrics

**User Story:** As a server administrator, I want to see translation usage statistics so that I can identify missing or unused translations.

#### Acceptance Criteria

1. THE R18nConfiguration SHALL include a `metricsEnabled` boolean flag defaulting to false.
2. WHEN metrics are enabled, THE system SHALL track key usage counts.
3. WHEN metrics are enabled, THE system SHALL track missing key occurrences.
4. WHEN metrics are enabled, THE system SHALL track locale distribution.
5. THE R18nManager SHALL provide a `getMetrics()` method returning TranslationMetrics.
6. THE TranslationMetrics SHALL include `getMostUsedKeys(int limit)` method.
7. THE TranslationMetrics SHALL include `getMissingKeyOccurrences()` method.
8. THE TranslationMetrics SHALL include `getLocaleDistribution()` method.
9. THE PR18nCommand SHALL include a `metrics` subcommand to display statistics.

### Requirement 12: Thread Safety Improvements

**User Story:** As a developer, I want the translation system to be thread-safe so that it works correctly in async contexts.

#### Acceptance Criteria

1. THE R18nManager.instance field SHALL use AtomicReference for thread-safe access.
2. THE TranslationLoader.translations map SHALL use ConcurrentHashMap (already implemented).
3. THE cache operations SHALL be thread-safe using Caffeine's built-in concurrency.
4. THE metrics counters SHALL use AtomicLong for thread-safe increments.
5. THE LocaleStorage implementations SHALL be thread-safe.
