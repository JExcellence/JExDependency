# Implementation Plan

- [x] 1. Create modern enum and type system
  - Replace EStageRarityType with EEvolutionRarityType using modern enum patterns
  - Create CobblestoneGeneratorType enum with clean structure
  - Add utility methods and modern enum features
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 1.1 Create EEvolutionRarityType enum
  - Implement modern enum with display names and level comparisons
  - Add utility methods for rarity operations
  - Use clean, descriptive naming without excessive prefixes
  - _Requirements: 2.1, 2.2, 3.1, 3.2_

- [x] 1.2 Create CobblestoneGeneratorType enum
  - Define generator types with modern enum structure
  - Add properties and utility methods
  - _Requirements: 2.1, 2.2_

- [x] 2. Implement optimized core entities with Lombok
  - Create OneblockPlayer entity using full Lombok optimization
  - Implement Island entity with embedded components and clean relationships
  - Use modern Java patterns and eliminate boilerplate code
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 2.1 Create OneblockPlayer entity
  - Implement with @Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor
  - Use @EqualsAndHashCode(of = "uniqueId") for proper equality
  - Add clean constructors for Bukkit Player integration
  - _Requirements: 1.1, 1.2, 1.5, 3.1, 3.2, 3.5_

- [x] 2.2 Create Island entity with embedded components
  - Implement main Island entity with Lombok annotations
  - Embed IslandRegion and OneblockCore components
  - Use clean relationship mappings with proper cascade types
  - Add business logic methods for member and access management
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.3_

- [x] 2.3 Create IslandRegion embeddable component
  - Implement as @Embeddable with coordinate management
  - Add utility methods for region containment and center calculation
  - Use LocationConverter for spawn location persistence
  - _Requirements: 1.1, 1.2, 1.5, 3.1, 3.2, 7.1, 7.2_

- [x] 2.4 Create OneblockCore embeddable component
  - Implement evolution progression tracking with clean methods
  - Add experience management and prestige functionality
  - Use LocationConverter for oneblock location persistence
  - _Requirements: 1.1, 1.2, 1.5, 3.1, 3.2, 7.1, 7.2_

- [x] 3. Implement evolution system with builder pattern
  - Create abstract OneblockEvolution base class with modern inheritance
  - Implement CustomEvolution and PredefinedEvolution subclasses
  - Create EvolutionBuilder with fluent API for easy evolution creation
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3.1 Create OneblockEvolution abstract base class
  - Implement with Lombok and proper JPA inheritance strategy
  - Add relationships to evolution content entities
  - Include methods for content retrieval by rarity using modern Java features
  - _Requirements: 1.1, 1.2, 1.5, 2.1, 2.2, 3.1, 3.2, 6.1, 6.2_

- [x] 3.2 Create evolution content entities
  - Implement EvolutionBlock, EvolutionEntity, and EvolutionItem with Lombok
  - Use optimized converters for Material and ItemStack lists
  - Establish clean many-to-one relationships with OneblockEvolution
  - _Requirements: 1.1, 1.2, 1.5, 2.1, 2.2, 3.1, 3.2, 6.1, 6.2, 7.1, 7.2, 7.3, 7.4_

- [x] 3.3 Create EvolutionBuilder with fluent API
  - Implement builder pattern for easy evolution configuration
  - Add method chaining for all content types (blocks, entities, items)
  - Include bulk methods for common patterns (ores, mobs, saplings)
  - Support custom item suppliers and lambda-based configuration
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3.4 Create CustomEvolution and PredefinedEvolution classes



  - Extend OneblockEvolution for different evolution types
  - Implement proper discrimination for JPA inheritance
  - Add specific functionality for each evolution type
  - _Requirements: 2.1, 2.2, 8.1, 8.2, 8.3, 8.4_

- [x] 4. Implement enhanced item factory system
  - Create EnhancedItemFactory with modern builder patterns
  - Implement ItemBuilder for fluent item creation
  - Add support for custom enchantments, display names, and lore
  - Create helper methods for armor sets and weapon creation
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 4.1 Create ItemBuilder with fluent API
  - Implement builder pattern for ItemStack creation
  - Support method chaining for all item properties
  - Add specialized methods for leather armor coloring
  - Include validation and error handling
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 4.2 Create EnhancedItemFactory with predefined items
  - Implement factory methods for god weapons and armor sets
  - Use ItemBuilder for consistent item creation
  - Add support for evolution-specific items
  - Include I18n integration points for localized names
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 5. Implement optimized converter system
  - Create efficient converters for complex object serialization
  - Implement LocationConverter, ItemStackListConverter, MaterialListConverter
  - Add proper error handling and thread safety
  - Optimize for performance and memory usage
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 5.1 Create LocationConverter
  - Implement efficient Location to String serialization
  - Handle null values and invalid worlds gracefully
  - Add proper error handling and logging
  - _Requirements: 7.1, 7.4, 7.5_

