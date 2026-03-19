# JExOneblock Repository Layer

This document describes the comprehensive repository layer implementation for JExOneblock, following the JEHibernate patterns and modern Java practices.

## Architecture Overview

The repository layer is built on top of JEHibernate's `CachedRepository` base class, providing:

- **Asynchronous operations** using `CompletableFuture`
- **Automatic caching** with configurable strategies
- **Type-safe operations** with generic parameters
- **Dependency injection** support via `@InjectRepository`
- **Modern Java features** (streams, optionals, method references)

## Repository Structure

### Core Entities

| Repository | Entity | Cache Key | Description |
|------------|--------|-----------|-------------|
| `OneblockPlayerRepository` | `OneblockPlayer` | `UUID` | Player data management |
| `OneblockIslandRepository` | `OneblockIsland` | `String` | Island data management |
| `OneblockEvolutionRepository` | `OneblockEvolution` | `String` | Evolution system data |

### Evolution Content

| Repository | Entity | Cache Key | Description |
|------------|--------|-----------|-------------|
| `EvolutionBlockRepository` | `EvolutionBlock` | `Long` | Block configurations per evolution/rarity |
| `EvolutionEntityRepository` | `EvolutionEntity` | `Long` | Entity spawn configurations |
| `EvolutionItemRepository` | `EvolutionItem` | `Long` | Item drop configurations |

### Island Management

| Repository | Entity | Cache Key | Description |
|------------|--------|-----------|-------------|
| `OneblockIslandMemberRepository` | `OneblockIslandMember` | `Long` | Island membership management |
| `OneblockIslandBanRepository` | `OneblockIslandBan` | `Long` | Island ban system |
| `OneblockVisitorSettingsRepository` | `OneblockVisitorSettings` | `Long` | Visitor permission settings |
| `OneblockRegionRepository` | `OneblockRegion` | `Long` | Coordinate-based regions |

### Infrastructure

| Repository | Entity | Cache Key | Description |
|------------|--------|-----------|-------------|
| `IslandInfrastructureRepository` | `IslandInfrastructure` | `UUID` | Island automation and storage systems |

## Key Features

### 1. Asynchronous Operations

All repository operations return `CompletableFuture` for non-blocking execution:

```java
// Basic async operations
CompletableFuture<Optional<OneblockPlayer>> playerFuture = 
    playerRepository.findByUuidAsync(playerUuid);

CompletableFuture<OneblockPlayer> savedPlayer = 
    playerRepository.createOrUpdateAsync(player);
```

### 2. Intelligent Caching

Repositories automatically cache entities by their primary keys:

```java
// First call hits database
Optional<OneblockPlayer> player1 = playerRepository.findByUuidAsync(uuid).join();

// Second call uses cache
Optional<OneblockPlayer> player2 = playerRepository.findByUuidAsync(uuid).join();
```

### 3. Complex Query Support

Repositories provide domain-specific query methods:

```java
// Evolution queries
CompletableFuture<List<OneblockEvolution>> evolutions = 
    evolutionRepository.findByLevelRangeAsync(1, 10);

CompletableFuture<Optional<OneblockEvolution>> nextEvolution = 
    evolutionRepository.findNextEvolutionAsync(currentLevel);

// Island membership queries
CompletableFuture<List<OneblockIslandMember>> activeMembers = 
    memberRepository.findActiveByIslandAsync(islandId);

CompletableFuture<Boolean> isMember = 
    memberRepository.isMemberAsync(islandId, playerUuid);
```

### 4. Batch Operations

Efficient bulk operations for performance:

```java
// Count operations
CompletableFuture<Long> memberCount = 
    memberRepository.countActiveByIslandAsync(islandId);

// Bulk deletions
CompletableFuture<Void> cleanup = 
    banRepository.deleteByIslandAsync(islandId);
```

### 5. Time-Based Queries

Support for temporal operations:

```java
// Find expired bans
CompletableFuture<List<OneblockIslandBan>> expiredBans = 
    banRepository.findExpiredBansAsync();

// Deactivate expired bans
CompletableFuture<Integer> deactivatedCount = 
    banRepository.deactivateExpiredBansAsync();
```

## Usage Examples

### Basic CRUD Operations

```java
@Inject
private OneblockPlayerRepository playerRepository;

// Create new player
OneblockPlayer newPlayer = OneblockPlayer.builder()
    .uniqueId(playerUuid)
    .playerName("TestPlayer")
    .build();

CompletableFuture<OneblockPlayer> savedPlayer = 
    playerRepository.createAsync(newPlayer);

// Find existing player
CompletableFuture<Optional<OneblockPlayer>> existingPlayer = 
    playerRepository.findByUuidAsync(playerUuid);

// Update player
existingPlayer.thenCompose(player -> {
    if (player.isPresent()) {
        player.get().setPlayerName("UpdatedName");
        return playerRepository.updateAsync(player.get());
    }
    return CompletableFuture.completedFuture(null);
});
```

