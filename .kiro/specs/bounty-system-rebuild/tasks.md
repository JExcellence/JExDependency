# Implementation Plan

This implementation plan breaks down the bounty system rebuild into discrete, manageable tasks. Each task builds incrementally on previous work, with testing integrated throughout. The plan follows an implementation-first approach: implement features before writing corresponding tests.

## Task List

- [x] 1. Set up project structure and core interfaces




  - Create package structure for bounty system (service, entity, repository, view, exception)
  - Define sealed BountyService interface with method signatures
  - Define all record DTOs (Bounty, BountyCreationRequest, ClaimInfo, HunterStats, RewardItem)
  - Define all enumerations (BountyStatus, ClaimMode, DistributionMode, HunterSortOrder)
  - Define sealed exception hierarchy (BountyException and subclasses)
  - _Requirements: 15.1, 20.1, 20.2, 20.3_

- [x] 2. Implement database entities and repositories




  - [x] 2.1 Update RBounty entity with modern patterns


    - Refactor RBounty to use modern Java patterns where appropriate
    - Add/update indexes for performance (target, commissioner, active, expires_at)
    - Ensure proper JPA annotations and relationships
    - _Requirements: 17.1, 17.2, 17.3, 17.4_
  
  - [x] 2.2 Update BountyHunterStats entity


    - Refactor BountyHunterStats with modern patterns
    - Add indexes for leaderboard queries (bounties_claimed, total_reward_value)
    - Implement recordClaim method for atomic updates
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_
  
  - [x] 2.3 Create/update RBountyRepository interface


    - Define custom query methods (findActiveByTarget, findByCommissioner, findAllActive, findExpired, countActive)
    - Add pagination support for bounty listing
    - Optimize queries with proper indexing hints
    - _Requirements: 6.1, 6.2, 9.1, 12.2_
  
  - [x] 2.4 Create/update BountyHunterStatsRepository interface


    - Define custom query methods (findByPlayerUniqueId, findTopByBountiesClaimed, findTopByRewardValue, countPlayersAbove)
    - Add pagination support for leaderboards
    - _Requirements: 8.1, 8.2, 17.5_
  
  - [x] 2.5 Write unit tests for entities


    - Test RBounty state transitions (active → claimed, active → expired)
    - Test BountyHunterStats calculations
    - Test record validation and defensive copies
    - _Requirements: 21.1_

- [x] 3. Implement PremiumBountyService



  - [x] 3.1 Create PremiumBountyService class implementing BountyService


    - Implement constructor with repository dependencies
    - Implement isPremium() returning true
    - Implement getMaxBountiesPerPlayer() and getMaxRewardItems() with configurable values
    - _Requirements: 18.1, 18.4, 18.5, 18.6_
  
  - [x] 3.2 Implement bounty query operations


    - Implement getAllBounties(page, pageSize) with async repository calls
    - Implement getBountyByTarget(UUID) with async lookup
    - Implement getBountiesByCommissioner(UUID) with async filtering
    - Implement getTotalBountyCount() with async count
    - Convert entity results to Bounty records
    - _Requirements: 6.1, 9.1, 16.1, 16.4_
  
  - [x] 3.3 Implement bounty creation


    - Implement createBounty(BountyCreationRequest) with validation
    - Validate target is not commissioner (prevent self-targeting)
    - Validate commissioner has sufficient currency balance
    - Calculate total estimated value from rewards
    - Set expiration time based on configuration
    - Save bounty entity asynchronously
    - _Requirements: 2.5, 12.1, 16.1_
  
  - [x] 3.4 Implement bounty claiming


    - Implement claimBounty(bountyId, hunterUuid) with state updates
    - Mark bounty as claimed with hunter UUID and timestamp
    - Update hunter statistics (increment claims, add reward value, update highest)
    - Distribute rewards based on configured distribution mode
    - Handle concurrent claim attempts
    - _Requirements: 10.5, 10.6, 10.7, 17.1, 17.2, 17.3, 17.4_
  
  - [x] 3.5 Implement bounty expiration


    - Implement expireBounty(bountyId) to mark bounty inactive
    - Implement scheduled task to check for expired bounties
    - Refund rewards to commissioner if configured
    - _Requirements: 12.2, 12.3_
  
  - [x] 3.6 Implement hunter statistics operations


    - Implement getHunterStats(UUID) with async lookup
    - Implement getTopHunters(limit, sortOrder) with pagination
    - Implement getHunterRank(UUID) with ranking calculation
    - Create new BountyHunterStats entity for first-time hunters
    - _Requirements: 8.1, 8.2, 17.5, 17.6_
  
  - [ ] 3.7 Write property test for bounty creation
    - **Property 9: Successful bounty creation**
    - **Validates: Requirements 2.5**
  
  - [ ] 3.8 Write property test for claim state updates
    - **Property 43: Claim state update**
    - **Validates: Requirements 10.5**
  
  - [ ] 3.9 Write property test for statistics updates
    - **Property 66: Bounties claimed increment**
    - **Property 67: Total reward value accumulation**
    - **Property 68: Highest bounty update**
    - **Property 69: Timestamp update**
    - **Validates: Requirements 17.1, 17.2, 17.3, 17.4**
  
  - [x] 3.10 Write unit tests for PremiumBountyService



    - Test all service methods with mock repositories
    - Test error handling and exception throwing
    - Test async operation completion
    - _Requirements: 21.2_

