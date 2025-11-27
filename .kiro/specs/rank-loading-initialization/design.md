# Design Document

## Overview

This design document describes the implementation approach for fixing the rank system initialization in RDQ2. The solution involves integrating the existing RankSystemFactory and RankConfigurationLoader into the plugin startup sequence, ensuring ranks are loaded from YAML files and persisted to the database before the plugin becomes fully operational.

## Architecture

### Component Interaction Flow

```
Plugin Startup (onEnable)
    ↓
performCoreEnableAsync()
    ↓
initializeRepositories()
    ├─> Initialize RRankRepository
    ├─> Initialize RRankTreeRepository
    └─> Initialize RankSystemFactory
    ↓
loadRankSystemAsync()
    ├─> RankConfigurationLoader.loadRankSystem()
    │   ├─> Load rank-system.yml
    │   ├─> Load rank/paths/*.yml files
    │   └─> Return RankSystemState
    ├─> RankValidationService.validate()
    └─> RankEntityService.persistRankSystem()
        ├─> Persist RRankTree entities
        ├─> Persist RRank entities
        └─> Establish relationships
    ↓
runSync() → performPostEnableSync()
    ├─> Initialize views
    ├─> Register commands
    └─> Mark plugin as enabled
```

### Key Components

1. **RDQ.java** - Main plugin class that orchestrates initialization
2. **RankSystemFactory** - Coordinates rank loading and persistence
3. **RankConfigurationLoader** - Reads and parses YAML files
4. **RankValidationService** - Validates loaded configurations
5. **RankEntityService** - Persists configurations to database
6. **RRankRepository** - Database access for rank entities
7. **RRankTreeRepository** - Database access for rank tree entities

## Components and Interfaces

### Modified: RDQ.java

**Purpose:** Integrate rank loading into the plugin startup sequence

**Key Changes:**
- Add `rankSystemFactory` field
- Initialize `RankSystemFactory` in `initializeRepositories()`
- Add `loadRankSystemAsync()` method
- Call `loadRankSystemAsync()` in `performCoreEnableAsync()`
- Add `reloadRankSystem()` method for runtime reloading

**Method Signatures:**
```java
private void initializeRepositories() {
    // ... existing repository initialization ...
    rankSystemFactory = new RankSystemFactory(this);
}

private CompletableFuture<Void> loadRankSystemAsync() {
    return CompletableFuture.runAsync(() -> {
        try {
            LOGGER.info("Loading rank system...");
            rankSystemFactory.loadAndPersistRankSystem();
            LOGGER.info("Rank system loaded successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load rank system", e);
        }
    }, executor);
}

public CompletableFuture<Void> reloadRankSystem() {
    return loadRankSystemAsync();
}
```

### Modified: RankSystemFactory.java

**Purpose:** Orchestrate the complete rank loading and persistence workflow

**Key Changes:**
- Add `loadAndPersistRankSystem()` public method
- Coordinate between loader, validator, and entity service
- Provide detailed logging at each stage
- Handle errors gracefully

**Method Signatures:**
```java
public void loadAndPersistRankSystem() {
    RankSystemState state = loader.loadRankSystem();
    validator.validate(state);
    entityService.persistRankSystem(state);
    
    int treeCount = state.getTreeSections().size();
    int rankCount = state.getRankSections().values().stream()
        .mapToInt(Map::size)
        .sum();
    
    LOGGER.info(String.format("Loaded %d rank trees with %d total ranks", treeCount, rankCount));
}
```

### Existing: RankConfigurationLoader.java

**Purpose:** Load rank configurations from YAML files

**Current Implementation:** Already complete and functional
- Loads rank-system.yml for system settings
- Loads all files from rank/paths/ directory
- Parses YAML into configuration sections
- Returns RankSystemState with all loaded data

**No changes needed** - this class is already implemented correctly

### Existing: RankValidationService.java

**Purpose:** Validate loaded rank configurations