### Chaining Operations

```java
// Complex operation chain
playerRepository.findByUuidAsync(playerUuid)
    .thenCompose(player -> {
        if (player.isPresent()) {
            return islandRepository.findByOwnerAsync(playerUuid);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    })
    .thenCompose(island -> {
        if (island.isPresent()) {
            return memberRepository.findActiveByIslandAsync(island.get().getId());
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    })
    .thenAccept(members -> {
        // Process active members
        members.forEach(member -> {
            // Handle each member
        });
    })
    .exceptionally(throwable -> {
        logger.error("Operation failed", throwable);
        return null;
    });
```

### Repository Manager Usage

```java
@Inject
private RepositoryManager repositoryManager;

// Access repositories through manager
OneblockPlayerRepository playerRepo = repositoryManager.getPlayerRepository();
OneblockIslandRepository islandRepo = repositoryManager.getIslandRepository();

// Get statistics
String stats = repositoryManager.getStatistics();
logger.info("Repository statistics: {}", stats);
```

## Error Handling

All async operations can complete exceptionally. Handle errors appropriately:

```java
repository.findByUuidAsync(uuid)
    .thenAccept(result -> {
        // Handle successful result
        if (result.isPresent()) {
            processEntity(result.get());
        }
    })
    .exceptionally(throwable -> {
        // Handle database errors
        if (throwable instanceof PersistenceException) {
            logger.error("Database error occurred", throwable);
        } else {
            logger.error("Unexpected error", throwable);
        }
        return null;
    });
```

## Performance Considerations

### Caching Strategy

- **Entity caching** by primary key for fast retrieval
- **Query result caching** for frequently accessed data
- **Lazy loading** for related entities to avoid N+1 problems
- **Connection pooling** managed by JEHibernate

### Best Practices

1. **Use batch operations** for bulk data manipulation
2. **Leverage caching** by accessing entities by their cache keys when possible
3. **Chain operations** using CompletableFuture composition
4. **Handle errors** gracefully with proper exception handling
5. **Use entity graphs** for complex queries with multiple joins

### Query Optimization

```java
// Efficient: Uses cache key
CompletableFuture<Optional<OneblockPlayer>> player = 
    playerRepository.findByUuidAsync(uuid);

// Less efficient: Custom query
CompletableFuture<Optional<OneblockPlayer>> player = 
    playerRepository.findByNameAsync(playerName);

// Batch operation: More efficient for multiple entities
CompletableFuture<List<OneblockIslandMember>> members = 
    memberRepository.findByIslandAsync(islandId);
```

## Integration with JEHibernate

The repositories seamlessly integrate with JEHibernate's features:

- **BaseEntity/BaseEntity** support for automatic ID management
- **Audit fields** (created/updated timestamps) handled automatically
- **Soft deletes** supported where configured
- **Entity validation** using Bean Validation annotations
- **Transaction management** handled by the underlying framework

## Migration from Legacy Code

When migrating from direct JPA/Hibernate usage:

```java
// Old approach - direct EntityManager usage
EntityManager em = entityManagerFactory.createEntityManager();
try {
    em.getTransaction().begin();
    OneblockPlayer player = em.find(OneblockPlayer.class, playerId);
    player.setPlayerName("NewName");
    em.merge(player);
    em.getTransaction().commit();
} finally {
    em.close();
}

// New approach - Repository pattern
playerRepository.findByUuidAsync(playerUuid)
    .thenCompose(player -> {
        if (player.isPresent()) {
            player.get().setPlayerName("NewName");
            return playerRepository.updateAsync(player.get());
        }
        return CompletableFuture.completedFuture(null);
    });
```

## Future Enhancements

Planned improvements for the repository layer:

1. **Reactive Streams** support for high-throughput scenarios
2. **Distributed caching** with Redis integration
3. **Query metrics** and performance monitoring
4. **Automatic retry** mechanisms for transient failures
5. **Read replicas** support for read-heavy workloads

## Conclusion

The JExOneblock repository layer provides a modern, efficient, and type-safe way to interact with the database. By leveraging JEHibernate's capabilities and following modern Java practices, it offers excellent performance, maintainability, and developer experience.

The asynchronous nature ensures that database operations don't block the main server thread, while the caching layer provides excellent performance for frequently accessed data. The comprehensive query methods cover all common use cases while maintaining flexibility for custom requirements.