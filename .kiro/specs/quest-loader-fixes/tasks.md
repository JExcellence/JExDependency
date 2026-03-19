# Implementation Plan

- [x] 1. Fix Core Quest Loading


  - Fix YAML parsing to handle flat structure (no nested `quest:` section)
  - Update parseTask() to accept ConfigurationSection instead of Map
  - Parse task type, target, and amount fields
  - Add comprehensive logging throughout loading process
  - Test that quests load and appear in GUI
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3, 6.4, 6.5_





- [ ] 2. Implement Quest Reward Loading
  - [ ] 2.1 Create reward parsing methods
    - Implement loadQuestRewards() method
    - Implement parseQuestReward() method
    - Handle CURRENCY reward type (currency_id, amount)
    - Handle EXPERIENCE reward type (amount)
    - Handle ITEM reward type (material, amount, display_name_key, lore_key, enchantments)
    - Handle PERK reward type (perk_id, duration)

    - Handle COMMAND reward type (command with placeholders)
    - Serialize complex reward data to JSON
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 2.2 Create task reward parsing methods

    - Implement loadTaskRewards() method
    - Implement parseTaskReward() method
    - Link task rewards to parent task
    - _Requirements: 2.5_





  - [ ] 2.3 Add reward entity relationships
    - Ensure Quest.rewards collection is properly initialized
    - Ensure QuestTask.rewards collection is properly initialized
    - Add cascade operations for reward persistence
    - _Requirements: 2.4, 2.5_

- [ ] 3. Implement Quest Requirement Loading
  - [x] 3.1 Create requirement parsing methods

    - Implement loadQuestRequirements() method
    - Implement parseQuestRequirement() method
    - Handle LEVEL requirement type (min_level)
    - Handle CURRENCY requirement type (currency_id, amount)
    - Handle QUEST_COMPLETE requirement type (quest_id)

    - Handle PERMISSION requirement type (permission_node)
    - Handle ITEM requirement type (material, amount)
    - Serialize complex requirement data to JSON


    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 3.2 Create task requirement parsing methods
    - Implement loadTaskRequirements() method
    - Implement parseTaskRequirement() method
    - Link task requirements to parent task
    - _Requirements: 3.4_




  - [ ] 3.3 Handle prerequisites
    - Parse prerequisites list from YAML
    - Store prerequisite quest identifiers
    - _Requirements: 3.5_


- [ ] 4. Enhance Task Detail Loading
  - Parse task type field (KILL_MOBS, CRAFT_ITEMS, BREAK_BLOCKS, etc.)
  - Parse task target field (entity/item/block type)
  - Parse task amount field (required completion count)
  - Parse task optional flag

  - Parse task difficulty enum
  - Store additional task data in taskDataJson field
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_


- [ ] 5. Implement Effects and Metadata Loading
  - [ ] 5.1 Parse effects section
    - Parse start_particle and complete_particle
    - Parse start_sound and complete_sound

    - Parse start_title and complete_title (with fade timings)

    - Serialize effects to JSON and store in Quest.effectsJson
    - _Requirements: 5.1, 5.2_

  - [ ] 5.2 Parse metadata section
    - Parse author, created, modified, version fields
    - Parse tags list
    - Serialize metadata to JSON and store in Quest.metadataJson
    - _Requirements: 5.3, 5.4_


  - [ ] 5.3 Parse failure conditions
    - Parse fail_on_death, fail_on_logout, fail_on_timeout flags
    - Serialize to JSON and store in Quest.failureConditionsJson
    - _Requirements: 5.5_


  - [ ] 5.4 Parse additional attributes
    - Parse chain_id and chain_order
    - Parse quest_type (MAIN, SIDE, DAILY, WEEKLY, CHALLENGE)
    - Parse hidden, auto_start, show_in_log flags
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 6. Add Entity Field Extensions
  - [x] 6.1 Extend Quest entity


    - Add effectsJson field (TEXT column)
    - Add metadataJson field (TEXT column)
    - Add failureConditionsJson field (TEXT column)
    - Add chainId field
    - Add chainOrder field
    - Add questType field
    - Add hidden, autoStart, showInLog boolean fields
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 6.2 Extend QuestTask entity
    - Add taskType field
    - Add target field
    - Add amount field
    - Add taskDataJson field (TEXT column)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 7. Implement Error Handling and Logging
  - Add try-catch blocks around all parsing operations
  - Log file-level errors and continue with other files
  - Log quest-level errors and continue with other quests
  - Log task/reward/requirement errors and continue loading
  - Add logParsingError() utility method
  - Add summary statistics logging
  - _Requirements: 1.5, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 8. Add JSON Serialization Utilities
  - Implement serializeToJson() method using Gson or Jackson
  - Handle null values gracefully
  - Handle nested objects and lists
  - Add error handling for serialization failures
  - _Requirements: 2.2, 3.2, 5.1, 5.3, 5.5_

- [ ] 9. Implement Reload Support
  - [ ] 9.1 Add reload() method
    - Clear existing quest cache
    - Re-run loadConfigurations()
    - Invalidate QuestCacheManager caches
    - _Requirements: 8.4, 8.5_

  - [ ] 9.2 Handle incremental updates
    - Detect existing quests and update instead of create
    - Clear and replace tasks, rewards, requirements
    - Preserve quest progress data
    - _Requirements: 8.1, 8.2, 8.3_

- [ ]* 10. Add Unit Tests
  - Write tests for YAML parsing
  - Write tests for reward parsing (all types)
  - Write tests for requirement parsing (all types)
  - Write tests for task parsing
  - Write tests for effects/metadata parsing
  - Write tests for error handling
  - Write tests for JSON serialization
  - _Requirements: All_

- [ ]* 11. Add Integration Tests
  - Test database persistence of quests
  - Test database persistence of tasks
  - Test database persistence of rewards
  - Test database persistence of requirements
  - Test update logic for existing quests
  - Test cache invalidation
  - Test reload functionality
  - _Requirements: All_

- [ ] 12. Update Documentation
  - Update YAML format documentation
  - Add examples for all reward types
  - Add examples for all requirement types
  - Document effects and metadata structure
  - Add troubleshooting guide
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
