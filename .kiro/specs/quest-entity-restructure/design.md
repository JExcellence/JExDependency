# Design Document

## Overview

This design document outlines the technical architecture for restructuring the quest entity system in RaindropQuests (RDQ). The new design follows JEHibernate best practices, mirrors the successful rank system patterns, and integrates with RPlatform's progression, requirement, and reward systems.

The restructure introduces a comprehensive entity hierarchy with proper relationship mapping, progress tracking, and historical record keeping. All entities extend BaseEntity for consistent ID and timestamp management, and Quest entities implement IProgressionNode for prerequisite-based progression chains.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Quest System Layer                       │
├─────────────────────────────────────────────────────────────┤
│  QuestService │ QuestProgressTracker │ QuestCompletionTracker│
└────────────┬────────────────────────────────────────┬────────┘
             │                                         │
┌────────────▼─────────────────────────────────────────▼────────┐
│                    Entity Layer (JEHibernate)                 │
├───────────────────────────────────────────────────────────────┤
│  Quest │ QuestCategory │ QuestTask │ Requirements │ Rewards   │
│  PlayerQuestProgress │ PlayerTaskProgress │ CompletionHistory │
└────────────┬──────────────────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────────────────┐
│                   RPlatform Integration                        │
├───────────────────────────────────────────────────────────────┤
│  IProgressionNode │ RequirementService │ RewardService         │
│  ProgressionValidator │ BaseRequirement │ BaseReward           │
└───────────────────────────────────────────────────────────────┘
```

### Entity Hierarchy

```
BaseEntity (JEHibernate)
├── Quest (implements IProgressionNode<Quest>)
├── QuestCategory
├── QuestTask
├── QuestRequirement
├── QuestReward
├── QuestTaskRequirement
├── QuestTaskReward
├── QuestCategoryRequirement
├── QuestCategoryReward
├── PlayerQuestProgress
├── PlayerTaskProgress
├── PlayerTaskRequirementProgress
└── QuestCompletionHistory
```

## Components and Interfaces

### Core Quest Entity

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Represents a quest with tasks, requirements, rewards, and progression metadata.

**Key Responsibilities:**
- Store quest metadata (identifier, difficulty, repeatability, time limits)
- Manage relationships to category, tasks, requirements, and rewards
- Implement IProgressionNode for prerequisite validation
- Provide helper methods for relationship management

**Relationships:**
- ManyToOne to QuestCategory (LAZY)
- OneToMany to QuestTask (LAZY, CASCADE ALL, orphanRemoval)
- OneToMany to QuestRequirement (EAGER, CASCADE ALL, orphanRemoval)
- OneToMany to QuestReward (EAGER, CASCADE ALL, orphanRemoval)
- ElementCollection for prerequisite quest IDs (EAGER)
- ElementCollection for unlocked quest IDs (EAGER)

### Quest Category Entity

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Organizes quests into hierarchical categories with their own requirements and rewards.

**Key Responsibilities:**
- Group related quests for navigation
- Define category-level requirements and rewards
- Support category prerequisite chains
- Manage display ordering

**Relationships:**
- OneToMany to Quest (LAZY, CASCADE ALL, orphanRemoval)
- OneToMany to QuestCategoryRequirement (EAGER, CASCADE ALL, orphanRemoval)
- OneToMany to QuestCategoryReward (EAGER, CASCADE ALL, orphanRemoval)
- ElementCollection for prerequisite category IDs (EAGER)

### Quest Task Entity

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Represents individual objectives within a quest.

**Key Responsibilities:**
- Define task-specific requirements and rewards
- Support sequential or parallel task completion
- Manage task ordering within quest
- Track task difficulty independently

**Relationships:**
- ManyToOne to Quest (LAZY)
- OneToMany to QuestTaskRequirement (EAGER, CASCADE ALL, orphanRemoval)
- OneToMany to QuestTaskReward (EAGER, CASCADE ALL, orphanRemoval)

### Requirement Entities

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Link quests/tasks/categories to RPlatform BaseRequirement system.

**Entities:**
- QuestRequirement (quest-level prerequisites)
- QuestTaskRequirement (task-level prerequisites)
- QuestCategoryRequirement (category-level prerequisites)

**Key Responsibilities:**
- Reference BaseRequirement for actual requirement logic
- Provide UI representation via IconSection
- Manage display ordering
- Offer convenience methods (isMet, calculateProgress, consume)

**Relationships:**
- ManyToOne to parent entity (Quest/QuestTask/QuestCategory)
- ManyToOne to BaseRequirement (EAGER)

### Reward Entities

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Link quests/tasks/categories to RPlatform BaseReward system.

**Entities:**
- QuestReward (quest completion rewards)
- QuestTaskReward (task completion rewards)
- QuestCategoryReward (category completion rewards)

**Key Responsibilities:**
- Reference BaseReward for actual reward logic
- Provide UI representation via IconSection
- Manage display ordering and auto-grant behavior
- Offer convenience methods (grant, getEstimatedValue)

**Relationships:**
- ManyToOne to parent entity (Quest/QuestTask/QuestCategory)
- ManyToOne to BaseReward (EAGER)


### Player Progress Entities

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Track player's active quest state and task progress.

#### PlayerQuestProgress

**Key Responsibilities:**
- Track active quest for a player
- Store start time and expiration time
- Track completion status and timestamp
- Calculate remaining time for time-limited quests

**Relationships:**
- ManyToOne to RDQPlayer (EAGER)
- ManyToOne to Quest (EAGER)
- OneToMany to PlayerTaskProgress (LAZY, CASCADE ALL, orphanRemoval)
- Unique constraint on (player_id, quest_id)

#### PlayerTaskProgress

**Key Responsibilities:**
- Track progress for individual tasks (0.0 to 1.0)
- Store completion status and timestamp
- Manage task requirement progress
- Provide progress increment and reset methods

**Relationships:**
- ManyToOne to PlayerQuestProgress (EAGER)
- ManyToOne to QuestTask (EAGER)
- OneToMany to PlayerTaskRequirementProgress (LAZY, CASCADE ALL, orphanRemoval)
- Unique constraint on (quest_progress_id, task_id)

#### PlayerTaskRequirementProgress

**Key Responsibilities:**
- Track progress for individual task requirements (0.0 to 1.0)
- Support incremental progress updates
- Determine requirement completion status

**Relationships:**
- ManyToOne to PlayerTaskProgress (EAGER)
- ManyToOne to QuestTaskRequirement (EAGER)
- Unique constraint on (task_progress_id, requirement_id)

### Quest Completion History Entity

**Package:** `com.raindropcentral.rdq.database.entity.quest`

**Purpose:** Maintain historical record of all quest completions for statistics and repeat tracking.

**Key Responsibilities:**
- Record each quest completion with timestamp
- Track completion count for repeatable quests
- Store time taken to complete quest
- Calculate cooldown remaining for repeat attempts
- Validate if player can repeat quest based on max completions

**Relationships:**
- ManyToOne to RDQPlayer (EAGER)
- ManyToOne to Quest (EAGER)
- Index on (player_id, quest_id)
- Index on completed_at for temporal queries

## Data Models

### Quest Entity Schema

```java
@Entity
@Table(name = "rdq_quest")
public class Quest extends BaseEntity implements IProgressionNode<Quest> {
    
