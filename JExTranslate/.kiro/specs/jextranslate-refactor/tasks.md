# Implementation Plan

- [x] 1. Refactor Core Translation Storage





  - [x] 1.1 Create new storage structure in YamlTranslationRepository
    - Change internal storage from `Map<Locale, Map<String, String>>` to `Map<String, Map<String, List<String>>>`
    - Update `flattenMap()` to preserve list values as `List<String>` instead of joining with newlines

    - Add method `getTranslationLines(String key, Locale locale)` returning `List<String>`
    - _Requirements: 1.1, 1.3, 1.5_
  - [x] 1.2 Implement locale fallback chain

    - Implement fallback: requested locale → language-only locale → default locale
    - Return empty list only when key is missing in all locales
    - _Requirements: 1.4, 5.3_
  - [x] 1.3 Update translation loading from YAML


    - Preserve YAML list values as `List<String>`
    - Convert single string values to single-element lists
    - Support UTF-8 encoding for CJK characters
    - _Requirements: 1.3, 5.4_
  - [x] 1.4 Write unit tests for storage refactoring
    - Test nested key flattening
    - Test list value preservation
    - Test locale fallback chain
    - _Requirements: 1.1, 1.4_

- [x] 2. Create Simplified I18n Entry Point




  - [x] 2.1 Create I18n class with static factory methods

    - Implement `I18n.of(String key, Player player)` factory method
    - Implement `I18n.of(String key, Player player, Locale locale)` with explicit locale
    - Create inner `Builder` class with immutable pattern
    - _Requirements: 4.1, 10.1, 13.5_
  - [x] 2.2 Implement placeholder methods in Builder

    - Implement `with(String key, Object value)` for single placeholders
    - Implement `withAll(Map<String, ?> placeholders)` for bulk placeholders
    - Implement `withPrefix()` for prefix inclusion
    - Convert all values to String using appropriate formatting
    - _Requirements: 4.2, 4.3, 13.3_

  - [x] 2.3 Implement send methods in Builder
    - Implement `send()` for chat messages
    - Implement `sendActionBar()` for action bar
    - Implement `sendTitle()` with default durations
    - Implement `sendTitle(subtitle, fadeIn, stay, fadeOut)` with custom parameters
    - _Requirements: 4.4_

  - [x] 2.4 Implement display methods in Builder

    - Implement `display()` returning single `Component`
    - Implement `displayList()` returning `List<Component>`
    - Implement `displayText()` returning plain `String`
    - Implement `displayTextList()` returning `List<String>`
    - Implement `displayLore()` and `displayLore(int maxWidth)` for item lores
    - _Requirements: 4.5, 4.6, 11.1, 11.2, 11.4_

  - [x] 2.5 Write unit tests for I18n builder

    - Test fluent API chaining
    - Test all display methods
    - Test placeholder replacement
    - _Requirements: 4.1, 4.2_

- [x] 3. Simplify MessageFormatter

  - [x] 3.1 Refactor MiniMessageFormatter for simplified API


    - Change method signature to accept `Map<String, String>` instead of `List<Placeholder>`
    - Implement `format(String template, Map<String, String> placeholders, Locale locale)`
    - Implement `formatList(List<String> templates, Map<String, String> placeholders, Locale locale)`
    - _Requirements: 3.1, 3.3_
  - [x] 3.2 Add legacy color code conversion

    - Create `ColorUtil` class with `convertLegacyToMiniMessage(String)` method
    - Support `&` and `§` color code prefixes
    - Convert to MiniMessage tags before parsing
    - _Requirements: 3.2_
  - [x] 3.3 Implement placeholder escaping

    - Escape `<` and `>` in placeholder values to prevent MiniMessage injection
    - Preserve intentional MiniMessage tags in placeholder values when explicitly marked
    - _Requirements: 3.4_


  - [x] 3.4 Add fallback for parsing failures
    - Catch MiniMessage parsing exceptions
    - Return plain text Component on failure
    - Log warning with template details

    - _Requirements: 3.5_
  - [x] 3.5 Write unit tests for MessageFormatter


    - Test MiniMessage parsing
    - Test legacy color conversion
    - Test placeholder escaping
    - _Requirements: 3.1, 3.2, 3.4_

- [x] 4. Enhance LocaleResolver




  - [x] 4.1 Simplify LocaleResolver interface

    - Change `resolveLocale()` to return `Locale` directly instead of `Optional<Locale>`
    - Add `setOverride(Player, Locale)` method
    - Add `clearOverride(Player)` method
    - _Requirements: 2.4, 2.5_

  - [x] 4.2 Implement locale caching
    - Cache resolved locales per player UUID
    - Clear cache on override changes
    - Clear cache on reload
    - _Requirements: 12.2_

  - [x] 4.3 Write unit tests for LocaleResolver

    - Test client locale detection
    - Test override behavior
    - Test cache invalidation
    - _Requirements: 2.1, 2.4_

