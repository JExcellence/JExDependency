

# Implementation Plan

- [x] 1. Project Setup and Core Infrastructure



  - [x] 1.1 Rename existing RDQ folder to RDQ2 and create new RDQ project structure


    - Create new RDQ/ directory with rdq-common, rdq-free, rdq-premium submodules
    - Set up root build.gradle.kts with version catalog reference
    - Create gradle/libs.versions.toml with all dependency versions
    - Create settings.gradle.kts including all submodules
    - _Requirements: 1.1, 1.2_

  - [x] 1.2 Configure rdq-common module build

    - Create rdq-common/build.gradle.kts with library conventions
    - Add dependencies: RPlatform, JExTranslate, JExCommand, JExHibernate, inventory-framework
    - Configure test dependencies: JUnit 5, Mockito, MockBukkit, jqwik
    - Set up resource processing for YAML configs
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6_


  - [x] 1.3 Configure rdq-free and rdq-premium module builds

    - Create rdq-free/build.gradle.kts with shadowJar configuration
    - Create rdq-premium/build.gradle.kts with shadowJar configuration
    - Both depend on rdq-common
    - Configure plugin.yml and paper-plugin.yml generation
    - _Requirements: 1.1, 2.4_

  - [x] 1.4 Create core package structure and base classes


    - Create package-info.java for all packages
    - Create RDQCore.java in rdq-common with shared initialization logic
    - Create AsyncExecutor with virtual thread support and fallback
    - Create CacheManager with Caffeine configuration
    - _Requirements: 1.7, 11.1, 11.3_

- [x] 2. Shared Domain Models and Interfaces




  - [x] 2.1 Create sealed Result type and error hierarchy

    - Implement Result<T> sealed interface with Success and Failure records
    - Implement RDQError sealed interface with all error types (NotFound, InsufficientFunds, OnCooldown, etc.)
    - Create RDQException class wrapping RDQError
    - Create ErrorHandler with pattern matching for translations
    - _Requirements: 5.7, 9.4_



  - [x] 2.2 Create player domain models




    - Create RDQPlayer entity record with JPA annotations
    - Create PlayerDataService interface
    - Create DefaultPlayerDataService implementation with caching
    - Create RDQPlayerRepository extending JExHibernate base repository


    - _Requirements: 7.1, 7.4_

  - [x] 2.3 Write unit tests for player service

    - Test player creation and retrieval
    - Test cache behavior
    - _Requirements: 12.1_

