# Requirements Document

## Introduction

This feature involves modernizing and refactoring the JExOneblock entity classes to align with current project standards and improve code quality. The system manages a oneblock/island gameplay mechanic with customizable stages/phases that trigger different events and contain editable configurations.

## Glossary

- **OneblockSystem**: The core system managing oneblock islands and their progression
- **Island**: A player-owned area containing a oneblock with associated properties and members
- **OneblockCore**: The central block that players interact with and that evolves through stages
- **Stage**: A phase or evolution level of the oneblock with specific blocks, entities, and items
- **StageProgression**: The system managing advancement through different oneblock stages
- **IslandRegion**: The protected area boundaries around an island
- **PlayerEntity**: The player data entity for the oneblock system
- **StageConfiguration**: The editable configuration defining what appears in each stage
- **RaritySystem**: The system categorizing stage content by rarity levels

## Requirements

### Requirement 1

**User Story:** As a developer, I want modernized entity classes that follow current naming conventions, so that the codebase is consistent and maintainable.

#### Acceptance Criteria

1. WHEN refactoring entity classes, THE OneblockSystem SHALL remove "JE" prefixes from all class names
2. WHEN creating new entity classes, THE OneblockSystem SHALL use "OneblockPlayer" instead of "JEPlayer"
3. WHEN creating new entity classes, THE OneblockSystem SHALL use "Island" instead of "JEIsland"
4. WHEN creating new entity classes, THE OneblockSystem SHALL use "OneblockCore" instead of "JEIslandOneblock"
5. WHEN creating new entity classes, THE OneblockSystem SHALL use "IslandRegion" instead of "JEIslandRegion"

### Requirement 2

**User Story:** As a developer, I want improved entity structure and relationships, so that the code is more maintainable and follows JPA best practices.

#### Acceptance Criteria

1. WHEN designing entity relationships, THE OneblockSystem SHALL use proper JPA annotations for all entity mappings
2. WHEN creating entity classes, THE OneblockSystem SHALL extend BaseEntity for consistent entity behavior
3. WHEN defining entity relationships, THE OneblockSystem SHALL use appropriate cascade types and fetch strategies
4. WHEN implementing entity classes, THE OneblockSystem SHALL include proper equals and hashCode methods
5. WHEN creating embedded entities, THE OneblockSystem SHALL use @Embeddable annotation correctly

### Requirement 3

**User Story:** As a developer, I want modernized stage system entities, so that oneblock stages are properly structured and editable.

#### Acceptance Criteria

1. WHEN creating stage entities, THE OneblockSystem SHALL rename "JEIslandStage" to "OneblockStage"
2. WHEN creating stage content entities, THE OneblockSystem SHALL rename "JEStageBlock" to "StageBlock"
3. WHEN creating stage content entities, THE OneblockSystem SHALL rename "JEStageEntity" to "StageEntity"
4. WHEN creating stage content entities, THE OneblockSystem SHALL rename "JEStageItem" to "StageItem"
5. WHEN implementing stage entities, THE OneblockSystem SHALL maintain proper parent-child relationships

### Requirement 4

**User Story:** As a developer, I want proper database converters for complex data types, so that Bukkit objects are correctly persisted and retrieved.

#### Acceptance Criteria

1. WHEN persisting Location objects, THE OneblockSystem SHALL use LocationConverter for proper serialization
2. WHEN persisting World objects, THE OneblockSystem SHALL use WorldConverter for proper serialization
3. WHEN persisting ItemStack lists, THE OneblockSystem SHALL use ItemStackListConverter for proper serialization
4. WHEN persisting Material lists, THE OneblockSystem SHALL use MaterialListConverter for proper serialization
5. WHEN persisting complex objects, THE OneblockSystem SHALL ensure all converters are properly referenced

### Requirement 5

**User Story:** As a developer, I want clean and consistent entity constructors, so that entities can be properly instantiated with required data.

#### Acceptance Criteria

1. WHEN creating entity constructors, THE OneblockSystem SHALL provide protected no-argument constructors for JPA
2. WHEN creating entity constructors, THE OneblockSystem SHALL provide public constructors with required parameters
3. WHEN initializing embedded objects, THE OneblockSystem SHALL set proper default values in constructors
4. WHEN creating relationships, THE OneblockSystem SHALL properly initialize collections in constructors
5. WHEN validating constructor parameters, THE OneblockSystem SHALL use @NotNull annotations where appropriate

### Requirement 6

**User Story:** As a developer, I want improved stage configuration system, so that stages can be easily customized and managed.

#### Acceptance Criteria

1. WHEN creating stage configurations, THE OneblockSystem SHALL support multiple rarity levels for content
2. WHEN managing stage content, THE OneblockSystem SHALL allow separate configuration of blocks, entities, and items
3. WHEN implementing stage progression, THE OneblockSystem SHALL track experience and level requirements
4. WHEN creating stage templates, THE OneblockSystem SHALL provide base classes for easy extension
5. WHEN validating stage data, THE OneblockSystem SHALL ensure proper data integrity constraints

### Requirement 7

**User Story:** As a developer, I want consistent package structure, so that the codebase follows project organization standards.

#### Acceptance Criteria

1. WHEN organizing entity packages, THE OneblockSystem SHALL use consistent package naming conventions
2. WHEN creating entity classes, THE OneblockSystem SHALL place them in appropriate sub-packages
3. WHEN implementing converters, THE OneblockSystem SHALL place them in a dedicated converter package
4. WHEN creating stage entities, THE OneblockSystem SHALL organize them in a stages sub-package
5. WHEN structuring the codebase, THE OneblockSystem SHALL follow the established project patterns