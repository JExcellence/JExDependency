# Quest Configuration System - Design

## Architecture Overview

The quest configuration system follows the same pattern as the rank system, using ConfigMapper's ConfigSection framework for type-safe YAML parsing and entity generation.

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    QuestSystemFactory                        │
│  - Loads YAML files using ConfigKeeper/ConfigManager        │
│  - Converts ConfigSections → JPA Entities                   │
│  - Validates prerequisites and relationships                 │
│  - Persists to database                                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ├─────────────────────────────────────────┐
                   │                                         │
         ┌─────────▼──────────┐                  ┌──────────▼─────────┐
         │ QuestSystemSection │                  │   ConfigKeeper     │
         │  (Main Config)     │                  │  (File Loading)    │
         └─────────┬──────────┘                  └────────────────────┘
                   │
         ┌─────────▼──────────────┐
         │ QuestCategorySection   │
         │  - identifier          │
         │  - icon                │
         │  - requirements        │
         │  - rewards             │
         │  - quests (nested)     │
         └─────────┬──────────────┘
                   │
         ┌─────────▼──────────────┐
         │   QuestSection         │
         │  - identifier          │
         │  - difficulty          │
         │  - icon                │
         │  - prerequisites       │
         │  - unlocks             │
         │  - requirements        │
         │  - rewards             │
         │  - tasks (nested)      │
         └─────────┬──────────────┘
                   │
         ┌─────────▼──────────────┐
         │  QuestTaskSection      │
         │  - identifier          │
         │  - taskType            │
         │  - targetAmount        │
         │  - requirements        │
         │  - rewards             │
         └────────────────────────┘
```

## Class Structure

### Package: `com.raindropcentral.rdq.config.quest`

#### QuestSystemSection
```java
@CSAlways
public class QuestSystemSection extends AConfigSection {
    private Integer maxActiveQuests;
    private Boolean enableQuestLog;
    private Boolean enableQuestTracking;
    private Boolean enableQuestNotifications;
    private Map<String, QuestCategorySection> categories;
    
    // Getters with defaults
    public Integer getMaxActiveQuests() { return maxActiveQuests == null ? 5 : maxActiveQuests; }
    public Boolean getEnableQuestLog() { return enableQuestLog == null || enableQuestLog; }
    // ...
}
```

#### QuestCategorySection
```java
@CSAlways
public class QuestCategorySection extends AConfigSection {
    private String displayNameKey;
    private String descriptionKey;
    private Integer displayOrder;
    private IconSection icon;
    private Boolean isEnabled;
    private Map<String, BaseRequirementSection> requirements;
    private Map<String, RewardSection> rewards;
    private Map<String, QuestSection> quests;
    
    @CSIgnore
    private String categoryId;
    
    @Override
    public void afterParsing(List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        // Auto-generate i18n keys
        if (categoryId != null) {
            if (displayNameKey == null) {
                displayNameKey = "quest.category." + categoryId + ".name";
            }
            if (descriptionKey == null) {
                descriptionKey = "quest.category." + categoryId + ".description";
            }
            
            // Process nested quests
            if (quests != null) {
                for (Map.Entry<String, QuestSection> entry : quests.entrySet()) {
                    QuestSection quest = entry.getValue();
                    quest.setCategoryId(categoryId);
                    quest.setQuestId(entry.getKey());
                    quest.afterParsing(new ArrayList<>());
                }
            }
        }
    }
}
```

#### QuestSection
```java
@CSAlways
public class QuestSection extends AConfigSection {
    private String displayNameKey;
    private String descriptionKey;
    private IconSection icon;
    private String difficulty;
    private Boolean isEnabled;
    private Integer maxCompletions;
    private Integer cooldownSeconds;
    private Integer timeLimitSeconds;
    private List<String> prerequisites;
    private List<String> unlocks;
    private Map<String, BaseRequirementSection> requirements;
    private Map<String, RewardSection> rewards;
    private Map<String, QuestTaskSection> tasks;
    
    @CSIgnore
    private String categoryId;
    
    @CSIgnore
    private String questId;
    
    @Override
    public void afterParsing(List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        // Auto-generate i18n keys
        if (categoryId != null && questId != null) {
            if (displayNameKey == null) {
                displayNameKey = "quest." + categoryId + "." + questId + ".name";
            }
            if (descriptionKey == null) {
                descriptionKey = "quest." + categoryId + "." + questId + ".description";
            }
            
            // Process nested tasks
            if (tasks != null) {
                for (Map.Entry<String, QuestTaskSection> entry : tasks.entrySet()) {
                    QuestTaskSection task = entry.getValue();
                    task.setCategoryId(categoryId);
                    task.setQuestId(questId);
                    task.setTaskId(entry.getKey());
                    task.afterParsing(new ArrayList<>());
                }
            }
        }
    }
}
```

#### QuestTaskSection
```java
@CSAlways
public class QuestTaskSection extends AConfigSection {
    private String displayNameKey;
    private String descriptionKey;
    private IconSection icon;
    private String taskType;
    private Integer targetAmount;
    private Boolean isOptional;
    private Map<String, BaseRequirementSection> requirements;
    private Map<String, RewardSection> rewards;
    
