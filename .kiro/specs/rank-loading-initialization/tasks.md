# Implementation Plan

## Note: Adapting from Old RDQ Implementation

The old RDQ (in `/RDQ` folder) has a working rank loading system using `RankTreeLoader` that loads YAML files into in-memory repositories. The new RDQ2 uses database-backed repositories with Hibernate. We'll adapt the old approach to work with the new architecture.

- [x] 1. Add rank loading invocation to RDQ.java

  - [x] 1.1 Add `rankSystemFactory` field to RDQ class


    - Declare field after other repository fields
    - Initialize in `initializeRepositories()` after repository initialization
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.2 Create `loadRankSystemAsync()` method


    - Return CompletableFuture<Void> for async execution
    - Execute on the plugin's executor service
    - Call `rankSystemFactory.loadAndPersistRankSystem()`
    - Add try-catch block with appropriate logging
    - Log "Loading rank system..." at start
    - Log "Rank system loaded successfully" on completion
    - _Requirements: 2.1, 2.7, 7.1, 7.2_

  - [x] 1.3 Integrate rank loading into startup sequence


    - Call `loadRankSystemAsync()` in `performCoreEnableAsync()` method
    - Chain the future after repository initialization
    - Ensure rank loading completes before `performPostEnableSync()`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 2. Implement RankSystemFactory.loadAndPersistRankSystem()


  - [x] 2.1 Add public `loadAndPersistRankSystem()` method


    - Call `loader.loadRankSystem()` to get RankSystemState
    - Call `validator.validate(state)` to validate loaded state
    - Call `entityService.persistRankSystem(state)` to persist to database
    - Calculate statistics (tree count, rank count)
    - Log "Loaded X rank trees with Y total ranks"
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 7.2_

  - [x] 2.2 Add error handling

    - Wrap each stage in try-catch blocks
    - Log specific errors for each stage with file names
    - Continue with partial data when possible
    - Don't throw exceptions that would disable the plugin
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 7.4, 7.5_

- [ ] 3. Implement RankEntityService.persistRankSystem()
  - [x] 3.1 Create main `persistRankSystem(RankSystemState state)` method

    - Accept RankSystemState parameter
    - Call `persistRankTrees(state)` and store result
    - Call `persistRanks(state, persistedTrees)` and store result
    - Call `establishRankRelationships(persistedRanks, state)`
    - Log "Persisted X rank trees and Y ranks to database"
    - _Requirements: 3.1, 3.9, 7.3_


  - [ ] 3.2 Implement `persistRankTrees()` helper method
    - Iterate through state.getTreeSections()
    - For each tree, check if exists using rankTreeRepository.findByIdentifier()
    - If exists, update fields from RankTreeSection
    - If not exists, create new RRankTree entity
    - Save using rankTreeRepository.save()
    - Return Map<String, RRankTree> of identifier to entity
    - _Requirements: 3.2, 3.3_


  - [ ] 3.3 Implement `persistRanks()` helper method
    - Iterate through state.getRankSections()
    - For each rank, check if exists using rankRepository.findByIdentifier()
    - If exists, update fields from RankSection
    - If not exists, create new RRank entity
    - Associate rank with its RRankTree from persistedTrees map
    - Save using rankRepository.save()
    - Return Map<String, RRank> of identifier to entity
    - _Requirements: 3.4, 3.5_

  - [x] 3.4 Implement `establishRankRelationships()` helper method

    - Iterate through persistedRanks
    - For each rank, get previousRanks and nextRanks from RankSection
    - Look up RRank entities from persistedRanks map
    - Set previousRanks and nextRanks collections on RRank entity
    - Save updated entities using rankRepository.save()
    - _Requirements: 3.6, 3.7_


  - [ ] 3.5 Implement `persistRequirements()` helper method
    - For each rank, iterate through requirements from RankSection
    - Create RRankUpgradeRequirement entities
    - Associate with RRank and RRequirement entities
    - Save using playerRankUpgradeProgressRepository
    - _Requirements: 3.5_

  - [x] 3.6 Add transaction management and error handling

    - Wrap persistence operations in try-catch blocks
    - Log errors with entity details
    - Continue with remaining entities on partial failure
    - Don't throw exceptions that would stop the entire loading process
    - _Requirements: 3.8, 4.4_

- [x] 4. Add error handling for edge cases



  - [x] 4.1 Handle missing rank-system.yml



    - Detect missing file in RankConfigurationLoader
    - Log warning message
    - Use default rank system settings from code
    - Continue with rank tree loading
    - _Requirements: 4.1_



  - [x] 4.2 Handle missing rank path files

    - Detect empty rank/paths/ directory in RankConfigurationLoader
    - Log warning message
    - Return empty RankSystemState
    - Don't disable plugin
    - _Requirements: 4.5_


  - [x] 4.3 Handle malformed YAML files

    - Catch YAML parsing exceptions in RankConfigurationLoader
    - Log warning with file name and error message
    - Skip problematic file
    - Continue with remaining files
    - _Requirements: 2.5, 4.2_



  - [ ] 4.4 Handle database persistence failures
    - Catch exceptions during entity save operations
    - Log error with entity identifier and exception
    - Continue with remaining entities
    - Log summary of failures at end
    - _Requirements: 4.4_



- [x] 5. Add reload support


  - [-] 5.1 Add `reloadRankSystem()` method to RDQ.java



    - Create public method returning CompletableFuture<Void>
    - Clear existing rank data from repositories
    - Call `loadRankSystemAsync()`
    - Log "Reloading rank system..."
    - _Requirements: 6.1, 6.2, 6.5_


  - [x] 5.2 Add admin reload command



    - Create `/rdq admin reload ranks` command handler
    - Add permission check for rdq.admin.reload
    - Call `rdq.reloadRankSystem()` method
    - Send success/failure message to command sender
    - _Requirements: 6.1, 6.5_

- [ ]* 6. Add unit tests
  - [ ]* 6.1 Test RankSystemFactory orchestration
    - Test loadAndPersistRankSystem() success path
    - Test error handling in each stage
    - Test logging output
    - _Requirements: 2.1-2.7, 7.1-7.5_

  - [ ]* 6.2 Test RankEntityService persistence
    - Test persistRankTrees() with new trees
    - Test persistRankTrees() with existing trees (update)
    - Test persistRanks() with new ranks
    - Test persistRanks() with existing ranks (update)
    - Test establishRankRelationships()
    - Test error handling and partial failures
    - _Requirements: 3.1-3.9_

  - [ ]* 6.3 Test error handling scenarios
    - Test missing rank-system.yml handling
    - Test missing rank path files handling
    - Test malformed YAML handling
    - Test database connection failure handling
    - _Requirements: 4.1-4.5_


- [ ] 7. Verify and test deployment
  - [x] 7.1 Build and test locally



    - Build plugin JAR
    - Start test server with H2 database
    - Verify logs show "Loading rank system..."
    - Verify logs show "Loaded X rank trees with Y total ranks"
    - Verify logs show "Persisted X rank trees and Y ranks to database"
    - Open rank GUI and verify ranks appear
    - _Requirements: All_

  - [ ] 7.2 Test error scenarios
    - Test with missing rank-system.yml
    - Test with missing rank path files
    - Test with malformed YAML file
    - Verify appropriate warning messages
    - Verify plugin doesn't disable
    - _Requirements: 4.1-4.5_

  - [ ] 7.3 Test reload functionality
    - Start server and verify ranks load
    - Run `/rdq admin reload ranks` command
    - Verify ranks reload successfully
    - Verify message sent to command sender
    - _Requirements: 6.1-6.5_

