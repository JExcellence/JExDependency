# Implementation Plan

- [ ] 1. Implement InfrastructureService
  - [ ] 1.1 Create InfrastructureService class implementing IInfrastructureService
    - Create `InfrastructureService.java` in `service/` package
    - Inject `InfrastructureManager`, `InfrastructureTickProcessor`, and `IslandInfrastructureRepository`
    - Implement `getInfrastructure()` method delegating to manager
    - Implement `getInfrastructureAsync()` method using repository
    - Implement `getManager()` and `getTickProcessor()` adapter methods
    - Add `start()` and `stop()` lifecycle methods
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 1.2 Integrate InfrastructureService into plugin lifecycle
    - Update `JExOneblock.initializeService()` to create and set InfrastructureService
    - Call `infraService.start()` during enable
    - Call `infraService.stop()` during disable
    - Register infrastructure for online players on enable
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 1.3 Update InfrastructurePlayerListener for service integration
    - Modify `onPlayerJoin` to register infrastructure via service
    - Modify `onPlayerQuit` to unregister infrastructure via service
    - _Requirements: 2.4, 2.5_

- [ ] 2. Create PIsland command structure
  - [ ] 2.1 Create EIslandAction enum
    - Create `EIslandAction.java` in `command/player/island/` package
    - Define all action values: MAIN, INFO, HOME, TP, SETHOME, MEMBERS, SETTINGS, UPGRADES, BIOME, EVOLUTION, ONEBLOCK, PRESTIGE, CREATE, DELETE, INVITE, KICK, BAN, UNBAN, TOP, HELP
    - Add `requiresIsland()` method to each action
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 2.2 Create EIslandPermission enum
    - Create `EIslandPermission.java` implementing IPermission
    - Define permissions for each subcommand following `jexoneblock.island.<action>` pattern
    - _Requirements: 10.1, 10.2, 10.4_

  - [ ] 2.3 Create PIslandSection configuration class
    - Create `PIslandSection.java` extending CommandSection
    - Define command name, aliases, and description
    - _Requirements: 3.1_

  - [ ] 2.4 Create PIsland command class
    - Create `PIsland.java` extending PlayerCommand
    - Implement `onPlayerInvocation()` with action switch
    - Implement `onPlayerTabCompletion()` for all subcommands
    - Add permission checks for each action
    - Handle no-island case for actions requiring island
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 10.3_

- [ ] 3. Extend IOneblockService interface
  - [ ] 3.1 Add missing methods to IOneblockService
    - Add `getPlayerIsland(Player)` method
    - Add `getIslandCore(Long)` method
    - Add `getAllEvolutions()` method
    - Add `getCurrentEvolution(Long)` method
    - Add `prestigeIsland(Long)` method
    - _Requirements: 4.1, 5.1, 14.1, 15.4_

  - [ ] 3.2 Implement new methods in PremiumOneblockService
    - Implement all new IOneblockService methods
    - Use repositories for data access
    - Use EvolutionFactory for evolution retrieval
    - _Requirements: 4.1, 5.1, 14.1, 15.4_

  - [ ] 3.3 Implement new methods in FreeOneblockService
    - Implement all new IOneblockService methods with free edition limitations
    - _Requirements: 4.1, 5.1, 14.1, 15.4_

- [ ] 4. Implement island information commands
  - [ ] 4.1 Implement info subcommand in PIsland
    - Create `sendIslandInfo()` method
    - Display level, experience, blocks broken, member count
    - Use i18n for all messages
    - _Requirements: 4.1_

  - [ ] 4.2 Implement level subcommand in PIsland
    - Create `sendLevelInfo()` method
    - Display current level and experience progress
    - _Requirements: 4.2_

  - [ ] 4.3 Implement top subcommand in PIsland
    - Create `showTopIslands()` method
    - Query top islands by level from repository
    - Display paginated leaderboard
    - _Requirements: 4.4_

- [ ] 5. Implement teleportation commands
  - [ ] 5.1 Implement home/tp subcommand in PIsland
    - Create `teleportToIsland()` method
    - Use OneblockService.teleportToIsland()
    - Send success/failure messages
    - _Requirements: 6.1, 6.4_

  - [ ] 5.2 Implement sethome subcommand in PIsland
    - Create `setIslandHome()` method
    - Validate player is within island region
    - Update island spawn location
    - Send success/error messages
    - _Requirements: 6.2, 6.3_

- [ ] 6. Implement member management commands
  - [ ] 6.1 Implement invite subcommand in PIsland
    - Create `invitePlayer()` method
    - Parse target player from args
    - Send invitation through island service
    - _Requirements: 7.2_

  - [ ] 6.2 Implement kick subcommand in PIsland
    - Create `kickPlayer()` method
    - Validate executor has permission
    - Remove member from island
    - _Requirements: 7.3_

  - [ ] 6.3 Implement ban/unban subcommands in PIsland
    - Create `banPlayer()` and `unbanPlayer()` methods
    - Use island ban repository
    - _Requirements: 7.4, 7.5_

- [ ] 7. Create BiomeManager
  - [ ] 7.1 Create BiomeManager class
    - Create `BiomeManager.java` in `manager/island/` package
    - Inject DistributedWorkloadRunnable
    - Create DistributedBiomeChanger instance
    - Implement `changeBiome()` method with progress callback
    - Implement `isChangingBiome()` and `cancelBiomeChange()` methods
    - Track active biome change tasks
    - _Requirements: 11.3, 11.4_

- [ ] 8. Create IslandUpgradeManager
  - [ ] 8.1 Create IslandUpgradeManager class
    - Create `IslandUpgradeManager.java` in `manager/island/` package
    - Define UpgradeType enum with SIZE, MEMBER_SLOTS, STORAGE_TIER, BIOME_TIER
    - Implement `applyUpgrade()` method
    - Implement `calculateCost()` method
    - Implement resource checking and consumption
    - _Requirements: 12.3, 12.4, 12.5_

