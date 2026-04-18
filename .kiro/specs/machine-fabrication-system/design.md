# Machine Fabrication System - Design Document

## Overview

The Machine Fabrication System provides an automated crafting framework centered around the Fabricator machine. Players construct multi-block structures, configure crafting recipes, and automate resource processing through a permission-gated, upgradeable machine system. The design leverages existing RDQ infrastructure including JEHibernate for persistence, R18n for internationalization, the Inventory Framework for GUIs, and the perk/requirement system for progression gating.

## Architecture

### High-Level Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                     Machine System Layer                     │
├─────────────────────────────────────────────────────────────┤
│  MachineManager  │  MachineFactory  │  MachineRegistry     │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌──────▼──────┐  ┌────────▼────────┐
│   Fabricator   │  │   Storage   │  │    Upgrade      │
│   Component    │  │  Component  │  │   Component     │
└────────────────┘  └─────────────┘  └─────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                    Persistence Layer                         │
├─────────────────────────────────────────────────────────────┤
│  MachineRepository  │  MachineCache  │  Entity Models       │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                      View Layer (GUIs)                       │
├─────────────────────────────────────────────────────────────┤
│  MachineMainView  │  StorageView  │  UpgradeView  │ etc.   │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.raindropcentral.rdq.machine/
├── IMachineService.java                    # Service interface
├── MachineService.java                     # Service implementation
├── MachineManager.java                     # Core manager singleton
├── MachineFactory.java                     # Machine instance factory
├── MachineRegistry.java                    # Active machine registry
│
├── config/
│   ├── MachineSystemSection.java           # System-wide config
│   ├── FabricatorSection.java              # Fabricator-specific config
│   ├── MachineStructureSection.java        # Multi-block structure config
│   ├── MachineUpgradeSection.java          # Upgrade definitions
│   └── MachineFuelSection.java             # Fuel type definitions
│
├── entity/
│   ├── Machine.java                        # Base machine entity
│   ├── MachineStorage.java                 # Storage entry entity
│   ├── MachineUpgrade.java                 # Upgrade entry entity
│   ├── MachineTrust.java                   # Trust entry entity
│   └── MachineRecipe.java                  # Recipe configuration entity
│
├── repository/
│   ├── MachineRepository.java              # Machine data access
│   ├── MachineStorageRepository.java       # Storage data access
│   └── MachineCache.java                   # In-memory cache
│
├── component/
│   ├── FabricatorComponent.java            # Fabricator logic
│   ├── StorageComponent.java               # Storage management
│   ├── UpgradeComponent.java               # Upgrade system
│   ├── FuelComponent.java                  # Fuel management
│   ├── RecipeComponent.java                # Recipe handling
│   └── TrustComponent.java                 # Trust/security
│
├── structure/
│   ├── MultiBlockStructure.java            # Structure definition
│   ├── StructureValidator.java             # Structure validation
│   └── StructureDetector.java              # Structure detection
│
├── view/
│   ├── MachineMainView.java                # Main machine GUI
│   ├── MachineStorageView.java             # Storage management GUI
│   ├── MachineTrustView.java               # Trust management GUI
│   ├── MachineUpgradeView.java             # Upgrade GUI
│   └── MachineRecipeView.java              # Recipe configuration GUI
│
├── command/
│   └── MachineCommand.java                 # /rq machine commands
│
├── listener/
│   ├── MachineBlockListener.java           # Block place/break events
│   ├── MachineInteractListener.java        # Player interaction events
│   └── MachineChunkListener.java           # Chunk load/unload events
│
├── task/
│   ├── MachineCraftingTask.java            # Async crafting task
│   └── MachineAutoSaveTask.java            # Periodic save task
│
└── type/
    ├── EMachineType.java                   # Machine type enum
    ├── EMachineState.java                  # Machine state enum
    ├── EUpgradeType.java                   # Upgrade type enum
    └── EStorageType.java                   # Storage type enum
```

## Components and Interfaces

### 1. IMachineService Interface

```java
public interface IMachineService {
    // Machine lifecycle
    CompletableFuture<Machine> createMachine(UUID ownerUuid, EMachineType type, Location location);
    CompletableFuture<Boolean> deleteMachine(Long machineId);
    CompletableFuture<Machine> getMachine(Long machineId);
    CompletableFuture<List<Machine>> getPlayerMachines(UUID playerUuid);
    