- [x] 4. Implement FreeBountyService




  - [x] 4.1 Create FreeBountyService class implementing BountyService


    - Implement in-memory storage using ConcurrentHashMap
    - Implement isPremium() returning false
    - Implement getMaxBountiesPerPlayer() returning 1
    - Implement getMaxRewardItems() with free edition limit
    - _Requirements: 18.2, 18.5, 18.6_
  
  - [x] 4.2 Implement static bounty loading

    - Load static bounty configurations from bounty.yml
    - Validate configured targets exist
    - Parse reward items and currencies from configuration
    - _Requirements: 19.1, 19.2, 19.5_
  
  - [x] 4.3 Implement free edition bounty operations

    - Implement createBounty with 1 active bounty limit enforcement
    - Implement bounty query operations using in-memory storage
    - Implement bounty claiming with slot release
    - Implement hunter statistics with in-memory tracking
    - _Requirements: 19.3, 19.4_
  
  - [x] 4.4 Write property test for free edition limits


    - **Property 78: Free edition bounty limit**
    - **Property 79: Bounty slot release**
    - **Validates: Requirements 19.3, 19.4**
  
  - [x] 4.5 Write unit tests for FreeBountyService


    - Test in-memory storage operations
    - Test static bounty loading
    - Test 1 bounty limit enforcement
    - _Requirements: 21.2_

- [x] 5. Implement claim mode logic




  - [x] 5.1 Create DamageTracker component


    - Track damage dealt to players within configured time window
    - Store damage amounts by attacker UUID
    - Clean up expired damage records
    - _Requirements: 10.1, 10.3_
  
  - [x] 5.2 Implement LAST_HIT claim mode


    - Attribute bounty to player who dealt final blow
    - Handle edge cases (environmental death, suicide)
    - _Requirements: 10.2_
  
  - [x] 5.3 Implement MOST_DAMAGE claim mode

    - Attribute bounty to player with highest damage in tracking window
    - Handle ties (first attacker wins)
    - _Requirements: 10.3_
  
  - [x] 5.4 Implement DAMAGE_SPLIT claim mode

    - Calculate proportional reward distribution
    - Distribute rewards to all damage dealers
    - Handle minimum damage threshold
    - _Requirements: 10.4_
  
  - [x] 5.5 Write property tests for claim modes


    - **Property 40: Last hit attribution**
    - **Property 41: Most damage attribution**
    - **Property 42: Damage split distribution**
    - **Validates: Requirements 10.2, 10.3, 10.4**
  
  - [x] 5.6 Write unit tests for claim mode logic


    - Test each claim mode with various damage scenarios
    - Test edge cases (ties, environmental death)
    - _Requirements: 21.3_

