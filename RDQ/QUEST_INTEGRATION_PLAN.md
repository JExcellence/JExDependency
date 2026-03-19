# Quest System Integration Plan

## Overview

This document outlines the remaining work to integrate the new quest entities with the existing system, following the rank system patterns.

## Completed ✅

- All 13 core entities (Tasks 1-8)
- QuestRepository (basic implementation)

## Remaining Work

### 1. Repository Layer (Task 9)

Following RRankRepository pattern, create:

**9.1 QuestRepository** ✅ (Basic - needs completion)
- Add findByCategory method
- Add findByDifficulty method
- Add findByCategoryWithDetails (batch load)

**9.2 QuestCategoryRepository**
```java
public class QuestCategoryRepository extends CachedRepository<QuestCategory, Long, String> {
    CompletableFuture<Optional<QuestCategory>> findByIdentifier(String identifier);
    CompletableFuture<List<QuestCategory>> findByEnabled(boolean enabled);
    CompletableFuture<List<QuestCategory>> findAllOrdered(); // by displayOrder
}
```

**9.3 PlayerQuestProgressRepository**
```java
public class PlayerQuestProgressRepository extends CachedRepository<PlayerQuestProgress, Long, UUID> {
    CompletableFuture<List<PlayerQuestProgress>> findByPlayer(UUID playerId);
    CompletableFuture<Optional<PlayerQuestProgress>> findByPlayerAndQuest(UUID playerId, Long questId);
    CompletableFuture<List<PlayerQuestProgress>> findActiveByPlayer(UUID playerId); // not completed
}
```

**9.4 QuestCompletionHistoryRepository**
```java
public class QuestCompletionHistoryRepository extends CachedRepository<QuestCompletionHistory, Long, Long> {
    CompletableFuture<List<QuestCompletionHistory>> findByPlayer(UUID playerId);
    CompletableFuture<Optional<QuestCompletionHistory>> findLatestByPlayerAndQuest(UUID playerId, Long questId);
    CompletableFuture<Long> countByPlayerAndQuest(UUID playerId, Long questId);
}
```

### 2. Service Layer Integration (Task 12)

**12.1 Update QuestService**
- Integrate Quest entity (replace old structure)
- Use QuestRepository for quest lookups
- Integrate with ProgressionValidator for prerequisites
- Update quest creation/retrieval methods

**12.2 Update QuestProgressTracker**
- Use PlayerQuestProgressRepository
- Use PlayerTaskProgressRepository  
- Implement progress caching for online players (like SimplePerkCache)
- Load on join, save on quit pattern
- Auto-save task for crash protection

**12.3 Update QuestCompletionTracker**
- Use QuestCompletionHistoryRepository
- Implement ICompletionTracker<Quest> interface
- Update repeatability checking logic
- Update cooldown calculations

**12.4 Update QuestEventListener**
- Work with new entity structure
- Fire events for requirement/reward operations
- Update quest start/complete/task complete handlers

**12.5 Update QuestCacheManager**
- Cache Quest entities (like rank system)
- Cache QuestCategory entities
- Cache player progress for online players
- Implement cache invalidation logic

**12.6 Integrate with RPlatform**
- QuestRequirement → RequirementService
- QuestReward → RewardService
- Quest → ProgressionValidator
- Test circular dependency detection

### 3. View Layer Updates (Task 13)

**13.1 Update Quest List Views**
- Use new QuestCategory entity
- Use new Quest entity
- Update filtering/sorting logic
- Update pagination

**13.2 Update Quest Detail Views**
- Display new requirement structure
- Display new reward structure
- Update task display logic
- Update progress display logic

**13.3 Update Quest Progress Views**
- Use PlayerQuestProgress
- Use PlayerTaskProgress
- Update requirement progress display
- Update time remaining display

**13.4 Update Quest History Views**
- Use QuestCompletionHistory
- Update statistics display
- Update cooldown display

## Implementation Priority