    // Core Identity
    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;
    
    // Display
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    // Configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 32)
    private QuestDifficulty difficulty;
    
    @Column(name = "repeatable", nullable = false)
    private boolean repeatable = false;
    
    @Column(name = "max_completions", nullable = false)
    private int maxCompletions = 0;
    
    @Column(name = "cooldown_seconds", nullable = false)
    private long cooldownSeconds = 0;
    
    @Column(name = "time_limit_seconds", nullable = false)
    private long timeLimitSeconds = 0;
    
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
    
    // Progression
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdq_quest_prerequisites")
    @Column(name = "prerequisite_quest_id", length = 64)
    @OrderColumn(name = "prerequisite_order")
    private List<String> previousQuestIds = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdq_quest_unlocks")
    @Column(name = "unlocked_quest_id", length = 64)
    @OrderColumn(name = "unlock_order")
    private List<String> nextQuestIds = new ArrayList<>();
    
    // Child Entities
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<QuestTask> tasks = new ArrayList<>();
    
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<QuestRequirement> requirements = new HashSet<>();
    
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<QuestReward> rewards = new HashSet<>();
}
```

### QuestCategory Entity Schema

```java
@Entity
@Table(name = "rdq_quest_category")
public class QuestCategory extends BaseEntity {
    
    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;
    
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdq_quest_category_prerequisites")
    @Column(name = "prerequisite_category_id", length = 64)
    private List<String> previousCategoryIds = new ArrayList<>();
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quest> quests = new ArrayList<>();
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<QuestCategoryRequirement> requirements = new HashSet<>();
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<QuestCategoryReward> rewards = new HashSet<>();
}
```

### QuestTask Entity Schema

```java
@Entity
@Table(name = "rdq_quest_task")
public class QuestTask extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    @Column(name = "task_identifier", nullable = false, length = 64)
    private String taskIdentifier;
    
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 32)
    private TaskDifficulty difficulty;
    
    @Column(name = "sequential", nullable = false)
    private boolean sequential = false;
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<QuestTaskRequirement> requirements = new HashSet<>();
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<QuestTaskReward> rewards = new HashSet<>();
}
```

### PlayerQuestProgress Entity Schema

```java
@Entity
@Table(name = "rdq_player_quest_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "quest_id"}))
public class PlayerQuestProgress extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "questProgress", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlayerTaskProgress> taskProgress = new ArrayList<>();
    
    @Version
    @Column(name = "version")
    private int version;
}
```

### QuestCompletionHistory Entity Schema

```java
@Entity
@Table(name = "rdq_quest_completion_history",
       indexes = {
           @Index(name = "idx_completion_player_quest", columnList = "player_id,quest_id"),
           @Index(name = "idx_completion_date", columnList = "completed_at")
       })
