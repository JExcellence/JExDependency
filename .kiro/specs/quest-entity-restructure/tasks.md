# Implementation Plan

This implementation plan breaks down the quest entity restructure into discrete, manageable coding tasks. Each task builds incrementally on previous work, with all context documents (requirements, design) available during implementation.

## Task Organization

Tasks are organized into logical groups:
1. Enums and supporting types
2. Core entity structure
3. Requirement and reward entities
4. Progress tracking entities
5. Repository layer
6. Migration utilities
7. Service layer integration
8. Testing

- [x] 1. Create enhanced difficulty enums




- [ ] 1.1 Create QuestDifficulty enum with five levels
  - Implement TRIVIAL, EASY, NORMAL, HARD, EXTREME levels
  - Add level numbers (1-5), translation keys, and color codes
  - Include getters for level, translationKey, and color


  - _Requirements: 14_

- [x] 1.2 Create TaskDifficulty enum with five levels





  - Implement TRIVIAL, EASY, MEDIUM, HARD, EXTREME levels
  - Add level numbers (1-5), translation keys, and color codes
  - Include getters for level, translationKey, and color
  - _Requirements: 15_


- [ ] 2. Create core Quest entity
- [ ] 2.1 Create Quest entity class structure
  - Extend BaseEntity and implement IProgressionNode<Quest>
  - Add @Entity and @Table annotations with rdq_quest table name
  - Add indexes for identifier, category_id, enabled, difficulty
  - Add serialVersionUID field
  - _Requirements: 1, 16, 17_


- [ ] 2.2 Add Quest core fields
  - Add identifier field (String, unique, length 64)
  - Add category field (ManyToOne to QuestCategory, LAZY)
  - Add icon field (IconSection with converter, LONGTEXT)
  - Add difficulty field (QuestDifficulty enum, STRING)

  - Add enabled field (boolean, default true)
  - _Requirements: 1_

- [ ] 2.3 Add Quest repeatability fields
  - Add repeatable field (boolean, default false)


  - Add maxCompletions field (int, default 0)
  - Add cooldownSeconds field (long, default 0)
  - Add timeLimitSeconds field (long, default 0)
  - _Requirements: 1_


- [ ] 2.4 Add Quest progression fields
  - Add previousQuestIds ElementCollection (EAGER, with CollectionTable)
  - Add nextQuestIds ElementCollection (EAGER, with CollectionTable)
  - Add OrderColumn annotations for both collections
  - _Requirements: 1_


- [ ] 2.5 Add Quest child entity relationships
  - Add tasks OneToMany relationship (LAZY, CASCADE ALL, orphanRemoval)
  - Add requirements OneToMany relationship (EAGER, CASCADE ALL, orphanRemoval)
  - Add rewards OneToMany relationship (EAGER, CASCADE ALL, orphanRemoval)


  - Add @OrderBy for tasks by orderIndex
  - _Requirements: 1, 22, 23_

- [ ] 2.6 Implement Quest constructors
  - Add protected no-args constructor for JPA

  - Add public constructor with identifier, category, icon, difficulty
  - Initialize collections in constructors
  - _Requirements: 1_

- [x] 2.7 Implement Quest getters and setters

  - Add getters for all fields
  - Add setters for mutable fields
  - Add convenience methods (isRepeatable, hasTimeLimit, getTimeLimit, getCooldown)

  - _Requirements: 1_




- [ ] 2.8 Implement Quest relationship management methods
  - Add addTask/removeTask methods managing bidirectional relationships
  - Add addRequirement/removeRequirement methods
  - Add addReward/removeReward methods
  - Add getTasksOrdered, getRequirementsOrdered, getRewardsOrdered methods


  - _Requirements: 1, 18, 24_

- [ ] 2.9 Implement Quest IProgressionNode methods
  - Implement getIdentifier returning identifier field
  - Implement getPreviousNodeIdentifiers returning previousQuestIds
  - Implement getNextNodeIdentifiers returning nextQuestIds


  - _Requirements: 1, 19_

- [ ] 2.10 Implement Quest equals, hashCode, and toString
  - Implement equals based on ID if present, otherwise identifier

  - Implement hashCode based on ID if present, otherwise identifier
  - Implement toString with id, identifier, difficulty, repeatable, enabled
  - _Requirements: 27, 28_

