# Implementation Plan

- [x] 1. Create PlayerService with repository injection




  - Create `PlayerService` class in `com.raindropcentral.core.service` package
  - Add `@InjectRepository` annotation for `RPlayerRepository`
  - Implement `findByUuidAsync()` method delegating to repository
  - Implement `findByNameAsync()` method delegating to repository
  - Implement `existsByUuidAsync()` method delegating to repository
  - Implement `createAsync()` method delegating to repository
  - Implement `updateAsync()` method delegating to repository
  - Implement `createOrUpdateAsync()` method delegating to repository
  - Add comprehensive JavaDoc documentation
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.5_

- [x] 2. Create StatisticService with multi-repository injection



  - Create `StatisticService` class in `com.raindropcentral.core.service` package
  - Add `@InjectRepository` annotations for `RPlayerStatisticRepository`, `RStatisticRepository`, and `RPlayerRepository`
  - Implement `findByPlayerAsync()` method to retrieve player statistics
  - Implement `findStatisticValueAsync()` method to get specific statistic values
  - Implement `hasStatisticAsync()` method to check statistic existence
  - Implement `addOrReplaceStatisticAsync()` method for statistic upserts
  - Implement `removeStatisticAsync()` method for statistic deletion
  - Implement `getStatisticCountForPluginAsync()` method for counting statistics
  - Add comprehensive JavaDoc documentation
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 3.2, 3.4, 3.5_

- [x] 3. Create InventoryService with repository injection



  - Create `InventoryService` class in `com.raindropcentral.core.service` package
  - Add `@InjectRepository` annotations for `RPlayerInventoryRepository`, `RPlayerRepository`, and `RCentralServerRepository`
  - Implement `findByPlayerAndServerAsync()` method
  - Implement `findLatestByPlayerAndServerAsync()` method
  - Implement `saveInventoryAsync()` method
  - Implement `deleteByPlayerAndServerAsync()` method
  - Add comprehensive JavaDoc documentation
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 3.3, 3.4, 3.5_

- [x] 4. Update RCoreImpl to use RepositoryManager registration



  - Remove repository instance fields (`rPlayerRepository`, `rPlayerStatisticRepository`, `rStatisticRepository`)
  - Add service instance fields (`playerService`, `statisticService`, `inventoryService`)
  - Refactor `initializeRepositories()` to use `RepositoryManager.register()` for all repositories
  - Register `RPlayerRepository` with `RPlayer::getUniqueId` as cache key
  - Register `RPlayerStatisticRepository` with `RPlayerStatistic::getId` as cache key
  - Register `RStatisticRepository` with `RAbstractStatistic::getId` as cache key
  - Register `RPlayerInventoryRepository` with `RPlayerInventory::getId` as cache key
  - Register `RCentralServerRepository` with `RCentralServer::getId` as cache key
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 5.1, 5.2, 7.1_

- [x] 5. Create service initialization method in RCoreImpl

  - Create `initializeServices()` method in `RCoreImpl`
  - Instantiate `PlayerService` and inject repositories using `RepositoryManager.getInstance().injectInto()`
  - Instantiate `StatisticService` and inject repositories
  - Instantiate `InventoryService` and inject repositories
  - Call `initializeServices()` after `initializeRepositories()` in the enable sequence
  - Add error handling for injection failures with plugin disable on error
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_


- [ ] 6. Remove repository getter methods from RCoreImpl
  - Remove `getRPlayerRepository()` method
  - Remove `getRPlayerStatisticRepository()` method
  - Remove `getRStatisticRepository()` method
  - Add `getPlayerService()` method returning `PlayerService`
  - Add `getStatisticService()` method returning `StatisticService`
  - Add `getInventoryService()` method returning `InventoryService`
  - Update JavaDoc to reflect new service-based architecture
  - _Requirements: 7.2, 7.5_

- [x] 7. Refactor RCoreService adapter to use services


  - Update RCoreService implementation to hold service references instead of backend reference
  - Refactor `findPlayerAsync(UUID)` to delegate to `playerService.findByUuidAsync()`
  - Refactor `findPlayerAsync(OfflinePlayer)` to delegate to `playerService.findByUuidAsync()`
  - Refactor `findPlayerByNameAsync()` to delegate to `playerService.findByNameAsync()`
  - Refactor `playerExistsAsync(UUID)` to delegate to `playerService.existsByUuidAsync()`
  - Refactor `playerExistsAsync(OfflinePlayer)` to delegate to `playerService.existsByUuidAsync()`
  - Refactor `createPlayerAsync()` to delegate to `playerService.createAsync()`
  - Refactor `updatePlayerAsync()` to delegate to `playerService.updateAsync()`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 6.1, 6.2, 6.3, 6.4_