    @CSIgnore
    private String categoryId;
    
    @CSIgnore
    private String questId;
    
    @CSIgnore
    private String taskId;
    
    @Override
    public void afterParsing(List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        // Auto-generate i18n keys
        if (categoryId != null && questId != null && taskId != null) {
            if (displayNameKey == null) {
                displayNameKey = "quest." + categoryId + "." + questId + ".task." + taskId + ".name";
            }
            if (descriptionKey == null) {
                descriptionKey = "quest." + categoryId + "." + questId + ".task." + taskId + ".description";
            }
        }
    }
}
```

## Factory Processing Flow

### 1. Load Configuration
```java
public void initialize() {
    // Load main config file
    ConfigKeeper<QuestSystemSection> keeper = ConfigManager.createKeeper(
        new File(dataFolder, "quests/quest-system.yml"),
        QuestSystemSection.class,
        new EvaluationEnvironmentBuilder()
    );
    
    questSystemSection = keeper.getConfig();
    
    // Process categories
    for (Map.Entry<String, QuestCategorySection> entry : questSystemSection.getCategories().entrySet()) {
        String categoryId = entry.getKey();
        QuestCategorySection categorySection = entry.getValue();
        categorySection.setCategoryId(categoryId);
        categorySection.afterParsing(new ArrayList<>());
        
        processCategory(categorySection);
    }
}
```

### 2. Convert to Entities
```java
private void processCategory(QuestCategorySection section) {
    // Create or update category entity
    QuestCategory category = categoryRepository.findById(section.getCategoryId())
        .orElse(new QuestCategory(section.getCategoryId()));
    
    // Update properties
    category.setDisplayOrder(section.getDisplayOrder());
    category.setEnabled(section.getEnabled());
    
    // Process icon
    // Process requirements
    // Process rewards
    
    categoryRepository.save(category);
    
    // Process nested quests
    for (Map.Entry<String, QuestSection> entry : section.getQuests().entrySet()) {
        processQuest(category, entry.getValue());
    }
}
```

### 3. Validate Relationships
```java
private void validatePrerequisites() {
    for (Quest quest : allQuests) {
        for (String prereqId : quest.getPrerequisiteIds()) {
            if (!questExists(prereqId)) {
                LOGGER.warning("Quest " + quest.getIdentifier() + 
                    " has invalid prerequisite: " + prereqId);
            }
        }
    }
}
```

## File Organization

### Single File Approach (Recommended)
```
resources/quests/
└── quest-system.yml    # All categories, quests, and tasks in one file
```

**Pros:**
- Single source of truth
- Easy to see all relationships
- Simpler loading logic

**Cons:**
- Large file size
- Harder to navigate

### Multi-File Approach (Alternative)
```
resources/quests/
├── quest-system.yml          # System settings only
└── categories/
    ├── tutorial.yml          # Category with nested quests
    ├── combat.yml
    └── mining.yml
```

**Pros:**
- Organized by category
- Easier to navigate
- Better for team collaboration

**Cons:**
- More complex loading logic
- Need to merge configurations

## Migration Strategy

### Phase 1: Create ConfigSections
1. Create all ConfigSection classes
2. Add proper Javadoc
3. Implement afterParsing() methods
4. Test with sample YAML

### Phase 2: Refactor Factory
1. Update QuestSystemFactory to use ConfigKeeper
2. Implement ConfigSection → Entity conversion
3. Add validation logic
4. Test with existing data

### Phase 3: Migrate YAML Files
1. Convert existing quest YAMLs to new format
2. Verify all data migrates correctly
3. Update documentation

### Phase 4: Cleanup
1. Remove old parsing code
2. Update tests
3. Final validation

## Error Handling

### Configuration Errors
- Invalid YAML syntax → Log error, skip file
- Missing required fields → Use defaults, log warning
- Invalid references → Log error, skip relationship

### Entity Errors
- Duplicate IDs → Update existing entity
- Foreign key violations → Log error, skip
- OptimisticLockException → Retry with fresh entity

## Performance Considerations

1. **Batch Processing**: Process all entities in batches to reduce database round-trips
2. **Caching**: Cache loaded ConfigSections for hot-reload
3. **Lazy Loading**: Don't load task details until needed
4. **Transaction Management**: Use single transaction per category

## Testing Strategy

1. **Unit Tests**: Test each ConfigSection's afterParsing()
2. **Integration Tests**: Test full factory flow
3. **Migration Tests**: Verify old data converts correctly
4. **Performance Tests**: Measure load time with large datasets
