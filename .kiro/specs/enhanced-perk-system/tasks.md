

## Implementation Plan

- [x] 1. Clean up old perk system files




  - Delete or archive old perk-related files that are commented out
  - Remove old perk entity classes if they exist
  - Clean up old perk configuration files
  - _Requirements: All requirements (preparation step)_

- [x] 2. Create database entities and converters




- [x] 2.1 Create Perk entity


  - Write Perk entity class with Hibernate annotations
  - Include fields: id, identifier, perkType, category, enabled, displayOrder, icon, configJson, version, timestamps
  - Add relationships to PerkRequirement and PerkUnlockReward
  - _Requirements: 1.5, 13.1, 13.2_

- [x] 2.2 Create PlayerPerk entity


  - Write PlayerPerk entity class with Hibernate annotations
  - Include fields: id, player, perk, unlocked, enabled, active, cooldownExpiresAt, statistics, timestamps
  - Add unique constraint on (player_id, perk_id)
  - _Requirements: 2.2, 3.2, 13.1, 13.2_

- [x] 2.3 Create PerkRequirement entity


  - Write PerkRequirement entity class with Hibernate annotations
  - Include fields: id, perk, displayOrder, requirement (JSON), icon
  - Use RequirementConverter for AbstractRequirement serialization
  - _Requirements: 1.2, 2.5, 13.1_

- [x] 2.4 Create PerkUnlockReward entity


  - Write PerkUnlockReward entity class with Hibernate annotations
  - Include fields: id, perk, displayOrder, reward (JSON), icon
  - Use RewardConverter for AbstractReward serialization
  - _Requirements: 2.3, 13.1_


- [x] 2.5 Create Hibernate repositories

  - Create PerkRepository extending base repository
  - Create PlayerPerkRepository extending base repository
  - Add custom query methods for common operations
  - _Requirements: 13.2, 13.4_

- [x] 3. Create configuration sections





- [x] 3.1 Create PerkEffectSection


  - Write PerkEffectSection extending AConfigSection
  - Include fields for potion effects, event triggers, special types, custom config
  - Add validation in afterParsing method
  - _Requirements: 1.1, 10.1, 11.1_

- [x] 3.2 Create PerkSection


  - Write PerkSection extending AConfigSection
  - Include fields: identifier, perkType, category, enabled, displayOrder, icon, requirements, unlockRewards, effect
  - Add auto-generation of i18n keys in afterParsing
  - _Requirements: 1.1, 1.2, 14.1, 14.2_

- [x] 3.3 Create PerkSystemSection


  - Write PerkSystemSection extending AConfigSection
  - Include fields: enabled, maxEnabledPerksPerPlayer, cooldownMultiplier, UI settings, notifications, integration settings
  - Add default values for missing fields
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [x] 4. Implement PerkSystemFactory





- [x] 4.1 Create factory initialization logic


  - Write PerkSystemFactory class with initialization method
  - Load perk-system.yml configuration
  - Load all perk YAML files from perks directory
  - Handle configuration errors gracefully
  - _Requirements: 1.1, 1.4, 9.1, 15.1_

- [x] 4.2 Implement perk entity creation


  - Convert PerkSection to Perk entity
  - Convert requirements to PerkRequirement entities
  - Convert rewards to PerkUnlockReward entities
  - Persist entities using Hibernate repositories
  - _Requirements: 1.2, 1.3, 1.5, 13.2_

- [x] 4.3 Implement configuration reload


  - Add reload method to factory
  - Update existing perk entities with new configuration
  - Preserve player perk ownership and states
  - Handle reload errors without breaking existing state
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [x] 5. Implement PerkManagementService





- [x] 5.1 Implement perk ownership methods


  - Write grantPerk method to create PlayerPerk association
  - Write revokePerk method to delete PlayerPerk association
  - Write hasUnlocked method to check ownership
  - Add async variants for all methods
  - _Requirements: 2.2, 2.3, 13.2_

- [x] 5.2 Implement enable/disable methods


  - Write enablePerk method with limit checking
  - Write disablePerk method
  - Write togglePerk method
  - Update database state for all operations
  - _Requirements: 3.1, 3.2, 3.3, 3.5_

- [x] 5.3 Implement query methods


  - Write getUnlockedPerks method
  - Write getEnabledPerks method
  - Write getActivePerks method
  - Write getAvailablePerks method with category filtering
  - _Requirements: 7.1, 13.4_

- [x] 5.4 Implement limit checking methods


  - Write canEnableAnotherPerk method
  - Write getEnabledPerkCount method
  - Write getMaxEnabledPerks method
  - _Requirements: 3.1, 3.5, 9.2_

- [x] 6. Implement PerkRequirementService





- [x] 6.1 Implement requirement checking


  - Write canUnlock method to check all requirements
  - Write checkRequirements method returning individual results
  - Integrate with existing requirement system
  - _Requirements: 2.1, 2.5_

- [x] 6.2 Implement progress tracking


  - Write getRequirementProgress method
  - Write getOverallProgress method
  - Use existing requirement progress calculation
  - _Requirements: 2.5, 7.3_

