# Configuration Loading and Rank Path Fixes

## Issues Identified and Fixed

### 1. Quest Configuration File Not Found ✅ FIXED
**Problem**: Quest system was trying to load `quest-system.yml` using Bukkit's `YamlConfiguration` directly, but this doesn't follow the proper ConfigManager/ConfigKeeper pattern that the rank system uses.

**Solution Implemented**:
Updated `QuestConfigLoader.java` to use ConfigManager/ConfigKeeper pattern:

```java
/**
 * Loads the quest system configuration using ConfigManager/ConfigKeeper.
 */
@NotNull
private QuestSystemSection loadSystemConfig() {
    try {
        ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), FILE_PATH);
        ConfigKeeper<QuestSystemSection> cfgKeeper = new ConfigKeeper<>(
            cfgManager, 
            FILE_NAME, 
            QuestSystemSection.class
        );
        return cfgKeeper.rootSection;
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error loading quest system config, using defaults", e);
        return new QuestSystemSection(new EvaluationEnvironmentBuilder());
    }
}
```

**Changes Made**:
1. ✅ Removed Bukkit's `YamlConfiguration` import
2. ✅ Added ConfigManager and ConfigKeeper imports
3. ✅ Created `loadSystemConfig()` method following RankSystemFactory pattern
4. ✅ Updated `loadCategories()` to accept `QuestSystemSection` instead of `YamlConfiguration`
5. ✅ Updated category loading to use `QuestCategorySection` objects
6. ✅ Removed manual file path handling - ConfigManager handles this automatically

**Benefits**:
- Consistent with rank system configuration loading
- Automatic file creation and default handling
- Better error handling and logging
- Type-safe configuration access

### 2. Rank Path List Not Opening ⚠️ NEEDS INVESTIGATION
**Problem**: When clicking on rank trees, the RankPathOverview is not opening properly.

**Analysis**:
- ✅ RankPathOverview IS registered in ViewFrame (line 362 in RDQ.java)
- ✅ Click handler in `RankTreeOverviewView` calls `openRankPathOverview()` correctly
- ✅ All required state parameters are passed: plugin, player, rankTree, previewMode

**Possible Causes**:
1. Exception during view initialization (check console logs)
2. State values might be null when passed
3. RankPathOverview.onOpen() might be throwing an exception
4. Player might not have permission to open the view

**Debugging Steps Needed**:
1. Check server console for exceptions when clicking rank trees
2. Add debug logging to RankPathOverview.onOpen()
3. Verify that rdqPlayer state is not null
4. Check if rankTree is properly initialized
5. Test with both left-click (preview) and right-click (progression)

**Recommended Debug Logging**:
```java
// In RankPathOverview.onOpen()
@Override
public void onOpen(OpenContext context) {
    super.onOpen(context);
    LOGGER.info("=== RankPathOverview Opening ===");
    LOGGER.info("Player: " + context.getPlayer().getName());
    
    try {
        RRankTree tree = selectedRankTree.get(context);
        LOGGER.info("RankTree: " + (tree != null ? tree.getIdentifier() : "NULL"));
    } catch (Exception e) {
        LOGGER.severe("Failed to get rankTree: " + e.getMessage());
    }
    
    try {
        Boolean preview = isPreviewMode.get(context);
        LOGGER.info("Preview mode: " + preview);
    } catch (Exception e) {
        LOGGER.severe("Failed to get preview mode: " + e.getMessage());
    }
    
    try {
        RDQPlayer player = currentPlayer.get(context);
        LOGGER.info("RDQPlayer: " + (player != null ? player.getUniqueId() : "NULL"));
    } catch (Exception e) {
        LOGGER.severe("Failed to get RDQPlayer: " + e.getMessage());
    }
}
```

## Testing Checklist

### Quest Configuration Loading
- [x] QuestConfigLoader updated to use ConfigManager/ConfigKeeper
- [x] No compilation errors
- [ ] Test: Server starts without "Quest configuration file not found" warning
- [ ] Test: Quest categories load correctly from YAML
- [ ] Test: Quest categories are persisted to database
- [ ] Test: Quest category view displays all categories

### Rank Path Opening
- [x] RankPathOverview is registered in ViewFrame
- [ ] Test: Click on rank tree in RankTreeOverviewView
- [ ] Test: Check console for exceptions
- [ ] Test: Left-click opens preview mode
- [ ] Test: Right-click opens progression mode
- [ ] Test: All state values are properly initialized
- [ ] Test: View renders correctly

## Next Steps

1. **Test Quest Configuration Loading**:
   - Restart server and check logs
   - Verify no warnings about missing quest-system.yml
   - Check that categories are loaded into database

2. **Debug Rank Path Opening**:
   - Add debug logging to RankPathOverview.onOpen()
   - Test clicking on rank trees
   - Check console for exceptions or error messages
   - Verify state initialization

3. **If Rank Path Still Not Opening**:
   - Check if player has required permissions
   - Verify rankTree entity is fully loaded (not lazy-loaded proxy)
   - Check if view size/layout is causing issues
   - Test with a fresh player (no existing rank data)

## Files Modified

1. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/quest/QuestConfigLoader.java`
   - Updated imports to use ConfigManager/ConfigKeeper
   - Added `loadSystemConfig()` method
   - Updated `loadCategories()` to use QuestSystemSection
   - Removed manual file handling

## Files to Check

1. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/view/ranks/RankPathOverview.java`
   - Add debug logging to onOpen() method
   - Check state initialization

2. Server console logs
   - Look for exceptions when clicking rank trees
   - Check for any warnings or errors related to views
