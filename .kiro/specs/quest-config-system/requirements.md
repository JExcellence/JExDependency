# Quest Configuration System - Requirements

## Overview
Create a clean, organized configuration system for the quest system using ConfigSection, ConfigManager, and ConfigKeeper, following the same pattern as the rank system.

## Goals
1. Replace the current ad-hoc YAML loading with a structured ConfigSection-based approach
2. Enable clean, type-safe configuration loading and validation
3. Support hierarchical quest definitions (Categories → Quests → Tasks)
4. Integrate with the existing entity system
5. Provide automatic i18n key generation
6. Support requirements and rewards at all levels

## Current State
- Quest entities are defined and working
- Basic YAML files exist but use manual parsing
- QuestSystemFactory exists but needs refactoring
- No ConfigSection classes for quests yet

## Required Components

### 1. Configuration Sections
Following the rank system pattern, we need:

#### QuestCategorySection
- `identifier`: String (category ID)
- `displayNameKey`: String (i18n key, auto-generated)
- `descriptionKey`: String (i18n key, auto-generated)
- `displayOrder`: Integer
- `icon`: IconSection
- `isEnabled`: Boolean
- `requirements`: Map<String, BaseRequirementSection>
- `rewards`: Map<String, RewardSection>
- `quests`: Map<String, QuestSection> (nested)

#### QuestSection
- `identifier`: String (quest ID)
- `displayNameKey`: String (i18n key, auto-generated)
- `descriptionKey`: String (i18n key, auto-generated)
- `icon`: IconSection
- `difficulty`: String (EASY, MEDIUM, HARD, EXPERT, MASTER)
- `isEnabled`: Boolean
- `maxCompletions`: Integer (null = unlimited)
- `cooldownSeconds`: Integer (null = no cooldown)
- `timeLimitSeconds`: Integer (null = no time limit)
- `prerequisites`: List<String> (quest IDs)
- `unlocks`: List<String> (quest IDs)
- `requirements`: Map<String, BaseRequirementSection>
- `rewards`: Map<String, RewardSection>
- `tasks`: Map<String, QuestTaskSection> (nested)

#### QuestTaskSection
- `identifier`: String (task ID)
- `displayNameKey`: String (i18n key, auto-generated)
- `descriptionKey`: String (i18n key, auto-generated)
- `icon`: IconSection
- `taskType`: String (KILL, COLLECT, INTERACT, LOCATION, etc.)
- `targetAmount`: Integer
- `isOptional`: Boolean
- `requirements`: Map<String, BaseRequirementSection>
- `rewards`: Map<String, RewardSection>

#### QuestSystemSection
- `maxActiveQuests`: Integer
- `enableQuestLog`: Boolean
- `enableQuestTracking`: Boolean
- `enableQuestNotifications`: Boolean
- `categories`: Map<String, QuestCategorySection>

### 2. Factory Refactoring
Update `QuestSystemFactory` to:
- Use ConfigKeeper/ConfigManager for file loading
- Process QuestSystemSection → Entity conversion
- Handle nested structures (Category → Quest → Task)
- Validate prerequisites and unlocks
- Create/update database entities
- Support hot-reload

### 3. File Structure
```
resources/quests/
├── quest-system.yml          # Main system config
└── categories/
    ├── tutorial.yml          # Category with nested quests
    ├── combat.yml
    ├── mining.yml
    └── challenge.yml
```

### 4. YAML Format Example
```yaml
# quest-system.yml
maxActiveQuests: 5
enableQuestLog: true
enableQuestTracking: true
enableQuestNotifications: true

categories:
  tutorial:
    displayOrder: 1
    icon:
      type: BOOK
    quests:
      welcome_to_server:
        difficulty: EASY
        icon:
          type: COMPASS
        tasks:
          talk_to_npc:
            taskType: INTERACT
            targetAmount: 1
```

## Success Criteria
- [ ] All ConfigSection classes created with proper Javadoc
- [ ] QuestSystemFactory refactored to use ConfigSections
- [ ] Existing quest YAML files migrated to new format
- [ ] System loads and saves to database correctly
- [ ] Auto-generated i18n keys work
- [ ] Requirements and rewards integrate properly
- [ ] Zero compilation warnings
- [ ] Clean, maintainable code following rank system pattern

## Dependencies
- ConfigMapper library (already used by rank system)
- Existing entity classes
- RequirementFactory and RewardFactory
- IconSection (reuse from utility package)

## Timeline
1. Create ConfigSection classes (1-2 hours)
2. Refactor QuestSystemFactory (2-3 hours)
3. Migrate YAML files (1 hour)
4. Testing and validation (1 hour)

Total estimated time: 5-7 hours