**Current Implementation:** Already complete and functional
- Validates rank tree references
- Validates rank relationships (previousRanks, nextRanks)
- Validates requirement configurations
- Logs warnings for invalid configurations

**No changes needed** - this class is already implemented correctly

### Modified: RankEntityService.java

**Purpose:** Persist loaded configurations to the database

**Key Changes:**
- Add `persistRankSystem(RankSystemState state)` method
- Implement upsert logic (update if exists, insert if new)
- Establish bidirectional relationships between entities
- Provide transaction management
- Log persistence statistics

**Method Signatures:**
```java
public void persistRankSystem(RankSystemState state) {
    Map<String, RRankTree> persistedTrees = persistRankTrees(state);
    Map<String, RRank> persistedRanks = persistRanks(state, persistedTrees);
    establishRankRelationships(persistedRanks, state);
    
    LOGGER.info(String.format("Persisted %d rank trees and %d ranks to database", 
        persistedTrees.size(), persistedRanks.size()));
}

private Map<String, RRankTree> persistRankTrees(RankSystemState state) {
    // Implementation
}

private Map<String, RRank> persistRanks(RankSystemState state, Map<String, RRankTree> trees) {
    // Implementation
}

private void establishRankRelationships(Map<String, RRank> ranks, RankSystemState state) {
    // Implementation
}
```

## Data Models

### RankSystemState (Existing)

```java
public class RankSystemState {
    private RankSystemSection rankSystemSection;
    private Map<String, RankTreeSection> treeSections;
    private Map<String, Map<String, RankSection>> rankSections;
    
    // Getters and builder
}
```

### RRankTree Entity (Existing)

```java
@Entity
@Table(name = "r_rank_tree")
public class RRankTree extends AbstractEntity {
    private String identifier;
    private String displayNameKey;
    private String descriptionKey;
    private Integer displayOrder;
    private Boolean enabled;
    private IconSection icon;
    
    @OneToMany(mappedBy = "rankTree")
    private Set<RRank> ranks;
    
    // Relationships, getters, setters
}
```

### RRank Entity (Existing)

```java
@Entity
@Table(name = "r_rank")
public class RRank extends AbstractEntity {
    private String identifier;
    private String displayNameKey;
    private String descriptionKey;
    private Integer tier;
    private Integer weight;
    private String luckPermsGroup;
    private Boolean enabled;
    private IconSection icon;
    
    @ManyToOne
    private RRankTree rankTree;
    
    @ManyToMany
    private Set<RRank> previousRanks;
    
    @ManyToMany
    private Set<RRank> nextRanks;
    
    @OneToMany(mappedBy = "rank")
    private Set<RRankUpgradeRequirement> upgradeRequirements;
    
    // Getters, setters
}
```

## Error Handling

### Configuration Loading Errors

**Scenario:** YAML file is malformed or missing required fields

**Handling:**
1. RankConfigurationLoader catches parsing exceptions
2. Logs warning with file name and error details
3. Skips the problematic file
4. Continues loading other files
5. Returns partial RankSystemState

**Example Log:**
```
[WARNING] Failed to load rank tree from warrior.yml: Missing required field 'displayOrder'
```

### Validation Errors

**Scenario:** Rank references non-existent previous/next rank

**Handling:**
1. RankValidationService detects invalid reference
2. Logs warning with rank identifier and invalid reference
3. Removes invalid reference from configuration
4. Continues validation

**Example Log:**
```
[WARNING] Rank 'warrior:fighter' references non-existent next rank 'warrior:invalid_rank'
```

### Database Persistence Errors

**Scenario:** Database constraint violation or connection failure

**Handling:**
1. RankEntityService catches persistence exceptions
2. Logs error with entity details and exception
3. Rolls back transaction for that entity
4. Continues with remaining entities
5. Reports partial success

**Example Log:**
```
[ERROR] Failed to persist rank 'warrior:champion': Duplicate key constraint violation
```

### Critical Failure Handling

**Scenario:** Complete rank loading failure (e.g., no YAML files found)