public class QuestCompletionHistory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
    
    @Column(name = "completion_count", nullable = false)
    private int completionCount;
    
    @Column(name = "time_taken_seconds", nullable = false)
    private long timeTakenSeconds;
}
```


## Enums

### QuestDifficulty Enum

```java
public enum QuestDifficulty {
    TRIVIAL(1, "quest.difficulty.trivial", NamedTextColor.GRAY),
    EASY(2, "quest.difficulty.easy", NamedTextColor.GREEN),
    NORMAL(3, "quest.difficulty.normal", NamedTextColor.YELLOW),
    HARD(4, "quest.difficulty.hard", NamedTextColor.RED),
    EXTREME(5, "quest.difficulty.extreme", NamedTextColor.DARK_RED);
    
    private final int level;
    private final String translationKey;
    private final NamedTextColor color;
    
    QuestDifficulty(int level, String translationKey, NamedTextColor color) {
        this.level = level;
        this.translationKey = translationKey;
        this.color = color;
    }
    
    public int getLevel() { return level; }
    public String getTranslationKey() { return translationKey; }
    public NamedTextColor getColor() { return color; }
}
```

### TaskDifficulty Enum

```java
public enum TaskDifficulty {
    TRIVIAL(1, "task.difficulty.trivial", NamedTextColor.GRAY),
    EASY(2, "task.difficulty.easy", NamedTextColor.GREEN),
    MEDIUM(3, "task.difficulty.medium", NamedTextColor.YELLOW),
    HARD(4, "task.difficulty.hard", NamedTextColor.RED),
    EXTREME(5, "task.difficulty.extreme", NamedTextColor.DARK_RED);
    
