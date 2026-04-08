# Requirements Document

## Introduction

The quest configuration loading system needs to be fixed and enhanced to properly load quest definitions from YAML files and persist them to the database with full support for rewards, requirements, task details, and effects.

## Glossary

- **QuestConfigLoader**: The system component responsible for loading quest configurations from YAML files
- **Quest Definition**: A YAML file containing complete quest configuration including tasks, rewards, requirements, and effects
- **Quest Entity**: The database entity representing a quest
- **Task Entity**: The database entity representing a quest task
- **Reward Entity**: The database entity representing quest or task rewards
- **Requirement Entity**: The database entity representing quest or task requirements

## Requirements

### Requirement 1: Fix Quest Definition Loading

**User Story:** As a server administrator, I want quest definitions to load correctly from YAML files, so that quests appear in the quest GUI

#### Acceptance Criteria

1. WHEN the server starts, THE QuestConfigLoader SHALL parse quest definition YAML files with the correct flat structure (no nested `quest:` section)
2. WHEN a quest definition file is loaded, THE QuestConfigLoader SHALL extract the identifier, category, difficulty, icon, and attributes correctly
3. WHEN a quest definition contains tasks, THE QuestConfigLoader SHALL parse each task with its type, target, amount, and icon properties
4. WHEN quest loading completes, THE QuestConfigLoader SHALL log the number of successfully loaded quests
5. IF a quest definition file has parsing errors, THEN THE QuestConfigLoader SHALL log a warning and continue loading other quests

### Requirement 2: Implement Reward Loading

**User Story:** As a server administrator, I want quest rewards to be loaded from YAML files, so that players receive rewards upon quest completion

#### Acceptance Criteria

1. WHEN a quest definition contains a rewards section, THE QuestConfigLoader SHALL parse each reward entry
2. WHEN parsing a reward, THE QuestConfigLoader SHALL extract the type, amount, and type-specific data (currency_id, material, enchantments, etc.)
3. WHEN a reward has display properties, THE QuestConfigLoader SHALL store the display_name_key and lore_key for i18n
4. WHEN rewards are parsed, THE QuestConfigLoader SHALL create QuestReward entities linked to the quest
5. WHEN a task has rewards, THE QuestConfigLoader SHALL create QuestTaskReward entities linked to the task

### Requirement 3: Implement Requirement Loading

**User Story:** As a server administrator, I want quest requirements to be loaded from YAML files, so that quests have proper entry conditions

#### Acceptance Criteria

1. WHEN a quest definition contains a requirements section, THE QuestConfigLoader SHALL parse each requirement entry
2. WHEN parsing a requirement, THE QuestConfigLoader SHALL extract the type, amount, and type-specific data
3. WHEN requirements are parsed, THE QuestConfigLoader SHALL create QuestRequirement entities linked to the quest
4. WHEN a task has requirements, THE QuestConfigLoader SHALL create QuestTaskRequirement entities linked to the task
5. WHEN a quest has prerequisites, THE QuestConfigLoader SHALL store the prerequisite quest identifiers

### Requirement 4: Implement Task Detail Loading

**User Story:** As a server administrator, I want task details to be fully loaded from YAML files, so that task tracking works correctly

#### Acceptance Criteria

1. WHEN a task definition is parsed, THE QuestConfigLoader SHALL extract the task type (KILL_MOBS, CRAFT_ITEMS, etc.)
2. WHEN a task has a target, THE QuestConfigLoader SHALL store the target entity/item/block type
3. WHEN a task has an amount, THE QuestConfigLoader SHALL store the required completion amount
4. WHEN a task has optional flag, THE QuestConfigLoader SHALL set the optional property correctly
5. WHEN a task has difficulty, THE QuestConfigLoader SHALL parse and set the TaskDifficulty enum value

### Requirement 5: Implement Effects and Metadata Loading

**User Story:** As a server administrator, I want quest effects and metadata to be loaded from YAML files, so that quests have visual/audio feedback and proper documentation

#### Acceptance Criteria

1. WHEN a quest definition contains an effects section, THE QuestConfigLoader SHALL parse particle, sound, and title effects
2. WHEN effects are parsed, THE QuestConfigLoader SHALL store them in a JSON format in the quest entity
3. WHEN a quest definition contains metadata, THE QuestConfigLoader SHALL parse author, version, tags, and timestamps
4. WHEN metadata is parsed, THE QuestConfigLoader SHALL store it in a JSON format in the quest entity
5. WHEN failure_conditions are defined, THE QuestConfigLoader SHALL parse and store them

### Requirement 6: Add Comprehensive Logging

**User Story:** As a server administrator, I want detailed logging during quest loading, so that I can debug configuration issues

#### Acceptance Criteria

1. WHEN quest loading starts, THE QuestConfigLoader SHALL log the definitions directory path
2. WHEN processing each category directory, THE QuestConfigLoader SHALL log the category name and file count
3. WHEN loading each quest file, THE QuestConfigLoader SHALL log the quest identifier being processed
4. WHEN parsing fails, THE QuestConfigLoader SHALL log the specific error with file name and line context
5. WHEN loading completes, THE QuestConfigLoader SHALL log summary statistics (categories, quests, tasks loaded)

### Requirement 7: Handle Missing Entity Classes

**User Story:** As a developer, I want the loader to handle missing reward/requirement entity classes gracefully, so that the system doesn't crash

#### Acceptance Criteria

1. WHEN reward entity classes are missing, THE QuestConfigLoader SHALL log a warning and skip reward loading
2. WHEN requirement entity classes are missing, THE QuestConfigLoader SHALL log a warning and skip requirement loading
3. WHEN task requirement/reward classes are missing, THE QuestConfigLoader SHALL log a warning and continue with basic task loading
4. WHEN entity methods are missing, THE QuestConfigLoader SHALL use reflection or fallback to JSON storage
5. IF critical entity classes are missing, THEN THE QuestConfigLoader SHALL throw a clear exception with guidance

### Requirement 8: Support Incremental Updates

**User Story:** As a server administrator, I want to reload quest configurations without restarting the server, so that I can test changes quickly

#### Acceptance Criteria

1. WHEN a quest already exists in the database, THE QuestConfigLoader SHALL update its properties instead of creating a duplicate
2. WHEN updating a quest, THE QuestConfigLoader SHALL clear and replace existing tasks, rewards, and requirements
3. WHEN a quest is removed from YAML files, THE QuestConfigLoader SHALL optionally disable it in the database
4. WHEN reload is triggered, THE QuestConfigLoader SHALL provide a reload() method that re-processes all files
5. WHEN reload completes, THE QuestConfigLoader SHALL invalidate relevant caches
