# Implementation Plan

- [x] 1. Create Service Layer





  - [x] 1.1 Create IHomeService interface in jexhome-common


    - Define async methods: createHome, deleteHome, findHome, getPlayerHomes, teleportToHome
    - Add validation methods: canCreateHome, getMaxHomesForPlayer, getHomeCount
    - Add premium feature methods: getHomesByCategory, getFavoriteHomes, setHomeCategory, toggleFavorite
    - Add isPremium() method for edition detection


    - _Requirements: 1.1, 1.4, 1.5_


  - [x] 1.2 Create FreeHomeService in jexhome-free

    - Implement IHomeService with 3 home limit


    - Disable premium features (categories, favorites)
    - Return upgrade messages for premium feature attempts
    - _Requirements: 9.1, 9.2, 9.5_





  - [x] 1.3 Create PremiumHomeService in jexhome-premium

    - Implement IHomeService with configurable limits
    - Enable all premium features

    - Use HomeRepository for persistence
    - _Requirements: 9.3, 9.4_

- [x] 2. Create HomeFactory for Business Logic

  - [x] 2.1 Create HomeFactory singleton class


    - Implement getInstance() and initialize() pattern from BountyFactory
    - Add home cache using ConcurrentHashMap
    - Inject IHomeService and HomeSystemConfig
    - _Requirements: 7.1, 7.2_


  - [x] 2.2 Implement core factory methods


    - createHome() with permission and limit validation

    - deleteHome() with cache invalidation
    - getPlayerHomes() with caching
    - _Requirements: 7.3, 7.4_

  - [x] 2.3 Implement teleport warmup system


    - Create TeleportWarmupTask for delayed teleport

    - Handle movement cancellation based on config
    - Handle damage cancellation based on config
    - Show countdown in action bar
    - _Requirements: 7.5_




- [x] 3. Enhance Home Entity

  - [x] 3.1 Add metadata fields to Home entity

    - Add category (String, default "default")




    - Add favorite (boolean, default false)
    - Add description (String, nullable)
    - Add icon (String, default "PLAYER_HEAD")
    - Add visitCount (int, default 0)

    - Add lastVisited (LocalDateTime, nullable)
    - Add createdAt (LocalDateTime, not null)
    - _Requirements: 4.1, 4.2_


  - [x] 3.2 Add entity methods

    - recordVisit() to increment visitCount and update lastVisited

    - getFormattedLocation() for display
    - Ensure LocationConverter is properly used
    - _Requirements: 4.3, 4.4_

  - [x] 3.3 Update HomeRepository with new query methods

    - Add findByCategory(UUID, String)
    - Add findFavorites(UUID)
    - Ensure countByPlayerUuid works correctly





    - _Requirements: 4.5_