    private final int level;
    private final String translationKey;
    private final NamedTextColor color;
    
    TaskDifficulty(int level, String translationKey, NamedTextColor color) {
        this.level = level;
        this.translationKey = translationKey;
        this.color = color;
    }
    
    public int getLevel() { return level; }
    public String getTranslationKey() { return translationKey; }
    public NamedTextColor getColor() { return color; }
}
```

## Relationship Management Patterns

### Bidirectional Relationship Helper Methods

Following the rank system pattern, all parent entities provide helper methods for managing child relationships:

```java
// Quest entity
public boolean addTask(@NotNull final QuestTask task) {
    if (this.tasks.contains(task)) {
        return false;
    }
    boolean added = this.tasks.add(task);
    if (added && task.getQuest() != this) {
        task.setQuest(this);
    }
    return added;
}

public boolean removeTask(@NotNull final QuestTask task) {
    boolean removed = this.tasks.remove(task);
    if (removed && task.getQuest() == this) {
        task.setQuest(null);
    }
    return removed;
}

public boolean addRequirement(@NotNull final QuestRequirement requirement) {
    if (this.requirements.contains(requirement)) {
        return false;
    }
    boolean added = this.requirements.add(requirement);
    if (added && requirement.getQuest() != this) {
        requirement.setQuest(this);
    }
    return added;
}

// Similar patterns for rewards, etc.
```

### Ordered Collection Access

```java
// Quest entity
@NotNull
public List<QuestTask> getTasksOrdered() {
    return this.tasks.stream()
        .sorted(Comparator.comparingInt(QuestTask::getOrderIndex))
        .collect(Collectors.toList());
}

@NotNull
public List<QuestRequirement> getRequirementsOrdered() {
    return this.requirements.stream()
        .sorted(Comparator.comparingInt(QuestRequirement::getDisplayOrder))
        .collect(Collectors.toList());
}

@NotNull
public List<QuestReward> getRewardsOrdered() {
    return this.rewards.stream()
        .sorted(Comparator.comparingInt(QuestReward::getDisplayOrder))
        .collect(Collectors.toList());
}
```

## IProgressionNode Implementation

### Quest Progression Integration

```java
@Override
@NotNull
public String getIdentifier() {
    return this.identifier;
}

@Override
@NotNull
public List<String> getPreviousNodeIdentifiers() {
    return this.previousQuestIds != null ? this.previousQuestIds : List.of();
}