- [ ] 8. Refactor RCoreService statistic methods to use StatisticService
  - Refactor `findPlayerStatisticsAsync(UUID)` to delegate to `statisticService.findByPlayerAsync()`
  - Refactor `findPlayerStatisticsAsync(OfflinePlayer)` to delegate to `statisticService.findByPlayerAsync()`
  - Refactor `findStatisticValueAsync(UUID, String, String)` to delegate to `statisticService.findStatisticValueAsync()`
  - Refactor `findStatisticValueAsync(OfflinePlayer, String, String)` to delegate to `statisticService.findStatisticValueAsync()`
  - Refactor `hasStatisticAsync(UUID, String, String)` to delegate to `statisticService.hasStatisticAsync()`
  - Refactor `hasStatisticAsync(OfflinePlayer, String, String)` to delegate to `statisticService.hasStatisticAsync()`
  - Refactor `removeStatisticAsync(UUID, String, String)` to delegate to `statisticService.removeStatisticAsync()`
  - Refactor `removeStatisticAsync(OfflinePlayer, String, String)` to delegate to `statisticService.removeStatisticAsync()`
  - Refactor `addOrReplaceStatisticAsync(UUID, RAbstractStatistic)` to delegate to `statisticService.addOrReplaceStatisticAsync()`
  - Refactor `addOrReplaceStatisticAsync(OfflinePlayer, RAbstractStatistic)` to delegate to `statisticService.addOrReplaceStatisticAsync()`
  - Refactor `getStatisticCountForPluginAsync(UUID, String)` to delegate to `statisticService.getStatisticCountForPluginAsync()`
  - Refactor `getStatisticCountForPluginAsync(OfflinePlayer, String)` to delegate to `statisticService.getStatisticCountForPluginAsync()`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 6.1, 6.2, 6.3, 6.4_


- [ ] 9. Update RCentralService to use repository injection
  - Add `@InjectRepository` annotation for `RCentralServerRepository` in `RCentralService`
  - Update `RCoreImpl.initializeComponents()` to inject repositories into `RCentralService` using `RepositoryManager.getInstance().injectInto()`
  - Remove manual repository passing to `RCentralService` if present
  - Update `RCentralService` methods to use injected repository
  - _Requirements: 1.1, 1.2, 5.3_


- [ ] 10. Update commands to use service injection
  - Identify all command classes that currently access repositories
  - Update command constructors to accept service instances instead of `RCoreImpl`
  - Update `CommandFactory.registerAllCommandsAndListeners()` to pass services to commands
  - Refactor command logic to use service methods instead of direct repository access

  - _Requirements: 1.1, 1.2, 7.4_

- [ ] 11. Update listeners to use service injection
  - Identify all listener classes that currently access repositories
  - Update listener constructors to accept service instances instead of `RCoreImpl`
  - Update `CommandFactory.registerAllCommandsAndListeners()` to pass services to listeners

  - Refactor listener logic to use service methods instead of direct repository access
  - _Requirements: 1.1, 1.2, 7.4_

- [ ] 12. Create usage documentation
  - Create `REPOSITORY_INJECTION_USAGE.md` in `docs/` directory
  - Document how to create new service classes with `@InjectRepository`
  - Provide examples of single-repository service pattern
  - Provide examples of multi-repository service pattern
  - Document repository registration process
  - Document service initialization sequence
  - Include troubleshooting section for common injection issues

  - Add examples of injecting services into commands and listeners
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 13. Verify and test the refactoring


  - Build the project to ensure no compilation errors
  - Run existing tests to verify backward compatibility
  - Manually test player operations (create, find, update)
  - Manually test statistic operations (add, find, remove, count)
  - Manually test inventory operations if applicable
  - Verify RCentralService connection functionality
  - Test command execution to ensure service injection works
  - Verify no null pointer exceptions during initialization
  - Check logs for proper initialization messages
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.3_