- [ ] 3. Create QuestCategory entity
- [x] 3.1 Create QuestCategory entity class structure


  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_category table name
  - Add indexes for identifier, enabled, display_order
  - Add serialVersionUID field

  - _Requirements: 2, 16, 17_

- [x] 3.2 Add QuestCategory fields


  - Add identifier field (String, unique, length 64)
  - Add icon field (IconSection with converter, LONGTEXT)



  - Add displayOrder field (int, default 0)
  - Add enabled field (boolean, default true)
  - Add previousCategoryIds ElementCollection (EAGER)
  - _Requirements: 2_

- [x] 3.3 Add QuestCategory child entity relationships

  - Add quests OneToMany relationship (LAZY, CASCADE ALL, orphanRemoval)
  - Add requirements OneToMany relationship (EAGER, CASCADE ALL, orphanRemoval)
  - Add rewards OneToMany relationship (EAGER, CASCADE ALL, orphanRemoval)
  - _Requirements: 2, 22, 23_

- [ ] 3.4 Implement QuestCategory constructors and methods
  - Add protected no-args constructor for JPA
  - Add public constructor with identifier and icon


  - Add factory method create(identifier, icon)
  - Add getters and setters
  - _Requirements: 2_



- [ ] 3.5 Implement QuestCategory relationship management
  - Add addQuest/removeQuest methods
  - Add addRequirement/removeRequirement methods
  - Add addReward/removeReward methods
  - _Requirements: 2, 18, 24_


- [ ] 3.6 Implement QuestCategory equals, hashCode, and toString
  - Implement equals based on ID if present, otherwise identifier
  - Implement hashCode based on ID if present, otherwise identifier





  - Implement toString with id, identifier, enabled
  - _Requirements: 27, 28_


- [ ] 4. Create QuestTask entity
- [ ] 4.1 Create QuestTask entity class structure
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_task table name

  - Add indexes for quest_id, task_identifier, order_index
  - Add serialVersionUID field
  - _Requirements: 3, 16, 17_

- [ ] 4.2 Add QuestTask fields
  - Add quest field (ManyToOne to Quest, LAZY)
  - Add taskIdentifier field (String, length 64)

  - Add icon field (IconSection with converter, LONGTEXT)
  - Add orderIndex field (int, default 0)
  - Add difficulty field (TaskDifficulty enum, STRING)
  - Add sequential field (boolean, default false)


  - _Requirements: 3_

- [ ] 4.3 Add QuestTask child entity relationships
  - Add requirements OneToMany relationship (EAGER, CASCADE ALL, orphanRemoval)
  - Add rewards OneToMany relationship (EAGER, CASCADE ALL, orphanRemoval)
  - Add unique constraint on (quest_id, task_identifier)
  - _Requirements: 3, 22, 23_



- [ ] 4.4 Implement QuestTask constructors and methods
  - Add protected no-args constructor for JPA
  - Add public constructor with quest, taskIdentifier, icon, orderIndex
  - Add getters and setters





  - Add relationship management methods (addRequirement, addReward, etc.)
  - _Requirements: 3, 18, 24_

- [ ] 4.5 Implement QuestTask equals, hashCode, and toString
  - Implement equals based on ID if present, otherwise quest and taskIdentifier
  - Implement hashCode based on ID if present, otherwise quest and taskIdentifier
  - Implement toString with id, taskIdentifier, orderIndex, difficulty, sequential
  - _Requirements: 27, 28_


- [ ] 5. Create requirement entities
- [ ] 5.1 Create QuestRequirement entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_requirement table name
  - Add quest field (ManyToOne to Quest, EAGER)
  - Add requirement field (ManyToOne to BaseRequirement, EAGER)
  - Add icon field (IconSection with converter, LONGTEXT)

  - Add displayOrder field (int, default 0)
  - Add Version field for optimistic locking
  - _Requirements: 4, 16, 21_



- [ ] 5.2 Implement QuestRequirement constructors and methods
  - Add protected no-args constructor for JPA
  - Add public constructor with quest, requirement, icon
  - Add getters and setters
  - Add convenience methods: isMet, calculateProgress, consume
  - Add setQuest method managing bidirectional relationship
  - _Requirements: 4, 18_