- [x] 5.2 Create ItemStackListConverter
  - Implement efficient ItemStack list serialization
  - Use optimized JSON or binary format
  - Handle complex ItemStack properties (enchantments, meta)
  - _Requirements: 7.2, 7.4, 7.5_

- [x] 5.3 Create MaterialListConverter
  - Implement Material enum list serialization
  - Handle invalid materials gracefully with fallbacks
  - Optimize for common use cases
  - _Requirements: 7.3, 7.4, 7.5_

- [x] 6. Create evolution factory and management system








  - Implement EvolutionFactory with supplier pattern for lazy initialization
  - Create evolution registration and discovery mechanisms
  - Add validation and error reporting for evolution configurations
  - Support plugin-based evolution extensions




  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_


- [x] 6.1 Create EvolutionFactory with supplier pattern


  - Implement factory with lazy initialization using suppliers


  - Add automatic registration mechanisms for evolutions
  - Include validation and error reporting
  - _Requirements: 8.1, 8.2, 8.4_

- [x] 6.2 Convert existing evolution implementations


  - Refactor ZeusStage to ZeusEvolution using new structure
  - Update all existing evolution classes to use EvolutionBuilder
  - Maintain existing content and rarity configurations
  - _Requirements: 2.1, 2.2, 8.1, 8.3_

- [x] 7. Implement database optimization and indexing


  - Add proper database indexes for frequently queried fields
  - Optimize entity relationships with appropriate fetch strategies
  - Implement query optimization and performance monitoring
  - Add database constraints for data integrity


  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7.1 Add database indexes and constraints
  - Create indexes on UUID fields, evolution names, and foreign keys
  - Add proper database constraints for data integrity
  - Optimize table structure for query performance
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 7.2 Optimize entity relationships and fetch strategies
  - Configure appropriate fetch types for all relationships


  - Use @EntityGraph for controlled eager loading where needed
  - Implement pagination for large collections
  - _Requirements: 6.1, 6.2, 6.3, 6.5_



- [ ] 8. Consolidate entity implementations
  - Remove duplicate entity implementations between root src and JExOneblock module
  - Ensure consistent Lombok usage across all entities
  - Update import statements and references to use single source of truth


  - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2_

- [ ] 8.1 Consolidate OneblockPlayer entity
  - Choose between root src and JExOneblock module implementation
  - Update the chosen implementation to use full Lombok optimization
  - Remove duplicate implementation and update all references
  - _Requirements: 1.1, 1.2, 3.1, 3.2_

- [ ] 8.2 Consolidate Island entity implementations
  - Merge features from both Island implementations
  - Ensure embedded components (IslandRegion, OneblockCore) are properly integrated
  - Use consistent Lombok annotations and business logic methods
  - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.2, 6.1, 6.2, 6.3_

- [ ] 8.3 Consolidate embeddable components
  - Merge IslandRegion implementations to use best features from both
  - Consolidate OneblockCore implementations with consistent field naming
  - Ensure proper converter usage and validation
  - _Requirements: 1.1, 1.2, 3.1, 3.2, 7.1, 7.2_

- [ ]* 9. Create comprehensive test suite
  - Write unit tests for all entities and builders
  - Create integration tests for evolution system
  - Test database operations and constraint validation
  - Add performance tests for large data sets
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 9.1 Create unit tests for entities and builders
  - Test entity creation, validation, and business logic
  - Test EvolutionBuilder fluent API and validation



  - Test ItemBuilder and factory methods
  - _Requirements: 1.1, 1.2, 4.1, 4.2, 5.1, 5.2_

- [ ]* 9.2 Create integration tests for evolution system
  - Test complete evolution lifecycle operations
  - Test entity relationships and cascade operations
  - Test converter functionality with real data
  - _Requirements: 2.1, 2.2, 6.1, 6.2, 7.1, 7.2_

- [ ] 10. Finalize migration and validation
  - Create migration scripts for smooth transition from old entities
  - Validate all entity relationships and database constraints
  - Ensure backward compatibility during transition period
  - Update all references to use evolution terminology
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 10.1 Create migration scripts and validation
  - Implement database migration scripts for table renames
  - Create data migration utilities for existing data
  - Validate entity mappings and constraint generation
  - Test migration process with realistic data volumes
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_