- [x] 6. Implement reward distribution modes





  - [x] 6.1 Implement INSTANT distribution mode


    - Add reward items directly to hunter's inventory
    - Drop excess items if inventory full
    - Credit currency rewards to economy balance
    - _Requirements: 11.1, 11.5_
  
  - [x] 6.2 Implement VIRTUAL distribution mode


    - Credit rewards to hunter's virtual storage
    - Integrate with virtual storage system
    - _Requirements: 11.2_
  
  - [x] 6.3 Implement DROP distribution mode


    - Drop reward items at target's death location
    - Handle world boundaries and invalid locations
    - _Requirements: 11.3_
  
  - [x] 6.4 Implement CHEST distribution mode


    - Place chest at death location
    - Fill chest with reward items
    - Handle chest placement failures
    - _Requirements: 11.4_
  
  - [x] 6.5 Write property tests for distribution modes


    - **Property 46: Instant distribution**
    - **Property 47: Virtual distribution**
    - **Property 48: Drop distribution**
    - **Property 49: Chest distribution**
    - **Property 16: Currency distribution**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5**
  
  - [x] 6.6 Write unit tests for distribution modes

    - Test each distribution mode with various reward sets
    - Test edge cases (full inventory, invalid locations)
    - _Requirements: 21.4_

- [x] 7. Implement bounty event listeners





  - [x] 7.1 Create PlayerDeathListener


    - Listen for player death events
    - Check if victim has active bounty
    - Determine killer based on claim mode
    - Trigger bounty claim process
    - _Requirements: 10.1, 10.5_
  
  - [x] 7.2 Create PlayerJoinListener


    - Apply visual indicators to players with active bounties
    - Restore bounty state after logout
    - _Requirements: 14.1, 14.2, 14.5_
  
  - [x] 7.3 Create PlayerQuitListener


    - Preserve bounty state on logout
    - Clean up temporary data
    - _Requirements: 14.5_
  
  - [x] 7.4 Write unit tests for event listeners


    - Test death event handling with various scenarios
    - Test join/quit event handling
    - _Requirements: 21.2_

- [x] 8. Implement visual indicators





  - [x] 8.1 Create VisualIndicatorManager


    - Apply tab prefix to players with active bounties
    - Apply name color to players with active bounties
    - Remove indicators when bounty claimed/expired
    - _Requirements: 14.1, 14.2, 14.4_
  
  - [x] 8.2 Implement particle effects


    - Spawn particles around players with active bounties
    - Use configured particle type and interval
    - Stop particles when bounty claimed/expired
    - _Requirements: 14.3, 14.4_
  
  - [x] 8.3 Write property tests for visual indicators


    - **Property 59: Tab prefix application**
    - **Property 60: Name color application**
    - **Property 61: Particle spawning**
    - **Property 62: Visual indicator cleanup**
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.4**
  
  - [x] 8.4 Write unit tests for visual indicators


    - Test indicator application and removal
    - Test particle spawning and stopping
    - _Requirements: 21.2_

- [x] 9. Implement announcement system





  - [x] 9.1 Create BountyAnnouncementManager


    - Broadcast bounty creation messages using JExTranslate
    - Broadcast bounty claim messages using JExTranslate
    - Support global and radius-based broadcasts
    - Respect announcement enable/disable configuration
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_
  
  - [x] 9.2 Write property tests for announcements


    - **Property 53: Creation announcement**
    - **Property 54: Claim announcement**
    - **Property 55: Global broadcast**
    - **Property 56: Radius broadcast**
    - **Property 57: Disabled announcements**
    - **Property 58: JExTranslate integration**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5, 13.6**
  
  - [x] 9.3 Write unit tests for announcement system


    - Test message broadcasting with various configurations
    - Test JExTranslate integration
    - _Requirements: 21.2_

- [x] 10. Implement BountyMainView





  - [x] 10.1 Create BountyMainView class extending BaseView


    - Define view key, size, and layout
    - Initialize state for RDQ plugin instance
    - _Requirements: 1.1, 1.2, 15.1, 15.2_
  
  - [x] 10.2 Implement main menu rendering


    - Render decorative glass panes
    - Render create bounty button
    - Render bounty list button
    - Render leaderboard button
    - Render my bounties button
    - _Requirements: 1.2, 1.3_
  
  - [x] 10.3 Implement navigation button handlers


    - Handle create button click → open BountyCreationView
    - Handle list button click → open BountyListView
    - Handle leaderboard button click → open BountyLeaderboardView
    - Handle my bounties button click → open MyBountiesView
    - _Requirements: 1.4_
  
  - [x] 10.4 Write property test for navigation


    - **Property 1: Navigation button routing**
    - **Validates: Requirements 1.4**