- [ ] 5.3 Implement QuestRequirement equals and hashCode
  - Implement equals based on ID if present, otherwise requirement, quest, displayOrder
  - Implement hashCode based on ID if present, otherwise requirement, quest, displayOrder
  - _Requirements: 27_



- [ ] 5.4 Create QuestTaskRequirement entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_task_requirement table name
  - Add task field (ManyToOne to QuestTask, EAGER)
  - Add requirement field (ManyToOne to BaseRequirement, EAGER)
  - Add icon, displayOrder fields
  - Add constructors, getters, setters, convenience methods

  - _Requirements: 6, 16_

- [ ] 5.5 Create QuestCategoryRequirement entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_category_requirement table name
  - Add category field (ManyToOne to QuestCategory, EAGER)
  - Add requirement field (ManyToOne to BaseRequirement, EAGER)
  - Add icon, displayOrder fields
  - Add constructors, getters, setters, convenience methods

  - _Requirements: 8, 16_

- [x] 6. Create reward entities

- [ ] 6.1 Create QuestReward entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_reward table name
  - Add quest field (ManyToOne to Quest, EAGER)
  - Add reward field (ManyToOne to BaseReward, EAGER)
  - Add icon field (IconSection with converter, LONGTEXT)

  - Add displayOrder field (int, default 0)
  - Add autoGrant field (boolean, default true)
  - Add Version field for optimistic locking
  - _Requirements: 5, 16, 21_


- [ ] 6.2 Implement QuestReward constructors and methods
  - Add protected no-args constructor for JPA
  - Add public constructor with quest, reward, icon
  - Add getters and setters
  - Add convenience methods: grant, getEstimatedValue
  - Add setQuest method managing bidirectional relationship
  - _Requirements: 5, 18_

- [ ] 6.3 Implement QuestReward equals and hashCode
  - Implement equals based on ID if present, otherwise reward, quest, displayOrder
  - Implement hashCode based on ID if present, otherwise reward, quest, displayOrder

  - _Requirements: 27_

- [ ] 6.4 Create QuestTaskReward entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_task_reward table name
  - Add task field (ManyToOne to QuestTask, EAGER)
  - Add reward field (ManyToOne to BaseReward, EAGER)
  - Add icon, displayOrder, autoGrant fields
  - Add constructors, getters, setters, convenience methods
  - _Requirements: 7, 16_

- [ ] 6.5 Create QuestCategoryReward entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_category_reward table name
  - Add category field (ManyToOne to QuestCategory, EAGER)









  - Add reward field (ManyToOne to BaseReward, EAGER)
  - Add icon, displayOrder, autoGrant fields
  - Add constructors, getters, setters, convenience methods
  - _Requirements: 9, 16_


- [ ] 7. Create player progress entities
- [ ] 7.1 Create PlayerQuestProgress entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_player_quest_progress table name
  - Add unique constraint on (player_id, quest_id)
  - Add indexes for player_id, quest_id, completed

  - Add serialVersionUID field
  - _Requirements: 10, 16, 17_

- [ ] 7.2 Add PlayerQuestProgress fields
  - Add player field (ManyToOne to RDQPlayer, EAGER)
  - Add quest field (ManyToOne to Quest, EAGER)
  - Add startedAt field (LocalDateTime, non-null)
  - Add expiresAt field (LocalDateTime, nullable)
  - Add completed field (boolean, default false)
  - Add completedAt field (LocalDateTime, nullable)
  - Add Version field for optimistic locking
  - _Requirements: 10, 21_

- [ ] 7.3 Add PlayerQuestProgress child relationships
  - Add taskProgress OneToMany relationship (LAZY, CASCADE ALL, orphanRemoval)
  - _Requirements: 10, 22_

- [ ] 7.4 Implement PlayerQuestProgress constructors and methods
  - Add protected no-args constructor for JPA
  - Add public constructor with player and quest
  - Add getters and setters
  - Add convenience methods: isExpired, getRemainingTime, getElapsedTime
  - _Requirements: 10, 25_

