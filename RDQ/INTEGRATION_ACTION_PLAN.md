# Quest System Integration - Action Plan

## Current Status

Based on code review:
- ✅ All entities created
- ✅ All repositories created
- ✅ Cache layer complete
- ✅ Views refactored
- ⚠️ Service layer partially integrated
- ❌ Quest completion not implemented
- ❌ Reward system not implemented
- ❌ Task tracking not implemented

## Immediate Action Items

### 1. Complete I18n Integration (15 minutes)

**Status**: Keys ready, needs manual integration

**Action**:
1. Open `RDQ/QUEST_I18N_ADDITIONS.yml`
2. Open `RDQ/rdq-common/src/main/resources/translations/en_US.yml`
3. Copy all sections from QUEST_I18N_ADDITIONS.yml to en_US.yml:
   - Quest category icons (quest.category.*)
   - Quest definitions (quest.{quest_id}.*)
   - Quest reward items (item.*)
   - View keys (view.quest.detail.*, view.quest.list.*, view.quest.category.*)

**Verification**:
```bash
cd RDQ
./gradlew clean rdq-common:build
```

**Expected Result**: Build successful, views display with proper translations

---

### 2. Complete QuestServiceImpl Integration (2-3 hours)

**Status**: Partially implemented, needs completion

#### 2.1 Fix Constructor Dependencies

**File**: `QuestServiceImpl.java` (lines 58-67)

**Current Issue**:
```java
this.progressionValidator = null; // TODO: Get from plugin
this.completionTracker = null; // TODO: Get from plugin
```

**Action**:
```java
this.progressionValidator = plugin.getQuestProgressionValidator();
this.completionTracker = plugin.getQuestCompletionTracker();
```

**Verification**: Check that RDQ.java has these getters

#### 2.2 Complete abandonQuest() Method

**File**: `QuestServiceImpl.java` (line 244+)

**Current Issue**: Method incomplete (cuts off at line 275)

**Action**: Complete the method implementation:
```java
@Override
@NotNull
public CompletableFuture<QuestAbandonResult> abandonQuest(
        @NotNull final UUID playerId,
        @NotNull final String questIdentifier
) {
    LOGGER.fine("Abandoning quest " + questIdentifier + " for player " + playerId);
    
    return isQuestActive(playerId, questIdentifier)
            .thenCompose(isActive -> {
                if (!isActive) {
                    return CompletableFuture.completedFuture(
                            QuestAbandonResult.notActive(questIdentifier)
                    );
                }
                
                // Find quest progress
                Optional<PlayerQuestProgress> progressOpt = progressCache
                        .getActiveQuests(playerId)
                        .stream()
                        .filter(p -> p.getQuest().getIdentifier().equals(questIdentifier))
                        .findFirst();
                
                if (progressOpt.isEmpty()) {
                    return CompletableFuture.completedFuture(
                            QuestAbandonResult.notActive(questIdentifier)
                    );
                }
                
                PlayerQuestProgress progress = progressOpt.get();
                
                // Remove from cache (marks as dirty for deletion)
                progressCache.removeProgress(playerId, progress.getQuest().getId());
                
                LOGGER.info("Successfully abandoned quest " + questIdentifier + " for player " + playerId);
                
                return CompletableFuture.completedFuture(
                        QuestAbandonResult.success(questIdentifier)
                );
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to abandon quest " + questIdentifier, ex);
                return QuestAbandonResult.error(questIdentifier, ex.getMessage());
            });
}
```

#### 2.3 Implement getActiveQuests() Method

**File**: `QuestServiceImpl.java`