@Override
@NotNull
public List<String> getNextNodeIdentifiers() {
    return this.nextQuestIds != null ? this.nextQuestIds : List.of();
}
```

### Integration with ProgressionValidator

```java
// In QuestService or similar
public boolean canStartQuest(Player player, Quest quest) {
    // Use ProgressionValidator to check prerequisites
    ProgressionValidator<Quest> validator = new CachedProgressionValidator<>(
        questCompletionTracker,
        questRepository::findByIdentifier
    );
    
    ProgressionState state = validator.getProgressionState(quest, player.getUniqueId());
    return state.getStatus() == ProgressionStatus.AVAILABLE;
}
```

## Error Handling

### Validation Errors

```java
// Quest entity validation
public void validate() {
    if (identifier == null || identifier.trim().isEmpty()) {
        throw new IllegalStateException("Quest identifier cannot be null or empty");
    }
    if (category == null) {
        throw new IllegalStateException("Quest must belong to a category");
    }
    if (icon == null) {
        throw new IllegalStateException("Quest must have an icon");
    }
    if (maxCompletions < 0) {
        throw new IllegalArgumentException("Max completions cannot be negative");
    }
    if (cooldownSeconds < 0) {
        throw new IllegalArgumentException("Cooldown seconds cannot be negative");
    }
    if (timeLimitSeconds < 0) {
        throw new IllegalArgumentException("Time limit seconds cannot be negative");
    }
}
```

### Optimistic Locking Handling

```java
// In service layer
public void updateQuestProgress(PlayerQuestProgress progress) {
    try {
        repository.update(progress);
    } catch (OptimisticLockException e) {
        // Reload and retry
        PlayerQuestProgress fresh = repository.findById(progress.getId())
            .orElseThrow(() -> new IllegalStateException("Progress no longer exists"));
        
        // Merge changes and retry
        mergeProgressChanges(fresh, progress);
        repository.update(fresh);
    }
}
```

### Circular Dependency Detection

```java
// Handled by ProgressionValidator
try {
    validator.validateNoCycles(quest);
} catch (CircularDependencyException e) {
    logger.severe("Circular dependency detected in quest chain: " + e.getCycle());
    throw new IllegalStateException("Quest prerequisite chain contains a cycle", e);
}
```

## Testing Strategy

### Unit Tests

**Entity Tests:**
- Test entity construction and validation
- Test relationship management methods (add/remove)
- Test equals and hashCode implementations
- Test convenience methods (isExpired, getRemainingTime, etc.)
- Test progress increment and capping logic

**Example:**
```java
@Test
void testAddTask_ManagesBidirectionalRelationship() {
    Quest quest = new Quest("test_quest", category, icon, QuestDifficulty.NORMAL);
    QuestTask task = new QuestTask(quest, "task_1", icon, 0);
    
    assertTrue(quest.addTask(task));
    assertEquals(quest, task.getQuest());
    assertTrue(quest.getTasks().contains(task));
}

@Test
void testPlayerTaskProgress_CapsProgressAt1() {
    PlayerTaskProgress progress = new PlayerTaskProgress(questProgress, task);
    progress.setProgress(1.5);
    
    assertEquals(1.0, progress.getProgress(), 0.001);
    assertTrue(progress.isCompleted());
}
```

### Integration Tests

**Repository Tests:**
- Test CRUD operations
- Test cascade operations (delete parent deletes children)
- Test unique constraints
- Test custom query methods
- Test optimistic locking

**Example:**
```java
@Test
void testCascadeDelete_RemovesChildEntities() {
    Quest quest = createTestQuest();
    quest.addTask(createTestTask());
    quest.addRequirement(createTestRequirement());
    
    questRepository.create(quest);
    Long questId = quest.getId();
    
    questRepository.delete(questId);
    
    assertFalse(questRepository.findById(questId).isPresent());
    assertTrue(questTaskRepository.findByQuest(questId).isEmpty());
    assertTrue(questRequirementRepository.findByQuest(questId).isEmpty());
}
```

### Progression Tests

**IProgressionNode Tests:**
- Test prerequisite validation
- Test circular dependency detection
- Test progression state calculation
- Test integration with QuestCompletionTracker

**Example:**
```java
@Test
void testProgressionValidator_DetectsCircularDependency() {
    Quest quest1 = createQuest("quest_1");
    Quest quest2 = createQuest("quest_2");
    Quest quest3 = createQuest("quest_3");
    
    quest1.setPreviousQuestIds(List.of("quest_3"));
    quest2.setPreviousQuestIds(List.of("quest_1"));
    quest3.setPreviousQuestIds(List.of("quest_2"));
    
    assertThrows(CircularDependencyException.class, () -> {
        validator.validateNoCycles(quest1);
    });
}
```


## Migration Strategy

### Phase 1: Create New Entity Structure

1. Create all new entity classes in parallel with existing structure
2. Use different table names to avoid conflicts (rdq_quest vs existing tables)
3. Create repositories for new entities
4. Implement and test all relationship management

### Phase 2: Data Migration

```java
public class QuestEntityMigration {
    