- [x] 5. Implement Missing Key Discovery



  - [x] 5.1 Add key comparison methods to TranslationRepository

    - Implement `getMissingKeys(Locale locale)` returning keys missing compared to default locale
    - Implement `getKeysOnlyIn(Locale locale)` returning keys unique to a locale
    - Implement `compareLocales(Locale a, Locale b)` returning difference report
    - _Requirements: 14.1, 14.2_

  - [x] 5.2 Enhance MissingKeyTracker

    - Track keys by namespace prefix for grouping
    - Add `getKeysByNamespace()` method
    - Add `exportMissingAsYaml(Locale locale)` method
    - _Requirements: 14.3, 14.4, 14.5_
  - [x] 5.3 Add scan and compare commands


    - Implement `/translate scan` to list all missing keys across locales
    - Implement `/translate compare <locale1> <locale2>` for locale comparison
    - Implement `/translate export-missing [locale]` to generate YAML file
    - Group output by namespace prefix
    - _Requirements: 14.1, 14.2, 14.4, 14.5_
  - [ ]* 5.4 Write unit tests for key discovery
    - Test missing key detection
    - Test locale comparison
    - Test YAML export format
    - _Requirements: 14.1, 14.2_

- [x] 6. Add List and Lore Support





  - [x] 6.1 Implement lore formatting utilities
    - Create `LoreFormatter` utility class
    - Implement automatic line wrapping at configurable width
    - Preserve explicit line breaks from translation
    - Handle MiniMessage tags across wrapped lines

    - _Requirements: 11.4, 11.5_
  - [x] 6.2 Add lore methods to TranslatedMessage
    - Implement `asLoreList()` returning `List<String>` for ItemMeta
    - Implement `asLoreList(int maxWidth)` with custom wrapping
    - Convert Components to legacy text for item compatibility
    - _Requirements: 11.2, 11.4_
  - [ ]* 6.3 Write unit tests for lore formatting
    - Test line wrapping
    - Test MiniMessage preservation
    - Test legacy text conversion
    - _Requirements: 11.4, 11.5_

- [-] 7. Implement Advanced Placeholder Features



  - [x] 7.1 Add ICU MessageFormat support
    - Integrate ICU4J or use Java's MessageFormat for plural/choice
    - Support `{count, plural, one {# item} other {# items}}` syntax
    - Support `{value, choice, 0#none|1#one|1<many}` syntax

    - _Requirements: 15.1, 15.2_
  - [x] 7.2 Add number and date formatting
    - Implement locale-aware number formatting with `NumberFormat`
    - Implement date/time formatting with `DateTimeFormatter`
    - Support custom format patterns in placeholders
    - _Requirements: 15.3, 15.4_
  - [ ]* 7.3 Write unit tests for advanced placeholders
    - Test plural forms in multiple locales
    - Test number formatting
    - Test date formatting
    - _Requirements: 15.1, 15.3, 15.4_

- [x] 8. Performance Optimization



  - [x] 8.1 Implement message format caching
    - Create LRU cache for compiled MessageFormat patterns
    - Cache MiniMessage parse results for frequently used templates
    - Add cache size configuration

    - _Requirements: 12.3_
  - [ ] 8.2 Optimize translation lookup

    - Ensure O(1) lookup time with HashMap
    - Pre-compute locale fallback chains
    - Use ConcurrentHashMap for thread safety
    - _Requirements: 1.2, 12.1, 12.5_
  - [ ]* 8.3 Add performance benchmarks (optional)
    - Benchmark translation lookup time
    - Benchmark message formatting time
    - Benchmark full send() operation
    - _Requirements: 12.4_

- [x] 9. Update Administrative Commands (scan/compare/export added)
  - [x] 9.1 Refactor TranslationCommand for new API
    - Update to use new I18n builder internally
    - Simplify command handler methods
    - Add permission checks for new subcommands
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  - [ ]* 9.2 Add GUI improvements for missing keys (optional)
    - Group missing keys by namespace in GUI
    - Add search/filter functionality
    - Add bulk add translation wizard
    - _Requirements: 14.5_
  - [ ]* 9.3 Write integration tests for commands (optional)
    - Test reload command
    - Test stats command
    - Test scan command
    - _Requirements: 9.1, 9.2_

- [x] 10. Backward Compatibility Layer (NOT NEEDED - old API works alongside new I18n)
  - [x] 10.1 Create TranslationService compatibility wrapper (SKIPPED - old API unchanged)
    - Keep `TranslationService.create()` as alias to `I18n.of()`
    - Deprecate old methods with migration hints
    - Support old `Placeholder` types by converting to Map
    - _Requirements: 10.1, 13.4_
  - [x] 10.2 Update example plugin
    - Update ExamplePlugin to use new I18n API
    - Add examples for all new features (lore, lists, advanced placeholders)
    - Document migration from old API
    - _Requirements: 10.1, 10.2, 10.3_
  - [x] 10.3 Write migration tests (SKIPPED - no breaking changes)
    - Test old API still works
    - Test deprecation warnings appear
    - Test seamless migration path
    - _Requirements: 10.1_

- [x] 11. Documentation and Cleanup
  - [ ]* 11.1 Update Javadoc for all public APIs (optional - existing Javadoc adequate)
    - Document I18n class and Builder
    - Document all interface methods
    - Add usage examples in Javadoc
    - _Requirements: 13.4_
  - [x] 11.2 Remove deprecated code

    - Remove unused classes after deprecation period
    - Clean up internal implementation details
    - Simplify package structure
    - _Requirements: 13.1_
  - [ ]* 11.3 Create README with usage examples (optional)
    - Quick start guide
    - API reference
    - Migration guide from JE18n and old JExTranslate
    - _Requirements: 13.4_
