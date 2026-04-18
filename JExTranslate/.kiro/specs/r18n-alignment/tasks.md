# Implementation Plan

- [x] 1. Create Core Configuration Classes



  - [x] 1.1 Create R18nConfiguration record

    - Implement immutable record with defaultLocale, supportedLocales, translationDirectory fields
    - Add keyValidationEnabled, placeholderAPIEnabled, legacyColorSupport, debugMode flags
    - Implement `withDefaultLocale()`, `withSupportedLocales()`, `withKeyValidationEnabled()` methods
    - Add `isLocaleSupported()`, `getBestMatchingLocale()`, `defaultConfiguration()` methods
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8_



  - [x] 1.2 Create R18nSection YAML config class




    - Extend AConfigSection for YAML configuration mapping
    - Add defaultLanguage and supportedLanguages fields




    - Support full locale codes (en_GB, de_DE format)
    - _Requirements: 11.1, 11.4_

- [x] 2. Create Version Detection System
  - [x] 2.1 Create VersionDetector class
    - Implement ServerType enum (PAPER, PURPUR, FOLIA, SPIGOT, BUKKIT)
    - Add detection methods using Class.forName() for each server type
    - Parse Minecraft version from server package name
    - Implement `isModern()`, `isLegacy()`, `hasNativeAdventure()` methods
    - Add `getEnvironmentSummary()` for logging
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [x] 2.2 Create VersionedMessageSender class
    - Accept VersionDetector and BukkitAudiences in constructor
    - Implement `sendMessage(Player, Component)` with version-aware logic
    - Implement `sendMessage(CommandSender, Component)` for generic senders
    - Implement `broadcast(Component)` and `console(Component)` methods
    - Use LegacyComponentSerializer for legacy server fallback
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_


- [x] 3. Create I18n Version Wrapper System
  - [x] 3.1 Create II18nVersionWrapper interface
    - Define PREFIX_KEY constant
    - Add `sendMessage()`, `sendMessages()` methods
    - Add `displayMessage()`, `displayMessages()` returning generic type T
    - Add `getPrefix()`, `getMessagesByKey()`, `getPrefixByKey()` methods
    - Add `getFormattedMessage()`, `getRawMessagesByKey()`, `asPlaceholder()` methods
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

  - [x] 3.2 Create UniversalI18nWrapper class
    - Implement II18nVersionWrapper with Component type
    - Accept Player, key, placeholders map, includePrefix in constructor
    - Resolve translations from R18n.getTranslations() static map
    - Support both `{placeholder}` and `%placeholder%` formats
    - Use MiniMessage for parsing, convert legacy colors
    - Send via R18n.getAudiences().player(player)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_

  - [x] 3.3 Create I18nConsoleWrapper class
    - Implement II18nVersionWrapper with Component type
    - Use R18n.getDefaultLocale() for locale resolution
    - Send via R18n.getAudiences().console()
    - Throw UnsupportedOperationException for displayMessage/displayMessages
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 3.4 Create VersionWrapper factory class
    - Accept Player, key, placeholders, includePrefix in constructor
    - Return UniversalI18nWrapper for all versions (Adventure handles compatibility)
    - Provide `getI18nVersionWrapper()` method
    - _Requirements: 7.1_






- [x] 4. Create R18n Legacy Static Class
  - [x] 4.1 Create R18n class with static patterns
    - Add static `r18n` instance field
    - Add static `TRANSLATIONS` map with `Map<String, Map<String, List<String>>>` structure
    - Add static `DEFAULT_LOCALE` string field
    - Add static `audiences` BukkitAudiences field
    - Implement constructor accepting JavaPlugin and message string
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 4.2 Implement R18n translation loading
    - Implement `loadTranslations()` method
    - Create translations directory if not exists
    - Extract bundled files from JAR using JarFile enumeration
    - Load YAML files and merge into TRANSLATIONS map
    - Add programmatic translations for internal messages
    - _Requirements: 1.7, 1.8_

  - [x] 4.3 Implement R18n Adventure integration
    - Implement `initializeAdventure()` creating BukkitAudiences
    - Implement `closeAdventure()` for resource cleanup
    - Add `getAudiences()`, `getTranslations()`, `getDefaultLocale()` static getters
    - Mark class and methods as @Deprecated with migration hints
    - _Requirements: 1.2, 1.4, 1.5, 1.6_

