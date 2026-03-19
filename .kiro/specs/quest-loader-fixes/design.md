# Quest Loader Fixes - Design Document

## Overview

This design addresses the critical issues in the QuestConfigLoader that prevent quests from loading correctly, and adds support for rewards, requirements, task details, effects, and metadata. The solution maintains backward compatibility while adding comprehensive YAML parsing capabilities.

## Architecture

### Component Structure

```
QuestConfigLoader
├── loadConfigurations() - Main entry point
├── loadSystemConfig() - Load quest-system.yml
├── loadCategories() - Load category definitions
├── loadQuestDefinitions() - Scan and load quest YAML files
├── loadQuestDefinition() - Parse single quest file
├── parseTask() - Parse task definition
├── parseRewards() - Parse quest rewards
├── parseRequirements() - Parse quest requirements
├── parseTaskRewards() - Parse task rewards
├── parseTaskRequirements() - Parse task requirements
├── parseEffects() - Parse visual/audio effects
├── parseMetadata() - Parse quest metadata
└── parseFailureConditions() - Parse failure conditions
```

### Data Flow

```
YAML Files → QuestConfigLoader → Entity Creation → Database Persistence → Cache Invalidation
```

## Components and Interfaces

### 1. Enhanced QuestConfigLoader

**Purpose**: Load and parse quest configurations with full feature support

**Key Methods**:

```java
public class QuestConfigLoader {
    // Existing methods (fixed)
    private void loadQuestDefinition(File questFile, QuestCategory category);
    private QuestTask parseTask(String taskKey, ConfigurationSection taskSection, Quest quest, int sortOrder);
    
    // New methods for rewards
    private void loadQuestRewards(ConfigurationSection questSection, Quest quest);
    private void loadTaskRewards(ConfigurationSection taskSection, QuestTask task);
    private QuestReward parseQuestReward(ConfigurationSection rewardSection, Quest quest, int index);
    private QuestTaskReward parseTaskReward(ConfigurationSection rewardSection, QuestTask task, int index);
    
    // New methods for requirements
    private void loadQuestRequirements(ConfigurationSection questSection, Quest quest);
    private void loadTaskRequirements(ConfigurationSection taskSection, QuestTask task);
    private QuestRequirement parseQuestRequirement(ConfigurationSection reqSection, Quest quest, int index);
    private QuestTaskRequirement parseTaskRequirement(ConfigurationSection reqSection, QuestTask task, int index);
    
    // New methods for effects and metadata
    private void parseEffects(ConfigurationSection effectsSection, Quest quest);
    private void parseMetadata(ConfigurationSection metadataSection, Quest quest);
    private void parseFailureConditions(ConfigurationSection failureSection, Quest quest);
    
    // Utility methods
    private String serializeToJson(Object data);
    private void logParsingError(String context, Exception e);
}
```

### 2. Reward Parsing Strategy

**Approach**: Parse rewards into entity objects with type-specific data stored in JSON

**Reward Types Supported**:
- CURRENCY: currency_id, amount
- EXPERIENCE: amount
- ITEM: material, amount, display_name_key, lore_key, enchantments, custom_model_data
- PERK: perk_id, duration
- COMMAND: command (with placeholder support)

**Entity Structure**:
```java
QuestReward / QuestTaskReward {
    Long id;
    String type;  // CURRENCY, EXPERIENCE, ITEM, PERK, COMMAND
    Double amount;
    String data;  // JSON for type-specific properties
    boolean enabled;
}
```

### 3. Requirement Parsing Strategy

**Approach**: Parse requirements into entity objects with type-specific data stored in JSON

**Requirement Types Supported**:
- LEVEL: min_level
- CURRENCY: currency_id, amount
- QUEST_COMPLETE: quest_id
- PERMISSION: permission_node
- ITEM: material, amount

**Entity Structure**:
```java
QuestRequirement / QuestTaskRequirement {
    Long id;
    String type;  // LEVEL, CURRENCY, QUEST_COMPLETE, PERMISSION, ITEM
    Double amount;
    String data;  // JSON for type-specific properties
    boolean enabled;
}
```

### 4. Task Parsing Enhancement

**Current Issue**: Tasks are parsed from a list structure, but YAML uses a map structure

**Solution**: Update parseTask to accept ConfigurationSection instead of Map

**Enhanced Task Properties**:
- identifier (key from YAML)
- type (KILL_MOBS, CRAFT_ITEMS, etc.)
- target (entity/item/block type)
- amount (required count)
- optional (boolean flag)
- difficulty (TaskDifficulty enum)
- icon (material, display_name_key, description_key)

### 5. Effects and Metadata Storage

**Approach**: Store complex structures as JSON in entity fields

**Effects Structure**:
```json
{
  "start_particle": "CRIT",
  "complete_particle": "CRIT_MAGIC",
  "start_sound": "ITEM_ARMOR_EQUIP_IRON",
  "complete_sound": "ENTITY_PLAYER_LEVELUP",
  "start_title": {
    "title_key": "quest.combat_basic.start.title",
    "subtitle_key": "quest.combat_basic.start.subtitle",
    "fade_in": 10,
    "stay": 70,
    "fade_out": 20
  },
  "complete_title": { ... }
}
```

**Metadata Structure**:
```json
{
  "author": "RDQ Team",
  "created": "2026-03-14",
  "modified": "2026-03-14",
  "version": "1.0.0",
  "tags": ["combat", "beginner", "tutorial", "chain"]
}
```

