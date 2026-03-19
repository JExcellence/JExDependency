# Quest System Reorganization Plan

## Problem
The quest system currently has everything under `com.raindropcentral.rdq.quest` with many subdirectories, which doesn't follow the pattern used by perk and rank systems.

## Current Structure (WRONG)
```
rdq/quest/
├── cache/              ❌ Should be at rdq/cache/quest/
├── command/            ❌ Should be at rdq/command/quest/
├── config/             ❌ Already exists at rdq/config/quest/ (duplicate!)
├── entity/             ❌ Should be at rdq/database/entity/quest/ (already exists!)
├── event/              ❌ Should be at rdq/event/quest/
├── listener/           ❌ Should be at rdq/listener/ (top-level)
├── model/              ✅ Keep (quest-specific models)
├── progression/        ✅ Keep (quest-specific progression)
├── repository/         ❌ Should be at rdq/database/repository/quest/ (already exists!)
├── requirement/        ❌ Should be at rdq/requirement/ (top-level)
├── reward/             ❌ Should be at rdq/reward/ (top-level)
├── service/            ❌ Should be at rdq/service/quest/
├── view/               ❌ Should be at rdq/view/quest/
└── QuestSystemFactory.java ✅ Keep
```

## Target Structure (CORRECT - Following Perk/Rank Pattern)
```
rdq/
├── cache/
│   └── quest/
│       ├── PlayerQuestProgressCache.java
│       ├── QuestProgressAutoSaveTask.java
│       └── QuestCacheManager.java
├── command/
│   └── quest/
│       └── QuestCommand.java
├── config/
│   └── quest/                    (already exists)
│       ├── QuestSystemSection.java
│       ├── QuestSection.java
│       └── QuestTaskSection.java
├── database/
│   ├── entity/quest/             (already exists)
│   └── repository/quest/         (already exists)
├── event/
│   └── quest/
│       ├── QuestStartEvent.java
│       ├── QuestCompleteEvent.java
│       └── TaskCompleteEvent.java
├── listener/
│   ├── QuestProgressCacheListener.java
│   ├── QuestEventListener.java
│   └── QuestCacheListener.java
├── quest/                        (minimal - quest-specific only)
│   ├── model/
│   │   ├── QuestDifficulty.java
│   │   ├── QuestStartResult.java
│   │   ├── QuestProgress.java
│   │   └── ActiveQuest.java
│   ├── progression/
│   │   └── QuestCompletionTracker.java
│   └── QuestSystemFactory.java
├── requirement/
│   ├── QuestCompletionRequirement.java
│   └── QuestTaskCompletionRequirement.java
├── reward/
│   └── QuestReward.java
├── service/
│   └── quest/
│       ├── QuestService.java
│       ├── QuestServiceImpl.java
│       ├── QuestProgressTracker.java
│       └── QuestProgressTrackerImpl.java
└── view/
    └── quest/
        ├── QuestCategoryView.java
        ├── QuestListView.java
        ├── QuestDetailView.java
        └── QuestAbandonConfirmationView.java
```

## Files to Move

### 1. Cache Files
**From:** `rdq/quest/cache/`
**To:** `rdq/cache/quest/`
- PlayerQuestProgressCache.java
- QuestProgressAutoSaveTask.java
- QuestCacheManager.java
- PlayerQuestCacheManager.java (old - maybe delete)
- QuestCacheListener.java → Move to `rdq/listener/`

### 2. Event Files
**From:** `rdq/quest/event/`
**To:** `rdq/event/quest/`
- QuestStartEvent.java
- QuestCompleteEvent.java
- TaskCompleteEvent.java

### 3. Listener Files
**From:** `rdq/quest/listener/`
**To:** `rdq/listener/`
- QuestProgressCacheListener.java
- QuestEventListener.java
- QuestCacheListener.java

### 4. Service Files
**From:** `rdq/quest/service/`
**To:** `rdq/service/quest/`
- QuestService.java
- QuestServiceImpl.java
- QuestProgressTracker.java
- QuestProgressTrackerImpl.java
- QuestLimitEnforcer.java (maybe delete if not used)

### 5. View Files
**From:** `rdq/quest/view/`
**To:** `rdq/view/quest/`
- QuestCategoryView.java
- QuestListView.java
- QuestDetailView.java
- QuestAbandonConfirmationView.java

### 6. Requirement Files
**From:** `rdq/quest/requirement/`
**To:** `rdq/requirement/`
- QuestCompletionRequirement.java
- QuestTaskCompletionRequirement.java

### 7. Reward Files
**From:** `rdq/quest/reward/`
**To:** `rdq/reward/`
- QuestReward.java

### 8. Keep in rdq/quest/ (Quest-Specific)
- model/ (all files)
- progression/ (all files)
- QuestSystemFactory.java

### 9. Delete/Ignore
- rdq/quest/config/ (duplicate - use rdq/config/quest/)
- rdq/quest/entity/ (duplicate - use rdq/database/entity/quest/)
- rdq/quest/repository/ (duplicate - use rdq/database/repository/quest/)
- rdq/quest/command/ (if empty or outdated)

## Migration Steps

1. Create new package directories
2. Move files to new locations
3. Update package declarations in moved files
4. Update imports in all files that reference moved classes
5. Update RDQ.java initialization
6. Delete old empty directories
7. Test compilation

## Benefits

1. ✅ Consistent with perk/rank system structure
2. ✅ Easier to find files (by type, not by feature)
3. ✅ No duplicate packages (config, entity, repository)
4. ✅ Cleaner separation of concerns
5. ✅ Follows Java package conventions
