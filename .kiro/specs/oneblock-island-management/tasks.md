# Implementation Plan

- [x] 1. Implement IInfrastructureService




  - [ ] 1.1 Create InfrastructureServiceImpl class
    - Implement IInfrastructureService interface in `service/InfrastructureServiceImpl.java`
    - Add constructor with InfrastructureManager, InfrastructureTickProcessor, and IslandInfrastructureRepository
    - Implement getInfrastructure(Long islandId, UUID playerId) with caching
    - Implement getInfrastructureAsync(Long islandId) returning CompletableFuture
    - Implement getManager() and getTickProcessor() getters
    - Add initialize() and shutdown() methods for lifecycle management


    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 1.2 Integrate InfrastructureServiceImpl into JExOneblock
    - Update JExOneblock.java to create InfrastructureServiceImpl in initializeService()





    - Call infrastructureService.initialize() in onEnable()
    - Call infrastructureService.shutdown() in onDisable()
    - Register infrastructure for players on join, unregister on quit


    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 2. Create PIsland command structure

  - [x] 2.1 Create EIslandAction enum


    - Create `command/player/island/EIslandAction.java`
    - Define actions: MAIN, INFO, STATS, LEVEL, TOP, EVOLUTION, ONEBLOCK, PRESTIGE, HOME, TP, SETHOME, MEMBERS, INVITE, KICK, BAN, UNBAN, LEAVE, SETTINGS, VISITORS, BIOME, UPGRADES, CREATE, DELETE, HELP
    - _Requirements: 2.1, 2.2_



  - [x] 2.2 Create EIslandPermission enum

    - Create `command/player/island/EIslandPermission.java`
    - Define permission nodes following pattern `jexoneblock.island.<action>`
    - Include base COMMAND permission
    - _Requirements: 10.1, 10.2, 10.3, 10.4_




  - [x] 2.3 Create PIslandSection configuration class

    - Create `command/player/island/PIslandSection.java`
    - Extend appropriate command section base class
    - Configure command name, aliases (island, is, oneblock, ob), description


    - _Requirements: 2.1_

  - [x] 2.4 Create PIsland command class

    - Create `command/player/island/PIsland.java`
    - Extend PlayerCommand with @Command annotation
    - Implement onPlayerInvocation with switch on EIslandAction



    - Implement onPlayerTabCompletion for subcommand suggestions
    - Add permission checks using hasNoPermission()
    - Handle no-island case with localized message
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Implement island service classes


  - [x] 3.1 Create IslandMemberService

    - Create `service/IslandMemberService.java`
    - Implement addMember(), removeMember(), updateRole(), getMembers(), getActiveMembers()
    - Implement getMemberRole() for permission checking
    - Use OneblockIslandMemberRepository for persistence
    - _Requirements: 9.1, 9.3, 10.1, 10.3, 11.1_

  - [x] 3.2 Create IslandBanService

    - Create `service/IslandBanService.java`
    - Implement banPlayer() with duration and reason support
    - Implement unbanPlayer() with unbannedBy tracking
    - Implement getActiveBans(), isBanned()
    - Add cleanupExpiredBans() for periodic cleanup
    - _Requirements: 7.1, 7.4, 8.1, 8.2_

  - [x] 3.3 Create IslandInviteService

    - Create `service/IslandInviteService.java`
    - Implement sendInvite() creating pending OneblockIslandMember
    - Implement acceptInvite(), declineInvite()
    - Implement getPendingInvites()
    - Add configurable expiration and cleanupExpiredInvites()
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.6_

  - [x] 3.4 Create BiomeService


    - Create `service/BiomeService.java`
    - Inject DistributedBiomeChanger
    - Implement changeBiome() with async operation and progress callback
    - Implement getAvailableBiomes(), canUseBiome()
    - Implement getBiomesByCategory() for organized display
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [x] 3.5 Create UpgradeService


    - Create `service/UpgradeService.java`
    - Define UpgradeType enum (SIZE_EXPANSION, MEMBER_SLOTS, STORAGE_CAPACITY, BIOME_TIER)
    - Implement purchaseUpgrade(), getCurrentLevel(), getNextLevelRequirements()
    - Implement canAffordUpgrade() checking resources
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6_

  - [x] 3.6 Create PrestigeService


    - Create `service/PrestigeService.java`
    - Define PrestigeRequirements and PrestigeRewards records
    - Implement canPrestige(), getRequirements(), getRewards()
    - Implement performPrestige() resetting progress and applying bonuses
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6_