    // Machine operations
    CompletableFuture<Boolean> toggleMachine(Long machineId, boolean enabled);
    CompletableFuture<Boolean> setRecipe(Long machineId, ItemStack[] recipe);
    CompletableFuture<Boolean> addFuel(Long machineId, int amount);
    
    // Storage operations
    CompletableFuture<Boolean> depositItems(Long machineId, ItemStack item);
    CompletableFuture<ItemStack> withdrawItems(Long machineId, Material material, int amount);
    CompletableFuture<Map<Material, Integer>> getStorageContents(Long machineId);
    
    // Trust operations
    CompletableFuture<Boolean> addTrustedPlayer(Long machineId, UUID playerUuid);
    CompletableFuture<Boolean> removeTrustedPlayer(Long machineId, UUID playerUuid);
    CompletableFuture<List<UUID>> getTrustedPlayers(Long machineId);
    
    // Upgrade operations
    CompletableFuture<Boolean> applyUpgrade(Long machineId, EUpgradeType type);
    CompletableFuture<Map<EUpgradeType, Integer>> getUpgrades(Long machineId);
    
    // Validation
    boolean canInteract(Player player, Machine machine);
    boolean hasPermission(Player player, EMachineType type);
}
```

### 2. MachineManager - Core Orchestrator

**Responsibilities:**
- Manages active machine instances in memory
- Coordinates between components
- Handles machine lifecycle events
- Provides centralized access to machine operations

**Key Methods:**
```java
public class MachineManager {
    private final MachineCache cache;
    private final MachineFactory factory;
    private final MachineRegistry registry;
    
    public Machine getActiveMachine(Location location);
    public void registerMachine(Machine machine);
    public void unregisterMachine(Long machineId);
    public void startCraftingCycle(Machine machine);
    public void stopCraftingCycle(Machine machine);
}
```

### 3. Component System

Each component handles a specific aspect of machine functionality:

#### FabricatorComponent
- Manages crafting logic
- Validates recipes
- Executes crafting cycles
- Applies upgrade modifiers

#### StorageComponent
- Manages virtual storage
- Handles item deposits/withdrawals
- Tracks storage capacity
- Integrates with physical storage blocks

#### UpgradeComponent
- Validates upgrade requirements
- Applies upgrade effects
- Calculates performance modifiers
- Persists upgrade state

#### FuelComponent
- Tracks fuel levels
- Validates fuel types
- Calculates consumption rates
- Applies efficiency modifiers

#### RecipeComponent
- Validates crafting recipes
- Locks/unlocks recipe configuration
- Matches recipes against Minecraft crafting system
- Stores recipe data

#### TrustComponent
- Manages trust list
- Validates player permissions
- Handles owner operations
- Enforces security rules

## Data Models

### Machine Entity

```java
@Entity
@Table(name = "rdq_machines")
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID ownerUuid;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EMachineType machineType;
    
    @Column(nullable = false, length = 50)
    private String world;
    
    @Column(nullable = false)
    private Integer x;
    
    @Column(nullable = false)
    private Integer y;
    
    @Column(nullable = false)
    private Integer z;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EMachineState state;
    
    @Column(nullable = false)
    private Integer fuelLevel = 0;
    
    @Column(columnDefinition = "TEXT")
    private String recipeData; // JSON serialized recipe
    
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MachineStorage> storage = new ArrayList<>();
    
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MachineUpgrade> upgrades = new ArrayList<>();
    
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MachineTrust> trustedPlayers = new ArrayList<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    
    // Business methods
    public boolean isActive() { return state == EMachineState.ACTIVE; }
    public Location getLocation() { /* construct from world, x, y, z */ }
    public boolean isTrusted(UUID playerUuid) { /* check trust list */ }
    public int getUpgradeLevel(EUpgradeType type) { /* get upgrade level */ }
}
```

### MachineStorage Entity

```java
@Entity
@Table(name = "rdq_machine_storage")
public class MachineStorage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String itemData; // Serialized ItemStack
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EStorageType storageType; // INPUT, OUTPUT, FUEL
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
```

### MachineUpgrade Entity

```java
@Entity
@Table(name = "rdq_machine_upgrades")
public class MachineUpgrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;
    
    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EUpgradeType upgradeType;
    
    @Column(nullable = false)
    private Integer level = 1;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant appliedAt;
}
```

### MachineTrust Entity

```java
@Entity
@Table(name = "rdq_machine_trust")
public class MachineTrust {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;
    
