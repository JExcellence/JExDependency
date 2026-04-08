# Generator Structure System Overhaul - Requirements

## Overview
Complete refactoring and enhancement of the cobblestone generator structure system in JExOneblock. This overhaul replaces the static `CobblestoneGeneratorType` enum with a dynamic, database-driven generator design system featuring 10 unique generator types with distinct visual designs, gameplay mechanics, and progression requirements.

## Goals
1. Replace static enum-based generator types with dynamic, configurable generator designs
2. Create 10 unique generator structure designs with distinct themes and mechanics
3. Implement proper database entities with relationships for structures, layers, and materials
4. Integrate with the evolution system for unlock requirements using `AbstractRequirement` pattern
5. Enhance visualization with particle effects, 3D previews, and animated building
6. Provide comprehensive owner configuration options
7. Use i18n for all user-facing strings
8. Follow RDQ-common patterns for services, repositories, and entities

## Functional Requirements

### FR-1: Generator Design System
- FR-1.1: Support 10 unique generator designs with distinct visual themes
- FR-1.2: Each generator must have configurable layers, materials, and patterns
- FR-1.3: Generators must support dynamic unlock requirements via `AbstractRequirement`
- FR-1.4: Generator designs must be loadable from configuration files
- FR-1.5: Support for custom particle effects per generator type
- FR-1.6: Support for generator upgrades and enhancements

### FR-2: Database Entities
- FR-2.1: `GeneratorDesign` entity - stores generator design metadata
- FR-2.2: `GeneratorDesignLayer` entity - stores layer patterns with FK to design
- FR-2.3: `GeneratorDesignMaterial` entity - stores material requirements per layer
- FR-2.4: `GeneratorDesignRequirement` entity - stores unlock requirements
- FR-2.5: `GeneratorDesignReward` entity - stores rewards/bonuses for building
- FR-2.6: `PlayerGeneratorStructure` entity - tracks player-built structures
- FR-2.7: All entities must extend `BaseEntity` and follow JPA patterns

### FR-3: Generator Designs (10 Types)
- FR-3.1: **Foundry Generator** - Industrial theme with furnaces and hoppers
- FR-3.2: **Aquatic Generator** - Water-based with prismarine and sea lanterns
- FR-3.3: **Volcanic Generator** - Lava-focused with magma blocks and basalt
- FR-3.4: **Crystal Generator** - Amethyst and glass-based ethereal design
- FR-3.5: **Mechanical Generator** - Redstone-powered with pistons and observers
- FR-3.6: **Nature Generator** - Organic with moss, leaves, and bee nests
- FR-3.7: **Nether Generator** - Nether materials with soul fire and blackstone
- FR-3.8: **End Generator** - End stone, purpur, and end rod aesthetics
- FR-3.9: **Ancient Generator** - Deepslate and sculk with ancient debris
- FR-3.10: **Celestial Generator** - Beacon-powered with diamond/netherite core

### FR-4: Requirement System Integration (via RPlatform)
- FR-4.1: Use RPlatform `Requirement` interface for all requirements
- FR-4.2: Extend RPlatform `AbstractRequirement` for OneBlock-specific requirements
- FR-4.3: Create `EvolutionLevelRequirement` for evolution progression
- FR-4.4: Create `BlocksBrokenRequirement` for block mining milestones
- FR-4.5: Create `PrestigeLevelRequirement` for prestige gates
- FR-4.6: Create `GeneratorTierRequirement` for tier progression (requires previous tier)
- FR-4.7: Use RPlatform `ItemRequirement` for material requirements
- FR-4.8: Use RPlatform `CurrencyRequirement` with support for:
  - Vault integration (single default economy)
  - JExEconomy integration (multiple named currencies via `currencyIdentifier`)
  - Configurable `EconomyProvider` enum (VAULT, JEXECONOMY)
- FR-4.9: Register OneBlock requirements via `PluginRequirementProvider`
- FR-4.10: Requirements must be configurable per generator design via YAML