- [ ] 7.5 Implement PlayerQuestProgress equals, hashCode, and toString
  - Implement equals based on ID if present, otherwise player and quest
  - Implement hashCode based on ID if present, otherwise player and quest
  - Implement toString with id, player name, quest identifier, completion status
  - _Requirements: 27, 28_

- [ ] 7.6 Create PlayerTaskProgress entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_player_task_progress table name
  - Add unique constraint on (quest_progress_id, task_id)
  - Add questProgress field (ManyToOne to PlayerQuestProgress, EAGER)
  - Add task field (ManyToOne to QuestTask, EAGER)
  - Add progress field (double, default 0.0)
  - Add completed field (boolean, default false)
  - Add completedAt field (LocalDateTime, nullable)
  - Add Version field for optimistic locking
  - _Requirements: 11, 16, 21_

- [ ] 7.7 Implement PlayerTaskProgress methods
  - Add constructors, getters, setters
  - Add incrementProgress method with capping at 1.0
  - Add resetProgress method
  - Add getProgressPercentage method (0-100)
  - Add isCompleted method checking progress >= 1.0
  - Add equals, hashCode, toString
  - _Requirements: 11, 25, 27, 28_

- [ ] 7.8 Create PlayerTaskRequirementProgress entity
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_player_task_requirement_progress table name
  - Add unique constraint on (task_progress_id, requirement_id)
  - Add taskProgress field (ManyToOne to PlayerTaskProgress, EAGER)
  - Add requirement field (ManyToOne to QuestTaskRequirement, EAGER)
  - Add progress field (double, default 0.0)
  - Add constructors, getters, setters, convenience methods
  - _Requirements: 12, 16_


- [ ] 8. Create QuestCompletionHistory entity
- [ ] 8.1 Create QuestCompletionHistory entity structure
  - Extend BaseEntity
  - Add @Entity and @Table annotations with rdq_quest_completion_history table name
  - Add indexes for (player_id, quest_id) and completed_at
  - Add serialVersionUID field
  - _Requirements: 13, 16, 17_

- [ ] 8.2 Add QuestCompletionHistory fields
  - Add player field (ManyToOne to RDQPlayer, EAGER)
  - Add quest field (ManyToOne to Quest, EAGER)
  - Add completedAt field (LocalDateTime, non-null)
  - Add completionCount field (int, non-null)
  - Add timeTakenSeconds field (long, non-null)
  - _Requirements: 13_

- [ ] 8.3 Implement QuestCompletionHistory methods
  - Add constructors with all required fields
  - Add getters and setters
  - Add convenience methods: canRepeat, getCooldownRemaining
  - Add equals, hashCode, toString
  - _Requirements: 13, 25, 27, 28_

- [-] 9. Create repository classes



- [ ] 9.1 Create QuestRepository
  - Extend BaseRepository<Quest>
  - Add findByIdentifier method
  - Add findByCategory method with pagination
  - Add findByDifficulty method
  - Add findByEnabled method
  - Add findByCategoryWithDetails method (batch load requirements/rewards)
  - _Requirements: 29, 33_

- [ ] 9.2 Create QuestCategoryRepository
  - Extend BaseRepository<QuestCategory>
  - Add findByIdentifier method
  - Add findByEnabled method
  - Add findAllOrdered method (sorted by displayOrder)
  - _Requirements: 29_

- [ ] 9.3 Create QuestTaskRepository
  - Extend BaseRepository<QuestTask>
  - Add findByQuest method
  - Add findByQuestAndIdentifier method
  - _Requirements: 29_

- [ ] 9.4 Create requirement repositories
  - Create QuestRequirementRepository extending BaseRepository
  - Create QuestTaskRequirementRepository extending BaseRepository
  - Create QuestCategoryRequirementRepository extending BaseRepository
  - Add findByQuest/findByTask/findByCategory methods
  - _Requirements: 29_

- [ ] 9.5 Create reward repositories
  - Create QuestRewardRepository extending BaseRepository
  - Create QuestTaskRewardRepository extending BaseRepository
  - Create QuestCategoryRewardRepository extending BaseRepository
  - Add findByQuest/findByTask/findByCategory methods
  - _Requirements: 29_