- [x] 11. Implement BountyCreationView





  - [x] 11.1 Create BountyCreationView class extending BaseView


    - Define view key, size, and layout
    - Initialize mutable states (target, rewards, rewardCurrencies, insertedItems)
    - Initialize computed states (targetButton, itemButton, currencyButton, confirmButton)
    - _Requirements: 2.1, 15.1, 15.2, 15.6_
  
  - [x] 11.2 Implement target selection


    - Render target selection button
    - Handle target button click → open player selection view
    - Update target state when player selected
    - Display target head and name after selection
    - Prevent self-targeting
    - _Requirements: 2.2, 2.3, 3.1, 3.3, 3.4, 3.5_
  
  - [x] 11.3 Implement reward item selection

    - Render item reward button (disabled without target)
    - Handle item button click → open reward item view
    - Store inserted items in creation context
    - Preserve items across view navigation
    - _Requirements: 2.2, 2.3, 2.4, 4.1, 4.3, 4.5_
  
  - [x] 11.4 Implement reward currency selection

    - Render currency reward button (disabled without target)
    - Handle currency button click → open currency selection view
    - Validate player balance before adding currency
    - Accumulate multiple currencies in rewards map
    - _Requirements: 2.2, 2.3, 5.1, 5.3, 5.4, 5.5_
  
  - [x] 11.5 Implement bounty confirmation

    - Render confirm button (disabled without target and rewards)
    - Handle confirm click → create bounty via BountyService
    - Clear temporary storage on success
    - Display success message using JExTranslate
    - _Requirements: 2.5, 2.6_
  
  - [x] 11.6 Implement view close handling


    - Refund inserted items on close without confirmation
    - Drop excess items if inventory full
    - Display refund message using JExTranslate
    - _Requirements: 2.7, 2.8_
  
  - [x] 11.7 Write property tests for creation view


    - **Property 2: Target selection enables rewards**
    - **Property 3: No target disables rewards**
    - **Property 4: Item insertion updates state**
    - **Property 6: State persistence across navigation**
    - **Property 7: Target selection updates state**
    - **Property 8: Target display after selection**
    - **Property 10: Creation clears temporary storage**
    - **Property 11: Refund on cancellation**
    - **Validates: Requirements 2.2, 2.3, 2.4, 2.6, 2.7, 3.3, 3.4, 4.5**

- [x] 12. Implement BountyRewardView





  - [x] 12.1 Create BountyRewardView class extending BaseView


    - Define view key, size, and layout
    - Initialize state for reward items
    - _Requirements: 4.1, 4.2, 15.1, 15.2_
  
  - [x] 12.2 Implement reward item insertion

    - Render empty slots for item insertion
    - Handle item insertion → update reward items state
    - Handle item removal → update reward items state
    - _Requirements: 4.2, 4.3, 4.4_
  
  - [x] 12.3 Implement item merging

    - Merge similar items by stacking amounts
    - Preserve total item count
    - _Requirements: 4.6_
  
  - [x] 12.4 Write property tests for reward view


    - **Property 5: Item removal updates state**
    - **Property 12: Similar items stack**
    - **Validates: Requirements 4.4, 4.6**

- [x] 13. Implement BountyListView


  - [x] 13.1 Create BountyListView class extending BaseView


    - Define view key, size, and layout
    - Initialize state for bounties and current page
    - _Requirements: 6.1, 15.1, 15.2_
  
  - [x] 13.2 Implement bounty list rendering

    - Fetch active bounties asynchronously via BountyService
    - Display bounties as paginated player heads
    - Show target head, name, reward summary, expiration time
    - _Requirements: 6.1, 6.2, 6.3_
  
  - [x] 13.3 Implement pagination

    - Render pagination controls when needed
    - Handle page navigation → load next page
    - Update view with new page data
    - _Requirements: 6.5, 6.6_
  
  - [x] 13.4 Implement bounty detail navigation

    - Handle bounty entry click → open BountyDetailView
    - Pass bounty data to detail view
    - _Requirements: 6.4_
  
  - [ ] 13.5 Write property tests for list view



    - **Property 17: Async bounty loading**
    - **Property 18: Paginated display**
    - **Property 19: Complete bounty information**
    - **Property 20: Bounty detail navigation**
    - **Property 21: Pagination controls**
    - **Property 22: Page navigation**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6**