- [x] 3. Rank System Implementation





  - [x] 3.1 Create rank domain models


    - Create RankTree record with id, displayNameKey, iconMaterial, ranks list
    - Create Rank record with id, treeId, tier, weight, luckPermsGroup, requirements
    - Create RankRequirement sealed interface with StatisticRequirement, PermissionRequirement, CurrencyRequirement, ItemRequirement variants
    - Create PlayerRankData record with activePaths and unlockedRanks
    - Create PlayerRankPath and PlayerRank entity records
    - _Requirements: 3.1, 3.2_

  - [x] 3.2 Create rank repositories


    - Create RankRepository for rank definitions (loaded from YAML)
    - Create RankTreeRepository for rank tree definitions
    - Create PlayerRankRepository for player rank progress persistence
    - Create PlayerRankPathRepository for active path tracking
    - _Requirements: 3.3, 7.4_

  - [x] 3.3 Create rank service interfaces and implementations


    - Create RankService sealed interface with FreeRankService and PremiumRankService permits
    - Implement FreeRankService with single active tree, linear progression
    - Implement PremiumRankService with multiple trees, cross-tree switching
    - Implement requirement checking with pattern matching
    - _Requirements: 3.4, 3.5, 14.1, 14.2_

  - [x] 3.4 Create rank configuration loader


    - Create RankSystemConfig record for rank-system.yml
    - Create RankTreeLoader to parse rank/paths/*.yml files
    - Load rank definitions into memory on startup
    - Support hot-reload via admin command
    - _Requirements: 3.2, 6.1, 6.2_

  - [x] 3.5 Implement LuckPerms integration for ranks


    - Create LuckPermsRankAdapter using RPlatform's LuckPermsService
    - Assign/remove permission groups on rank unlock
    - Apply prefix/suffix on rank change
    - Handle LuckPerms not present gracefully
    - _Requirements: 3.6, 11.4_


  - [x] 3.6 Create rank GUI views

    - Create RankMainView showing available rank trees
    - Create RankTreeView showing ranks in a tree with progress
    - Create RankDetailView showing rank requirements and unlock button
    - Create RankProgressView showing current progress toward next rank
    - Use inventory-framework with pagination
    - _Requirements: 3.8, 10.1, 10.2, 10.5_

  - [x] 3.7 Create rank notification system


    - Create RankNotificationService for rank unlock announcements
    - Support title, subtitle, actionbar, sound notifications
    - Use JExTranslate for all messages
    - Configure notification settings via rank-system.yml
    - _Requirements: 3.7, 9.1, 9.3_


  - [x] 3.8 Write unit tests for rank service

    - Test rank unlocking with met/unmet requirements
    - Test linear progression enforcement
    - Test cross-tree switching (premium)
    - _Requirements: 12.1_

- [x] 4. Bounty System Implementation





  - [x] 4.1 Create bounty domain models

    - Create Bounty entity record with placer, target, amount, status, timestamps
    - Create BountyRequest record with validation in compact constructor
    - Create BountyStatus enum (ACTIVE, CLAIMED, EXPIRED, CANCELLED)
    - Create ClaimResult record with bounty, reward, distributionMode
    - Create HunterStats record with kills, deaths, earnings
    - Create DistributionMode enum (INSTANT, CHEST, DROP, VIRTUAL)
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 4.2 Create bounty repositories


    - Create BountyRepository with findActiveByTarget, findAllActive, claimAll methods
    - Create HunterStatsRepository with getTopHunters, updateStats methods
    - Implement async operations using JExHibernate
    - _Requirements: 4.4, 7.4_



  - [x] 4.3 Create bounty service interfaces and implementations


    - Create BountyService sealed interface with FreeBountyService and PremiumBountyService permits
    - Implement createBounty with economy withdrawal and validation
    - Implement claimBounty with reward distribution
    - Implement getActiveBounties with caching
    - Implement getLeaderboard for hunter stats


    - _Requirements: 4.1, 4.3, 4.9, 14.1, 14.2_


  - [x] 4.4 Create bounty configuration

    - Create BountyConfig record for bounty.yml


    - Configure min/max amounts, expiration, distribution modes
    - Configure announcement settings
    - _Requirements: 4.5, 6.1_

  - [x] 4.5 Implement Vault economy integration


    - Create EconomyService interface
    - Create VaultEconomyAdapter using Vault API
    - Handle Vault not present gracefully
    - Support withdraw and deposit operations
    - _Requirements: 4.9, 11.4_





  - [x] 4.6 Create bounty event listeners

    - Create BountyDeathListener for player kills
    - Implement damage tracking to attribute kills correctly
    - Trigger bounty claim on valid kill
    - Handle edge cases (combat log, indirect kills)
    - _Requirements: 4.6, 4.10_






  - [x] 4.7 Create bounty GUI views
    - Create BountyMainView with create, list, leaderboard options
    - Create BountyCreationView with target selection and amount input
    - Create BountyListView showing active bounties with pagination


    - Create BountyDetailView showing bounty info
    - Create BountyLeaderboardView showing top hunters
    - Create MyBountiesView showing player's placed/targeted bounties
    - _Requirements: 4.7, 10.1, 10.2, 10.5_






  - [x] 4.8 Create bounty announcement system
    - Create BountyAnnouncementService for bounty events
    - Announce bounty creation to configured scope
    - Announce bounty claim with hunter/target info
    - Use JExTranslate for all messages
    - _Requirements: 4.8, 9.1, 9.3_


  - [x] 4.9 Create bounty expiration task

    - Create BountyExpirationTask running periodically
    - Mark expired bounties and refund placers
    - Clean up expired bounties from cache
    - _Requirements: 4.5_

  - [x] 4.10 Write unit tests for bounty service

    - Test bounty creation with valid/invalid requests
    - Test self-targeting rejection
    - Test bounty claiming
    - Test insufficient funds handling
    - _Requirements: 12.1_

- [x] 5. Perk System Implementation





  - [x] 5.1 Create perk domain models

    - Create Perk record with id, type, effect, cooldown, duration, requirements
    - Create PerkType sealed interface with Toggleable, EventBased, Passive variants
    - Create PerkEffect sealed interface with PotionEffect, AttributeModifier, Flight, ExperienceMultiplier, DeathPrevention, Custom variants
    - Create PerkRequirement sealed interface with RankRequired, PermissionRequired, CurrencyRequired, LevelRequired variants
    - Create PlayerPerkState record with unlocked, active, cooldownExpiry
    - _Requirements: 5.1, 5.3, 5.10_

  - [x] 5.2 Create perk repositories


    - Create PerkRepository for perk definitions (loaded from YAML)
    - Create PlayerPerkRepository for player perk state persistence
    - Create PerkProgressRepository for requirement progress tracking
    - _Requirements: 5.5, 7.4_

  - [x] 5.3 Create perk service interfaces and implementations


    - Create PerkService sealed interface with FreePerkService and PremiumPerkService permits
    - Implement getAvailablePerks filtering by requirements
    - Implement unlockPerk with requirement validation
    - Implement activatePerk/deactivatePerk with cooldown handling
    - _Requirements: 5.1, 5.3, 5.4, 14.1, 14.2_

  - [x] 5.4 Create perk runtime system


    - Create PerkRuntime class managing single perk state
    - Implement activate/deactivate with effect application
    - Implement cooldown tracking per player
    - Create PerkRegistry holding all active PerkRuntime instances
    - Create PerkTypeRegistry for perk type definitions
    - _Requirements: 5.4, 5.6_

  - [x] 5.5 Implement perk effects with pattern matching


    - Implement PotionEffect application using Bukkit API
    - Implement AttributeModifier using Bukkit attributes
    - Implement Flight toggle with combat check
    - Implement ExperienceMultiplier via event listener
    - Implement DeathPrevention via event listener
    - Create CustomPerkHandlers registry for extensibility
    - _Requirements: 5.9, 5.10_

  - [x] 5.6 Create perk configuration loader


    - Create PerkConfigLoader to parse perks/*.yml files
    - Load perk definitions into PerkRegistry on startup
    - Support hot-reload via admin command
    - Validate perk configurations on load
    - _Requirements: 5.2, 6.1, 6.4_

  - [x] 5.7 Create perk event listeners


    - Create PerkEventBus for perk-related events
    - Create ExperienceMultiplierListener for XP perks
    - Create DeathPreventionListener for death save perks
    - Create PerkCleanupListener for player disconnect
    - _Requirements: 5.7, 11.6_

  - [x] 5.8 Create perk GUI views


    - Create PerkMainView showing perk categories
    - Create PerkListView showing available perks with pagination
    - Create PerkDetailView showing perk info, requirements, activate button
    - Create PerkUnlockView for purchasing perks
    - Create PerkAdminView for admin management
    - _Requirements: 5.8, 10.1, 10.2, 10.5_

  - [x] 5.9 Write unit tests for perk runtime


    - Test perk activation/deactivation
    - Test cooldown enforcement
    - Test effect application
    - _Requirements: 12.1_

- [x] 6. Command System Integration





  - [x] 6.1 Create command YAML definitions


    - Create commands/rdq.yml for main command with subcommands
    - Create commands/rank.yml for rank commands
    - Create commands/bounty.yml for bounty commands
    - Create commands/perk.yml for perk commands
    - Define permissions, aliases, tab completions
    - _Requirements: 8.1, 8.3_


  - [x] 6.2 Create command handlers

    - Create RDQCommand as main entry point
    - Create RankCommand with view, progress, admin subcommands
    - Create BountyCommand with create, list, claim, admin subcommands
    - Create PerkCommand with list, activate, deactivate, admin subcommands
    - Use JExCommand's CommandFactory for registration
    - _Requirements: 8.2, 8.4, 8.5, 8.6_






  - [x] 6.3 Implement tab completion providers

    - Create PlayerNameCompleter for online players
    - Create RankNameCompleter for available ranks
    - Create PerkNameCompleter for available perks
    - Create BountyTargetCompleter for players with bounties
    - _Requirements: 8.2_

- [x] 7. Translation System Integration





  - [x] 7.1 Create translation files

    - Create translations/en_US.yml as base translation
    - Create translations for all 18 locales (de_DE, fr_FR, es_ES, etc.)
    - Define keys for all user-facing messages
    - Include placeholders for dynamic values
    - _Requirements: 9.3, 9.4_


  - [x] 7.2 Create translation service wrapper

    - Create TranslationService wrapping JExTranslate
    - Implement get(key, placeholders) with locale detection
    - Implement sendMessage(player, key, placeholders)
    - Track missing keys for developer review
    - _Requirements: 9.1, 9.2, 9.5, 9.6_

- [x] 8. Plugin Entry Points






  - [x] 8.1 Create RDQFree entry point

    - Create RDQFree.java extending JavaPlugin
    - Initialize RDQCore with free edition services
    - Register free edition service implementations
    - Create plugin.yml and paper-plugin.yml
    - _Requirements: 14.1, 14.4_


  - [x] 8.2 Create RDQPremium entry point

    - Create RDQPremium.java extending JavaPlugin
    - Initialize RDQCore with premium edition services
    - Register premium edition service implementations
    - Create plugin.yml and paper-plugin.yml
    - _Requirements: 14.2, 14.4_


  - [x] 8.3 Implement edition feature gating

    - Create EditionFeatures interface for feature checks
    - Implement FreeEditionFeatures with limited features
    - Implement PremiumEditionFeatures with full features
    - Show appropriate messages when free users access premium features
    - _Requirements: 14.3, 14.5_

- [x] 9. Resource Files and Configuration






  - [x] 9.1 Create default configuration files

    - Create bounty/bounty.yml with default settings
    - Create rank/rank-system.yml with default settings
    - Create rank/paths/*.yml for default rank trees (warrior, cleric, mage, rogue, merchant, ranger)
    - Create perks/*.yml for all 15 default perks
    - _Requirements: 6.3, 6.6_

  - [x] 9.2 Create database configuration


    - Create database/hibernate.properties for JExHibernate
    - Support H2, MySQL, MariaDB, PostgreSQL configuration
    - Configure connection pooling settings
    - _Requirements: 7.2, 7.6_




  - [x] 9.3 Create permission configuration

    - Create permissions/permissions.yml defining all permission nodes
    - Document permission hierarchy
    - _Requirements: 8.3_

- [x] 10. Documentation





  - [x] 10.1 Create README.md


    - Write architecture overview
    - Document setup instructions
    - Include developer guidelines
    - Add configuration examples
    - _Requirements: 13.1_


  - [x] 10.2 Create CHANGELOG.md

    - Document version 6.0.0 as initial release
    - Establish changelog format
    - _Requirements: 13.4_


  - [x] 10.3 Add Javadoc to public APIs

    - Document all public interfaces
    - Document all public service methods
    - Document configuration classes
    - _Requirements: 13.2_

- [x] 11. Final Integration and Testing



  - [x] 11.1 Wire all components in RDQCore


    - Initialize all repositories
    - Initialize all services with dependencies
    - Register all event listeners
    - Register all commands
    - Initialize view frame with all views
    - _Requirements: 1.3, 1.4, 1.5, 1.6_



  - [x] 11.2 Create integration test suite
    - Test full bounty creation and claim flow
    - Test full rank progression flow
    - Test full perk activation flow
    - Use MockBukkit for server simulation
    - _Requirements: 12.2_


  - [x] 11.3 Run full test suite and fix issues


    - Execute all unit tests
    - Execute all integration tests
    - Fix any failing tests
    - Verify code coverage meets 70% target
    - _Requirements: 12.3_
