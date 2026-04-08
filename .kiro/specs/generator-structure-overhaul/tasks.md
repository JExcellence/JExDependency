# Generator Structure System Overhaul - Implementation Tasks

## Phase 1: Foundation - Database Entities & Repositories

### Task 1.1: Create Generator Design Entities
- [ ] Create `EGeneratorDesignType` enum with 10 types (Foundry, Aquatic, Volcanic, Crystal, Mechanical, Nature, Nether, End, Ancient, Celestial)
- [ ] Create `GeneratorDesign` entity with proper JPA annotations
- [ ] Create `GeneratorDesignLayer` entity with FK to design
- [ ] Create `GeneratorDesignMaterial` entity for material tracking
- [ ] Create `GeneratorDesignRequirement` entity with requirement data
- [ ] Create `GeneratorDesignReward` entity for rewards/bonuses
- [ ] Create `PlayerGeneratorStructure` entity for player-built structures
- [ ] Create `MaterialPatternConverter` for pattern serialization
- [ ] Create `GeneratorRequirementConverter` for requirement serialization

### Task 1.2: Create Repositories
- [ ] Create `GeneratorDesignRepository` extending `CachedRepository`
- [ ] Create `GeneratorDesignLayerRepository`
- [ ] Create `GeneratorDesignRequirementRepository`
- [ ] Create `PlayerGeneratorStructureRepository`
- [ ] Add async methods for all CRUD operations
- [ ] Implement caching strategy for design data

## Phase 2: Requirement System

### Task 2.1: Create Generator Requirement Framework
- [x] Create `GeneratorRequirement` sealed interface following RDQ pattern
- [x] Create `AbstractGeneratorRequirement` base class
- [x] Create `EvolutionLevelRequirement` implementation
- [x] Create `BlocksBrokenRequirement` implementation
- [x] Create `PrestigeLevelRequirement` implementation
- [x] Create `ItemRequirement` implementation
- [x] Create `CurrencyRequirement` implementation
- [x] Create `CompositeGeneratorRequirement` for multiple requirements

### Task 2.2: Create Requirement Service
- [x] Create `GeneratorRequirementService` class
- [x] Implement `checkRequirements(Player, GeneratorDesign)` method
- [x] Implement `calculateProgress(Player, GeneratorRequirement)` method
- [x] Implement `consumeRequirements(Player, GeneratorDesign)` method
- [x] Add caching for requirement checks

## Phase 3: Generator Design Definitions

### Task 3.1: Create Design Registry
- [x] Create `GeneratorDesignRegistry` class
- [x] Implement design registration system
- [x] Implement design lookup by key/type
- [x] Add configuration loading support

### Task 3.2: Implement 10 Generator Designs
- [x] Create `FoundryDesign` (Tier 1) - 5x5x3, furnaces/hoppers
- [x] Create `AquaticDesign` (Tier 2) - 5x5x4, prismarine/sea lanterns
- [x] Create `VolcanicDesign` (Tier 3) - 7x7x4, magma/basalt
- [x] Create `CrystalDesign` (Tier 4) - 7x7x5, amethyst/glass
- [x] Create `MechanicalDesign` (Tier 5) - 9x9x5, pistons/redstone
- [x] Create `NatureDesign` (Tier 6) - 9x9x6, moss/leaves
- [x] Create `NetherDesign` (Tier 7) - 11x11x6, blackstone/soul fire
- [x] Create `EndDesign` (Tier 8) - 11x11x7, end stone/purpur
- [x] Create `AncientDesign` (Tier 9) - 13x13x7, deepslate/sculk
- [x] Create `CelestialDesign` (Tier 10) - 15x15x8, beacons/netherite

### Task 3.3: Create Design Configuration
- [x] Create `GeneratorStructureConfig` class
- [x] Create `GeneratorDesignSection` for per-design config
- [x] Create `GeneratorEffectsSection` for particle config
- [x] Create `GeneratorRequirementSection` for requirement config
- [x] Add YAML configuration file template

## Phase 4: Core Services

