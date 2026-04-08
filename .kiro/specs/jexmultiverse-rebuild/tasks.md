# Implementation Plan

- [x] 1. Set up project structure and build configuration



  - Create Gradle build files for jexmultiverse-common, jexmultiverse-free, jexmultiverse-premium modules
  - Configure dependencies: RPlatform, Hibernate, InventoryFramework, JExTranslate, Caffeine
  - Set up plugin.yml and paper-plugin.yml for each edition
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Implement database layer





  - [x] 2.1 Create MVWorldType enum


    - Define DEFAULT, VOID, PLOT types
    - _Requirements: 3.3_

  - [x] 2.2 Create LocationConverter

    - Implement AttributeConverter for Location to JSON serialization
    - Handle null locations and world references
    - _Requirements: 2.5_

  - [x] 2.3 Create MVWorld entity

    - Define JPA entity with all fields: identifier, type, environment, spawnLocation, globalizedSpawn, pvpEnabled, enterPermission
    - Implement Builder pattern for construction
    - _Requirements: 2.1, 2.2_

  - [x] 2.4 Create MVWorldRepository

    - Extend AbstractCRUDRepository with Caffeine caching
    - Implement findByIdentifier, findByGlobalSpawn, findAll with cache support
    - Implement async variants: findByIdentifierAsync, findByGlobalSpawnAsync, findAllAsync
    - _Requirements: 2.3, 2.4_

- [x] 3. Implement world generators





  - [x] 3.1 Create VoidChunkGenerator and VoidBiomeProvider

    - Generate empty void world with THE_VOID biome
    - Set fixed spawn at y=96
    - Disable all generation flags
    - _Requirements: 7.1, 7.4_

  - [x] 3.2 Create PlotChunkGenerator, PlotBiomeProvider, PlotBlockPopulator, PlotLayer

    - Generate grid-based plot world with configurable parameters
    - Implement plot borders, roads, and floor layers
    - Use PLAINS biome
    - _Requirements: 7.2, 7.3, 7.4_

- [x] 4. Implement service layer





  - [x] 4.1 Create IMultiverseService interface

    - Define isPremium, getMaxWorlds, getMaxWorldTypes methods
    - Define world CRUD operations returning CompletableFuture
    - Define spawn management operations
    - _Requirements: 3.1, 3.2, 4.1, 4.2_

  - [x] 4.2 Create WorldFactory

    - Implement world cache management
    - Implement createBukkitWorld with generator selection based on MVWorldType
    - Implement loadAllWorlds for startup
    - Implement unloadWorld for cleanup
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 5. Implement API layer 

  - [x] 5.1 Create IMultiverseAdapter interface

    - Define getGlobalMVWorld, getMVWorld, hasMultiverseSpawn, spawn methods
    - All methods return CompletableFuture
    - _Requirements: 8.1, 8.3_

  - [x] 5.2 Create MultiverseAdapter implementation

    - Implement all interface methods using repository and TeleportFactory
    - Handle fallback logic for spawn (global -> world -> default)
    - _Requirements: 8.1, 8.2, 8.3_

- [x] 6. Implement command layer






  - [x] 6.1 Create command enums and sections

    - Create EMultiverseAction enum: CREATE, DELETE, EDIT, TELEPORT, LOAD, HELP
    - Create EMultiversePermission enum implementing IPermissionNode
    - Create PMultiverseSection extending ACommandSection
    - Create ESpawnPermission enum and PSpawnSection
    - _Requirements: 5.1, 5.2, 5.4_

  - [x] 6.2 Create PMultiverse command

    - Implement PlayerCommand with action routing
    - Implement handleCreate, handleDelete, handleEdit, handleTeleport, handleLoad, help methods
    - Implement tab completion for actions, world names, environments, types
    - _Requirements: 5.1, 5.3, 5.4, 5.5_
  - [x] 6.3 Create PSpawn command


    - Implement PlayerCommand for spawn teleportation
    - Use MultiverseAdapter.spawn() for teleportation logic
    - _Requirements: 5.2, 4.4_

  - [x] 6.4 Create command YAML configuration files

    - Create commands/pmultiverse.yml with permissions and messages
    - Create commands/pspawn.yml with permissions and messages
    - _Requirements: 5.3_

- [x] 7. Implement view layer






  - [x] 7.1 Create MultiverseEditorView

    - Extend BaseView with 6-row layout
    - Implement spawn location setting slot with click handler
    - Implement global spawn toggle slot with click handler
    - Implement PvP toggle slot (if applicable)
    - Use I18n for all display text
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 8. Implement listener layer





  - [x] 8.1 Create SpawnListener


    - Handle PlayerSpawnLocationEvent with HIGHEST priority
    - Handle PlayerRespawnEvent with HIGHEST priority
    - Implement handleSpawn logic: check global spawn, fallback to world spawn
    - _Requirements: 10.1, 10.2, 10.3_

- [x] 9. Implement main plugin class






  - [x] 9.1 Create abstract JExMultiverse class

    - Define fields: plugin, edition, executor, platform, viewFrame, worldFactory
    - Inject MVWorldRepository using @InjectRepository
    - Implement onEnable: initialize platform, repositories, components, views, load worlds
    - Implement onDisable: shutdown executor, cleanup resources
    - Define abstract methods: getStartupMessage, getMetricsId, registerViews, createMultiverseService
    - Register MultiverseAdapter as Bukkit service
    - _Requirements: 1.1, 1.5, 8.2_

- [x] 10. Implement free edition





  - [x] 10.1 Create FreeMultiverseService


    - Implement IMultiverseService with limited functionality
    - Set max worlds limit (e.g., 3)
    - Restrict to DEFAULT and VOID world types only
    - _Requirements: 1.3_

  - [x] 10.2 Create FreeJExMultiverse

    - Extend JExMultiverse
    - Implement abstract methods with free edition values
    - _Requirements: 1.3_

- [x] 11. Implement premium edition





  - [x] 11.1 Create PremiumMultiverseService

    - Implement IMultiverseService with full functionality
    - No world limits
    - All world types available
    - _Requirements: 1.4_

  - [x] 11.2 Create PremiumJExMultiverse

    - Extend JExMultiverse
    - Implement abstract methods with premium edition values
    - _Requirements: 1.4_

- [x] 12. Create translation files






  - [x] 12.1 Create en_US.yml translation file

    - Add prefix with gradient styling
    - Add all multiverse command messages with placeholders
    - Add multiverse_editor_ui messages for GUI
    - Add spawn command messages
    - Add teleport messages
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 12.2 Create de_DE.yml translation file

    - Translate all keys from en_US.yml to German
    - _Requirements: 9.2_

- [x] 13. Create resource configuration files






  - [x] 13.1 Create hibernate.properties and database configuration

    - Configure entity classes for MVWorld
    - _Requirements: 2.1_

  - [x] 13.2 Create plugin.yml and paper-plugin.yml for each module

    - Define plugin metadata, dependencies, permissions
    - _Requirements: 1.1_
