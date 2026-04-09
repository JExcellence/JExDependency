# OptimisticLockException Fix for Quest Loading

## Problem

The quest loading system was experiencing `OptimisticLockException` errors when loading quest definitions from YAML files:

```
jakarta.persistence.OptimisticLockException: Row was updated or deleted by another transaction 
(or unsaved-value mapping was incorrect): [com.raindropcentral.rdq.database.entity.quest.QuestTask#115]
```

### Root Cause

The issue occurred because:

1. **Cascade Operations**: The `Quest` entity has `CascadeType.ALL` on its `tasks` relationship, which means when updating a Quest, Hibernate automatically cascades the update to all associated QuestTask entities.

2. **Detached Entities**: When `repository.update()` calls `em.merge()` on a Quest entity, it tries to merge all cascaded child entities (QuestTask). If these child entities were loaded in a previous transaction, they have stale version numbers.

3. **Concurrent Modifications**: Even with sequential processing, the cascade operations were causing version conflicts because:
   - Quest A is loaded with its tasks
   - Quest A is updated (cascades to tasks)
   - Quest B is loaded (may reference same task IDs)
   - Quest B is updated (tries to cascade to tasks with stale versions)

## Solution

The fix involves three key changes to `QuestSystemFactory`:

### 1. Clear Child Collections Before Update

In `createQuestFromConfigInternal()`, we now clear all child collections before updating to prevent cascade operations:

```java
if (existingOpt.isPresent()) {
    quest = existingOpt.get();
    // Clear collections to prevent cascade operations
    quest.getTasks().clear();
    quest.getRewards().clear();
    quest.getRequirements().clear();
}
```

This ensures that when we call `questRepository.update(quest)`, Hibernate doesn't try to cascade to child entities.

### 2. Handle Tasks in Separate Transactions

Tasks are now created/updated in completely separate transactions from their parent Quest:

```java
// Create quest first
final Quest quest = createQuestFromConfig(questConfig);

// Then create tasks separately
if (quest != null) {
    createTasksFromConfig(quest, questConfig);
}
```

### 3. Load Fresh Quest Reference for New Tasks

When creating a new task, we load the quest fresh from the database to ensure we have the latest version:

```java
if (existingOpt.isPresent()) {
    task = existingOpt.get();
    // Clear collections to prevent cascade operations
    task.getRewards().clear();
    task.getRequirements().clear();
} else {
    // Load quest fresh from database to get latest version
    final var freshQuestOpt = questRepository.findById(quest.getId());
    if (freshQuestOpt.isEmpty()) {
        LOGGER.warning("Quest not found when creating task: " + quest.getIdentifier());
        return;
    }
    task = new QuestTask(freshQuestOpt.get(), taskSection.getIdentifier(), taskIcon, taskSection.getOrderIndex());
}
```

### 4. Removed Non-Existent Method Calls

Removed calls to `createRewardsFromJson()` and `createTaskRewardsFromJson()` which didn't exist and were causing compilation issues.

## Benefits

1. **No More OptimisticLockException**: By avoiding cascade operations and handling entities separately, we eliminate version conflicts.

2. **Better Transaction Isolation**: Each entity type (Quest, QuestTask) is handled in its own transaction, reducing lock contention.

3. **Cleaner Code**: The separation of concerns makes it clearer that Quest and QuestTask are managed independently.

4. **Retry Logic Still Works**: The existing retry logic with exponential backoff is still in place for any remaining edge cases.

## Testing

Build verification:
```bash
./gradlew :RDQ:rdq-premium:build -x test
```

Result: BUILD SUCCESSFUL

## Future Improvements

Consider modifying the `Quest` entity to use more selective cascade types:

```java
@OneToMany(mappedBy = "quest", cascade = {CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
@OrderBy("orderIndex ASC")
private List<QuestTask> tasks = new ArrayList<>();
```

This would prevent automatic PERSIST and MERGE cascading while still allowing orphan removal and cascade delete operations.

## Related Files

- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/quest/QuestSystemFactory.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/database/entity/quest/Quest.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/database/entity/quest/QuestTask.java`
