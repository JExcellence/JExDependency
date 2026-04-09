# Implementation Plan

## Overview
This plan addresses all 46 compilation errors in the RDQ quest system by fixing entity relationships, implementing IProgressionNode interface, adding missing RDQ methods, and simplifying the QuestAbandonResult API.

## Tasks

- [x] 1. Fix Entity Relationship Methods


  - Add @OneToMany collections and management methods to entities
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 1.1 Add QuestTask relationship methods


  - Add requirements and rewards @OneToMany collections
  - Add addRequirement(), removeRequirement(), getRequirements() methods
  - Add addReward(), removeReward(), getRewards() methods
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_



- [ ] 1.2 Add Quest relationship methods
  - Add requirements and rewards @OneToMany collections
  - Add addRequirement(), removeRequirement(), getRequirements() methods

  - Add addReward(), removeReward(), getRewards() methods
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_



- [ ] 1.3 Add QuestCategory relationship methods
  - Add requirements and rewards @OneToMany collections
  - Add addRequirement(), removeRequirement(), getRequirements() methods
  - Add addReward(), removeReward(), getRewards() methods


  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 2. Implement IProgressionNode for Quest
  - Make Quest implement IProgressionNode<Quest>


  - Add prerequisite tracking fields
  - Implement all required interface methods
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_



- [ ] 3. Implement IProgressionNode for RRank
  - Make RRank implement IProgressionNode<RRank>
  - Add prerequisite tracking fields if missing
  - Implement all required interface methods
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ] 4. Add Missing RDQ Methods
  - Add playerQuestProgressCache field with @Getter
  - Add questProgressionValidator field with @Getter
  - Initialize both in initializeQuestSystem()
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 5. Fix Repository Method Names
  - Add findByIdentifier() to RRankRepository


  - Add findByPlayerIdAndIsActive() to RPlayerRankRepository
  - Add findByPlayerId() to RPlayerRankRepository
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 6. Simplify QuestAbandonResult API
  - Replace sealed interface with flat record
  - Add static factory methods with i18n keys
  - Update QuestServiceImpl to use new API
  - Update QuestDetailView to use new API
  - Update QuestCommand to use I18n.Builder
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 7. Build and Verify
  - Run gradle clean build
  - Verify zero compilation errors
  - Verify zero warnings
  - _Requirements: All_
