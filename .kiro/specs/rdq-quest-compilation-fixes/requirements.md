# Requirements Document

## Introduction

The RDQ quest system has multiple compilation errors preventing the plugin from building. These errors stem from:
1. Missing methods in entity classes (QuestTask, Quest, QuestCategory)
2. Type constraint violations with ProgressionValidator
3. Missing methods in the RDQ main class
4. Incorrect repository method names

## Glossary

- **Quest System**: The RDQ plugin's quest management functionality
- **Entity**: JPA database entity class
- **Repository**: Data access layer for entities
- **ProgressionValidator**: RPlatform component for validating quest prerequisites
- **IProgressionNode**: Interface that progression nodes must implement
- **QuestTask**: Individual task within a quest
- **Quest**: Collection of tasks players complete
- **QuestCategory**: Grouping of related quests

## Requirements

### Requirement 1: Fix QuestTask Entity Missing Methods

**User Story:** As a developer, I need QuestTask to have proper relationship management methods so that requirement and reward entities can establish bidirectional relationships.

#### Acceptance Criteria

1. WHEN QuestTaskRequirement constructor calls task.addRequirement(this), THE QuestTask entity SHALL have an addRequirement(QuestTaskRequirement) method
2. WHEN QuestTaskRequirement calls task.getRequirements(), THE QuestTask entity SHALL have a getRequirements() method returning List<QuestTaskRequirement>
3. WHEN QuestTaskReward constructor calls task.addReward(this), THE QuestTask entity SHALL have an addReward(QuestTaskReward) method
4. WHEN QuestTaskReward calls task.getRewards(), THE QuestTask entity SHALL have a getRewards() method returning List<QuestTaskReward>
5. THE QuestTask entity SHALL maintain @OneToMany relationships with cascade=ALL and orphanRemoval=true

### Requirement 2: Fix Quest Entity Missing Methods

**User Story:** As a developer, I need Quest to have proper relationship management methods so that requirement and reward entities can establish bidirectional relationships.

#### Acceptance Criteria

1. WHEN QuestRequirement constructor calls quest.addRequirement(this), THE Quest entity SHALL have an addRequirement(QuestRequirement) method
2. WHEN QuestRequirement calls quest.getRequirements(), THE Quest entity SHALL have a getRequirements() method returning List<QuestRequirement>
3. WHEN QuestReward constructor calls quest.addReward(this), THE Quest entity SHALL have an addReward(QuestReward) method
4. WHEN QuestReward calls quest.getRewards(), THE Quest entity SHALL have a getRewards() method returning List<QuestReward>
5. THE Quest entity SHALL maintain @OneToMany relationships with cascade=ALL and orphanRemoval=true

### Requirement 3: Fix QuestCategory Entity Missing Methods

**User Story:** As a developer, I need QuestCategory to have proper relationship management methods so that requirement and reward entities can establish bidirectional relationships.

#### Acceptance Criteria

1. WHEN QuestCategoryRequirement constructor calls category.addRequirement(this), THE QuestCategory entity SHALL have an addRequirement(QuestCategoryRequirement) method
2. WHEN QuestCategoryRequirement calls category.getRequirements(), THE QuestCategory entity SHALL have a getRequirements() method returning List<QuestCategoryRequirement>
3. WHEN QuestCategoryReward constructor calls category.addReward(this), THE QuestCategory entity SHALL have an addReward(QuestCategoryReward) method
4. WHEN QuestCategoryReward calls category.getRewards(), THE QuestCategory entity SHALL have a getRewards() method returning List<QuestCategoryReward>
5. THE QuestCategory entity SHALL maintain @OneToMany relationships with cascade=ALL and orphanRemoval=true

### Requirement 4: Fix Quest IProgressionNode Implementation

**User Story:** As a developer, I need Quest to implement IProgressionNode<Quest> so that it can be used with ProgressionValidator without type constraint violations.

#### Acceptance Criteria

1. THE Quest entity SHALL implement IProgressionNode<Quest> interface
2. THE Quest entity SHALL provide getIdentifier() method returning String
3. THE Quest entity SHALL provide getPreviousNodeIdentifiers() method returning List<String>
4. THE Quest entity SHALL provide getNextNodeIdentifiers() method returning List<String>
5. THE Quest entity SHALL provide isInitialNode() method returning boolean
6. THE ProgressionValidator<Quest> type SHALL compile without type constraint errors

### Requirement 5: Fix RDQ Missing Methods

**User Story:** As a developer, I need RDQ to expose quest system components so that services can access them without compilation errors.

#### Acceptance Criteria

1. WHEN QuestServiceImpl calls plugin.getPlayerQuestProgressCache(), THE RDQ class SHALL have a getPlayerQuestProgressCache() method
2. WHEN QuestServiceImpl calls plugin.getQuestProgressionValidator(), THE RDQ class SHALL have a getQuestProgressionValidator() method
3. THE RDQ class SHALL initialize playerQuestProgressCache field in initializeQuestSystem()
4. THE RDQ class SHALL initialize questProgressionValidator field in initializeQuestSystem()
5. THE methods SHALL be annotated with @Getter or have explicit getter methods

### Requirement 6: Fix RRank IProgressionNode Implementation

**User Story:** As a developer, I need RRank to implement IProgressionNode<RRank> so that it can be used with ProgressionValidator without type constraint violations.

#### Acceptance Criteria

1. THE RRank entity SHALL implement IProgressionNode<RRank> interface
2. THE RRank entity SHALL provide getIdentifier() method returning String
3. THE RRank entity SHALL provide getPreviousNodeIdentifiers() method returning List<String>
4. THE RRank entity SHALL provide getNextNodeIdentifiers() method returning List<String>
5. THE RRank entity SHALL provide isInitialNode() method returning boolean
6. THE ProgressionValidator<RRank> type SHALL compile without type constraint errors

### Requirement 7: Fix Repository Method Names

**User Story:** As a developer, I need repository methods to have consistent naming so that service classes can call them without compilation errors.

#### Acceptance Criteria

1. WHEN RankUpgradeService calls rankRepository.findByIdentifier(), THE RRankRepository SHALL have a findByIdentifier(String) method
2. WHEN RankCompletionTracker calls playerRankRepository.findByPlayerIdAndIsActive(), THE RPlayerRankRepository SHALL have a findByPlayerIdAndIsActive(UUID, boolean) method
3. WHEN RankCompletionTracker calls playerRankRepository.findByPlayerId(), THE RPlayerRankRepository SHALL have a findByPlayerId(UUID) method
4. THE repository methods SHALL return CompletableFuture<Optional<T>> for single results
5. THE repository methods SHALL return CompletableFuture<List<T>> for multiple results

### Requirement 8: Fix QuestAbandonResult API

**User Story:** As a developer, I need QuestAbandonResult to have a simple flat API with i18n keys so that error messages can be properly localized.

#### Acceptance Criteria

1. THE QuestAbandonResult SHALL be a record with success(), messageKey(), and questName() methods
2. THE QuestAbandonResult SHALL provide static factory methods: success(String), notActive(), notFound(String)
3. THE messageKey() method SHALL return i18n translation keys for each failure case
4. THE QuestCommand SHALL use I18n.Builder with result.messageKey() instead of raw sendMessage()
5. THE QuestDetailView SHALL use I18n.Builder with result.messageKey() instead of pattern matching
