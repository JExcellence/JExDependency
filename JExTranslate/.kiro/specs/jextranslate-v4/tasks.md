# Implementation Plan

- [x] 1. Remove Legacy API Classes






  - [x] 1.1 Delete old API classes

    - Delete `api/TranslationService.java`
    - Delete `api/TranslationRepository.java`
    - Delete `api/LocaleResolver.java`
    - Delete `api/MessageFormatter.java`
    - Delete `api/MissingKeyTracker.java`
    - Delete `api/Placeholder.java`
    - Delete `api/TranslatedMessage.java`
    - Delete `api/TranslationKey.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_


  - [x] 1.2 Delete old implementation classes

    - Delete `impl/LocaleResolverProvider.java`
    - Delete `impl/SimpleMissingKeyTracker.java`
    - Delete `impl/YamlTranslationRepository.java`
    - Delete `impl/MiniMessageFormatter.java` (if not used by new API)
    - _Requirements: 1.9_


  - [x] 1.3 Clean up remaining references

    - Remove any imports referencing deleted classes
    - Update tests that reference old API
    - _Requirements: 1.9_

- [x] 2. Add Dependencies





  - [x] 2.1 Update build.gradle.kts


    - Add Caffeine dependency for caching
    - Add Jackson dependencies for JSON support
    - Add JEHibernate as compileOnly dependency
    - Add Hibernate platform as compileOnly
    - _Requirements: 2.1, 4.4, 3.3_

- [x] 3. Enhance R18nConfiguration





  - [x] 3.1 Add new configuration fields


    - Add `cacheEnabled` boolean (default: true)
    - Add `cacheMaxSize` int (default: 1000)
    - Add `cacheExpireMinutes` int (default: 30)
    - Add `watchFiles` boolean (default: false)
    - Add `metricsEnabled` boolean (default: false)
    - Add `MissingKeyHandler` functional interface
    - Add `missingKeyHandler` field with default implementation
    - _Requirements: 2.3, 2.4, 2.5, 8.1, 11.1, 10.1, 10.2_


  - [x] 3.2 Add builder methods for new fields

    - Add `enableCache(boolean)` method
    - Add `cacheMaxSize(int)` method
    - Add `cacheExpireMinutes(int)` method
    - Add `enableFileWatcher(boolean)` method
    - Add `enableMetrics(boolean)` method
    - Add `onMissingKey(MissingKeyHandler)` method
    - _Requirements: 2.3, 8.1, 11.1, 10.4_

- [x] 4. Implement Translation Caching





  - [x] 4.1 Add cache to MessageProvider


    - Create `CacheKey` record with key, locale, placeholderHash
    - Initialize Caffeine cache based on configuration
    - Implement cache lookup before parsing
    - Store parsed Component in cache after parsing
    - _Requirements: 2.1, 2.2_


  - [x] 4.2 Implement cache management

    - Add `invalidateCache()` method to MessageProvider
    - Call invalidateCache on translation reload
    - Add `getCacheStats()` method returning Caffeine CacheStats
    - _Requirements: 2.6, 2.7_


  - [x] 4.3 Write cache tests

    - Test cache hit scenario
    - Test cache miss scenario
    - Test cache invalidation on reload
    - _Requirements: 2.1, 2.6_

- [x] 5. Implement LocaleStorage System







  - [x] 5.1 Create LocaleStorage interface


    - Define `getLocale(UUID)` returning Optional<String>
    - Define `setLocale(UUID, String)` method
    - Define `removeLocale(UUID)` method
    - Define `clearAll()` method
    - _Requirements: 3.1_


  - [x] 5.2 Create InMemoryLocaleStorage

    - Implement using ConcurrentHashMap
    - Implement all interface methods
    - _Requirements: 3.2_

  - [x] 5.3 Create PlayerLocale entity


    - Create entity in `storage/entity` package
    - Extend BaseEntity from JEHibernate
    - Add uniqueId (UUID), locale (String), updatedAt (Instant) fields
    - Add JPA annotations for table `r18n_player_locale`
    - _Requirements: 3.4_



  - [x] 5.4 Create PlayerLocaleRepository

    - Create repository in `storage/repository` package
    - Extend GenericCachedRepository<PlayerLocale, Long, UUID>
    - _Requirements: 3.5_


  - [x] 5.5 Create DatabaseLocaleStorage

    - Implement LocaleStorage interface
    - Accept PlayerLocaleRepository in constructor
    - Implement all methods delegating to repository
    - _Requirements: 3.3_



  - [x] 5.6 Integrate LocaleStorage into R18nManager






    - Add LocaleStorage field to R18nManager
    - Add `localeStorage(LocaleStorage)` to Builder
    - Add `withDatabaseStorage(EntityManagerFactory)` to Builder
    - Default to InMemoryLocaleStorage if not provided


    - _Requirements: 3.6, 3.7, 3.8_

  - [x] 5.7 Update MessageBuilder to use LocaleStorage




    - Check LocaleStorage first for player locale
    - Fall back to player.getLocale() if not found
    - _Requirements: 3.9_

- [x] 6. Implement JSON Translation Support





  - [x] 6.1 Add Jackson to TranslationLoader


    - Initialize ObjectMapper with proper configuration
    - Add method to detect file type by extension
    - _Requirements: 4.4_


  - [x] 6.2 Implement JSON file loading
    - Create `loadJsonFile(Path)` method
    - Parse JSON using Jackson ObjectMapper
    - Flatten nested objects to dot-notation keys
    - Support arrays for multi-line messages
    - _Requirements: 4.1, 4.2, 4.3_


  - [x] 6.3 Update file loading logic
    - Modify `loadTranslationFile` to check extension
    - Route to appropriate loader (JSON or YAML)
    - Handle YAML precedence when both exist

    - _Requirements: 4.5_

  - [x] 6.4 Update resource extraction

    - Extract .json files from JAR alongside .yml
    - _Requirements: 4.6_

- [x] 7. Implement Plural Support





  - [x] 7.1 Create PluralRules utility


    - Create `PluralRules` class in `util` package
    - Implement `select(String locale, int count)` method
    - Support English, German, French, Spanish, Russian, Arabic, CJK rules
    - _Requirements: 6.4_



  - [x] 7.2 Add count placeholder to MessageBuilder





    - Add `countPlaceholders` map field
    - Implement `count(String placeholder, int value)` method


    - _Requirements: 6.2_

  - [x] 7.3 Implement plural key resolution




    - Create `resolvePluralKey(String baseKey, String locale)` method
    - Check for `.one`, `.other`, etc. suffixes
    - Fall back to `.other` then base key
    - _Requirements: 6.1, 6.3, 6.5_

- [x] 8. Implement Console Command Support





  - [x] 8.1 Update PR18nCommand for console


    - Remove `sender instanceof Player` check for reload
    - Add console-friendly output for reload command
    - _Requirements: 7.1_


  - [x] 8.2 Add console missing keys output
    - Create text-based output for missing command
    - Strip MiniMessage formatting for console
    - Add pagination info in plain text
    - _Requirements: 7.2, 7.3, 7.4_

- [x] 9. Implement File Watcher





  - [x] 9.1 Create TranslationFileWatcher class


    - Implement Runnable interface
    - Initialize WatchService for translations directory
    - Register for ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE events
    - _Requirements: 8.2, 8.3, 8.4, 8.5_


  - [x] 9.2 Implement watcher loop
    - Poll for events with timeout
    - Filter for translation file extensions
    - Trigger reload callback on changes
    - Add debouncing to prevent rapid reloads
    - _Requirements: 8.3, 8.6_


  - [x] 9.3 Integrate watcher into R18nManager

    - Start watcher thread if watchFiles enabled
    - Stop watcher on shutdown
    - _Requirements: 8.2, 8.7_

- [x] 10. Implement Translation Export






  - [x] 10.1 Create TranslationExportService

    - Create `ExportFormat` enum (CSV, JSON, YAML)
    - Create `export(Path, ExportFormat, translations)` method
    - _Requirements: 9.1, 9.2_


  - [x] 10.2 Implement export formats

    - Implement CSV export with key,locale,value columns
    - Implement JSON export with flat structure per locale
    - Implement YAML export
    - _Requirements: 9.3, 9.4, 9.5_


  - [x] 10.3 Add export command

    - Add `export <format>` subcommand to PR18nCommand
    - Export to plugin data folder
    - _Requirements: 9.6_

- [x] 11. Implement Missing Key Handler





  - [x] 11.1 Create MissingKeyHandler interface

    - Define in R18nConfiguration as functional interface
    - Accept key, locale, placeholders parameters
    - Return nullable String
    - _Requirements: 10.1, 10.3_


  - [x] 11.2 Integrate handler into MessageProvider

    - Call handler when key is missing
    - Use default handler if none provided
    - Handle null return (suppress message)
    - _Requirements: 10.2, 10.5_

- [x] 12. Implement Translation Metrics



  - [x] 12.1 Create TranslationMetrics class


    - Add keyUsage map with AtomicLong counters
    - Add missingKeys map with AtomicLong counters
    - Add localeUsage map with AtomicLong counters
    - _Requirements: 11.2, 11.3, 11.4, 12.4_


  - [x] 12.2 Implement metrics methods
    - Implement `recordKeyUsage(key, locale)` method
    - Implement `recordMissingKey(key, locale)` method
    - Implement `getMostUsedKeys(limit)` method
    - Implement `getMissingKeyOccurrences()` method
    - Implement `getLocaleDistribution()` method
    - _Requirements: 11.5, 11.6, 11.7, 11.8_


  - [x] 12.3 Integrate metrics into MessageProvider

    - Record key usage on successful resolution
    - Record missing key on fallback
    - Check metricsEnabled before recording
    - _Requirements: 11.2, 11.3_



  - [x] 12.4 Add metrics command

    - Add `metrics` subcommand to PR18nCommand
    - Display top used keys, missing keys, locale distribution
    - _Requirements: 11.9_

- [x] 13. Thread Safety Improvements
  - [x] 13.1 Update R18nManager instance field
    - Change `private static R18nManager instance` to AtomicReference
    - Update getInstance() to use AtomicReference.get()
    - Update instance assignment to use AtomicReference.set()
    - _Requirements: 12.1_

  - [x] 13.2 Verify thread safety


    - Ensure all shared state uses concurrent collections
    - Verify cache operations are thread-safe
    - Verify metrics counters use AtomicLong
    - _Requirements: 12.2, 12.3, 12.4, 12.5_

- [x] 14. Update Documentation






  - [x] 14.1 Update R18N-USAGE.md

    - Document new caching configuration
    - Document LocaleStorage options
    - Document JSON file support
    - Document plural support syntax
    - Document file watcher feature
    - Document export command
    - Document metrics command
    - Document missing key handler
    - _Requirements: All_
