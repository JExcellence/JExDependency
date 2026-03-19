# Quest Configuration Loader - Implementation Complete

## Problem Solved

Quest categories were not loading when running `/rq quests` because the database was empty. The YAML configuration files existed but there was no mechanism to load them into the database.

## Solution Implemented

Created `QuestConfigLoader.java` following the same pattern as `RankSystemFactory.java`:

### Key Features

1. **Loads from YAML**: Reads `quests/quest-system.yml` configuration file
2. **Creates/Updates Entities**: Uses repository `create()` and `update()` methods
3. **Handles Existing Data**: Checks if categories exist and updates them instead of creating duplicates
4. **Integrated into Startup**: Called during `initializeQuestSystem()` before cache initialization

### Implementation Pattern (from RankSystemFactory)

```java
// Check if exists
Optional<QuestCategory> existing = categoryRepository.findByIdentifier(id).join();

if (existing.isPresent()) {
    // Update existing
    category = existing.get();
    // ... set properties ...
    categoryRepository.update(category);
} else {
    // Create new
    category = new QuestCategory();
    // ... set properties ...
    categoryRepository.create(category);
}
```

## Files Modified

1. **RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/quest/QuestConfigLoader.java**
   - Created new loader class
   - Loads quest categories from YAML
   - Persists to database using repository methods

2. **RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java**
   - Added import for `QuestConfigLoader`
   - Added call to `configLoader.loadConfigurations().join()` before cache initialization

## How It Works

### Startup Sequence

1. Server starts
2. `initializeQuestSystem()` is called
3. **NEW**: `QuestConfigLoader` loads YAML → Database
4. `QuestCacheManager` loads Database → Cache
5. Quest system ready

### Configuration Loading

The loader reads from `quests/quest-system.yml`:

```yaml
categories:
  tutorial:
    displayOrder: 0
    icon:
      material: BOOK
      displayNameKey: "quest.category.tutorial.name"
      descriptionKey: "quest.category.tutorial.description"
    enabled: true
```

And creates `QuestCategory` entities with:
- `identifier`: Category ID (e.g., "tutorial")
- `sortOrder`: From `displayOrder` field
- `enabled`: Whether category is active
- `iconMaterial`: Material for GUI display
- `displayName`: I18n key for name
- `description`: I18n key for description

## Categories Loaded

The system will load 13 quest categories:
1. tutorial
2. combat
3. mining
4. baker
5. hunter
6. miner
7. farmer
8. explorer
9. builder
10. enchanter
11. trader
12. daily
13. challenge

## Testing

To verify the fix works:

1. Start the server
2. Check logs for: "Loading quest configurations from YAML files..."
3. Check logs for: "Loaded X quest categories"
4. Run `/rq quests` command
5. Quest categories should now appear in the GUI

## Compilation Status

✅ **BUILD SUCCESSFUL** - All files compile without errors

## Next Steps

The quest categories will now load from YAML on server startup. However, individual quests within categories still need to be implemented (they are defined in separate YAML files under `quests/definitions/`).

For now, the categories will appear but will be empty. Quest loading can be added later following the same pattern.