**Handling:**
1. Log severe error with details
2. Do NOT disable the plugin
3. Mark rank system as unavailable
4. Allow other plugin features to function
5. Provide admin command to retry loading

## Testing Strategy

### Unit Tests

**RankConfigurationLoaderTest:**
- Test loading valid rank-system.yml
- Test loading valid rank tree YAML files
- Test handling missing files
- Test handling malformed YAML
- Test parsing all configuration sections

**RankValidationServiceTest:**
- Test validation of valid configurations
- Test detection of invalid rank references
- Test detection of circular dependencies
- Test validation of requirement configurations

**RankEntityServiceTest:**
- Test persistence of new rank trees
- Test updating existing rank trees
- Test persistence of new ranks
- Test updating existing ranks
- Test relationship establishment
- Test transaction rollback on error

### Integration Tests

**RankSystemIntegrationTest:**
- Test complete loading sequence from YAML to database
- Test rank system reload
- Test concurrent access during loading
- Test database state after loading
- Test player rank assignment after loading

### Manual Testing Checklist

- [ ] Plugin starts successfully with rank YAML files present
- [ ] Plugin starts successfully with missing rank YAML files
- [ ] Plugin starts successfully with malformed rank YAML files
- [ ] Rank trees appear in database after startup
- [ ] Ranks appear in database with correct relationships
- [ ] Rank GUI views display loaded ranks correctly
- [ ] Player can progress through ranks after loading
- [ ] Reload command successfully reloads rank system
- [ ] Logs show appropriate messages at each stage

## Performance Considerations

### Async Loading

All rank loading operations occur asynchronously on the executor thread pool to avoid blocking the main server thread during startup.

### Batch Operations

Database persistence uses batch operations where possible to minimize round trips:
- Batch insert rank trees
- Batch insert ranks
- Batch update relationships

### Caching

RRankRepository and RRankTreeRepository use Caffeine cache to minimize database queries after initial loading.

### Startup Time Impact

Expected impact: 100-500ms additional startup time depending on:
- Number of rank trees (typically 6)
- Number of ranks per tree (typically 5-7)
- Database backend (H2 faster than MySQL)
- Server hardware

## Migration and Deployment

### Database Schema

No schema changes required - all necessary tables and columns already exist:
- `r_rank_tree` table
- `r_rank` table
- `r_rank_upgrade_requirement` table
- Relationship join tables

### Configuration Migration

No configuration migration needed - YAML files already exist in resources directory:
- `rank/rank-system.yml`
- `rank/paths/warrior.yml`
- `rank/paths/cleric.yml`
- `rank/paths/mage.yml`
- `rank/paths/rogue.yml`
- `rank/paths/merchant.yml`
- `rank/paths/ranger.yml`

### Deployment Steps

1. Build updated plugin JAR
2. Stop server
3. Replace plugin JAR
4. Start server
5. Verify logs show "Rank system loaded successfully"
6. Verify ranks appear in database
7. Test rank GUI views

### Rollback Plan

If issues occur:
1. Stop server
2. Restore previous plugin JAR
3. Start server
4. Rank system will be unavailable but plugin will function

## Security Considerations

### Input Validation

- YAML files are loaded from plugin resources (trusted source)
- No user-provided YAML files are loaded
- All configuration values are validated before persistence

### Permission Checks

- Reload command requires admin permission
- Rank assignment requires appropriate permissions
- Database access uses configured credentials

### SQL Injection

- All database operations use JPA/Hibernate with parameterized queries
- No raw SQL queries with string concatenation

## Monitoring and Observability

### Startup Metrics

Log the following metrics during startup:
- Time taken to load YAML files
- Number of rank trees loaded
- Number of ranks loaded
- Time taken to persist to database
- Number of validation warnings

### Runtime Metrics

Track the following during operation:
- Rank system reload count
- Rank system reload failures
- Average rank loading time

### Health Checks

Provide health check endpoint/command:
- Rank system loaded: true/false
- Number of rank trees: X
- Number of ranks: Y
- Last loaded timestamp
- Last reload timestamp