    public void migrateQuests() {
        List<OldQuest> oldQuests = oldQuestRepository.findAll();
        
        for (OldQuest oldQuest : oldQuests) {
            Quest newQuest = new Quest(
                oldQuest.getIdentifier(),
                migrateCategory(oldQuest.getCategory()),
                oldQuest.getIcon(),
                oldQuest.getDifficulty()
            );
            
            // Migrate basic properties
            newQuest.setRepeatable(oldQuest.isRepeatable());
            newQuest.setMaxCompletions(oldQuest.getMaxCompletions());
            newQuest.setCooldownSeconds(oldQuest.getCooldownSeconds());
            newQuest.setTimeLimitSeconds(oldQuest.getTimeLimitSeconds());
            newQuest.setEnabled(oldQuest.isEnabled());
            
            // Migrate prerequisites
            newQuest.setPreviousQuestIds(oldQuest.getPreviousQuestIds());
            newQuest.setNextQuestIds(oldQuest.getNextQuestIds());
            
            // Migrate tasks
            for (OldQuestTask oldTask : oldQuest.getTasks()) {
                QuestTask newTask = migrateTask(oldTask, newQuest);
                newQuest.addTask(newTask);
            }
            
            // Migrate requirements (convert from JSON to entities)
            for (RequirementData reqData : oldQuest.getRequirementData()) {
                BaseRequirement baseReq = createBaseRequirement(reqData);
                QuestRequirement questReq = new QuestRequirement(
                    newQuest,
                    baseReq,
                    reqData.getIcon()
                );
                newQuest.addRequirement(questReq);
            }
            
            // Migrate rewards (convert from JSON to entities)
            for (RewardData rewData : oldQuest.getRewardData()) {
                BaseReward baseRew = createBaseReward(rewData);
                QuestReward questRew = new QuestReward(
                    newQuest,
                    baseRew,
                    rewData.getIcon()
                );
                newQuest.addReward(questRew);
            }
            
            questRepository.create(newQuest);
        }
    }
    
    public void migratePlayerProgress() {
        // Migrate active quest progress
        List<OldActiveQuest> oldActive = oldActiveQuestRepository.findAll();
        
        for (OldActiveQuest oldActive : oldActive) {
            PlayerQuestProgress newProgress = new PlayerQuestProgress(
                oldActive.getPlayer(),
                findNewQuest(oldActive.getQuestId())
            );
            
            newProgress.setStartedAt(oldActive.getStartedAt());
            newProgress.setExpiresAt(oldActive.getExpiresAt());
            newProgress.setCompleted(oldActive.isCompleted());
            newProgress.setCompletedAt(oldActive.getCompletedAt());
            
            // Migrate task progress
            for (OldTaskProgress oldTaskProg : oldActive.getTaskProgress()) {
                PlayerTaskProgress newTaskProg = new PlayerTaskProgress(
                    newProgress,
                    findNewTask(oldTaskProg.getTaskId())
                );
                newTaskProg.setProgress(oldTaskProg.getProgress());
                newTaskProg.setCompleted(oldTaskProg.isCompleted());
                newTaskProg.setCompletedAt(oldTaskProg.getCompletedAt());
                
                newProgress.addTaskProgress(newTaskProg);
            }
            
            playerQuestProgressRepository.create(newProgress);
        }
    }
    
    public void migrateCompletionHistory() {
        // Migrate historical completions
        List<OldQuestCompletion> oldCompletions = oldCompletionRepository.findAll();
        
        for (OldQuestCompletion oldComp : oldCompletions) {
            QuestCompletionHistory newHistory = new QuestCompletionHistory(
                oldComp.getPlayer(),
                findNewQuest(oldComp.getQuestId()),
                oldComp.getCompletedAt(),
                oldComp.getCompletionCount(),
                oldComp.getTimeTakenSeconds()
            );
            
            completionHistoryRepository.create(newHistory);
        }
    }
}
```

### Phase 3: Service Layer Updates

1. Update QuestService to use new entities
2. Update QuestProgressTracker to use new progress entities
3. Update QuestCompletionTracker to use new history entities
4. Update event listeners to work with new structure
5. Update cache managers for new entity types

### Phase 4: View Layer Updates

1. Update quest views to use new entity structure
2. Update requirement/reward display logic
3. Update progress tracking displays
4. Test all UI interactions

### Phase 5: Cleanup

1. Remove old entity classes
2. Drop old database tables
3. Remove migration code
4. Update documentation

### Rollback Strategy

```java
public class QuestEntityRollback {
    
