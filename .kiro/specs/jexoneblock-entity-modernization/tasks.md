# Implementation Plan

- [x] 1. Set up package structure and base infrastructure


  - Create new package structure following design specifications
  - Set up converter classes for Bukkit object serialization
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_


- [x] 1.1 Create package directories and converter classes

  - Create `com.jexcellence.oneblock.database.entity` package structure
  - Implement LocationConverter, WorldConverter, ItemStackListConverter, MaterialListConverter
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 2. Implement core player entity


  - Create modernized OneblockPlayer entity replacing JEPlayer
  - Follow RDQ-common patterns for player entity structure
  - _Requirements: 1.2, 2.1, 2.2, 5.1, 5.2_

- [x] 2.1 Create OneblockPlayer entity class


  - Implement OneblockPlayer extending BaseEntity with UUID and playerName fields
  - Add proper JPA annotations and constructors
  - Include equals/hashCode methods based on UUID
  - _Requirements: 1.2, 2.1, 2.2, 5.1, 5.2, 5.5_

- [x] 3. Implement embeddable components


  - Create IslandRegion and OneblockCore embeddable classes
  - Ensure proper converter usage for complex types
  - _Requirements: 1.4, 1.5, 2.5, 4.1, 4.2_

- [x] 3.1 Create IslandRegion embeddable class


  - Implement IslandRegion with coordinate fields and location converters
  - Add utility methods for region containment checks
  - Include spawn location management
  - _Requirements: 1.5, 2.5, 4.1, 4.2, 5.3_

- [x] 3.2 Create OneblockCore embeddable class


  - Implement OneblockCore with stage progression tracking
  - Add location converter for oneblock position
  - Include experience and prestige management methods
  - _Requirements: 1.4, 2.5, 4.1, 5.3_

- [x] 4. Implement main Island entity


  - Create Island entity replacing JEIsland with proper relationships
  - Integrate embeddable components and player relationships
  - _Requirements: 1.3, 2.1, 2.3, 2.4, 5.1, 5.2, 5.4_

- [x] 4.1 Create Island entity class


  - Implement Island entity with OneblockPlayer owner relationship
  - Embed IslandRegion and OneblockCore components
  - Add island properties (size, level, experience, coins, name, description)
  - _Requirements: 1.3, 2.1, 2.3, 5.1, 5.2, 5.3_

- [x] 4.2 Implement Island member and ban management


  - Add many-to-many relationships for members and banned players
  - Implement proper cascade types and collection initialization
  - _Requirements: 2.3, 2.4, 5.4_

- [x] 5. Implement stage system base classes


  - Create abstract OneblockStage and stage content entities
  - Establish proper parent-child relationships
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2_

- [x] 5.1 Create abstract OneblockStage entity


  - Implement OneblockStage base class with common stage properties
  - Add relationships to StageBlock, StageEntity, and StageItem
  - Include methods for content retrieval by rarity
  - _Requirements: 3.1, 3.5, 6.1, 6.2, 6.3_

- [x] 5.2 Create stage content entities


  - Implement StageBlock, StageEntity, and StageItem entities
  - Add proper converters for Material and ItemStack lists
  - Establish many-to-one relationships with OneblockStage
  - _Requirements: 3.2, 3.3, 3.4, 3.5, 4.3, 4.4_

- [x] 6. Implement stage configuration classes


  - Create CustomOneblockStage and PredefinedOneblockStage classes
  - Support editable and fixed stage configurations
  - _Requirements: 6.1, 6.2, 6.4, 6.5_

- [x] 6.1 Create CustomOneblockStage class


  - Extend OneblockStage for user-editable stages
  - Add configuration management methods
  - _Requirements: 6.1, 6.2, 6.4_

- [x] 6.2 Create PredefinedOneblockStage class


  - Extend OneblockStage for system-defined stages
  - Implement template pattern for fixed configurations
  - _Requirements: 6.1, 6.4, 6.5_

- [x] 7. Migrate existing stage implementations


  - Convert ZeusStage and other existing stages to new structure
  - Ensure proper data initialization and relationships
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.4_

- [x] 7.1 Convert ZeusStage to new structure


  - Refactor existing ZeusStage to extend PredefinedOneblockStage
  - Update initialization methods to use new entity relationships
  - Maintain existing stage content and rarity configurations
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.4_

- [ ]* 7.2 Create unit tests for stage entities
  - Write tests for stage creation and content management
  - Test rarity-based content retrieval methods
  - Verify proper entity relationships and cascade behavior
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 8. Finalize entity integration and validation



  - Ensure all entities work together properly
  - Validate JPA mappings and database constraints
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.5_

- [x] 8.1 Validate entity relationships and constraints


  - Test all JPA relationships and cascade operations
  - Verify proper constraint validation and error handling
  - Ensure database schema generation works correctly
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.5_

- [ ]* 8.2 Create integration tests for complete entity system
  - Write tests for full entity lifecycle operations
  - Test complex queries and relationship navigation
  - Verify performance with realistic data volumes
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_