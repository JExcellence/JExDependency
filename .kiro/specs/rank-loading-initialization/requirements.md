# Requirements Document

## Introduction

This document specifies the requirements for fixing the rank system initialization in the RDQ2 plugin. The rank system has been fully implemented with entities, repositories, services, views, and YAML configuration files, but the critical rank loading logic is not being invoked during plugin startup. This results in an empty rank system despite having complete configuration files (warrior.yml, cleric.yml, mage.yml, etc.) in the resources directory.

## Glossary

- **RDQ_System**: The complete RaindropQuests plugin consisting of rdq-common, rdq-free, and rdq-premium modules
- **RankSystemFactory**: The factory class responsible for orchestrating rank system initialization and loading
- **RankConfigurationLoader**: The service class that loads rank YAML files from the resources directory
- **RankEntityService**: The service class that persists loaded rank configurations to the database
- **Rank_Loading_Sequence**: The initialization process that reads YAML files, validates them, and persists them to the database
- **Plugin_Startup**: The onEnable lifecycle phase where repositories and services are initialized
- **RRankRepository**: The repository for managing RRank entities in the database
- **RRankTreeRepository**: The repository for managing RRankTree entities in the database
- **RankSystemState**: The data structure containing loaded rank system configuration

## Requirements

### Requirement 1: Rank System Factory Initialization

**User Story:** As a developer, I want the RankSystemFactory to be initialized during plugin startup, so that rank loading can be orchestrated.

#### Acceptance Criteria

1. THE RDQ_System SHALL instantiate RankSystemFactory in the initializeRepositories() method after repository initialization
2. THE RankSystemFactory SHALL be stored as an instance field in the RDQ class for later access
3. THE RankSystemFactory SHALL receive the RDQ instance and executor service as constructor parameters
4. THE RankSystemFactory SHALL initialize its internal RankConfigurationLoader, RankValidationService, and RankEntityService components
5. THE RDQ_System SHALL log successful RankSystemFactory initialization at INFO level

### Requirement 2: Rank Configuration Loading

**User Story:** As a server administrator, I want rank configurations to be loaded from YAML files during plugin startup, so that players can access the rank system.

#### Acceptance Criteria

1. THE RDQ_System SHALL invoke RankSystemFactory.loadRankSystem() asynchronously during the performCoreEnableAsync() phase
2. THE RankConfigurationLoader SHALL read rank-system.yml from the rank/ resources directory
3. THE RankConfigurationLoader SHALL read all YAML files from the rank/paths/ resources directory
4. THE RankConfigurationLoader SHALL parse YAML files into RankSystemSection, RankTreeSection, and RankSection objects
5. WHEN a YAML file is malformed or missing required fields, THEN THE RankConfigurationLoader SHALL log a warning and skip that file
6. THE RankConfigurationLoader SHALL return a RankSystemState object containing all loaded configurations
7. THE RDQ_System SHALL log the number of rank trees and ranks successfully loaded at INFO level

### Requirement 3: Rank Entity Persistence

**User Story:** As a developer, I want loaded rank configurations to be persisted to the database, so that they can be queried by the rank system services.

#### Acceptance Criteria

1. THE RankEntityService SHALL receive the loaded RankSystemState from RankSystemFactory
2. THE RankEntityService SHALL check if each rank tree already exists in the database using RRankTreeRepository
3. WHEN a rank tree does not exist in the database, THEN THE RankEntityService SHALL create and persist a new RRankTree entity
4. THE RankEntityService SHALL check if each rank already exists in the database using RRankRepository
5. WHEN a rank does not exist in the database, THEN THE RankEntityService SHALL create and persist a new RRank entity with its associated requirements and rewards
6. THE RankEntityService SHALL establish relationships between ranks (previousRanks, nextRanks) based on YAML configuration
7. THE RankEntityService SHALL establish relationships between rank trees (prerequisiteRankTrees, connectedRankTrees) based on YAML configuration
8. THE RankEntityService SHALL perform all database operations asynchronously using the provided executor
9. THE RDQ_System SHALL log the number of rank trees and ranks persisted to the database at INFO level

### Requirement 4: Error Handling and Validation

**User Story:** As a server administrator, I want clear error messages when rank loading fails, so that I can fix configuration issues.

#### Acceptance Criteria

1. WHEN rank-system.yml is missing, THEN THE RDQ_System SHALL log an error and use default rank system settings
2. WHEN a rank path YAML file is missing required fields, THEN THE RankValidationService SHALL log a warning with the file name and missing field
3. WHEN a rank references a non-existent previous or next rank, THEN THE RankValidationService SHALL log a warning and skip that relationship
4. WHEN database persistence fails, THEN THE RankEntityService SHALL log an error with the exception details and continue with remaining ranks
5. THE RDQ_System SHALL NOT disable the plugin if rank loading fails, but SHALL log a warning that the rank system is unavailable

### Requirement 5: Startup Sequence Integration

**User Story:** As a developer, I want rank loading to happen at the correct point in the plugin lifecycle, so that dependencies are available and timing is optimal.

#### Acceptance Criteria

1. THE RDQ_System SHALL initialize repositories (including RRankRepository and RRankTreeRepository) before attempting rank loading
2. THE RDQ_System SHALL perform rank loading asynchronously during performCoreEnableAsync() to avoid blocking the main thread
3. THE RDQ_System SHALL complete rank loading before the performPostEnableSync() phase where views and commands are registered
4. THE RDQ_System SHALL ensure the database connection is established before rank loading begins
5. THE RDQ_System SHALL wait for rank loading to complete before marking the plugin as fully enabled

### Requirement 6: Reload Support

**User Story:** As a server administrator, I want to reload rank configurations without restarting the server, so that I can test configuration changes quickly.

#### Acceptance Criteria

1. THE RDQ_System SHALL provide a reloadRankSystem() method that can be called after initial startup
2. THE reloadRankSystem() method SHALL clear existing rank data from repositories before reloading
3. THE reloadRankSystem() method SHALL invoke the same loading sequence as initial startup
4. THE reloadRankSystem() method SHALL update player rank associations if rank definitions have changed
5. THE RDQ_System SHALL log successful rank system reload at INFO level with the number of ranks loaded

### Requirement 7: Logging and Observability

**User Story:** As a developer, I want detailed logging during rank loading, so that I can debug issues and monitor the loading process.

#### Acceptance Criteria

1. THE RDQ_System SHALL log "Loading rank system..." at INFO level when rank loading begins
2. THE RDQ_System SHALL log "Loaded X rank trees with Y total ranks" at INFO level when loading completes successfully
3. THE RDQ_System SHALL log "Persisted X rank trees and Y ranks to database" at INFO level when persistence completes
4. THE RDQ_System SHALL log warnings for each skipped or invalid rank configuration with specific details
5. THE RDQ_System SHALL log errors with full stack traces when rank loading fails critically
6. THE RDQ_System SHALL log the time taken for rank loading at DEBUG level