- [ ] 9.6 Create PlayerQuestProgressRepository
  - Extend BaseRepository<PlayerQuestProgress>
  - Add findByPlayer method
  - Add findByPlayerAndQuest method
  - Add findActiveQuestsWithProgress method (batch load task progress)
  - Add findByPlayerAndCompleted method
  - _Requirements: 29, 33_

- [ ] 9.7 Create PlayerTaskProgressRepository
  - Extend BaseRepository<PlayerTaskProgress>
  - Add findByQuestProgress method
  - Add findByQuestProgressAndTask method
  - _Requirements: 29_

- [ ] 9.8 Create QuestCompletionHistoryRepository
  - Extend BaseRepository<QuestCompletionHistory>
  - Add findByPlayer method
  - Add findByPlayerAndQuest method
  - Add countCompletionsByPlayerAndQuest method
  - Add findLatestCompletionByPlayerAndQuest method
  - Add findCompletionsBetweenDates method


  - _Requirements: 29, 33_

- [ ] 10. Add comprehensive Javadoc documentation
- [ ] 10.1 Document Quest entity
  - Add class-level Javadoc with purpose, relationships, examples
  - Add field-level Javadoc for all fields
  - Add method-level Javadoc for all public methods
  - Include @author and @version tags
  - _Requirements: 31_

- [ ] 10.2 Document QuestCategory entity
  - Add comprehensive Javadoc following same pattern
  - Document relationship management patterns
  - _Requirements: 31_

- [ ] 10.3 Document QuestTask entity
  - Add comprehensive Javadoc following same pattern
  - Document sequential vs parallel task completion
  - _Requirements: 31_

- [ ] 10.4 Document requirement entities
  - Add Javadoc for QuestRequirement, QuestTaskRequirement, QuestCategoryRequirement
  - Document convenience methods and RPlatform integration
  - _Requirements: 31_

- [ ] 10.5 Document reward entities
  - Add Javadoc for QuestReward, QuestTaskReward, QuestCategoryReward
  - Document auto-grant behavior and RPlatform integration
  - _Requirements: 31_

- [ ] 10.6 Document progress entities
  - Add Javadoc for PlayerQuestProgress, PlayerTaskProgress, PlayerTaskRequirementProgress
  - Document progress tracking patterns and time limit handling
  - _Requirements: 31_

- [ ] 10.7 Document QuestCompletionHistory entity
  - Add comprehensive Javadoc
  - Document repeatability and cooldown tracking
  - _Requirements: 31_

- [ ] 10.8 Document repository classes
  - Add Javadoc for all repository classes
  - Document custom query methods and their use cases
  - _Requirements: 31_

- [ ] 11. Create migration utilities
- [ ] 11.1 Create QuestEntityMigration class
  - Create migration utility class
  - Implement migrateQuests method converting old Quest entities
  - Implement migrateCategories method
  - Implement migrateTasks method
  - _Requirements: 30_

- [ ] 11.2 Implement requirement/reward migration
  - Implement method to convert JSON requirement data to QuestRequirement entities
  - Implement method to convert JSON reward data to QuestReward entities
  - Create BaseRequirement instances from JSON data
  - Create BaseReward instances from JSON data
  - _Requirements: 30_

- [ ] 11.3 Implement progress migration
  - Implement migratePlayerProgress method
  - Convert old ActiveQuest to PlayerQuestProgress
  - Convert old task progress to PlayerTaskProgress
  - Preserve all timestamps and completion states
  - _Requirements: 30_

- [ ] 11.4 Implement history migration
  - Implement migrateCompletionHistory method
  - Convert old completion records to QuestCompletionHistory
  - Preserve completion counts and timestamps
  - _Requirements: 30_

- [ ] 11.5 Create rollback utilities
  - Implement QuestEntityRollback class
  - Implement rollbackToOldStructure method
  - Implement reverse migration logic
  - Add data validation before rollback
  - _Requirements: 30_


- [ ] 12. Integrate with existing services
- [ ] 12.1 Update QuestService for new entities
  - Update QuestService to use new Quest entity
  - Update quest creation methods
  - Update quest retrieval methods
  - Update quest validation methods
  - Integrate with ProgressionValidator for prerequisite checking
  - _Requirements: 32_

