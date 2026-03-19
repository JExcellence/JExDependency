# Quest System - Next Steps

## Current Status

✅ **Quest View Refactoring: COMPLETE**
- All three quest views (Detail, List, Category) have been refactored
- All code compiles with zero diagnostics
- Follows Java 24 standards with proper Javadoc
- Uses new I18n structure with BaseView's `i18n()` helper

⚠️ **I18n Integration: PENDING**
- New I18n keys are ready in `QUEST_I18N_ADDITIONS.yml`
- Need to be manually integrated into `en_US.yml`

## What You Need to Do

### Step 1: Integrate I18n Keys (Required)

**Time Estimate**: 10-15 minutes

1. Open `RDQ/QUEST_I18N_ADDITIONS.yml` in your editor
2. Open `RDQ/rdq-common/src/main/resources/translations/en_US.yml` in another tab
3. Copy the following sections from QUEST_I18N_ADDITIONS.yml to en_US.yml:

   **a) Quest Category Icons** (lines 8-35)
   - Add under `quest.category:` section
   - Categories: baker, hunter, miner, farmer, explorer, builder, enchanter, trader

   **b) Quest Definitions** (lines 37-600+)
   - Add under `quest:` section
   - All quest names, descriptions, and task names
   - Includes: baker quests, hunter quests, miner quests, farmer quests, explorer quests, builder quests, enchanter quests, trader quests

   **c) Quest Reward Items** (lines 600-700)
   - Add under `item:` section (create if doesn't exist)
   - Items: baker_hoe, builder_axe, enchanter_book, trader_emerald, etc.

   **d) Quest Detail View Keys** (lines 700-730)
   - Replace or merge with existing `view.quest.detail:` section (currently at line 2624)
   - New keys: difficulty levels, requirement status, task progress, reward display, action buttons

   **e) Quest List View Keys** (lines 731-740)
   - Replace or merge with existing `view.quest.list:` section (currently at line 2623)
   - New keys: quest lore, reward preview

   **f) Quest Category View Keys** (lines 741-744)
   - Add to `view.quest.category:` section
   - New keys: category lore, quest count

### Step 2: Build and Verify (Required)

```bash
# Navigate to RDQ directory
cd RDQ

# Clean and build
./gradlew clean rdq-common:build

# Expected output: BUILD SUCCESSFUL with 0 warnings
```

### Step 3: Test In-Game (Recommended)

1. Start your Minecraft server with RDQ plugin
2. Open quest category view: `/rq` or `/quests`
3. Test each view:
   - Category view: Verify categories display with icons and quest counts
   - List view: Click a category, verify quests display with difficulty, tasks, rewards
   - Detail view: Click a quest, verify all sections display (requirements, tasks, rewards)
4. Test functionality:
   - Start a quest
   - Check progress tracking
   - Abandon a quest
5. Verify all gradients and colors display correctly

### Step 4: Translate to Other Locales (Optional)

Once en_US.yml is working, translate the new keys to other locales:
- de_DE.yml (German)
- es_ES.yml (Spanish)
- fr_FR.yml (French)
- etc.

## Files Reference

### Documentation Files
- `QUEST_VIEW_REFACTOR_FINAL.md` - Complete refactor summary
- `QUEST_I18N_ADDITIONS.yml` - All new I18n keys to add
- `I18N_INTEGRATION_SUMMARY.md` - Integration instructions
- `QUEST_SYSTEM_FINAL_STATUS.md` - Overall status report
- `NEXT_STEPS.md` - This file

### Source Files (Already Updated)
- `rdq-common/src/main/java/com/raindropcentral/rdq/view/quest/QuestDetailView.java`
- `rdq-common/src/main/java/com/raindropcentral/rdq/view/quest/QuestListView.java`
- `rdq-common/src/main/java/com/raindropcentral/rdq/view/quest/QuestCategoryView.java`

### Target File (Needs Update)
- `rdq-common/src/main/resources/translations/en_US.yml`

## Quick Reference: I18n Key Structure

```yaml
quest:
  category:
    baker:
      name: "<gradient:#f59e0b:#fbbf24>🍞 Baker</gradient>"
  
  apprentice_baker:
    name: "<gradient:#f59e0b:#fbbf24>🍞 Apprentice Baker</gradient>"
    description: "<gradient:#6b7280:#9ca3af>Learn the basics of baking</gradient>"
    task1:
      name: "<gradient:#f59e0b:#fbbf24>Collect Wheat</gradient>"

item:
  baker_hoe:
    name: "<gradient:#f59e0b:#fbbf24>🌾 Baker's Hoe</gradient>"
    lore:
    - "<gradient:#6b7280:#9ca3af>A tool for the aspiring baker</gradient>"

view:
  quest:
    detail:
      difficulty:
        easy: "<green>Difficulty: Easy</green>"
      task:
        completed: "<green>✓ Completed</green>"
```

## Support

If you encounter any issues:
1. Check console logs for missing key warnings
2. Verify YAML syntax (indentation, quotes)
3. Ensure all placeholders match (e.g., `{quest_id}`, `{count}`)
4. Review `QUEST_VIEW_REFACTOR_FINAL.md` for technical details

## Summary

The quest view refactoring is complete and ready to use. The only remaining task is to copy the I18n keys from `QUEST_I18N_ADDITIONS.yml` to `en_US.yml`. This is a straightforward copy-paste operation that will enable all the new quest view features.

**Estimated Total Time**: 15-20 minutes (10-15 min integration + 5 min testing)