    @Column(nullable = false)
    private UUID trustedUuid;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant grantedAt;
}
```

## Configuration Structure

### machines.yml (System-Wide)

```yaml
machine-system:
  enabled: true
  cache:
    enabled: true
    auto-save-interval: 300 # seconds
    max-machines-per-player: 10
  
  permissions:
    require-permission: true
    base-permission: "rdq.machine"
  
  breaking:
    drop-items: false # Keep items in virtual storage
    drop-machine-item: true
    require-owner: false # Allow trusted players to break
```

### fabricator.yml (Fabricator-Specific)

```yaml
fabricator:
  enabled: true
  permission: "rdq.machine.fabricator"
  
  # Multi-block structure definition
  structure:
    core-block: DROPPER
    required-blocks:
      - type: HOPPER
        relative-positions:
          - {x: 0, y: -1, z: 0}  # Below core
          - {x: 1, y: 0, z: 0}   # East
          - {x: -1, y: 0, z: 0}  # West
      - type: CHEST
        relative-positions:
          - {x: 0, y: 0, z: 1}   # South (output)
  
  # Blueprint requirements
  blueprint:
    requirements:
      currency:
        type: "currency"
        currency-type: "vault"
        currency-amount: 10000.0
      items:
        type: "item"
        required-items:
          diamond:
            material: DIAMOND
            amount: 16
          redstone:
            material: REDSTONE_BLOCK
            amount: 4
  
  # Crafting configuration
  crafting:
    base-cooldown-ticks: 100 # 5 seconds
    recipe-grid-size: 3 # 3x3 grid
    max-output-stack-size: 64
  
  # Fuel system
  fuel:
    enabled: true
    base-consumption: 10 # Per craft
    fuel-types:
      coal:
        material: COAL
        energy-value: 100
      coal-block:
        material: COAL_BLOCK
        energy-value: 900
      lava-bucket:
        material: LAVA_BUCKET
        energy-value: 2000
  
  # Upgrade system
  upgrades:
    speed:
      max-level: 5
      effect-per-level: 0.10 # 10% faster per level
      requirements:
        level-1:
          type: "composite"
          operator: "AND"
          sub-requirements:
            currency:
              type: "currency"
              currency-type: "vault"
              currency-amount: 5000.0
            items:
              type: "item"
              required-items:
                redstone:
                  material: REDSTONE
                  amount: 32
    
    efficiency:
      max-level: 5
      effect-per-level: 0.15 # 15% chance to not consume fuel
      requirements:
        level-1:
          type: "item"
          required-items:
            diamond:
              material: DIAMOND
              amount: 8
    
    bonus-output:
      max-level: 3
      effect-per-level: 0.10 # 10% chance for double output
      requirements:
        level-1:
          type: "item"
          required-items:
            emerald:
              material: EMERALD
              amount: 16
    
    fuel-reduction:
      max-level: 5
      effect-per-level: 0.10 # 10% less fuel consumption
      requirements:
        level-1:
          type: "item"
          required-items:
            gold-ingot:
              material: GOLD_INGOT
              amount: 16
```

## Multi-Block Structure System

### Structure Detection Algorithm

```
1. Player places block at location L
2. Check if block type matches any core block type in configurations
3. If match found:
   a. Get structure definition for that machine type
   b. For each required block in structure:
      - Calculate absolute position from relative offset
      - Verify block at position matches required type
   c. If all blocks match:
      - Validate player has permission
      - Validate blueprint requirements
      - Create machine instance
      - Register in database and cache
      - Display success message
   d. If validation fails:
      - Display specific error message
      - Do not create machine