- [ ] 12.2 Update QuestProgressTracker for new progress entities
  - Update QuestProgressTracker to use PlayerQuestProgress
  - Update task progress tracking to use PlayerTaskProgress
  - Update requirement progress tracking to use PlayerTaskRequirementProgress
  - Implement progress caching for online players
  - _Requirements: 32_

- [ ] 12.3 Update QuestCompletionTracker for new history entity
  - Update QuestCompletionTracker to use QuestCompletionHistory
  - Implement ICompletionTracker interface for Quest entities
  - Update completion recording logic
  - Update repeatability checking logic
  - _Requirements: 32_

- [ ] 12.4 Update QuestEventListener for new entities
  - Update event handlers to work with new entity structure
  - Update quest start event handling
  - Update task complete event handling
  - Update quest complete event handling
  - Fire appropriate events for requirement/reward operations
  - _Requirements: 32_

- [ ] 12.5 Update QuestCacheManager for new entities
  - Update cache to store new Quest entities
  - Implement cache for QuestCategory entities
  - Implement cache for player progress entities
  - Update cache invalidation logic
  - _Requirements: 32, 33_

- [ ] 12.6 Integrate with RPlatform services
  - Integrate QuestRequirement with RequirementService
  - Integrate QuestReward with RewardService
  - Integrate Quest with ProgressionValidator
  - Test circular dependency detection
  - Test prerequisite validation
  - _Requirements: 32_

- [ ] 13. Update view layer
- [ ] 13.1 Update quest list views
  - Update quest category view to use new QuestCategory entity
  - Update quest list view to use new Quest entity
  - Update filtering and sorting logic
  - Update pagination logic
  - _Requirements: 32_

- [ ] 13.2 Update quest detail views
  - Update quest detail view to display new requirement structure
  - Update quest detail view to display new reward structure
  - Update task display logic
  - Update progress display logic
  - _Requirements: 32_

- [ ] 13.3 Update quest progress views
  - Update active quest view to use PlayerQuestProgress
  - Update task progress display to use PlayerTaskProgress
  - Update requirement progress display
  - Update time remaining display for time-limited quests
  - _Requirements: 32_

- [ ] 13.4 Update quest history views
  - Update completion history view to use QuestCompletionHistory
  - Update statistics display
  - Update cooldown display
  - _Requirements: 32_

- [ ]* 14. Create unit tests
- [ ]* 14.1 Create Quest entity tests
  - Test Quest construction and validation
  - Test relationship management methods (addTask, addRequirement, addReward)
  - Test IProgressionNode implementation
  - Test convenience methods (isRepeatable, hasTimeLimit, etc.)
  - Test equals and hashCode
  - _Requirements: 34_

- [ ]* 14.2 Create QuestCategory entity tests
  - Test QuestCategory construction and validation
  - Test relationship management methods
  - Test equals and hashCode
  - _Requirements: 34_

- [ ]* 14.3 Create QuestTask entity tests
  - Test QuestTask construction and validation
  - Test relationship management methods
  - Test equals and hashCode
  - _Requirements: 34_

- [ ]* 14.4 Create requirement entity tests
  - Test QuestRequirement, QuestTaskRequirement, QuestCategoryRequirement
  - Test convenience methods (isMet, calculateProgress, consume)
  - Test bidirectional relationship management
  - _Requirements: 34_

- [ ]* 14.5 Create reward entity tests
  - Test QuestReward, QuestTaskReward, QuestCategoryReward
  - Test convenience methods (grant, getEstimatedValue)
  - Test bidirectional relationship management
  - _Requirements: 34_

- [ ]* 14.6 Create progress entity tests
  - Test PlayerQuestProgress construction and methods
  - Test PlayerTaskProgress with progress capping
  - Test PlayerTaskRequirementProgress
  - Test time-based methods (isExpired, getRemainingTime)
  - _Requirements: 34_

- [ ]* 14.7 Create QuestCompletionHistory tests
  - Test QuestCompletionHistory construction
  - Test convenience methods (canRepeat, getCooldownRemaining)
  - _Requirements: 34_