### Task 4.1: Create Generator Design Service
- [ ] Create `GeneratorDesignService` class
- [ ] Implement `getDesign(String key)` method
- [ ] Implement `getAllDesigns()` method
- [ ] Implement `getAvailableDesigns(Player)` method
- [ ] Implement `isDesignEnabled(String key)` method
- [ ] Add design caching

### Task 4.2: Create Structure Detection Service
- [ ] Refactor existing `StructureDetectionService`
- [ ] Update to use new `GeneratorDesign` entities
- [ ] Implement `scanForStructures(Location, int radius)` method
- [ ] Implement `validateStructure(GeneratorDesign, Location)` method
- [ ] Implement `detectDesignType(Location)` method
- [ ] Add async validation support

### Task 4.3: Create Structure Build Service
- [ ] Refactor existing `StructureBuildService`
- [ ] Update to use new `GeneratorDesign` entities
- [ ] Implement `startBuild(Player, GeneratorDesign, Location)` method
- [ ] Implement `cancelBuild(Player)` method
- [ ] Implement `getBuildProgress(Player)` method
- [ ] Add material consumption from inventory
- [ ] Add build animation with configurable speed

### Task 4.4: Create Generator Structure Manager
- [ ] Create `GeneratorStructureManager` central manager class
- [ ] Inject all required services
- [ ] Implement `initialize()` method
- [ ] Implement `shutdown()` method
- [ ] Implement `reload()` method
- [ ] Implement `buildStructure(Player, GeneratorDesign, Location)` method
- [ ] Implement `destroyStructure(PlayerGeneratorStructure)` method
- [ ] Implement `activateStructure(PlayerGeneratorStructure)` method
- [ ] Implement `deactivateStructure(PlayerGeneratorStructure)` method

## Phase 5: Visualization System

### Task 5.1: Create Particle Effect Manager
- [x] Refactor existing `ParticleEffectManager`
- [x] Create `GeneratorParticleEffect` base class
- [x] Create `BuildParticleEffect` for construction
- [x] Create `ValidationParticleEffect` for validation feedback
- [x] Create `IdleParticleEffect` for active generators
- [x] Add per-design particle configurations

### Task 5.2: Create Structure Visualization Service
- [x] Create `StructureVisualizationService` class
- [x] Implement `showStructureOutline(Player, GeneratorDesign, Location)` method
- [x] Implement `showLayerPreview(Player, GeneratorDesignLayer, Location)` method
- [x] Implement `hidePreview(Player)` method
- [x] Implement `playBuildParticles(Location, Material)` method
- [x] Implement `playCompletionEffect(Location, GeneratorDesign)` method

### Task 5.3: Enhance 3D Visualization
- [x] Refactor existing `StructureVisualization3D`
- [x] Add rotation controls for preview
- [x] Add zoom controls
- [x] Add layer highlighting
- [x] Add material color coding
- [x] Optimize particle rendering for performance

## Phase 6: GUI Views

### Task 6.1: Create Generator Browser View
- [x] Create `GeneratorBrowserView` class extending `BaseView`
- [x] Implement grid layout for 10 generator types
- [x] Add locked/unlocked status indicators
- [x] Add tier progression display
- [x] Add click handlers for design details
- [x] Use i18n keys for all text

### Task 6.2: Create Generator Design Detail View
- [x] Create `GeneratorDesignDetailView` class
- [x] Implement 3D rotating preview section
- [x] Implement layer breakdown section
- [x] Implement material requirements section
- [x] Implement unlock requirements section
- [x] Add build button with requirement check
- [x] Use i18n keys for all text

### Task 6.3: Refactor Generator Layer Detail View
- [x] Refactor existing `GeneratorLayerDetailView`
- [x] Update to use new `GeneratorDesignLayer` entity
- [x] Add pattern visualization
- [x] Add material list with counts
- [x] Add navigation between layers
- [x] Use i18n keys for all text

### Task 6.4: Refactor Generator Materials View
- [x] Create/refactor `GeneratorMaterialsView`
- [x] Show all required materials
- [x] Show player inventory counts
- [x] Show missing materials highlighted
- [x] Add material gathering tips
- [x] Use i18n keys for all text