```

### Structure Validation

```java
public class StructureValidator {
    public ValidationResult validate(Location coreLocation, MachineStructureSection structure) {
        for (RequiredBlock required : structure.getRequiredBlocks()) {
            for (RelativePosition offset : required.getRelativePositions()) {
                Location checkLoc = coreLocation.clone().add(offset.x, offset.y, offset.z);
                Block block = checkLoc.getBlock();
                
                if (block.getType() != required.getType()) {
                    return ValidationResult.failure(
                        "Invalid block at " + formatLocation(checkLoc) +
                        ". Expected: " + required.getType() +
                        ", Found: " + block.getType()
                    );
                }
            }
        }
        return ValidationResult.success();
    }
}
```

## Crafting Cycle System

### Crafting Task Flow

```
┌─────────────────────────────────────────────────────────┐
│              Machine State: ACTIVE                       │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│  1. Check if recipe is set and valid                    │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│  2. Check if sufficient fuel exists                     │
│     - Calculate fuel cost with upgrades                 │
│     - If insufficient, disable machine                  │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│  3. Search storage for recipe ingredients               │
│     - Check virtual storage                             │
│     - Check attached physical storage                   │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│  4. If all ingredients available:                       │
│     - Consume ingredients from storage                  │
│     - Consume fuel (with efficiency upgrade chance)     │
│     - Calculate output (with bonus output chance)       │
│     - Add output to storage                             │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│  5. Apply cooldown with speed upgrade modifier          │
│     - Schedule next cycle                               │
└─────────────────────────────────────────────────────────┘
```

### MachineCraftingTask Implementation

```java
public class MachineCraftingTask extends BukkitRunnable {
    private final Machine machine;
    private final MachineManager manager;
    
    @Override
    public void run() {
        if (!machine.isActive()) {
            cancel();
            return;
        }
        
        // Async processing
        CompletableFuture.runAsync(() -> {
            CraftingResult result = processCrafting(machine);
            
            // Back to main thread for world operations
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    handleSuccessfulCraft(machine, result);
                } else {
                    handleFailedCraft(machine, result);
                }
            });
        });
    }
    
    private CraftingResult processCrafting(Machine machine) {
        // 1. Validate recipe
        // 2. Check fuel
        // 3. Check ingredients
        // 4. Consume resources
        // 5. Generate output
        // 6. Apply upgrades
        return result;
    }
}
```

## GUI System Design

### View Hierarchy

```
MachineMainView (BaseView)
├── Status Display
│   ├── Machine Type Icon
│   ├── State Toggle (ON/OFF)
│   ├── Fuel Level Bar
│   └── Recipe Preview
├── Navigation Buttons
│   ├── Storage Management → MachineStorageView
│   ├── Trust Management → MachineTrustView
│   ├── Upgrade System → MachineUpgradeView
│   └── Recipe Config → MachineRecipeView
└── Back Button (auto-placed)
```

### MachineMainView Layout

```java
@Override
protected String[] getLayout() {
    return new String[]{
        "XXXXXXXXX",  // X = decoration
        "XsTfRuXX",   // s=state, T=type, f=fuel, R=recipe, u=upgrades
        "XXXXXXXXX",
        "XnNNNNnX",   // n=navigation buttons
        "         "   // Back button auto-placed
    };
}
```

### Translation Structure

```yaml
view:
  machine:
    main:
      title: "<gradient:#FF6B00:#FFD700>Fabricator Machine</gradient>"
      items:
        state:
          on:
            name: "<green>⚡ Machine: ON</green>"
            lore:
              - "<gray>Click to turn OFF</gray>"
          off:
            name: "<red>⭘ Machine: OFF</red>"
            lore:
              - "<gray>Click to turn ON</gray>"
        fuel:
          name: "<yellow>⛽ Fuel: {current}/{max}</yellow>"
          lore:
            - "<gray>Consumption: {rate} per craft</gray>"
            - ""
            - "<green>Click to add fuel</green>"
        recipe:
          set:
            name: "<aqua>📋 Recipe Set</aqua>"
            lore:
              - "<gray>Output: {output}</gray>"
              - ""
              - "<yellow>Click to view/change</yellow>"
          not-set:
            name: "<red>📋 No Recipe</red>"
            lore:
              - "<gray>Click to configure</gray>
        storage:
          name: "<blue>📦 Storage</blue>"
          lore:
            - "<gray>Items: {count}</gray>"
            - ""
            - "<green>Click to manage</green>"
        trust:
          name: "<light_purple>👥 Trust List</light_purple>"
          lore:
            - "<gray>Trusted: {count} players</gray>"
            - ""
            - "<green>Click to manage</green>"
        upgrades:
          name: "<gold>⬆ Upgrades</gold>"
          lore:
            - "<gray>Speed: Level {speed}</gray>"
            - "<gray>Efficiency: Level {efficiency}</gray>"
            - "<gray>Bonus Output: Level {bonus}</gray>"
            - "<gray>Fuel Reduction: Level {fuel}</gray>"
            - ""
            - "<green>Click to upgrade</green>"