### FR-5: Visualization System
- FR-5.1: 3D structure preview in GUI with rotation controls
- FR-5.2: Layer-by-layer visualization with material highlighting
- FR-5.3: Particle-based structure outline in world
- FR-5.4: Animated building process with particle trails
- FR-5.5: Build progress tracking with visual feedback
- FR-5.6: Structure validation visualization (correct/incorrect blocks)

### FR-6: GUI Views
- FR-6.1: `GeneratorBrowserView` - Browse all available generator designs
- FR-6.2: `GeneratorDesignDetailView` - View specific design details
- FR-6.3: `GeneratorLayerDetailView` - View layer patterns and materials
- FR-6.4: `GeneratorMaterialsView` - View required materials with inventory check
- FR-6.5: `GeneratorVisualization3DView` - Interactive 3D preview
- FR-6.6: `GeneratorBuildProgressView` - Track building progress
- FR-6.7: All views must use i18n keys, not hardcoded strings

### FR-7: Services
- FR-7.1: `GeneratorDesignService` - CRUD operations for designs
- FR-7.2: `GeneratorStructureManager` - Central manager for all structure operations
- FR-7.3: `StructureDetectionService` - Detect and validate built structures
- FR-7.4: `StructureBuildService` - Handle automated building process
- FR-7.5: `StructureVisualizationService` - Handle particle effects and previews
- FR-7.6: `GeneratorRequirementService` - Check and consume requirements

### FR-8: Configuration
- FR-8.1: YAML-based generator design configuration
- FR-8.2: Configurable particle effects per generator
- FR-8.3: Configurable build animation speed
- FR-8.4: Configurable requirement multipliers
- FR-8.5: Configurable reward values
- FR-8.6: Enable/disable individual generator designs

## Non-Functional Requirements

### NFR-1: Performance
- NFR-1.1: Structure detection must complete within 100ms
- NFR-1.2: Particle effects must not cause client lag (max 50 particles/tick)
- NFR-1.3: Database queries must be async and cached appropriately
- NFR-1.4: GUI rendering must be smooth with pagination for large datasets

### NFR-2: Maintainability
- NFR-2.1: Follow existing RDQ-common patterns for consistency
- NFR-2.2: Use dependency injection for services
- NFR-2.3: Comprehensive JavaDoc documentation
- NFR-2.4: Unit tests for critical business logic

### NFR-3: Extensibility
- NFR-3.1: Easy to add new generator designs via configuration
- NFR-3.2: Plugin API for custom requirement types
- NFR-3.3: Event system for structure build/destroy events

## User Stories

### US-1: Player Generator Discovery
As a player, I want to browse available generator designs so I can plan my progression.

### US-2: Player Requirement Check
As a player, I want to see what requirements I need to meet to unlock a generator design.

### US-3: Player Structure Building
As a player, I want to build generator structures with visual guidance and feedback.

### US-4: Player Structure Activation
As a player, I want my built structure to be automatically detected and activated.

### US-5: Owner Configuration
As a server owner, I want to configure generator designs, requirements, and rewards.

### US-6: Owner Design Management
As a server owner, I want to enable/disable specific generator designs.

## References
- #[[file:JExOneblock/jexoneblock-common/src/main/java/de/jexcellence/oneblock/structure/GeneratorStructure.java]]
- #[[file:JExOneblock/jexoneblock-common/src/main/java/de/jexcellence/oneblock/structure/StructureLayer.java]]
- #[[file:JExOneblock/jexoneblock-common/src/main/java/de/jexcellence/oneblock/service/StructureDetectionService.java]]
- #[[file:JExOneblock/jexoneblock-common/src/main/java/de/jexcellence/oneblock/service/StructureBuildService.java]]
- #[[file:RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/requirement/AbstractRequirement.java]]
- #[[file:RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/requirement/Requirement.java]]
- #[[file:RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/database/entity/RRequirement.java]]
- #[[file:RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java]]
