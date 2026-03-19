# Quest Definition Loading Issue

## Problem

Quest categories are loading correctly from `quest-system.yml`, but the individual quest definitions from `definitions/{category}/*.yml` files are NOT being loaded. This results in empty categories in the GUI.

## Root Cause

The `QuestConfigLoader` only loads category metadata from `quest-system.yml`. There is NO code to load the individual quest definition files from the `definitions/` subdirectories.

## Current State

### What Works ✅
- Categories load from quest-system.yml
- Categories are persisted to database
- Categories display in GUI (but empty)

### What Doesn't Work ❌
- Individual quest definitions are NOT loaded
- Quest YAML files in `definitions/{category}/*.yml` are ignored
- Categories appear empty in GUI
- No quests available to players

## Quest Definition Files

There are 60+ quest definition files that need to be loaded:

```
definitions/
├── baker/ (6 quests)
├── builder/ (5 quests)
├── challenge/ (1 quest)
├── combat/ (6 quests)
├── daily/ (1 quest)
├── enchanter/ (5 quests)
├── explorer/ (5 quests)
├── farmer/ (5 quests)
├── hunter/ (5 quests)
├── miner/ (5 quests)
├── mining/ (1 quest)
├── trader/ (5 quests)
└── tutorial/ (1 quest)
```

## Required Implementation

The `QuestConfigLoader` needs to be extended to:

1. **Load Categories** (currently working)
   - Read quest-system.yml
   - Create QuestCategory entities
   - Persist to database

2. **Load Quest Definitions** (NOT implemented)
   - Scan `definitions/{category}/` directories
   - Load each `*.yml` file
   - Parse quest structure:
     - Quest metadata (name, description, difficulty)
     - Tasks and their requirements
     - Rewards
     - Prerequisites
   - Create Quest entities
   - Create QuestTask entities
   - Create QuestRequirement entities
   - Create QuestReward entities
   - Link quests to categories
   - Persist all to database

## Example Quest Definition Structure

```yaml
# definitions/combat/combat_basic.yml
quest:
  identifier: "combat_basic"
  displayNameKey: "quest.quests.combat_basic.name"
  descriptionKey: "quest.quests.combat_basic.description"
  category: "combat"
  difficulty: "EASY"
  repeatable: false
  
  tasks:
    kill_zombies:
      displayNameKey: "quest.quests.combat_basic.tasks.kill_zombies.name"
      descriptionKey: "quest.quests.combat_basic.tasks.kill_zombies.description"
      type: "KILL_ENTITY"
      target: "ZOMBIE"
      amount: 10
      
    kill_skeletons:
      displayNameKey: "quest.quests.combat_basic.tasks.kill_skeletons.name"
      descriptionKey: "quest.quests.combat_basic.tasks.kill_skeletons.description"
      type: "KILL_ENTITY"
      target: "SKELETON"
      amount: 5
      
  rewards:
    - type: "EXPERIENCE"
      amount: 100
    - type: "CURRENCY"
      amount: 50
```

## Implementation Steps

### Step 1: Create Quest Definition Loader
Create a method to load individual quest definition files:

```java
private void loadQuestDefinitions(@NotNull final QuestSystemSection systemConfig) {
    for (String categoryId : systemConfig.getCategories().keySet()) {
        File categoryDir = new File(
            plugin.getPlugin().getDataFolder(), 
            "quests/definitions/" + categoryId
        );
        
        if (!categoryDir.exists() || !categoryDir.isDirectory()) {
            continue;
        }
        
        File[] questFiles = categoryDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (questFiles == null) continue;
        
        for (File questFile : questFiles) {
            loadQuestDefinition(questFile, categoryId);
        }
    }
}
```

### Step 2: Parse Quest Definition
```java
private void loadQuestDefinition(File questFile, String categoryId) {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
    ConfigurationSection questSection = config.getConfigurationSection("quest");
    
    // Parse quest metadata
    String identifier = questSection.getString("identifier");
    String displayNameKey = questSection.getString("displayNameKey");
    // ... parse all fields
    
    // Create Quest entity
    Quest quest = new Quest();
    quest.setIdentifier(identifier);
    quest.setDisplayName(displayNameKey);
    // ... set all fields
    
    // Load tasks
    ConfigurationSection tasksSection = questSection.getConfigurationSection("tasks");
    for (String taskId : tasksSection.getKeys(false)) {
        QuestTask task = parseTask(tasksSection.getConfigurationSection(taskId));
        quest.addTask(task);
    }
    
    // Load rewards
    List<?> rewardsList = questSection.getList("rewards");
    for (Object rewardObj : rewardsList) {
        QuestReward reward = parseReward((Map<?, ?>) rewardObj);
        quest.addReward(reward);
    }
    
    // Persist to database
    questRepository.create(quest);
}
```

### Step 3: Update loadConfigurations()
```java
@NotNull
public CompletableFuture<Void> loadConfigurations() {
    return CompletableFuture.runAsync(() -> {
        try {
            final QuestSystemSection systemConfig = loadSystemConfig();
            
            // Load categories
            loadCategories(systemConfig);
            
            // Load quest definitions (NEW)
            loadQuestDefinitions(systemConfig);
            
            LOGGER.info("Quest configurations loaded successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load quest configurations", e);
            throw new RuntimeException("Quest configuration loading failed", e);
        }
    });
}
```

## Alternative: Use ConfigManager for Quest Definitions

Instead of manually parsing YAML, create config section classes:

```java
// QuestDefinitionSection.java
public class QuestDefinitionSection extends AConfigSection {
    private String identifier;
    private String displayNameKey;
    private String descriptionKey;
    private String category;
    private String difficulty;
    private Boolean repeatable;
    private Map<String, QuestTaskSection> tasks;
    private List<RewardSection> rewards;
    // ... getters/setters
}
```

Then load using ConfigKeeper:
```java
ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), "quests/definitions/" + categoryId);
ConfigKeeper<QuestDefinitionSection> cfgKeeper = new ConfigKeeper<>(
    cfgManager, 
    questFile.getName(), 
    QuestDefinitionSection.class
);
QuestDefinitionSection questDef = cfgKeeper.rootSection;
```

## Recommendation

1. **Short-term**: Revert QuestConfigLoader to use Bukkit YamlConfiguration (simpler for now)
2. **Medium-term**: Implement quest definition loading with Bukkit YAML
3. **Long-term**: Migrate to ConfigManager/ConfigKeeper for type safety

## Impact

- **High Priority**: Without this, the quest system is non-functional
- **Complexity**: Medium-High (requires parsing complex nested structures)
- **Time Estimate**: 2-4 hours for full implementation
- **Dependencies**: Requires understanding of Quest entity relationships

## Next Steps

1. Decide on approach (Bukkit YAML vs ConfigManager)
2. Implement quest definition loading
3. Test with existing quest YAML files
4. Verify quests appear in GUI
5. Test quest progression and completion