**Action**: Add method implementation:
```java
@Override
@NotNull
public CompletableFuture<List<ActiveQuest>> getActiveQuests(@NotNull final UUID playerId) {
    LOGGER.fine("Getting active quests for player " + playerId);
    
    try {
        // Check if player data is loaded
        if (!progressCache.isLoaded(playerId)) {
            LOGGER.warning("Player data not loaded for " + playerId);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        // Get from cache (instant access)
        List<PlayerQuestProgress> progressList = progressCache.getActiveQuests(playerId);
        
        // Convert to ActiveQuest DTOs
        List<ActiveQuest> activeQuests = progressList.stream()
                .map(this::toActiveQuest)
                .collect(Collectors.toList());
        
        LOGGER.fine("Retrieved " + activeQuests.size() + " active quests for player " + playerId);
        return CompletableFuture.completedFuture(activeQuests);
        
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to get active quests for player " + playerId, e);
        return CompletableFuture.failedFuture(e);
    }
}

private ActiveQuest toActiveQuest(PlayerQuestProgress progress) {
    Quest quest = progress.getQuest();
    int completedTasks = (int) progress.getTaskProgress().stream()
            .filter(PlayerTaskProgress::isCompleted)
            .count();
    int totalTasks = quest.getTasks().size();
    
    return new ActiveQuest(
            quest.getIdentifier(),
            quest.getDisplayName(),
            quest.getDescription(),
            completedTasks,
            totalTasks,
            progress.getStartedAt(),
            progress.getTimeLimit()
    );
}
```

#### 2.4 Implement getProgress() Method

**File**: `QuestServiceImpl.java`

**Action**: Add method implementation:
```java
@Override
@NotNull
public CompletableFuture<Optional<QuestProgress>> getProgress(
        @NotNull final UUID playerId,
        @NotNull final String questIdentifier
) {
    LOGGER.fine("Getting progress for quest " + questIdentifier + " for player " + playerId);
    
    try {
        // Check if player data is loaded
        if (!progressCache.isLoaded(playerId)) {
            LOGGER.warning("Player data not loaded for " + playerId);
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        // Find quest progress in cache
        Optional<PlayerQuestProgress> progressOpt = progressCache
                .getActiveQuests(playerId)
                .stream()
                .filter(p -> p.getQuest().getIdentifier().equals(questIdentifier))
                .findFirst();
        
        if (progressOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        // Convert to QuestProgress DTO
        QuestProgress questProgress = toQuestProgress(progressOpt.get());
        
        return CompletableFuture.completedFuture(Optional.of(questProgress));
        
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to get progress for quest " + questIdentifier, e);
        return CompletableFuture.failedFuture(e);
    }
}

private QuestProgress toQuestProgress(PlayerQuestProgress progress) {
    // Convert task progress
    Map<String, TaskProgress> taskProgressMap = progress.getTaskProgress().stream()
            .collect(Collectors.toMap(
                    tp -> tp.getTask().getIdentifier(),
                    this::toTaskProgress
            ));
    
    return new QuestProgress(
            progress.getQuest().getIdentifier(),
            progress.getStartedAt(),
            progress.isCompleted(),
            progress.getCompletedAt(),
            taskProgressMap
    );
}

private TaskProgress toTaskProgress(PlayerTaskProgress taskProgress) {
    return new TaskProgress(
            taskProgress.getTask().getIdentifier(),
            taskProgress.getCurrentProgress(),
            taskProgress.getTask().getRequiredProgress(),
            taskProgress.isCompleted()
    );
}
```

**Verification**:
```bash
cd RDQ
./gradlew clean rdq-common:build
# Should compile with zero errors
```

---

### 3. Implement Quest Completion (3-4 hours)

**Status**: Not implemented

#### 3.1 Create QuestRewardService

**File**: `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/service/quest/QuestRewardService.java`

**Purpose**: Handle reward granting

**Implementation**:
```java
package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for granting quest rewards to players.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public interface QuestRewardService {
    
    /**
     * Grants all rewards from a list to a player.
     *
     * @param player the player to grant rewards to
     * @param rewards the list of rewards to grant
     * @return a future that completes when all rewards are granted
     */
    @NotNull
    CompletableFuture<Void> grantRewards(
            @NotNull Player player,
            @NotNull List<QuestReward> rewards
    );
    
    /**
     * Grants a single reward to a player.
     *
     * @param player the player to grant the reward to
     * @param reward the reward to grant
     * @return a future that completes when the reward is granted
     */
    @NotNull
    CompletableFuture<Void> grantReward(
            @NotNull Player player,
            @NotNull QuestReward reward
    );
}
```

#### 3.2 Implement processQuestCompletion() in QuestServiceImpl

**File**: `QuestServiceImpl.java`