- [-] 4. Implement island views

  - [x] 4.1 Create IslandMainView


    - Create `view/island/IslandMainView.java`
    - Display island name, level, evolution, owner head
    - Add navigation buttons for all sub-views
    - Display quick stats (members, blocks broken, coins)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 4.2 Create EvolutionOverviewView



    - Create `view/island/EvolutionOverviewView.java`
    - Display current evolution name, level, progress percentage
    - Show blocks broken and blocks until next evolution
    - Add visual progress bar
    - Show unlockable content preview
    - Add navigation to EvolutionBrowserView



    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ] 4.3 Create EvolutionBrowserView
    - Create `view/island/EvolutionBrowserView.java`
    - Display all evolutions with pagination

    - Show lock/unlock status with requirements


    - Highlight current evolution
    - Click to view evolution details (blocks, items, entities by rarity)
    - _Requirements: 4.5, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_




  - [ ] 4.4 Create OneblockCoreView
    - Create `view/island/OneblockCoreView.java`
    - Display evolution name, level, experience progress bar
    - Show blocks broken count, prestige level
    - Display block drop rates by rarity tier
    - Show entity spawn chances, item drop chances



    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

  - [ ] 4.5 Create VisitorSettingsView
    - Create `view/island/VisitorSettingsView.java`
    - Display all 22 visitor permissions as toggle items
    - Implement click handlers to toggle permissions



    - Add preset buttons (Allow All, Deny All, Basic, Trusted)
    - Check MODERATOR+ role for access
    - Persist changes immediately
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_




  - [ ] 4.6 Create MembersListView
    - Create `view/island/MembersListView.java`
    - Display owner at top with crown indicator
    - List members sorted by role with online status
    - Support pagination
    - Click for management options (if permitted)



    - Show join date, last activity
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [ ] 4.7 Create MemberPermissionView
    - Create `view/island/MemberPermissionView.java`



    - Display all members with current roles
    - Click to open role selection interface
    - Implement role change with permission checks
    - Show member details (join date, invited by, activity)
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ] 4.8 Create BannedPlayersView
    - Create `view/island/BannedPlayersView.java`
    - Display active bans with player name, reason, date
    - Support pagination
    - Show expiration for temporary bans
    - Visual distinction between permanent/temporary
    - Click to unban or view details
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [ ] 4.9 Create IslandSettingsView
    - Create `view/island/IslandSettingsView.java`
    - Display name editor, description editor, privacy toggle
    - Validate name length and content
    - Add reset island button with confirmation
    - Check CO_OWNER+ role for access
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ] 4.10 Create BiomeSelectionView
    - Create `view/island/BiomeSelectionView.java`
    - Display biomes organized by category
    - Show locked biomes with requirements
    - Implement biome selection with progress feedback
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [ ] 4.11 Create IslandUpgradesView
    - Create `view/island/IslandUpgradesView.java`
    - Display upgrade categories (Size, Members, Storage, Biomes)
    - Show current level, next level benefits, requirements
    - Implement purchase click handler
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6_

  - [ ] 4.12 Create PrestigeConfirmView
    - Create `view/island/PrestigeConfirmView.java`
    - Display current prestige level, next level rewards
    - Show what will be reset vs preserved
    - Display XP multiplier bonus, drop rate bonus
    - Add confirm/cancel buttons
    - _Requirements: 15.3, 15.4, 15.5_

- [ ] 5. Implement PIsland command handlers
  - [ ] 5.1 Implement info/stats/level subcommands
    - Add info handler displaying island statistics
    - Add stats handler opening statistics GUI
    - Add level handler showing level and experience progress
    - Add top handler displaying leaderboard
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ] 5.2 Implement teleportation subcommands
    - Add home/tp handler teleporting to island spawn
    - Add sethome handler setting spawn location
    - Validate player is within island region for sethome
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ] 5.3 Implement member management subcommands
    - Add invite handler using IslandInviteService
    - Add kick handler with permission checks
    - Add ban handler with duration/reason support
    - Add unban handler
    - Add leave handler for members
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ] 5.4 Implement settings subcommands
    - Add settings handler opening IslandSettingsView
    - Add visitors handler opening VisitorSettingsView
    - Add biome handler opening BiomeSelectionView
    - Add upgrades handler opening IslandUpgradesView
    - _Requirements: 5.1, 5.4, 11.1, 12.1_

  - [ ] 5.5 Implement lifecycle subcommands
    - Add create handler for new islands
    - Add delete handler with confirmation
    - Add prestige handler checking requirements
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 15.1, 15.2_

- [ ] 6. Register views and commands
  - [ ] 6.1 Register all island views with ViewFrame
    - Update JExOneblock.registerViews() to include all 12 island views
    - Ensure proper view initialization order
    - _Requirements: 3.1_

  - [ ] 6.2 Register PIsland command
    - Add PIsland command registration in initializeComponents()
    - Configure command aliases (island, is, oneblock, ob)
    - _Requirements: 2.1_

- [ ] 7. Add translation keys
  - [ ] 7.1 Add island command translations
    - Add all island.command.* keys
    - Add island.info.* keys
    - Add island.teleport.* keys
    - _Requirements: 2.3, 2.4_

  - [ ] 7.2 Add member management translations
    - Add island.invite.* keys
    - Add island.kick.* keys
    - Add island.ban.* keys
    - Add island.unban.* keys
    - Add island.member.* keys
    - _Requirements: 6.4, 7.3, 9.2, 11.3_

  - [ ] 7.3 Add view and feature translations
    - Add island.settings.* keys
    - Add island.biome.* keys
    - Add island.upgrade.* keys
    - Add island.prestige.* keys
    - Add island.evolution.* keys
    - Add island.view.* title keys
    - _Requirements: 5.5, 11.4, 11.5, 12.5, 15.5_

- [ ]* 8. Write unit tests
  - [ ]* 8.1 Test InfrastructureServiceImpl
    - Test getInfrastructure caching behavior
    - Test getInfrastructureAsync returns proper CompletableFuture
    - Test lifecycle methods (initialize, shutdown)
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ]* 8.2 Test service classes
    - Test IslandMemberService operations
    - Test IslandBanService with expiration logic
    - Test IslandInviteService with expiration
    - _Requirements: 9.1, 9.3, 9.4, 7.1, 8.1_