- [x] 14. Implement BountyDetailView
  - [x] 14.1 Create BountyDetailView class extending BaseView


    - Define view key, size, and layout
    - Initialize state for bounty data
    - _Requirements: 7.1, 15.1, 15.2_
  
  - [x] 14.2 Implement detail rendering

    - Display target information and commissioner
    - Display all reward items with quantity and type
    - Display all reward currencies with name and amount
    - Display expiration time if present
    - Display claim information if claimed
    - Display expired status if expired
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_
  
  - [x] 14.3 Write property tests for detail view

    - **Property 23: Complete detail display**
    - **Property 24: Item detail format**
    - **Property 25: Currency detail format**
    - **Property 26: Expiration display**
    - **Property 27: Claim information display**
    - **Property 28: Expired status display**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6**

- [x] 15. Implement BountyLeaderboardView
  - [x] 15.1 Create BountyLeaderboardView class extending BaseView


    - Define view key, size, and layout
    - Initialize state for hunter statistics
    - _Requirements: 8.1, 15.1, 15.2_
  
  - [x] 15.2 Implement leaderboard rendering

    - Fetch top hunters asynchronously via BountyService
    - Display hunters ranked by bounties claimed
    - Show rank, name, bounties claimed, total reward value
    - Highlight viewer's own entry
    - _Requirements: 8.1, 8.2, 8.3, 8.5_
  
  - [x] 15.3 Implement leaderboard refresh

    - Schedule periodic updates
    - Refresh display with current statistics
    - _Requirements: 8.4_
  
  - [x] 15.4 Write property tests for leaderboard view

    - **Property 29: Async hunter loading**
    - **Property 30: Ranked display**
    - **Property 31: Complete hunter information**
    - **Property 32: Leaderboard refresh**
    - **Property 33: Self-highlighting**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

- [x] 16. Implement MyBountiesView
  - [x] 16.1 Create MyBountiesView class extending BaseView


    - Define view key, size, and layout
    - Initialize state for player's bounties
    - _Requirements: 9.1, 15.1, 15.2_
  
  - [x] 16.2 Implement my bounties rendering

    - Fetch bounties by commissioner asynchronously via BountyService
    - Display bounties with status indicators
    - Show active, claimed, and expired statuses
    - Display claimer information for claimed bounties
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  
  - [x] 16.3 Implement bounty detail navigation

    - Handle bounty click → open BountyDetailView
    - Pass bounty data to detail view
    - _Requirements: 9.6_
  
  - [x] 16.4 Write property tests for my bounties view

    - **Property 34: Commissioner bounty loading**
    - **Property 35: Status indicator display**
    - **Property 36: Active status display**
    - **Property 37: Claimed status display**
    - **Property 38: My bounty detail navigation**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.6**

- [x] 17. Implement configuration system



  - [x] 17.1 Create BountyConfig class


    - Load configuration from bounty.yml
    - Provide getters for all configuration values
    - Support hot-reloading configuration
    - Validate configuration on load
    - _Requirements: 12.1, 13.3, 13.4, 14.1, 14.2, 14.3_
  
  - [x] 17.2 Add configuration validation


    - Validate claim mode is valid enum value
    - Validate distribution mode is valid enum value
    - Validate expiry days is positive
    - Validate broadcast radius is valid (-1 or positive)
    - _Requirements: 12.1, 13.3, 13.4_
  
  - [x] 17.3 Write unit tests for configuration


    - Test configuration loading
    - Test validation logic
    - Test hot-reloading
    - _Requirements: 21.2_