```

## Error Handling

### Validation Errors

```java
public enum MachineError {
    INSUFFICIENT_PERMISSION("error.machine.permission"),
    INVALID_STRUCTURE("error.machine.structure"),
    INSUFFICIENT_RESOURCES("error.machine.resources"),
    MACHINE_NOT_FOUND("error.machine.not-found"),
    NOT_OWNER("error.machine.not-owner"),
    NOT_TRUSTED("error.machine.not-trusted"),
    INVALID_RECIPE("error.machine.recipe.invalid"),
    INSUFFICIENT_FUEL("error.machine.fuel.insufficient"),
    INSUFFICIENT_MATERIALS("error.machine.materials.insufficient"),
    STORAGE_FULL("error.machine.storage.full"),
    UPGRADE_MAX_LEVEL("error.machine.upgrade.max-level"),
    UPGRADE_REQUIREMENTS("error.machine.upgrade.requirements");
    
    private final String translationKey;
    
    public void send(Player player, Object... placeholders) {
        r18n.message(translationKey)
            .placeholders(placeholders)
            .withPrefix()
            .send(player);
    }
}
```

## Testing Strategy

### Unit Tests

1. **Structure Validation Tests**
   - Valid structure detection
   - Invalid structure rejection
   - Edge cases (missing blocks, wrong types)

2. **Recipe Validation Tests**
   - Valid recipe detection
   - Invalid recipe rejection
   - Shapeless vs shaped recipes

3. **Upgrade Calculation Tests**
   - Speed modifier calculations
   - Efficiency probability tests
   - Bonus output probability tests
   - Fuel reduction calculations

4. **Storage Management Tests**
   - Deposit operations
   - Withdrawal operations
   - Capacity limits
   - Item stacking

### Integration Tests

1. **Complete Crafting Cycle**
   - Build machine → Configure recipe → Add fuel → Add materials → Enable → Verify output

2. **Trust System**
   - Owner operations
   - Trusted player operations
   - Untrusted player rejection

3. **Database Persistence**
   - Save machine state
   - Load machine state
   - Update operations
   - Delete operations

### Performance Tests

1. **Multiple Active Machines**
   - 10+ machines crafting simultaneously
   - Memory usage monitoring
   - CPU usage monitoring

2. **Large Storage Operations**
   - 1000+ item stacks in storage
   - Bulk deposit/withdrawal
   - Search performance

## Migration and Deployment

### Database Migration

```sql
-- Create tables
CREATE TABLE rdq_machines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_uuid BINARY(16) NOT NULL,
    machine_type VARCHAR(50) NOT NULL,
    world VARCHAR(50) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    state VARCHAR(20) NOT NULL,
    fuel_level INT NOT NULL DEFAULT 0,
    recipe_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_uuid),
    INDEX idx_location (world, x, y, z)
);

CREATE TABLE rdq_machine_storage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    item_data TEXT NOT NULL,
    quantity INT NOT NULL,
    storage_type VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (machine_id) REFERENCES rdq_machines(id) ON DELETE CASCADE,
    INDEX idx_machine (machine_id)
);

CREATE TABLE rdq_machine_upgrades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    upgrade_type VARCHAR(30) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (machine_id) REFERENCES rdq_machines(id) ON DELETE CASCADE,
    UNIQUE KEY uk_machine_upgrade (machine_id, upgrade_type)
);

CREATE TABLE rdq_machine_trust (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT NOT NULL,
    trusted_uuid BINARY(16) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (machine_id) REFERENCES rdq_machines(id) ON DELETE CASCADE,
    UNIQUE KEY uk_machine_trust (machine_id, trusted_uuid)
);
```

### Deployment Steps

1. Add configuration files to server
2. Run database migrations
3. Restart server to load machine system
4. Grant permissions to players/groups
5. Announce feature to players with tutorial

## Future Enhancements

### JExWorkbench Integration

When JExWorkbench is implemented:

1. Add configuration option for grid size
2. Update RecipeComponent to support variable grid sizes
3. Update MachineRecipeView to render larger grids
4. Add validation for JExWorkbench recipes
5. Support custom crafting systems

### Additional Machine Types

Framework is designed to support additional machine types:

1. Create new machine type enum value
2. Create machine-specific configuration file
3. Implement machine-specific component
4. Add machine-specific views
5. Register in MachineFactory

### Performance Optimizations

1. Implement chunk-based machine loading
2. Add machine sleep mode for inactive machines
3. Batch database operations
4. Optimize storage search algorithms
5. Add caching layers for frequently accessed data