**Action**: Add method implementation:
```java
@Override
@NotNull
public CompletableFuture<Void> processQuestCompletion(
        @NotNull final UUID playerId,
        @NotNull final String questIdentifier
) {
    LOGGER.info("Processing quest completion for " + questIdentifier + " for player " + playerId);
    
    return getQuest(questIdentifier)
            .thenCompose(questOpt -> {
                if (questOpt.isEmpty()) {
                    LOGGER.warning("Quest not found: " + questIdentifier);
                    return CompletableFuture.completedFuture(null);
                }
                
                Quest quest = questOpt.get();
                
                // Find quest progress
                Optional<PlayerQuestProgress> progressOpt = progressCache
                        .getActiveQuests(playerId)
                        .stream()
                        .filter(p -> p.getQuest().getIdentifier().equals(questIdentifier))
                        .findFirst();
                
                if (progressOpt.isEmpty()) {
                    LOGGER.warning("Quest progress not found for " + questIdentifier);
                    return CompletableFuture.completedFuture(null);
                }
                
                PlayerQuestProgress progress = progressOpt.get();
                
                // Mark as completed
                progress.setCompleted(true);
                progress.setCompletedAt(Instant.now());
                
                // Update cache (marks as dirty)
                progressCache.updateProgress(playerId, progress);
                
                // Grant rewards
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    return rewardService.grantRewards(player, quest.getRewards())
                            .thenCompose(v -> {
                                // Fire completion event
                                QuestCompleteEvent event = new QuestCompleteEvent(player, quest);
                                plugin.getServer().getPluginManager().callEvent(event);
                                
                                // Unlock dependent quests
                                return unlockDependentQuests(playerId, quest);
                            });
                }
                
                return CompletableFuture.completedFuture(null);
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to process quest completion", ex);
                return null;
            });
}

private CompletableFuture<Void> unlockDependentQuests(UUID playerId, Quest completedQuest) {
    // Find quests that have this quest as a prerequisite
    List<Quest> allQuests = questCacheManager.getAllQuests();
    
    for (Quest quest : allQuests) {
        // Check if this quest has completedQuest as a prerequisite
        boolean hasPrerequisite = quest.getRequirements().stream()
                .anyMatch(req -> req instanceof QuestCompletionRequirement &&
                        ((QuestCompletionRequirement) req).getRequiredQuestId().equals(completedQuest.getId()));
        
        if (hasPrerequisite) {
            // Check if player can now start this quest
            canStartQuest(playerId, quest.getIdentifier())
                    .thenAccept(result -> {
                        if (result instanceof QuestStartResult.Success) {
                            // Send notification to player
                            Player player = plugin.getServer().getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                // TODO: Send quest unlocked message
                                LOGGER.info("Quest " + quest.getIdentifier() + " unlocked for player " + playerId);
                            }
                        }
                    });
        }
    }
    
    return CompletableFuture.completedFuture(null);
}
```

**Verification**:
```bash
cd RDQ
./gradlew clean rdq-common:build
```

---

## Testing Plan

### Unit Tests
1. Test QuestServiceImpl methods
2. Test cache operations
3. Test prerequisite validation
4. Test reward granting

### Integration Tests
1. Test complete quest flow (start → progress → complete)
2. Test quest abandonment
3. Test prerequisite unlocking
4. Test reward granting

### Manual Tests
1. Start a quest in-game
2. Complete tasks
3. Complete quest
4. Verify rewards granted
5. Verify dependent quests unlocked
6. Test abandonment

---

## Estimated Time

| Task | Time | Priority |
|------|------|----------|
| I18n Integration | 15 min | HIGH |
| Complete QuestServiceImpl | 2-3 hours | HIGH |
| Implement Quest Completion | 3-4 hours | HIGH |
| Testing | 2-3 hours | HIGH |

**Total**: 7-10 hours

---

## Success Criteria

- ✅ All I18n keys integrated
- ✅ QuestServiceImpl fully implemented
- ✅ Quest completion working
- ✅ Rewards granted correctly
- ✅ Dependent quests unlock
- ✅ Zero compilation errors
- ✅ Zero warnings
- ✅ All tests passing

---

## Next Session Goals

1. Integrate I18n keys (15 min)
2. Complete QuestServiceImpl (2-3 hours)
3. Start quest completion implementation (1-2 hours)

**Total Session Time**: 3-5 hours