- [x] 5. Create R18nManager Modern Builder
  - [x] 5.1 Create R18nManager class structure
    - Add private fields for plugin, configuration, translationLoader, messageProvider
    - Add keyValidator, versionDetector, audiences, messageSender fields
    - Add initialized boolean flag
    - Implement private constructor accepting Builder
    - _Requirements: 2.1_

  - [x] 5.2 Implement R18nManager.Builder class
    - Add `defaultLocale(String)` method
    - Add `supportedLocales(String...)` method
    - Add `enableKeyValidation(boolean)` method
    - Add `enablePlaceholderAPI(boolean)` method
    - Add `translationDirectory(String)` method
    - Implement `build()` returning R18nManager instance
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 5.3 Implement R18nManager lifecycle methods
    - Implement `initialize()` returning CompletableFuture<Void>
    - Initialize Adventure platform, VersionedMessageSender
    - Load translations, run validation if enabled
    - Implement `reload()` returning CompletableFuture<Void>
    - Implement `shutdown()` closing Adventure resources
    - _Requirements: 2.8, 2.10, 2.11_

  - [x] 5.4 Create MessageBuilder class for R18nManager
    - Accept R18nManager and key in constructor
    - Implement `placeholder(String, Object)` method
    - Implement `placeholders(Map)` method
    - Implement `withPrefix()` method
    - Implement `locale(String)` for explicit locale override
    - Implement `send(Player)`, `send(CommandSender)`, `broadcast()`, `console()` methods
    - Implement `toComponent(Player)`, `toComponents(Player)`, `toString(Player)` methods
    - _Requirements: 2.9_


- [x] 6. Create I18n Fluent Builder Entry Point
  - [x] 6.1 Create I18n class with Builder pattern
    - Add private II18nVersionWrapper field
    - Implement private constructor accepting Builder
    - Create console wrapper when player is null
    - Create VersionWrapper when player is provided
    - _Requirements: 3.1, 3.2_

  - [x] 6.2 Implement I18n.Builder class
    - Add placeholders map, player, key, includePrefix fields
    - Implement constructor with key and Player
    - Implement constructor with key only (console)
    - Add `withPlaceholder(String, Object)` method
    - Add `withPlaceholders(Map)` method
    - Add `includePrefix()` method
    - Implement `build()` returning I18n instance
    - _Requirements: 3.3, 3.4, 3.5_

  - [x] 6.3 Implement I18n message methods
    - Implement `sendMessage()` delegating to wrapper
    - Implement `sendMultiple()` delegating to wrapper
    - Implement `component()` returning formatted Component
    - Implement `children()` returning List<Component>
    - Add `getI18nVersionWrapper()` getter
    - _Requirements: 3.6, 3.7, 3.8, 3.9, 3.10_

- [x] 7. Create Translation Loading System
  - [x] 7.1 Create TranslationLoader class
    - Accept JavaPlugin and R18nConfiguration in constructor
    - Add translations ConcurrentHashMap and loadedLocales set
    - Implement `loadTranslations()` returning CompletableFuture<Void>
    - _Requirements: 13.1_

  - [x] 7.2 Implement YAML loading logic
    - Load YAML files from configured translations directory
    - Flatten nested keys using dot notation
    - Preserve YAML list values as List<String>
    - Support UTF-8 encoding for CJK characters
    - _Requirements: 13.2, 13.3, 13.4_

  - [x] 7.3 Implement resource extraction
    - Extract bundled files from plugin JAR
    - Only extract if file doesn't exist in data folder
    - Add programmatic translations for internal R18n messages
    - _Requirements: 13.5, 13.6_

  - [x] 7.4 Implement translation retrieval methods
    - Add `getRawTranslation(String key, String locale)` returning Optional<List<String>>
    - Add `hasKey(String key, String locale)` method
    - Add `getMissingKeys(String locale)` returning Set<String>
    - Add `getLoadedLocales()` and `getTotalKeyCount()` methods
    - _Requirements: 13.7, 13.8_

- [x] 8. Create MessageProvider with MiniMessage Support
  - [x] 8.1 Create MessageProvider class
    - Accept R18nConfiguration and VersionDetector in constructor
    - Initialize MiniMessage, LegacyComponentSerializer, PlainTextComponentSerializer
    - Check PlaceholderAPI availability
    - _Requirements: 14.1_

  - [x] 8.2 Implement message formatting
    - Implement `getComponent(String key, String locale, Map placeholders, boolean includePrefix)`
    - Implement `getComponents()` for multi-line messages
    - Implement `getString()` and `getStrings()` for plain text
    - _Requirements: 14.7, 14.8_

  - [x] 8.3 Implement placeholder processing
    - Replace both `{placeholder}` and `%placeholder%` formats
    - Convert legacy color codes (`&` and `§`) to MiniMessage format
    - Escape user-provided values to prevent MiniMessage injection
    - Support PlaceholderAPI when enabled and available
    - _Requirements: 14.2, 14.3, 14.4, 14.6_

  - [x] 8.4 Implement parsing with fallback
    - Parse MiniMessage tags including gradients, colors, decorations
    - Catch parsing exceptions and return plain text fallback
    - Log warnings for parsing failures
    - _Requirements: 14.1, 14.5_