    public void rollbackToOldStructure() {
        // 1. Stop using new entities in services
        questService.useOldEntities();
        
        // 2. Migrate data back to old structure
        migrateNewToOld();
        
        // 3. Drop new tables
        dropNewTables();
        
        // 4. Restore old service implementations
        restoreOldServices();
    }
    
    private void migrateNewToOld() {
        // Reverse migration logic
        // Convert new entities back to old format
        // Preserve all data that existed in old structure
    }
}
```

## Performance Considerations

### Query Optimization

**Batch Loading:**
```java
// Load quests with requirements and rewards in single query
@Query("SELECT DISTINCT q FROM Quest q " +
       "LEFT JOIN FETCH q.requirements " +
       "LEFT JOIN FETCH q.rewards " +
       "WHERE q.category.id = :categoryId")
List<Quest> findByCategoryWithDetails(@Param("categoryId") Long categoryId);
```

**Pagination:**
```java
public Page<Quest> findQuestsByCategory(Long categoryId, Pageable pageable) {
    return questRepository.findByCategory(categoryId, pageable);
}
```

**Efficient Progress Queries:**
```java
// Find active quests for player with task progress
@Query("SELECT DISTINCT p FROM PlayerQuestProgress p " +
       "LEFT JOIN FETCH p.taskProgress " +
       "WHERE p.player.id = :playerId AND p.completed = false")
List<PlayerQuestProgress> findActiveQuestsWithProgress(@Param("playerId") Long playerId);
```

### Caching Strategy

**Quest Cache:**
```java
public class QuestCacheManager {
    private final Cache<String, Quest> questCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(30))
        .build();
    
    public Quest getQuest(String identifier) {
        return questCache.get(identifier, id -> questRepository.findByIdentifier(id));
    }
}
```

**Player Progress Cache:**
```java
public class QuestProgressCache {
    // Cache active quest progress for online players
    private final Map<UUID, List<PlayerQuestProgress>> activeProgressCache = 
        new ConcurrentHashMap<>();
    
    public void loadPlayerProgress(UUID playerId) {
        List<PlayerQuestProgress> progress = 
            progressRepository.findActiveQuestsWithProgress(playerId);
        activeProgressCache.put(playerId, progress);
    }
    
    public void savePlayerProgress(UUID playerId) {
        List<PlayerQuestProgress> progress = activeProgressCache.remove(playerId);
        if (progress != null) {
            progress.forEach(progressRepository::update);
        }
    }
}
```

### Index Strategy

```sql
-- Quest lookups
CREATE INDEX idx_quest_identifier ON rdq_quest(identifier);
CREATE INDEX idx_quest_category ON rdq_quest(category_id);
CREATE INDEX idx_quest_enabled ON rdq_quest(enabled);
CREATE INDEX idx_quest_difficulty ON rdq_quest(difficulty);

-- Category lookups
CREATE INDEX idx_category_identifier ON rdq_quest_category(identifier);
CREATE INDEX idx_category_enabled ON rdq_quest_category(enabled);
CREATE INDEX idx_category_order ON rdq_quest_category(display_order);

-- Task lookups
CREATE INDEX idx_task_quest ON rdq_quest_task(quest_id);
CREATE INDEX idx_task_identifier ON rdq_quest_task(task_identifier);
CREATE INDEX idx_task_order ON rdq_quest_task(order_index);

-- Progress lookups
CREATE INDEX idx_progress_player ON rdq_player_quest_progress(player_id);
CREATE INDEX idx_progress_quest ON rdq_player_quest_progress(quest_id);
CREATE INDEX idx_progress_completed ON rdq_player_quest_progress(completed);

