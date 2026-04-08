# RDQ Quest System - Session Summary & Next Steps

## What Happened in Previous Session

The previous session was working on **Task 9: Create repository classes** from the implementation plan. Progress was made on creating `QuestRepository`, but the session hit token limits before completing the work.

## Critical Discovery

While reviewing the codebase, I discovered that **critical player progress tracking entities are missing**:

### Missing Entities
1. **PlayerQuestProgress** - Tracks which quests a player has started/completed
2. **PlayerTaskProgress** - Tracks progress on individual quest tasks

### What Exists
- ✅ `PlayerTaskRequirementProgress` - Tracks requirement progress (references missing `PlayerTaskProgress`)
- ✅ `Quest` - Main quest definition entity (well-structured with IProgressionNode)
- ✅ `QuestTask` - Task definitions
- ✅ `QuestRequirement` - Requirement definitions
- ✅ `QuestReward` - Reward definitions
- ✅ `QuestCompletionHistory` - Historical completion records

## The Problem

The code references `PlayerTaskProgress` in multiple places:
- `PlayerTaskRequirementProgress.java` (line 38) - has a ManyToOne relationship
- `QuestProgressTrackerImpl.java` - likely uses it for tracking
- Service layer - expects these entities to exist

**Without these entities, the quest progress tracking system cannot function.**

## Recommended Action Plan

### Option 1: Create Missing Entities First (Recommended)
**Pros:**
- Fixes the broken references immediately
- Allows repository work to proceed correctly
- Follows proper dependency order
- Enables testing as we go

**Cons:**
- Requires careful design of entity relationships
- More upfront work before seeing results

**Steps:**
1. Create `PlayerQuestProgress` entity
2. Create `PlayerTaskProgress` entity
3. Update `PlayerTaskRequirementProgress` relationships
4. Complete `QuestRepository`
5. Create remaining repositories
6. Implement caching layer
7. Update services

### Option 2: Continue with Repositories (Not Recommended)
**Pros:**
- Continues where previous session left off
- Completes one task at a time

**Cons:**
- Repositories will reference non-existent entities
- Cannot test until entities exist
- May need to refactor repositories later
- Wastes time on incomplete work

## Detailed Entity Design Proposal

### PlayerQuestProgress Entity

```java
@Entity
@Table(
    name = "rdq_player_quest_progress",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"player_id", "quest_id"}
    ),
    indexes = {
        @Index(name = "idx_player_quest_player", columnList = "player_id"),
        @Index(name = "idx_player_quest_quest", columnList = "quest_id"),
        @Index(name = "idx_player_quest_completed", columnList = "completed"),
        @Index(name = "idx_player_quest_started", columnList = "started_at")
    }
)
public class PlayerQuestProgress extends BaseEntity {
    
    // Player who started the quest
    @Column(name = "player_id", nullable = false)
    private UUID playerId;
    
    // The quest being progressed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    // When the quest was started
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    // When the quest was completed (null if not completed)
    @Column(name = "completed_at")
    private Instant completedAt;
    
    // Whether the quest is completed
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    // Task progress for this quest
    @OneToMany(mappedBy = "questProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerTaskProgress> taskProgress = new ArrayList<>();
    
    // Helper methods for managing task progress
    public void addTaskProgress(PlayerTaskProgress progress) { ... }
    public void removeTaskProgress(PlayerTaskProgress progress) { ... }
    public Optional<PlayerTaskProgress> getTaskProgress(QuestTask task) { ... }
    public boolean isTaskCompleted(QuestTask task) { ... }
    public int getCompletedTaskCount() { ... }
    public double getOverallProgress() { ... }
}
```

### PlayerTaskProgress Entity

```java
@Entity
@Table(
    name = "rdq_player_task_progress",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"quest_progress_id", "task_id"}
    ),
    indexes = {
        @Index(name = "idx_player_task_quest_progress", columnList = "quest_progress_id"),
        @Index(name = "idx_player_task_task", columnList = "task_id"),
        @Index(name = "idx_player_task_completed", columnList = "completed")
    }
)
public class PlayerTaskProgress extends BaseEntity {
    
    // The quest progress this task belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_progress_id", nullable = false)
    private PlayerQuestProgress questProgress;
    
    // The task being progressed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private QuestTask task;
    
    // Current progress value (for simple tasks)
    @Column(name = "current_progress", nullable = false)
    private long currentProgress = 0;
    
    // Whether the task is completed
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    // When the task was completed
    @Column(name = "completed_at")
    private Instant completedAt;
    
    // Requirement progress for this task (for complex tasks)
    @OneToMany(mappedBy = "taskProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerTaskRequirementProgress> requirementProgress = new ArrayList<>();
    
    // Helper methods
    public void incrementProgress(long amount) { ... }
    public void setProgress(long progress) { ... }
    public double getProgressPercentage() { ... }
    public boolean isCompleted() { ... }
    public void markCompleted() { ... }
}
```

## Entity Relationship Diagram

```
Quest (1) ----< (N) PlayerQuestProgress
                    |
                    | (1)
                    |
                    v
                   (N) PlayerTaskProgress
                        |
                        | (1)
                        |
                        v
                       (N) PlayerTaskRequirementProgress
                            |
                            | (N)
                            |
                            v
                           (1) QuestTaskRequirement
```