- [ ] 9. Create island GUI views
  - [ ] 9.1 Create IslandMainView
    - Create `IslandMainView.java` in `view/island/` package
    - Define 5-row layout with navigation items
    - Add slots for: Info, OneBlock, Evolution, Members, Settings, Upgrades, Biome, Prestige, Home
    - Implement click handlers to open sub-views
    - _Requirements: 3.1_

  - [ ] 9.2 Create IslandInfoView
    - Create `IslandInfoView.java` displaying island statistics
    - Show level, experience, blocks broken, member count, creation date
    - _Requirements: 4.1, 4.3_

  - [ ] 9.3 Create IslandMembersView
    - Create `IslandMembersView.java` with paginated member list
    - Show member heads with roles
    - Add management options for owner
    - _Requirements: 7.1_

  - [ ] 9.4 Create IslandSettingsView
    - Create `IslandSettingsView.java` with toggle options
    - Add visitor permission toggles
    - Add public/private toggle
    - Persist changes on click
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ] 10. Create BiomeSelectionView
  - [ ] 10.1 Create BiomeSelectionView
    - Create `BiomeSelectionView.java` in `view/island/` package
    - Define biome categories (plains, forest, desert, ocean, snow, nether, end)
    - Implement paginated biome display
    - Show locked biomes with requirements
    - _Requirements: 11.1, 11.2, 11.6_

  - [ ] 10.2 Implement biome selection click handler
    - Check player resources/permissions
    - Call BiomeManager.changeBiome()
    - Show progress feedback
    - Send completion message
    - _Requirements: 11.3, 11.4, 11.5_

- [ ] 11. Create IslandUpgradesView
  - [ ] 11.1 Create IslandUpgradesView
    - Create `IslandUpgradesView.java` in `view/island/` package
    - Display all upgrade types with current/next level
    - Show resource requirements
    - _Requirements: 12.1, 12.2, 12.3_

  - [ ] 11.2 Implement upgrade click handler
    - Call IslandUpgradeManager.applyUpgrade()
    - Handle success/failure responses
    - Refresh view on success
    - _Requirements: 12.4, 12.5, 12.6_

- [ ] 12. Create OneblockCoreView
  - [ ] 12.1 Create OneblockCoreView
    - Create `OneblockCoreView.java` in `view/island/` package
    - Display evolution name, level, experience progress bar
    - Show blocks broken count and prestige level
    - _Requirements: 13.1, 13.2_

  - [ ] 12.2 Implement drop rate displays
    - Show block drops by rarity tier
    - Show item drops by rarity tier
    - Show entity spawn chances
    - _Requirements: 13.3, 13.4, 13.5_

  - [ ] 12.3 Add evolution showcase navigation
    - Click evolution item to open EvolutionBrowserView
    - _Requirements: 13.6_

- [ ] 13. Create EvolutionBrowserView
  - [ ] 13.1 Create EvolutionBrowserView
    - Create `EvolutionBrowserView.java` in `view/island/` package
    - Implement paginated evolution display (21 per page)
    - Show locked evolutions with requirements
    - Highlight current evolution
    - _Requirements: 14.1, 14.2, 14.3_

  - [ ] 13.2 Implement evolution click handler
    - Open EvolutionDetailView for unlocked evolutions
    - _Requirements: 14.4_

  - [ ] 13.3 Add filtering support
    - Add category filter option
    - Add search by name option
    - _Requirements: 14.6_

- [ ] 14. Create EvolutionDetailView
  - [ ] 14.1 Create EvolutionDetailView
    - Create `EvolutionDetailView.java` in `view/island/` package
    - Display all blocks, items, entities for evolution
    - Organize by rarity tier with visual indicators
    - _Requirements: 14.4, 14.5_

- [ ] 15. Create PrestigeConfirmView
  - [ ] 15.1 Create PrestigeConfirmView
    - Create `PrestigeConfirmView.java` in `view/island/` package
    - Display what will be reset
    - Display prestige rewards (XP bonus, drop bonus, prestige points)
    - _Requirements: 15.3, 15.5_

  - [ ] 15.2 Implement prestige confirmation handler
    - Call OneblockService.prestigeIsland()
    - Update OneblockCore prestige level
    - Send completion notification
    - _Requirements: 15.4, 15.6_

- [ ] 16. Implement prestige command logic
  - [ ] 16.1 Implement prestige subcommand in PIsland
    - Create `handlePrestige()` method
    - Check prestige requirements
    - Show requirements if not met
    - Open PrestigeConfirmView if requirements met
    - _Requirements: 15.1, 15.2_

- [ ] 17. Register command and views
  - [ ] 17.1 Register PIsland command
    - Add PIslandSection to config
    - Register command in plugin initialization
    - _Requirements: 3.1_

  - [ ] 17.2 Register all island views with ViewFrame
    - Register IslandMainView, IslandInfoView, IslandMembersView, IslandSettingsView
    - Register IslandUpgradesView, BiomeSelectionView
    - Register OneblockCoreView, EvolutionBrowserView, EvolutionDetailView
    - Register PrestigeConfirmView
    - _Requirements: 3.1, 5.1, 11.1, 12.1, 13.1, 14.1, 15.3_

- [ ]* 18. Add translation keys
  - [ ]* 18.1 Add island command translation keys
    - Add keys for all command messages
    - Add keys for all view titles and items
    - Add keys for error messages
    - _Requirements: 3.2, 6.3, 6.4, 10.3, 11.5, 12.5_