-- History lookups
CREATE INDEX idx_history_player_quest ON rdq_quest_completion_history(player_id, quest_id);
CREATE INDEX idx_history_date ON rdq_quest_completion_history(completed_at);
```

## Integration Points

### RPlatform Integration

**RequirementService:**
```java
// In QuestRequirement
public boolean isMet(@NotNull final Player player) {
    return RequirementService.getInstance()
        .isMet(player, this.requirement.getRequirement());
}

public double calculateProgress(@NotNull final Player player) {
    return RequirementService.getInstance()
        .calculateProgress(player, this.requirement.getRequirement());
}

public void consume(@NotNull final Player player) {
    RequirementService.getInstance()
        .consume(player, this.requirement.getRequirement());
}
```

**RewardService:**
```java
// In QuestReward
public CompletableFuture<Boolean> grant(@NotNull final Player player) {
    return this.reward.grant(player);
}

public double getEstimatedValue() {
    return this.reward.getEstimatedValue();
}
```

**ProgressionValidator:**
```java
// In QuestService
public ProgressionState getQuestState(Player player, Quest quest) {
    ProgressionValidator<Quest> validator = new CachedProgressionValidator<>(
        questCompletionTracker,
        questRepository::findByIdentifier
    );
    
    return validator.getProgressionState(quest, player.getUniqueId());
}
```

### Event System Integration

```java
// Quest lifecycle events
public class QuestEventPublisher {
    
    public void publishQuestStarted(Player player, Quest quest) {
        Bukkit.getPluginManager().callEvent(
            new QuestStartEvent(player, quest)
        );
    }
    
    public void publishTaskCompleted(Player player, Quest quest, QuestTask task) {
        Bukkit.getPluginManager().callEvent(
            new TaskCompleteEvent(player, quest, task)
        );
    }
    
    public void publishQuestCompleted(Player player, Quest quest) {
        Bukkit.getPluginManager().callEvent(
            new QuestCompleteEvent(player, quest)
        );
    }
}
```

## Design Decisions and Rationale

### Why Separate Requirement/Reward Entities?

Following the rank system pattern, we use junction entities (QuestRequirement, QuestReward, etc.) rather than direct relationships to BaseRequirement/BaseReward. This provides:

1. **UI Metadata:** Each requirement/reward can have its own icon and display order
2. **Flexibility:** Same BaseRequirement can be used in multiple quests with different icons
3. **Convenience Methods:** Junction entities provide quest-specific convenience methods
4. **Consistency:** Matches the successful RRankUpgradeRequirement/RRankReward pattern

### Why Three Levels of Requirements/Rewards?

Requirements and rewards at quest, task, and category levels provide:

1. **Granular Control:** Different prerequisites for starting vs completing
2. **Incremental Rewards:** Reward players for task completion, not just quest completion
3. **Category Progression:** Lock entire categories behind prerequisites
4. **Flexibility:** Mix and match requirement levels for complex progression

### Why Separate Progress and History Entities?

Separating PlayerQuestProgress (active) from QuestCompletionHistory (historical) provides:

1. **Performance:** Active progress queries don't scan historical data
2. **Data Integrity:** Completed quests can be deleted from progress without losing history
3. **Statistics:** Easy to query completion counts, times, and patterns
4. **Repeatability:** Track multiple completions of same quest

### Why EAGER fetch for Requirements/Rewards?

Requirements and rewards use EAGER fetch because:

1. **Small Collections:** Typically 1-5 requirements/rewards per quest
2. **Always Needed:** UI always displays requirements and rewards together
3. **Avoid N+1:** Prevents separate queries for each requirement/reward
4. **Consistency:** Matches rank system pattern

This design provides a robust, scalable, and maintainable quest entity structure that integrates seamlessly with existing RDQ and RPlatform systems.