## Key Design Decisions

### 1. Progress Storage Strategy
**Decision:** Store both simple progress (long value) and complex progress (requirement collection)

**Rationale:**
- Simple tasks (e.g., "mine 10 stone") only need a counter
- Complex tasks (e.g., "complete 3 requirements") need granular tracking
- Storing both avoids complex polymorphism

### 2. Completion Tracking
**Decision:** Store completion status at both task and quest level

**Rationale:**
- Faster queries for "completed quests"
- Avoids calculating completion from task progress every time
- Denormalized but worth it for performance

### 3. Timestamp Storage
**Decision:** Store startedAt and completedAt timestamps

**Rationale:**
- Required for time-limited quests
- Required for cooldown calculations
- Useful for analytics and leaderboards

### 4. Fetch Strategy
**Decision:** Use LAZY fetching for most relationships

**Rationale:**
- Prevents N+1 query problems
- Allows selective loading based on use case
- Can use JOIN FETCH when needed

## Repository Implementation Order

1. **QuestRepository** (enhance existing)
   - Add findByCategory
   - Add findByDifficulty
   - Add batch loading methods

2. **QuestCategoryRepository** (new)
   - findByIdentifier
   - findByEnabled
   - findAllOrdered

3. **PlayerQuestProgressRepository** (new)
   - findByPlayer
   - findByPlayerAndQuest
   - findActiveByPlayer
   - findCompletedByPlayer

4. **PlayerTaskProgressRepository** (new)
   - findByQuestProgress
   - findByQuestProgressAndTask
   - findCompletedByQuestProgress

5. **QuestCompletionHistoryRepository** (new)
   - findByPlayer
   - findLatestByPlayerAndQuest
   - countByPlayerAndQuest

## Caching Strategy

### What to Cache
- ✅ Quest definitions (rarely change)
- ✅ QuestCategory definitions (rarely change)
- ✅ Active player quest progress (online players only)
- ❌ Quest completion history (query on demand)

### Cache Implementation Pattern

```java
public class PlayerQuestProgressCache {
    private final ConcurrentHashMap<UUID, List<PlayerQuestProgress>> cache;
    private final Set<UUID> dirtyPlayers;
    private final PlayerQuestProgressRepository repository;
    
    // Load on join
    public CompletableFuture<Void> loadPlayerAsync(UUID playerId) { ... }
    
    // Get from cache
    public List<PlayerQuestProgress> getActiveQuests(UUID playerId) { ... }
    
    // Update in cache
    public void updateProgress(UUID playerId, PlayerQuestProgress progress) { ... }
    
    // Save on quit
    public void savePlayer(UUID playerId) { ... }
    
    // Auto-save every 5 minutes
    public int autoSaveAll() { ... }
}
```

## Testing Strategy

### Unit Tests (Optional)
- Entity relationship management
- Progress calculation methods
- Repository CRUD operations

### Integration Tests (Recommended)
- Quest start flow
- Task progress tracking
- Quest completion flow
- Repeatability and cooldowns

### Manual Tests (Required)
1. Start a quest
2. Complete tasks in order
3. Complete the quest
4. Verify rewards granted
5. Test repeatability (if applicable)
6. Test cooldowns (if applicable)
7. Test prerequisites

## Estimated Effort

### Phase 1: Create Missing Entities (2-3 hours)
- PlayerQuestProgress entity with full Javadoc
- PlayerTaskProgress entity with full Javadoc
- Update PlayerTaskRequirementProgress relationships
- Test entity relationships

### Phase 2: Complete Repositories (2-3 hours)
- Enhance QuestRepository
- Create QuestCategoryRepository
- Create PlayerQuestProgressRepository
- Create PlayerTaskProgressRepository
- Create QuestCompletionHistoryRepository

### Phase 3: Implement Caching (2-3 hours)
- PlayerQuestProgressCache
- Auto-save task
- Load/save lifecycle integration

### Phase 4: Update Services (3-4 hours)
- Update QuestServiceImpl
- Update QuestProgressTrackerImpl
- Update QuestCompletionTracker
- Update QuestCacheManager

### Phase 5: Testing & Integration (2-3 hours)
- Integration tests
- Manual testing
- Bug fixes
- Documentation updates

**Total: 11-16 hours**

## Questions for You

Before I proceed, please confirm:

1. **Should I create the missing entities first?** (Recommended: Yes)
2. **Do the entity designs look correct?** (Review the proposals above)
3. **Any specific requirements for progress tracking?** (e.g., partial credit, bonus objectives)
4. **Should I proceed with Option 1 (create entities first)?** (Recommended: Yes)

## Next Steps (Awaiting Your Approval)

Once you approve, I will:

1. Create `PlayerQuestProgress` entity with full Javadoc
2. Create `PlayerTaskProgress` entity with full Javadoc
3. Update `PlayerTaskRequirementProgress` to fix relationships
4. Test entity relationships compile correctly
5. Move to repository implementation

Please review this summary and let me know how you'd like to proceed!