- [x] 6.3 Implement unlock logic


  - Write attemptUnlock method
  - Check all requirements before unlocking
  - Grant unlock rewards on success
  - Send notification to player
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 7. Implement PerkActivationService





- [x] 7.1 Implement activation/deactivation


  - Write activate method to apply perk effects
  - Write deactivate method to remove perk effects
  - Update PlayerPerk active state
  - Delegate to appropriate effect handlers
  - _Requirements: 4.1, 4.3, 5.2_


- [x] 7.2 Implement cooldown management

  - Write isOnCooldown method
  - Write getRemainingCooldown method
  - Write startCooldown method
  - Store cooldown expiration in PlayerPerk entity
  - _Requirements: 5.1, 5.3, 5.4, 5.5_

- [x] 7.3 Implement lifecycle methods

  - Write activateAllEnabledPerks method for player login
  - Write deactivateAllActivePerks method for player logout
  - Handle server restart scenarios
  - _Requirements: 4.4, 4.5_


- [x] 7.4 Implement event handling

  - Write handleEvent method to process game events
  - Check for event-triggered perks
  - Apply cooldown and trigger chance logic
  - _Requirements: 5.1, 5.2, 6.1, 6.2, 6.3_

- [x] 8. Implement effect handlers





- [x] 8.1 Implement PotionPerkHandler


  - Write applyPotionEffect method
  - Write removePotionEffect method
  - Write refreshPotionEffect method
  - Create scheduled task for continuous effect refresh
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 8.2 Implement SpecialPerkHandler for fly and glow


  - Write enableFly and disableFly methods
  - Write enableGlow and disableGlow methods
  - Handle player state changes
  - _Requirements: 11.1, 11.2_


- [x] 8.3 Implement SpecialPerkHandler for damage prevention

  - Write registerNoFallDamage and unregisterNoFallDamage methods
  - Create event listener for fall damage cancellation
  - _Requirements: 11.3_


- [x] 8.4 Implement SpecialPerkHandler for death protection

  - Write registerKeepInventory and unregisterKeepInventory methods
  - Write registerKeepExperience and unregisterKeepExperience methods
  - Create event listener for death event handling
  - _Requirements: 11.4, 11.5_

- [x] 8.5 Implement EventPerkHandler


  - Write registerEventPerk and unregisterEventPerk methods
  - Write processEvent method
  - Write shouldTrigger method for percentage-based perks
  - Write checkAndStartCooldown method
  - _Requirements: 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 6.4_

- [x] 9. Implement PerkReward integration





- [x] 9.1 Create PerkReward class

  - Write PerkReward extending AbstractReward
  - Implement grant method to unlock perk for player
  - Implement getTypeId returning "PERK"
  - Implement getEstimatedValue for reward value calculation
  - _Requirements: 12.1, 12.3_


- [x] 9.2 Register PerkReward with reward system

  - Register PERK reward type in RDQRewardSetup
  - Create PerkRewardSection configuration class
  - Add converter in RDQRewardSectionAdapter
  - _Requirements: 12.1, 12.2_

- [x] 9.3 Add PerkReward to rank configurations


  - Update example rank YAML files with perk rewards
  - Document perk reward configuration format
  - _Requirements: 12.3, 12.4, 12.5_

- [x] 10. Implement event listeners




- [x] 10.1 Create PerkEventListener for player lifecycle


  - Implement onPlayerJoin to activate enabled perks
  - Implement onPlayerQuit to deactivate active perks
  - Clean up player data on quit
  - _Requirements: 4.4, 4.5_


- [x] 10.2 Create PerkEventListener for perk triggers


  - Implement onPlayerDeath for death-related perks
  - Implement onEntityDamage for combat perks
  - Implement onPlayerMove for movement perks
  - Add more event handlers as needed
  - _Requirements: 5.1, 5.2, 6.1_

- [x] 10.3 Register event listeners


  - Register PerkEventListener in RDQ main class
  - Register SpecialPerkHandler listeners
  - _Requirements: 5.1, 11.3, 11.4, 11.5_

- [x] 11. Implement UI components




- [x] 11.1 Create PerkCardRenderer


  - Write renderPerkCard method to create ItemStack
  - Write buildLore method for perk description
  - Write buildStateLine for perk state display
  - Write buildProgressLine for requirement progress
  - Write buildCooldownLine for cooldown display
  - Use i18n translations with MiniMessage formatting
  - _Requirements: 7.2, 7.3, 14.1, 14.2_

- [x] 11.2 Create PerkOverviewView


  - Write view class extending AbstractView
  - Implement renderCategoryTabs for category selection
  - Implement renderPerkGrid for perk display
  - Implement renderPagination for page navigation
  - Handle perk click to open detail view
  - _Requirements: 7.1, 7.2, 7.5_

- [x] 11.3 Create PerkDetailView


  - Write view class extending AbstractView
  - Implement renderPerkHeader for perk information
  - Implement renderRequirements for unlock requirements
  - Implement renderUnlockRewards for rewards display
  - Implement renderPerkState for current state
  - Implement renderActions for enable/disable buttons
  - _Requirements: 7.5, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 11.4 Implement view interactions


  - Handle unlock attempt in PerkDetailView
  - Handle toggle enable/disable in PerkDetailView
  - Handle category change in PerkOverviewView
  - Handle page navigation in PerkOverviewView
  - Play sound effects and show visual feedback
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 12. Create perk commands