- [x] 4. Create Configuration Sections


  - [x] 4.1 Create TeleportSection config class


    - Add delay, cancelOnMove, cancelOnDamage fields
    - Add showCountdown, playSounds, showParticles fields
    - Implement getters with sensible defaults

    - _Requirements: 6.4_



  - [x] 4.2 Create ColorSchemeSection config class
    - Add primaryGradient (#1e3a8a:#60a5fa)

    - Add secondaryGradient (#ea580c:#fb923c)
    - Add successGradient, errorGradient, warningGradient
    - Add formatPrimary() and formatSecondary() helper methods
    - _Requirements: 6.3_



  - [x] 4.3 Update HomeSystemConfig to use new sections
    - Add TeleportSection teleport field
    - Add ColorSchemeSection colors field
    - Update getters to return section instances with defaults
    - _Requirements: 6.1, 6.2, 6.5_

  - [x] 4.4 Update home-system.yml config file




    - Add teleport section with all settings
    - Add colors section with gradient definitions
    - _Requirements: 6.2_

- [x] 5. Fix Command Implementations

  - [x] 5.1 Refactor PSetHome to use HomeFactory

    - Replace direct repository calls with HomeFactory.createHome()



    - Use proper async error handling with exceptionally()
    - Set location on new Home entity before saving

    - _Requirements: 2.1, 2.2_


  - [x] 5.2 Refactor PHome to use HomeFactory

    - Use HomeFactory for teleport operations
    - Implement teleport warmup via HomeFactory.teleportWithWarmup()
    - _Requirements: 2.3, 2.4_

  - [x] 5.3 Refactor PDelHome to use HomeFactory

    - Replace direct repository calls with HomeFactory.deleteHome()
    - Use proper async error handling
    - _Requirements: 2.5_

  - [x] 5.4 Add proper error handling to all commands

    - Catch and handle HomeLimitReachedException
    - Catch and handle HomeNotFoundException
    - Log errors at SEVERE level
    - Send user-friendly messages
    - _Requirements: 2.6, 2.7, 10.2, 10.3_

- [x] 6. Update Translation Files

  - [x] 6.1 Update en_US.yml with gradient colors


    - Update prefix with gradient
    - Update all home.* keys with gradient syntax
    - Update all sethome.* keys with gradient syntax
    - Update all delhome.* keys with gradient syntax
    - Add home_overview_ui.title key
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_







  - [x] 6.2 Update de_DE.yml with gradient colors
    - Mirror all changes from en_US.yml
    - Translate new keys to German

    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_



  - [x] 6.3 Add missing i18n keys
    - Add home.options_coming_soon

    - Add teleport.warmup, teleport.cancelled.moved, teleport.cancelled.damaged
    - Add sethome.home_limit_reached with placeholders
    - _Requirements: 3.6_

- [x] 7. Enhance HomeOverviewView

  - [x] 7.1 Update view layout with filter/sort buttons


    - Add filter row at top (F slots)
    - Add sort button (S slot)
    - Add create button (C slot)
    - Update pagination layout
    - _Requirements: 5.1, 5.3, 5.4_



  - [x] 7.2 Implement filter functionality
    - Add filterMode state
    - Implement "all", "favorites", "category" filters

    - Apply filter in getAsyncPaginationSource
    - _Requirements: 5.3_

  - [x] 7.3 Implement sort functionality

    - Add sortMode state
    - Implement "name", "created", "visited" sorts
    - Apply sort in getAsyncPaginationSource
    - _Requirements: 5.4_




  - [x] 7.4 Update renderEntry with gradient colors
    - Use ColorSchemeSection for formatting
    - Display visit count and last visited
    - Use home icon material
    - _Requirements: 5.2, 5.7_


  - [x] 7.5 Implement click handlers

    - Left-click: teleport via HomeFactory

    - Right-click: open HomeManagementView (placeholder)


    - _Requirements: 5.5, 5.6_



- [x] 8. Update JExHome Main Class

  - [x] 8.1 Add IHomeService field and initialization


    - Create service in edition-specific impl classes
    - Initialize HomeFactory with service and config
    - _Requirements: 1.1_

  - [x] 8.2 Update initializeComponents to use factory

    - Pass HomeFactory to commands instead of JExHome
    - Ensure proper initialization order
    - _Requirements: 7.1_

  - [x] 8.3 Add getHomeService() method

    - Return IHomeService for views and commands
    - _Requirements: 1.4_

- [x] 9. Create Custom Exceptions


  - [x] 9.1 Create HomeLimitReachedException

    - Include current count and max limit
    - _Requirements: 10.3_

  - [x] 9.2 Create HomeNotFoundException

    - Include home name
    - _Requirements: 10.3_

  - [x] 9.3 Create WorldNotLoadedException

    - Include world name
    - _Requirements: 10.3_

- [x] 10. Update Edition Implementations

  - [x] 10.1 Update JExHomeFreeImpl


    - Create FreeHomeService instance
    - Pass to JExHome initialization
    - _Requirements: 9.1, 9.2_

  - [x] 10.2 Update JExHomePremiumImpl

    - Create PremiumHomeService instance
    - Pass to JExHome initialization
    - _Requirements: 9.3, 9.4_

- [ ] 11. Write Unit Tests
  - [ ] 11.1 Test HomeFactory business logic
    - Test createHome with limit validation
    - Test deleteHome with cache invalidation
    - _Requirements: 7.3_

  - [ ] 11.2 Test IHomeService implementations
    - Test FreeHomeService limits
    - Test PremiumHomeService features
    - _Requirements: 9.1, 9.3_