- [x] 18. Implement bounty commands
  - [x] 18.1 Create /bounty command

    - Register command with plugin
    - Handle command execution → open BountyMainView
    - Add permission checks
    - _Requirements: 1.1_
  
  - [x] 18.2 Create /bounty admin commands

    - Implement /bounty admin create <target> - Create bounty on target
    - Implement /bounty admin delete <id> - Delete bounty by ID
    - Implement /bounty admin list - List all bounties
    - Implement /bounty admin reload - Reload configuration
    - Add admin permission checks
    - _Requirements: 12.2, 12.3_
  
  - [x] 18.3 Write unit tests for commands

    - Test command execution
    - Test permission checks
    - Test argument parsing
    - _Requirements: 21.2_

- [x] 19. Implement translation keys
  - [x] 19.1 Create translation keys for all views

    - Add keys for BountyMainView (title, buttons)
    - Add keys for BountyCreationView (title, buttons, messages)
    - Add keys for BountyListView (title, pagination)
    - Add keys for BountyDetailView (title, labels)
    - Add keys for BountyLeaderboardView (title, headers)
    - Add keys for MyBountiesView (title, status labels)
    - _Requirements: 13.6_
  
  - [x] 19.2 Create translation keys for announcements

    - Add keys for bounty creation announcements
    - Add keys for bounty claim announcements
    - Add placeholder support for player names, amounts
    - _Requirements: 13.1, 13.2, 13.6_
  
  - [x] 19.3 Create translation keys for error messages

    - Add keys for validation errors
    - Add keys for operation failures
    - Add keys for permission errors
    - _Requirements: 13.6, 16.3_
  
  - [x] 19.4 Add translations for all supported languages

    - Add English (en.yml) translations
    - Add German (de.yml) translations
    - Add other language translations as needed
    - _Requirements: 13.6_

- [x] 20. Integration and testing
  - [x] 20.1 Integrate all components

    - Wire BountyService into views
    - Wire event listeners into plugin
    - Wire visual indicators into plugin
    - Wire announcement system into plugin
    - Register all views with inventory-framework
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_
  
  - [x] 20.2 Test edition switching

    - Test switching between Premium and Free services
    - Verify edition-specific limits enforced
    - Verify static bounties load in Free edition
    - _Requirements: 18.1, 18.2, 18.3, 18.5, 18.6, 19.1, 19.2, 19.3, 19.4, 19.5_
  
  - [x] 20.3 Write property tests for edition behavior

    - **Property 72: Premium service selection**
    - **Property 73: Free service selection**
    - **Property 74: Static bounty support**
    - **Property 75: Edition-specific bounty limits**
    - **Property 76: Edition-specific item limits**
    - **Property 77: Static bounty structure**
    - **Property 78: Free edition bounty limit**
    - **Property 79: Bounty slot release**
    - **Property 80: Static bounty validation**
    - **Validates: Requirements 18.1, 18.2, 18.3, 18.5, 18.6, 19.1, 19.2, 19.3, 19.4, 19.5**
  
  - [x] 20.4 Write integration tests

    - Test service + repository interactions
    - Test view + service interactions
    - Test event + service interactions
    - Test config + service interactions
    - _Requirements: 21.2_

- [x] 21. Final checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.
  - Verify all property tests pass with 100 iterations
  - Verify all unit tests pass
  - Verify all integration tests pass
  - Check test coverage meets goals (80% line, 75% branch)
  - _Requirements: All_

- [x] 22. Documentation and cleanup
  - [x] 22.1 Update API documentation

    - Document all public interfaces
    - Add JavaDoc to all public methods
    - Include usage examples
    - _Requirements: All_
  
  - [x] 22.2 Create migration guide

    - Document steps to migrate from old bounty system
    - Include database migration scripts
    - Include configuration migration steps
    - _Requirements: All_
  
  - [x] 22.3 Clean up deprecated code

    - Remove old bounty view implementations
    - Remove old bounty service implementations
    - Update imports and references
    - _Requirements: All_
  
  - [x] 22.4 Performance testing


    - Test with large numbers of bounties (1000+)
    - Test with many concurrent users (50+)
    - Verify async operations don't block main thread
    - Verify database queries are optimized
    - _Requirements: All_