### Task 6.5: Create Generator Visualization 3D View
- [x] Create `GeneratorVisualization3DView` class
- [x] Implement interactive 3D preview
- [x] Add rotation controls (left/right arrows)
- [x] Add zoom controls
- [x] Add layer toggle
- [x] Add perspective switching
- [x] Use i18n keys for all text

### Task 6.6: Create Generator Build Progress View
- [x] Create `GeneratorBuildProgressView` class
- [x] Show overall build progress
- [x] Show current layer progress
- [x] Show materials consumed
- [x] Add cancel button
- [x] Use i18n keys for all text

### Task 6.7: Refactor Animated Generator Structure View
- [x] Refactor existing `AnimatedGeneratorStructureView`
- [x] Update to use new entities
- [x] Add smooth layer transitions
- [x] Add material highlighting
- [x] Use i18n keys for all text

## Phase 7: i18n Integration

### Task 7.1: Create Translation Keys
- [x] Add `generator.browser.*` keys
- [x] Add `generator.design.<type>.*` keys for all 10 types
- [x] Add `generator.layer.*` keys
- [x] Add `generator.requirement.*` keys
- [x] Add `generator.build.*` keys
- [x] Add `generator.error.*` keys

### Task 7.2: Update en_US.yml
- [x] Add all generator browser translations
- [x] Add all design name/description translations
- [x] Add all layer translations
- [x] Add all requirement translations
- [x] Add all build process translations

### Task 7.3: Update de_DE.yml
- [x] Add German translations for all keys

## Phase 8: Configuration & Commands

### Task 8.1: Create Configuration Files
- [x] Create `generator-structures.yml` configuration file
- [x] Add build settings section
- [x] Add detection settings section
- [x] Add per-design configuration sections
- [x] Add particle effect configurations

### Task 8.2: Create Admin Commands
- [x] Create `/generator reload` command
- [x] Create `/generator list` command
- [x] Create `/generator info <design>` command
- [x] Create `/generator enable <design>` command
- [x] Create `/generator disable <design>` command
- [x] Create `/generator give <player> <design>` command (bypass requirements)

### Task 8.3: Create Player Commands
- [x] Create `/generator browse` command (opens browser view)
- [x] Create `/generator build <design>` command
- [x] Create `/generator cancel` command
- [x] Create `/generator status` command

## Phase 9: Integration & Migration

### Task 9.1: Integrate with Evolution System
- [x] Add generator unlock checks to evolution progression
- [x] Add generator rewards to evolution milestones
- [x] Update evolution views to show generator unlocks

### Task 9.2: Migrate Existing Data
- [x] Create migration script for existing `CobblestoneGenerator` data
- [x] Map old `CobblestoneGeneratorType` to new `GeneratorDesignType`
- [x] Preserve player statistics and upgrades
- [x] Add rollback capability

### Task 9.3: Deprecate Old System
- [x] Mark `CobblestoneGeneratorType` as deprecated
- [x] Mark old `GeneratorStructure` as deprecated
- [x] Mark old `GeneratorStructureRegistry` as deprecated
- [x] Update all references to use new system
- [ ] Mark old `GeneratorStructure` as deprecated
- [ ] Mark old `GeneratorStructureRegistry` as deprecated
- [ ] Update all references to use new system

## Phase 10: Testing & Polish

### Task 10.1: Unit Tests
- [ ] Test requirement checking logic
- [ ] Test structure validation logic
- [ ] Test build process logic
- [ ] Test design registry

### Task 10.2: Integration Tests
- [ ] Test full build workflow
- [ ] Test structure detection
- [ ] Test GUI navigation
- [ ] Test configuration loading

### Task 10.3: Performance Testing
- [ ] Test particle effect performance
- [ ] Test structure detection performance
- [ ] Test GUI rendering performance
- [ ] Optimize as needed

### Task 10.4: Documentation
- [ ] Add JavaDoc to all public classes
- [ ] Create admin documentation
- [ ] Create player guide
- [ ] Update README

## Completion Checklist

- [ ] All 10 generator designs implemented and tested
- [ ] All GUI views working with i18n
- [ ] All services properly integrated
- [ ] Configuration system working
- [ ] Migration from old system complete
- [ ] Performance optimized
- [ ] Documentation complete