- [x] 12.1 Create /perks command


  - Create command class for opening perk overview GUI
  - Add permission checking
  - Register command in plugin.yml
  - _Requirements: 7.1_

- [x] 12.2 Create /perk admin commands


  - Create subcommand for granting perks to players
  - Create subcommand for revoking perks from players
  - Create subcommand for reloading perk configurations
  - Create subcommand for listing all perks
  - Add admin permission checking
  - _Requirements: 15.1_

- [x] 12.3 Add tab completion


  - Implement tab completion for perk identifiers
  - Implement tab completion for player names
  - Implement tab completion for subcommands
  - _Requirements: 12.1, 12.2_

- [x] 13. Create i18n translation keys





- [x] 13.1 Add perk system translations


  - Add perk state translations (locked, available, active, cooldown, disabled)
  - Add perk category translations (combat, movement, utility, etc.)
  - Add perk type translations
  - Add general perk system messages
  - _Requirements: 14.1, 14.2, 14.3_

- [x] 13.2 Add perk-specific translations


  - Add translation keys for each default perk (speed, fly, glow, etc.)
  - Add perk name and description keys
  - Add perk effect description keys
  - _Requirements: 14.1, 14.2_

- [x] 13.3 Add notification translations


  - Add unlock notification translations
  - Add activation notification translations
  - Add cooldown notification translations
  - Add error message translations
  - _Requirements: 2.4, 14.3, 14.5_

- [x] 14. Create default perk configurations




- [x] 14.1 Create potion effect perks


  - Create speed.yml for speed boost perk
  - Create night_vision.yml for night vision perk
  - Create jump_boost.yml for jump boost perk
  - Create strength.yml for strength perk
  - Create resistance.yml for resistance perk
  - Create fire_resistance.yml for fire resistance perk
  - Create saturation.yml for saturation perk
  - Create haste.yml for haste perk
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_


- [x] 14.2 Create special ability perks

  - Create fly.yml for flight perk
  - Create glow.yml for glow effect perk
  - Create no_fall_damage.yml for fall damage immunity
  - Create keep_inventory.yml for inventory protection
  - Create keep_experience.yml for experience protection
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 14.3 Create event-triggered perks


  - Create combat_heal.yml for healing on damage
  - Create double_experience.yml for XP multiplier
  - Create critical_strike.yml for damage boost
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_


- [x] 14.4 Create perk-system.yml

  - Create global perk system configuration file
  - Set default values for all settings
  - Add comments explaining each setting
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 15. Initialize perk system in RDQ main class





- [x] 15.1 Add perk system initialization


  - Create PerkSystemFactory instance in RDQ.initializeComponents
  - Call factory.initialize() during plugin startup
  - Handle initialization errors gracefully
  - _Requirements: 1.1, 1.5_

- [x] 15.2 Register perk services


  - Create PerkManagementService instance
  - Create PerkActivationService instance
  - Create PerkRequirementService instance
  - Store services in RDQ class for access
  - _Requirements: 2.1, 3.1, 4.1_

- [x] 15.3 Register perk views


  - Register PerkOverviewView with ViewFrame
  - Register PerkDetailView with ViewFrame
  - _Requirements: 7.1, 7.5_

- [x] 15.4 Start scheduled tasks


  - Start potion effect refresh task
  - Start cooldown cleanup task
  - _Requirements: 10.3, 5.5_

- [ ] 16. Testing and validation
- [ ]* 16.1 Write unit tests for services
  - Test PerkManagementService methods
  - Test PerkActivationService methods
  - Test PerkRequirementService methods
  - _Requirements: All service requirements_

- [ ]* 16.2 Write integration tests
  - Test database operations with Hibernate
  - Test requirement system integration
  - Test reward system integration
  - Test event handling
  - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [ ]* 16.3 Perform manual testing
  - Test all perk types (passive, event-triggered, percentage-based)
  - Test all UI interactions
  - Test cooldown timers
  - Test requirement progress tracking
  - Test data persistence across restarts
  - _Requirements: All requirements_

- [ ]* 16.4 Performance testing
  - Test with many perks and players
  - Monitor memory usage
  - Monitor database query performance
  - Optimize as needed
  - _Requirements: All requirements_

- [ ] 17. Documentation
- [ ]* 17.1 Create user documentation
  - Document how to use the perk system
  - Document available perks
  - Document how to unlock perks
  - Create configuration examples
  - _Requirements: All requirements_

- [ ]* 17.2 Create admin documentation
  - Document how to configure perks
  - Document how to create custom perks
  - Document admin commands
  - Document troubleshooting steps
  - _Requirements: 1.1, 9.1, 15.1_

- [ ]* 17.3 Create developer documentation
  - Document API for custom perk types
  - Document database schema
  - Document service interfaces
  - Document extension points
  - _Requirements: All requirements_