- [x] 9. Create Key Validation System
  - [x] 9.1 Create ValidationStatistics record
    - Add totalKeys, totalLocales, keysPerLocale, completenessPercentage, validationDurationMs fields
    - Add static `empty()` factory method
    - _Requirements: 12.6_

  - [x] 9.2 Create ValidationReport record
    - Add timestamp, missingKeys, unusedKeys, formatErrors, placeholderIssues, namingViolations fields
    - Add statistics field
    - Implement `hasIssues()`, `getTotalIssues()`, `getAllProblematicKeys()` methods
    - Implement `getValidationScore()` returning percentage
    - Implement `getSummary()` returning formatted string
    - Add Builder class for construction
    - _Requirements: 12.5, 12.6, 12.7, 12.8_

  - [x] 9.3 Create KeyValidator class
    - Accept R18nConfiguration in constructor
    - Implement `validateAllKeys()` returning ValidationReport
    - Detect missing keys across locales
    - Detect placeholder inconsistencies between locales
    - Detect MiniMessage format errors
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 10. Create Permission System
  - [x] 10.1 Create ER18nPermission enum
    - Implement IPermissionNode interface
    - Add COMMAND, HELP, RELOAD, MISSING permission nodes
    - Add internalName and fallbackNode fields
    - Implement `getInternalName()` and `getFallbackNode()` methods
    - _Requirements: 15.1, 15.2, 15.3, 15.4_

- [x] 11. Create Enhanced PR18n Command
  - [x] 11.1 Create PR18n command class structure
    - Extend PlayerCommand base class
    - Add loadedPlugin, r18nManager, messageSender fields
    - Add SUBCOMMANDS list and KEYS_PER_PAGE constant
    - Add MINI_MESSAGE static instance
    - _Requirements: 9.1_

  - [x] 11.2 Implement command routing
    - Handle pagination routing for `/r18n missing <locale> <page>`
    - Route to reload, missing, help subcommands
    - Check permissions using ER18nPermission enum
    - _Requirements: 9.1, 15.5_

  - [x] 11.3 Implement enhanced locale selection UI
    - Create gradient header/footer lines using MiniMessage
    - Display title and instructions with gradients
    - Create locale buttons grid (4 per row)
    - Add status indicators (✓ green, ⚠ yellow, ✗ red)
    - Add hover events with locale information
    - Add click events to navigate to missing keys
    - _Requirements: 9.2, 9.3, 9.4, 9.5_

  - [x] 11.4 Implement missing keys display with pagination
    - Display header with locale, page info, total keys
    - List missing keys with gradient formatting
    - Add hover events with key details and placeholders
    - Add click-to-copy functionality
    - _Requirements: 9.5, 9.8, 9.9_

  - [x] 11.5 Implement navigation bar
    - Create Previous button with hover/click events
    - Create page indicator with gradient
    - Create Next button with hover/click events
    - Create "Back to Locales" button
    - Disable buttons when at first/last page
    - _Requirements: 9.6, 9.7_

  - [x] 11.6 Implement reload and utility methods
    - Implement reload with loading message and success/failure feedback
    - Implement `getAvailableLocales()` from R18n.getTranslations()
    - Implement `findMissingKeysForLocale()` comparing to default locale
    - Implement `extractPlaceholders()` using regex pattern
    - Use VersionedMessageSender for all message delivery
    - _Requirements: 9.10_

  - [x] 11.7 Implement tab completion
    - Complete subcommands on first argument
    - Complete locale codes on second argument for missing
    - Complete page numbers on third argument
    - _Requirements: 9.1_


- [x] 12. Update ColorUtil for Legacy Conversion

  - [x] 12.1 Enhance ColorUtil class

    - Add `convertLegacyColorsToMiniMessage(String)` method
    - Convert `&` codes to MiniMessage tags
    - Convert `§` codes to MiniMessage tags
    - Support all color codes (0-9, a-f) and formatting codes (k-o, r)
    - _Requirements: 14.2_

- [x] 13. Integration, Cleanup and Documentation

  - [x] 13.1 Write unit tests for R18nConfiguration

    - Test immutability and defensive copying
    - Test `withDefaultLocale()` and `withSupportedLocales()`
    - Test `isLocaleSupported()` and `getBestMatchingLocale()`
    - _Requirements: 10.1, 10.6, 10.7_

  - [x] 13.2 Write unit tests for VersionDetector

    - Test server type detection logic
    - Test version parsing
    - Test `isModern()` and `hasNativeAdventure()`
    - _Requirements: 4.7, 4.8_


  - [x] 13.3 Write unit tests for I18n Builder



    - Test placeholder replacement
    - Test prefix inclusion
    - Test component generation
    - _Requirements: 3.3, 3.4, 3.5_

  - [x] 13.4 Delete unused/redundant classes


    - Remove ExamplePlugin.java (replaced by documentation)
    - Remove any empty stub classes
    - Clean up unused imports across all files
    - _Requirements: 1.1_

  - [x] 13.5 Create comprehensive R18N-USAGE.md documentation


    - Document R18n legacy API with examples
    - Document R18nManager modern API with builder examples
    - Document I18n fluent builder usage
    - Document version detection and cross-version compatibility
    - Document PR18n command usage and permissions
    - Document translation file format and locale codes
    - Include migration guide from old patterns
    - _Requirements: 1.1, 2.1, 3.1_


  - [-] 13.6 Wire new components into existing system

    - Update TranslationCommand to use VersionedMessageSender where appropriate
    - Ensure backward compatibility with existing TranslationService API
    - Verify all tests pass
    - _Requirements: 1.1, 2.1_