- [ ]* 14.8 Create difficulty enum tests
  - Test QuestDifficulty enum values and methods
  - Test TaskDifficulty enum values and methods
  - _Requirements: 34_

- [ ]* 15. Create integration tests
- [ ]* 15.1 Create repository integration tests
  - Test CRUD operations for all repositories
  - Test custom query methods
  - Test pagination
  - Test batch loading queries
  - _Requirements: 34_

- [ ]* 15.2 Create cascade operation tests
  - Test cascade delete from Quest to tasks/requirements/rewards
  - Test cascade delete from QuestCategory to quests
  - Test orphan removal
  - _Requirements: 34_

- [ ]* 15.3 Create optimistic locking tests
  - Test concurrent modifications to PlayerQuestProgress
  - Test OptimisticLockException handling
  - Test retry logic
  - _Requirements: 34_

- [ ]* 15.4 Create unique constraint tests
  - Test unique constraint on Quest identifier
  - Test unique constraint on (player_id, quest_id) for PlayerQuestProgress
  - Test unique constraint on (quest_progress_id, task_id) for PlayerTaskProgress
  - _Requirements: 34_

- [ ]* 15.5 Create IProgressionNode integration tests
  - Test prerequisite validation with ProgressionValidator
  - Test circular dependency detection
  - Test progression state calculation
  - Test integration with QuestCompletionTracker
  - _Requirements: 34_

- [ ]* 15.6 Create migration tests
  - Test migration from old Quest structure to new
  - Test data preservation during migration
  - Test rollback functionality
  - _Requirements: 34_

- [ ]* 15.7 Create service integration tests
  - Test QuestService with new entities
  - Test QuestProgressTracker with new progress entities
  - Test QuestCompletionTracker with new history entity
  - Test event firing and handling
  - _Requirements: 34_

- [ ]* 15.8 Create RPlatform integration tests
  - Test RequirementService integration
  - Test RewardService integration
  - Test ProgressionValidator integration
  - _Requirements: 34_

- [ ] 16. Performance optimization
- [ ] 16.1 Implement quest caching
  - Create QuestCacheManager with Caffeine cache
  - Implement cache loading strategies
  - Implement cache invalidation strategies
  - Add cache statistics logging
  - _Requirements: 33_

- [ ] 16.2 Implement progress caching
  - Create QuestProgressCache for online players
  - Implement load on join, save on quit pattern
  - Implement auto-save task for crash protection
  - Add dirty tracking for modified progress
  - _Requirements: 33_

- [ ] 16.3 Optimize database queries
  - Review and optimize all repository queries
  - Add batch loading where appropriate
  - Add query result caching hints
  - Profile query performance
  - _Requirements: 33_

- [ ] 16.4 Add database indexes
  - Verify all indexes are created correctly
  - Add any missing indexes identified during profiling
  - Test index usage with EXPLAIN queries
  - _Requirements: 33_

- [ ] 17. Final validation and cleanup
- [ ] 17.1 Run all tests and verify coverage
  - Run all unit tests
  - Run all integration tests
  - Verify minimum 80% code coverage
  - Fix any failing tests
  - _Requirements: 34_

- [ ] 17.2 Verify Javadoc completeness
  - Check all entities have complete Javadoc
  - Check all repositories have complete Javadoc
  - Generate Javadoc and verify no warnings
  - _Requirements: 31_

- [ ] 17.3 Perform code review
  - Review all entity classes for consistency
  - Review all repository classes for consistency
  - Review all service integrations
  - Verify adherence to JEHibernate patterns
  - _Requirements: All_

- [ ] 17.4 Update documentation
  - Update README with new entity structure
  - Update migration guide
  - Update developer documentation
  - Create entity relationship diagrams
  - _Requirements: 31_

- [ ] 17.5 Prepare for deployment
  - Create database migration scripts
  - Create rollback scripts
  - Document deployment procedure
  - Create deployment checklist
  - _Requirements: 30_

## Notes

- Tasks marked with * are optional testing tasks that can be skipped for faster MVP
- All tasks reference specific requirements from requirements.md
- Each task should be completed before moving to the next
- Context documents (requirements.md, design.md) are available during implementation
- Follow JEHibernate patterns established in rank system
- Maintain consistency with RPlatform integration patterns