## Data Models

### Quest Entity Extensions

```java
public class Quest {
    // Existing fields...
    
    // New fields for effects and metadata
    @Column(columnDefinition = "TEXT")
    private String effectsJson;
    
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    
    @Column(columnDefinition = "TEXT")
    private String failureConditionsJson;
    
    // Chain tracking
    private String chainId;
    private Integer chainOrder;
    
    // Quest type
    private String questType;  // MAIN, SIDE, DAILY, WEEKLY, CHALLENGE
    
    // Visibility
    private boolean hidden;
    private boolean autoStart;
    private boolean showInLog;
}
```

### QuestTask Entity Extensions

```java
public class QuestTask {
    // Existing fields...
    
    // New fields for task details
    private String taskType;  // KILL_MOBS, CRAFT_ITEMS, BREAK_BLOCKS, etc.
    private String target;    // Entity/Item/Block type
    private Integer amount;   // Required count
    
    @Column(columnDefinition = "TEXT")
    private String taskDataJson;  // Additional task-specific data
}
```

## Error Handling

### Parsing Error Strategy

1. **File-Level Errors**: Log warning, skip file, continue with other files
2. **Quest-Level Errors**: Log warning, skip quest, continue with other quests
3. **Task-Level Errors**: Log warning, skip task, continue with other tasks
4. **Reward/Requirement Errors**: Log warning, skip item, continue loading quest
5. **Critical Errors**: Throw exception with clear message and guidance

### Logging Levels

- **INFO**: Successful loads, summary statistics
- **FINE**: Individual quest/task processing
- **WARNING**: Parsing errors, missing files, invalid data
- **SEVERE**: Critical failures that prevent loading

## Testing Strategy

### Unit Tests

1. **Test YAML Parsing**: Verify correct parsing of all YAML structures
2. **Test Reward Parsing**: Verify all reward types parse correctly
3. **Test Requirement Parsing**: Verify all requirement types parse correctly
4. **Test Task Parsing**: Verify task details parse correctly
5. **Test Error Handling**: Verify graceful handling of malformed YAML
6. **Test JSON Serialization**: Verify effects/metadata serialize correctly

### Integration Tests

1. **Test Database Persistence**: Verify entities save correctly
2. **Test Update Logic**: Verify existing quests update correctly
3. **Test Cache Invalidation**: Verify caches refresh after loading
4. **Test Reload**: Verify reload() method works correctly

### Manual Testing

1. **Test Quest GUI**: Verify quests appear in categories
2. **Test Quest Details**: Verify all quest information displays correctly
3. **Test Rewards**: Verify rewards are granted upon completion
4. **Test Requirements**: Verify requirements block quest start correctly
5. **Test Effects**: Verify particles, sounds, titles display correctly

## Implementation Plan

### Phase 1: Fix Core Loading (Priority: CRITICAL)

1. Fix YAML structure parsing (remove nested `quest:` assumption)
2. Fix task parsing to use ConfigurationSection
3. Add comprehensive logging
4. Test basic quest loading

### Phase 2: Add Reward Support (Priority: HIGH)

1. Create reward parsing methods
2. Handle all reward types (CURRENCY, EXPERIENCE, ITEM, PERK, COMMAND)
3. Store reward data in entities
4. Test reward loading

### Phase 3: Add Requirement Support (Priority: HIGH)

1. Create requirement parsing methods
2. Handle all requirement types (LEVEL, CURRENCY, QUEST_COMPLETE, PERMISSION, ITEM)
3. Store requirement data in entities
4. Test requirement loading

### Phase 4: Add Task Details (Priority: HIGH)

1. Parse task type, target, amount
2. Store task-specific data in JSON
3. Test task detail loading

### Phase 5: Add Effects and Metadata (Priority: MEDIUM)

1. Parse effects section
2. Parse metadata section
3. Parse failure conditions
4. Store as JSON in quest entity
5. Test effects/metadata loading

### Phase 6: Add Reload Support (Priority: LOW)

1. Implement reload() method
2. Add cache invalidation
3. Test incremental updates

## Performance Considerations

1. **Async Loading**: All loading happens asynchronously to avoid blocking server startup
2. **Batch Database Operations**: Use batch inserts/updates where possible
3. **Lazy JSON Parsing**: Only parse JSON when needed (not during loading)
4. **Cache Warming**: Invalidate caches after loading to force refresh

## Security Considerations

1. **YAML Injection**: Use safe YAML loading (already handled by Bukkit's YamlConfiguration)
2. **Command Injection**: Sanitize command rewards before execution
3. **File Path Traversal**: Validate file paths stay within definitions directory
4. **Resource Limits**: Limit number of quests/tasks/rewards to prevent memory exhaustion

## Migration Strategy

### Backward Compatibility

- Existing quest entities remain unchanged
- New fields are nullable/optional
- Old YAML format still works (if it ever did)

### Database Migration

- Add new columns to Quest and QuestTask tables
- Add QuestReward, QuestRequirement, QuestTaskReward, QuestTaskRequirement tables
- Migration handled by Hibernate auto-update

## Related Documentation

- Quest Entity Structure: `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/database/entity/quest/`
- Quest YAML Examples: `RDQ/rdq-common/src/main/resources/quests/definitions/`
- JEHibernate Integration: `.kiro/steering/jehibernate-integration.md`
