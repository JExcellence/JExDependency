# Implementation Plan

- [x] 1. Set up project structure and build configuration


  - Create JExGlow module directory structure following JExHome pattern
  - Configure build.gradle.kts with Paper API, RPlatform, JExCommand, JExTranslate dependencies
  - Create plugin.yml with plugin metadata, dependencies (RCore), and API version 1.21
  - Create settings.gradle.kts to include JExGlow module in root project
  - _Requirements: 6.1, 6.2, 6.3, 6.4_



- [x] 2. Implement database layer



  - [x] 2.1 Create PlayerGlow entity class

    - Write PlayerGlow.java extending BaseEntity with fields: playerUuid, glowEnabled, enabledAt, enabledBy, updatedAt


    - Add JPA annotations: @Entity, @Table(name="jexglow_player_glow"), @Index on playerUuid
    - Implement constructors, getters, setters, equals, hashCode, toString methods
    - _Requirements: 1.2, 2.2, 3.2, 4.2_
  - [x] 2.2 Create GlowRepository class

    - Write GlowRepository.java extending CachedRepository<PlayerGlow, Long, UUID>
    - Implement findByPlayerUuid(UUID) returning CompletableFuture<Optional<PlayerGlow>>


    - Implement saveGlowState(PlayerGlow) returning CompletableFuture<PlayerGlow>
    - Implement deleteByPlayerUuid(UUID) returning CompletableFuture<Boolean>
    - _Requirements: 1.2, 2.2, 3.2, 4.2, 6.5_

- [x] 3. Implement service layer




  - [x] 3.1 Create IGlowService interface

    - Define enableGlow(UUID playerId, UUID adminId) returning CompletableFuture<Boolean>
    - Define disableGlow(UUID playerId) returning CompletableFuture<Boolean>
    - Define isGlowEnabled(UUID playerId) returning CompletableFuture<Boolean>
    - Define applyGlowEffect(Player player) returning CompletableFuture<Void>
    - Define removeGlowEffect(Player player) returning CompletableFuture<Void>
    - _Requirements: 1.1, 2.1, 3.1, 4.1_
  - [x] 3.2 Implement GlowService class


    - Write GlowService.java implementing IGlowService with GlowRepository dependency
    - Implement enableGlow: create/update PlayerGlow entity, persist to database, apply visual effect if player online
    - Implement disableGlow: update PlayerGlow entity, persist to database, remove visual effect if player online
    - Implement isGlowEnabled: query repository for player's glow state
    - Implement applyGlowEffect: schedule player.setGlowing(true) on main thread via RPlatform scheduler


    - Implement removeGlowEffect: schedule player.setGlowing(false) on main thread
    - Add error handling with CentralLogger for all database operations


    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.3, 4.1, 4.3, 8.1, 8.4, 8.5_



- [x] 4. Create GlowFactory for centralized service access




  - Write GlowFactory.java with static methods: initialize, getService, getRepository, reset
  - Implement singleton pattern following HomeFactory design
  - Add null checks and initialization validation
  - _Requirements: 6.1_

- [x] 5. Implement command layer


  - [x] 5.1 Create GlowCommandSection configuration class

    - Write GlowCommandSection.java extending ACommandSection
    - Define fields: name, aliases, description, usage, permission
    - _Requirements: 1.4, 2.4, 6.2, 7.1_
  - [x] 5.2 Create command configuration file


    - Write commands/glow.yml with command metadata
    - Set name: "glow", permission: "jexglow.admin", usage: "/glow <on|off> <player>"
    - _Requirements: 1.4, 2.4, 6.2_
  - [x] 5.3 Implement GlowCommand class

    - Write GlowCommand.java extending BukkitCommand with constructor(GlowCommandSection, JExGlow)
    - Implement execute method: validate permission, parse arguments (on/off, player name)


    - Resolve target player using Bukkit.getPlayer, return error if not found
    - Call GlowService.enableGlow or disableGlow based on argument
    - Send translated feedback messages using TranslationManager
    - Handle errors: invalid arguments → usage message, player not found → error message, service failure → error message
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 2.1, 2.3, 2.4, 2.5, 6.2, 7.3, 8.2_

- [x] 6. Implement event listener for persistence

  - Write GlowEventListener.java implementing Listener with constructor(JExGlow)


  - Implement onPlayerJoin: query glow state asynchronously, schedule glow application on main thread with 300ms delay if enabled


  - Implement onPlayerRespawn: query glow state asynchronously, schedule glow application on main thread with 100ms delay if enabled
  - Add error handling and logging for all async operations


  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 8.1, 8.4, 8.5_

- [x] 7. Implement PlaceholderAPI integration

  - Write GlowPlaceholder.java extending PlaceholderExpansion


  - Set identifier: "jexglow", placeholder: "status"
  - Implement onPlaceholderRequest: query repository for player's glow state, return "true" or "false"
  - Add caching via repository cache for 50ms target latency
  - Register placeholder conditionally if PlaceholderAPI is present
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 8. Implement main plugin class

  - Write JExGlow.java extending JavaPlugin
  - Implement onLoad: initialize JEDependency with remapping
  - Implement onEnable: initialize RPlatform asynchronously, initialize database resources, register repositories, create GlowService, initialize GlowFactory, register commands via CommandFactory, register event listeners, register PlaceholderAPI if available, initialize metrics
  - Implement onDisable: reset GlowFactory, shutdown executors, cleanup resources
  - Add error handling: disable plugin gracefully if database initialization fails
  - Follow JExHome initialization pattern with CompletableFuture-based async initialization
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 8.3, 8.4, 8.5_

- [x] 9. Create translation files

  - [x] 9.1 Create English translation file

    - Write translations/en_US.yml with keys: glow.command.description, glow.command.enabled, glow.command.disabled, glow.command.player-not-found, glow.command.usage, glow.error.database, glow.error.general
    - _Requirements: 7.1, 7.2, 7.4_
  - [x] 9.2 Create German translation file

    - Write translations/de_DE.yml with German translations for all keys from en_US.yml
    - _Requirements: 7.1, 7.2, 7.4_

- [x] 10. Create database configuration


  - Write database/hibernate.properties with H2 database configuration
  - Configure hibernate.hbm2ddl.auto=update for automatic schema creation
  - Set connection pool settings and logging levels
  - _Requirements: 6.4, 8.3_




- [-] 11. Update root project configuration

  - Add JExGlow module to root settings.gradle.kts include statement
  - Update root build.gradle.kts if needed for multi-module dependencies
  - _Requirements: 6.1_

- [ ]* 12. Write unit tests for service layer
  - Create GlowServiceTest.java with JUnit 5
  - Test enableGlow: verify database update and effect application
  - Test disableGlow: verify database update and effect removal
  - Test isGlowEnabled: verify correct boolean returned
  - Test error handling: verify exceptions caught and logged
  - Mock GlowRepository using Mockito
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 8.1_

- [ ]* 13. Write integration tests for repository layer
  - Create GlowRepositoryTest.java with JUnit 5 and H2 in-memory database
  - Test create PlayerGlow entity: verify persistence
  - Test query by UUID: verify retrieval
  - Test update glow state: verify changes persisted
  - Test delete entity: verify removal
  - _Requirements: 1.2, 2.2, 3.2, 4.2_