### Phase 1: Repositories (Critical)
1. Complete QuestRepository
2. Create QuestCategoryRepository
3. Create PlayerQuestProgressRepository
4. Create QuestCompletionHistoryRepository

### Phase 2: Core Services (Critical)
1. Update QuestService
2. Update QuestProgressTracker with caching
3. Update QuestCompletionTracker
4. Update QuestCacheManager

### Phase 3: Integration (Important)
1. Update QuestEventListener
2. Integrate with RPlatform services
3. Test progression validation

### Phase 4: Views (Important)
1. Update quest list views
2. Update quest detail views
3. Update progress views
4. Update history views

## Key Patterns to Follow

### Repository Pattern (from RRankRepository)
```java
public class XRepository extends CachedRepository<X, Long, KeyType> {
    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;
    
    public XRepository(ExecutorService executor, EntityManagerFactory emf, 
                      Class<X> entityClass, Function<X, KeyType> keyExtractor) {
        super(executor, emf, entityClass, keyExtractor);
        this.entityManagerFactory = emf;
        this.executor = executor;
    }
    
    public CompletableFuture<Optional<X>> findByIdentifier(String id) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                var results = em.createQuery("...", X.class)
                    .setParameter("id", id)
                    .getResultList();
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            } finally {
                em.close();
            }
        }, executor);
    }
}
```

### Caching Pattern (from SimplePerkCache)
```java
public class QuestProgressCache {
    private final ConcurrentHashMap<UUID, PlayerQuestProgress> cache;
    private final Set<UUID> dirtyPlayers;
    
    public CompletableFuture<Void> loadPlayerAsync(UUID playerId) {
        // Load from repository
    }
    
    public Optional<PlayerQuestProgress> getPlayer(UUID playerId) {
        // Get from cache
    }
    
    public void updatePlayer(UUID playerId, PlayerQuestProgress data) {
        cache.put(playerId, data);
        markDirty(playerId);
    }
    
    public void savePlayer(UUID playerId) {
        // Save to repository if dirty
    }
    
    public int autoSaveAll() {
        // Save all dirty players
    }
}
```

### Service Integration Pattern
```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    // Load progress async
    progressCache.loadPlayerAsync(player.getUniqueId())
        .exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load progress");
            return null;
        });
}

@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    
    // Save progress sync
    progressCache.savePlayer(player.getUniqueId());
}
```

## Testing Strategy

### Unit Tests (Optional)
- Repository CRUD operations
- Service logic
- Cache operations
- Progress calculations

### Integration Tests (Recommended)
- Quest start/complete flow
- Progress tracking
- Repeatability logic
- Cooldown management
- Prerequisite validation

### Manual Testing (Required)
- Create quest from YAML
- Start quest
- Complete tasks
- Complete quest
- Repeat quest (if repeatable)
- Check cooldowns
- Verify prerequisites

## Migration Strategy

### Option 1: Fresh Start (Recommended)
- Deploy with empty quest tables
- Load quests from YAML definitions
- No data migration needed

### Option 2: Data Migration
- Create migration utility
- Convert old Quest → new Quest
- Convert old progress → new PlayerQuestProgress
- Convert old history → new QuestCompletionHistory
- Validate data integrity

## Deployment Checklist

- [ ] All repositories created
- [ ] All services updated
- [ ] All views updated
- [ ] Cache management implemented
- [ ] Event handlers updated
- [ ] RPlatform integration tested
- [ ] Manual testing complete
- [ ] Documentation updated
- [ ] Database backup created
- [ ] Rollback plan prepared

## Estimated Effort

- **Repositories:** 2-3 hours
- **Service Integration:** 4-6 hours
- **View Updates:** 3-4 hours
- **Testing:** 2-3 hours
- **Total:** 11-16 hours

## Next Steps

1. Complete remaining repositories
2. Implement progress caching
3. Update core services
4. Update views
5. Test integration
6. Deploy

