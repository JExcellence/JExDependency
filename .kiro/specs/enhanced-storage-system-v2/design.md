# Enhanced Storage System V2 - Technical Design

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Enhanced Storage System V2                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   Config Layer  │  │  Service Layer  │  │    View Layer   │              │
│  │                 │  │                 │  │                 │              │
│  │ StorageConfig   │  │ StructureManager│  │ StorageBrowser  │              │
│  │ DesignConfig    │  │ DesignService   │  │ DesignDetailView│              │
│  │ UpgradeConfig   │  │ BuildService    │  │ InventoryView   │              │
│  │                 │  │ DetectionService│  │ UpgradeView     │              │
│  │                 │  │ InventoryService│  │ Visualization3D │              │
│  │                 │  │ UpgradeService  │  │ ManagementView  │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                    │                        │
│  ┌────────▼────────────────────▼────────────────────▼────────┐              │
│  │                      Repository Layer                      │              │
│  │                                                            │              │
│  │  StorageDesignRepository    │  PlayerStorageRepository    │              │
│  │  StorageInventoryRepository │  StorageUpgradeRepository   │              │
│  └────────────────────────────────────────────────────────────┘              │
│                                │                                             │
│  ┌────────────────────────────▼────────────────────────────────┐            │
│  │                       Entity Layer                           │            │
│  │                                                              │            │
│  │  StorageDesign ──┬── StorageDesignLayer                     │            │
│  │                  ├── StorageDesignMaterial                  │            │
│  │                  ├── StorageDesignRequirement               │            │
│  │                  └── StorageDesignReward                    │            │
│  │                                                              │            │
│  │  PlayerStorageStructure ──┬── StorageInventory              │            │
│  │                           └── StorageUpgrade                │            │
│  └──────────────────────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Package Structure

### RPlatform Requirement System Integration
Uses existing RPlatform requirement system:
- `Requirement` interface (sealed)
- `AbstractRequirement` base class
- `RequirementEntity` JPA wrapper
- `RequirementConverter` JSON converter

### JExOneblock Storage Package
```
de.jexcellence.oneblock.storage/
├── config/
│   ├── StorageSystemConfig.java
│   ├── StorageDesignSection.java
│   ├── StorageUpgradeSection.java
│   └── StorageEffectsSection.java
├── database/
│   ├── entity/
│   │   ├── StorageDesign.java
│   │   ├── StorageDesignLayer.java
│   │   ├── StorageDesignMaterial.java
│   │   ├── StorageDesignRequirement.java (uses RPlatform RequirementEntity)
│   │   ├── StorageDesignReward.java
│   │   ├── PlayerStorageStructure.java
│   │   ├── StorageInventory.java
│   │   └── StorageUpgrade.java
│   ├── repository/
│   │   ├── StorageDesignRepository.java
│   │   ├── StorageDesignLayerRepository.java
│   │   ├── PlayerStorageStructureRepository.java
│   │   ├── StorageInventoryRepository.java
│   │   └── StorageUpgradeRepository.java
│   └── converter/
│       ├── MaterialPatternConverter.java
│       ├── ItemStackArrayConverter.java
│       └── CompressedItemDataConverter.java
├── design/
│   ├── EStorageDesignType.java (enum for 8 types)
│   ├── EStorageCategory.java (enum: GENERAL, ORE, CROP, MOB, CURRENCY)
│   ├── StorageDesignRegistry.java
│   └── designs/
│       ├── BasicCrateDesign.java
│       ├── IronVaultDesign.java
│       ├── CrystalRepositoryDesign.java
│       ├── MechanicalWarehouseDesign.java
│       ├── DimensionalCacheDesign.java
│       ├── NetherVaultDesign.java
│       ├── EndArchiveDesign.java
│       └── CelestialTreasuryDesign.java
├── requirement/ (OneBlock-specific, extends RPlatform)
│   ├── StorageTierRequirement.java (extends AbstractRequirement)
│   ├── StorageCountRequirement.java (extends AbstractRequirement)
│   └── OneBlockStorageRequirementProvider.java (implements PluginRequirementProvider)
├── service/
│   ├── StorageStructureManager.java
│   ├── StorageDesignService.java
│   ├── StorageDetectionService.java
│   ├── StorageBuildService.java
│   ├── StorageVisualizationService.java
│   ├── StorageInventoryService.java
│   ├── StorageUpgradeService.java
│   ├── StorageCompressionService.java
│   ├── StorageAutomationService.java
│   └── StorageRequirementService.java (uses RPlatform RequirementService)
├── upgrade/
│   ├── EStorageUpgradeType.java (enum)
│   ├── StorageUpgradeHandler.java
│   └── upgrades/
│       ├── CapacityUpgradeHandler.java
│       ├── SortingUpgradeHandler.java
│       ├── FilterUpgradeHandler.java
│       ├── CompressionUpgradeHandler.java
│       ├── AutomationUpgradeHandler.java
│       ├── SpeedUpgradeHandler.java
│       └── ProtectionUpgradeHandler.java
├── visualization/
│   ├── ParticleEffectManager.java
│   ├── StorageVisualization3D.java
│   ├── StorageBuilder.java
│   ├── BuildProgressTracker.java
│   └── effects/
│       ├── StorageParticleEffect.java
│       ├── BuildParticleEffect.java
│       ├── ValidationParticleEffect.java
│       └── IdleParticleEffect.java
└── view/
    ├── StorageBrowserView.java
    ├── StorageDesignDetailView.java
    ├── StorageLayerDetailView.java
    ├── StorageMaterialsView.java
    ├── StorageVisualization3DView.java
    ├── StorageBuildProgressView.java
    ├── StorageInventoryView.java
    ├── StorageUpgradeView.java
    ├── StorageManagementView.java
    └── AnimatedStorageStructureView.java
```

## Database Entity Design

### StorageDesign Entity
```java
@Entity
@Table(name = "oneblock_storage_designs")
public class StorageDesign extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "design_key", unique = true, nullable = false)
    private String designKey; // e.g., "basic_crate", "iron_vault"
    
    @Column(name = "name_key", nullable = false)
    private String nameKey; // i18n key: "storage.design.basic_crate.name"
    
    @Column(name = "description_key", nullable = false)
    private String descriptionKey; // i18n key
    
    @Enumerated(EnumType.STRING)
    @Column(name = "design_type", nullable = false)
    private EStorageDesignType designType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private EStorageCategory category;
    
    @Column(name = "tier", nullable = false)
    private Integer tier; // 1-8 progression tier
    
    @Column(name = "base_capacity", nullable = false)
    private Integer baseCapacity; // Base slot count (27, 54, 81, etc.)
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "icon_material", nullable = false)
    private String iconMaterial; // Material for GUI display
    
    @Column(name = "particle_effect")
    private String particleEffect; // Custom particle effect key
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("layerIndex ASC")
    private List<StorageDesignLayer> layers = new ArrayList<>();
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageDesignRequirement> requirements = new ArrayList<>();
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageDesignReward> rewards = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
}
```

### StorageDesignLayer Entity
```java
@Entity
@Table(name = "oneblock_storage_design_layers")
public class StorageDesignLayer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private StorageDesign design;
    
    @Column(name = "layer_index", nullable = false)
    private Integer layerIndex;
    
    @Column(name = "name_key", nullable = false)
    private String nameKey; // i18n key
    
    @Column(name = "width", nullable = false)
    private Integer width;
    
    @Column(name = "depth", nullable = false)
    private Integer depth;
    
    @Column(name = "pattern", columnDefinition = "LONGTEXT", nullable = false)
    @Convert(converter = MaterialPatternConverter.java)
    private Material[][] pattern;
    
    @Column(name = "access_offset_x")
    private Integer accessOffsetX; // Where player interacts
    
    @Column(name = "access_offset_z")
    private Integer accessOffsetZ;
    
    @OneToMany(mappedBy = "layer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageDesignMaterial> materials = new ArrayList<>();
}
```

### StorageDesignRequirement Entity
```java
@Entity
@Table(name = "oneblock_storage_design_requirements")
public class StorageDesignRequirement extends RequirementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private StorageDesign design;
    
    // Inherits requirement, icon, displayOrder from RequirementEntity
}
```

### PlayerStorageStructure Entity
```java
@Entity
@Table(name = "oneblock_player_storage_structures")
public class PlayerStorageStructure extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "island_id", nullable = false)
    private Long islandId;
    
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private StorageDesign design;
    
    // Location fields
    @Column(name = "world_name")
    private String worldName;
    
    @Column(name = "location_x")
    private Integer locationX;
    
    @Column(name = "location_y")
    private Integer locationY;
    
    @Column(name = "location_z")
    private Integer locationZ;
    
    // Access location (where player interacts)
    @Column(name = "access_x")
    private Integer accessX;
    
    @Column(name = "access_y")
    private Integer accessY;
    
    @Column(name = "access_z")
    private Integer accessZ;
    
    // Status
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;
    
    // Capacity
    @Column(name = "current_capacity", nullable = false)
    private Integer currentCapacity; // With upgrades applied
    
    @Column(name = "items_stored", nullable = false)
    private Integer itemsStored = 0;
    
    // Statistics
    @Column(name = "total_items_stored", nullable = false)
    private Long totalItemsStored = 0L;
    
    @Column(name = "total_items_retrieved", nullable = false)
    private Long totalItemsRetrieved = 0L;
    
    // Upgrades
    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageUpgrade> upgrades = new ArrayList<>();
    
    // Inventory
    @OneToOne(mappedBy = "structure", cascade = CascadeType.ALL, orphanRemoval = true)
    private StorageInventory inventory;
    
    // Timestamps
    @Column(name = "built_at")
    private Long builtAt;
    
    @Column(name = "last_accessed")
    private Long lastAccessed;
}
```

### StorageInventory Entity
```java
@Entity
@Table(name = "oneblock_storage_inventories")
public class StorageInventory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    private PlayerStorageStructure structure;
    
    @Column(name = "inventory_data", columnDefinition = "LONGTEXT", nullable = false)
    @Convert(converter = ItemStackArrayConverter.class)
    private ItemStack[] items;
    
    @Column(name = "compressed_data", columnDefinition = "LONGTEXT")
    @Convert(converter = CompressedItemDataConverter.class)
    private Map<Material, Long> compressedItems; // Material -> count for compression
    
    @Column(name = "filter_whitelist", columnDefinition = "TEXT")
    private String filterWhitelist; // JSON array of allowed materials
    
    @Column(name = "filter_blacklist", columnDefinition = "TEXT")
    private String filterBlacklist; // JSON array of blocked materials
    
    @Column(name = "auto_sort_enabled", nullable = false)
    private Boolean autoSortEnabled = false;
    
    @Column(name = "compression_enabled", nullable = false)
    private Boolean compressionEnabled = false;
    
    @Column(name = "last_sorted")
    private Long lastSorted;
    
    @Column(name = "last_compressed")
    private Long lastCompressed;
}
```

### StorageUpgrade Entity
```java
@Entity
@Table(name = "oneblock_storage_upgrades")
public class StorageUpgrade extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    private PlayerStorageStructure structure;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "upgrade_type", nullable = false)
    private EStorageUpgradeType upgradeType;
    
    @Column(name = "level", nullable = false)
    private Integer level; // Current upgrade level
    
    @Column(name = "max_level", nullable = false)
    private Integer maxLevel; // Maximum level for this upgrade
    
    @Column(name = "applied_at", nullable = false)
    private Long appliedAt;
    
    @Column(name = "last_upgraded")
    private Long lastUpgraded;
}
```

## Storage Design Types

### EStorageDesignType Enum
```java
public enum EStorageDesignType {
    BASIC_CRATE("basic_crate", 1, 27, Material.CHEST),
    IRON_VAULT("iron_vault", 2, 54, Material.IRON_BLOCK),
    CRYSTAL_REPOSITORY("crystal_repository", 3, 81, Material.AMETHYST_BLOCK),
    MECHANICAL_WAREHOUSE("mechanical_warehouse", 4, 108, Material.PISTON),
    DIMENSIONAL_CACHE("dimensional_cache", 5, 135, Material.ENDER_CHEST),
    NETHER_VAULT("nether_vault", 6, 162, Material.NETHERITE_BLOCK),
    END_ARCHIVE("end_archive", 7, 189, Material.END_PORTAL_FRAME),
    CELESTIAL_TREASURY("celestial_treasury", 8, 216, Material.BEACON);
    
    private final String key;
    private final int tier;
    private final int baseCapacity;
    private final Material icon;
}
```

### EStorageCategory Enum
```java
public enum EStorageCategory {
    GENERAL("general", "Stores any item type"),
    ORE("ore", "Specialized for ores with auto-smelting"),
    CROP("crop", "Specialized for crops with auto-composting"),
    MOB("mob", "Specialized for mob drops with sorting"),
    CURRENCY("currency", "Stores currency items with conversion");
    
    private final String key;
    private final String description;
}
```

### EStorageUpgradeType Enum
```java
public enum EStorageUpgradeType {
    CAPACITY("capacity", 5, "Increases storage slots"),
    SORTING("sorting", 3, "Auto-sorts items by type"),
    FILTER("filter", 3, "Whitelist/blacklist items"),
    COMPRESSION("compression", 3, "Compresses stackable items"),
    AUTOMATION("automation", 3, "Auto-collect nearby items"),
    SPEED("speed", 5, "Faster item processing"),
    PROTECTION("protection", 1, "Prevents item loss on death");
    
    private final String key;
    private final int maxLevel;
    private final String description;
}
```

## 8 Storage Design Specifications

### 1. Basic Crate (Tier 1)
- **Theme**: Simple wooden storage
- **Size**: 3x3x2
- **Materials**: Oak Planks, Chests, Barrels
- **Base Capacity**: 27 slots (single chest)
- **Core Mechanic**: Basic item storage
- **Requirements**: Evolution Level 1, 100 blocks broken
- **Rewards**: Unlocks storage system
- **Available Upgrades**: Capacity (up to 36 slots)

### 2. Iron Vault (Tier 2)
- **Theme**: Metal reinforced storage
- **Size**: 5x5x3
- **Materials**: Iron Blocks, Iron Bars, Anvils, Hoppers
- **Base Capacity**: 54 slots (double chest)
- **Core Mechanic**: Basic sorting by item type
- **Requirements**: Evolution Level 10, 1,000 blocks broken, Basic Crate built
- **Rewards**: Sorting upgrade unlocked
- **Available Upgrades**: Capacity (up to 72 slots), Sorting (Level 1)

### 3. Crystal Repository (Tier 3)
- **Theme**: Magical crystalline storage
- **Size**: 5x5x4
- **Materials**: Amethyst Blocks, Glass, Tinted Glass, Glowstone
- **Base Capacity**: 81 slots (3 chests)
- **Core Mechanic**: Item filtering system
- **Requirements**: Evolution Level 20, 5,000 blocks broken, Iron Vault built
- **Rewards**: Filter upgrade unlocked
- **Available Upgrades**: Capacity (up to 108 slots), Sorting (Level 2), Filter (Level 1)

### 4. Mechanical Warehouse (Tier 4)
- **Theme**: Automated redstone storage
- **Size**: 7x7x4
- **Materials**: Pistons, Observers, Redstone Blocks, Droppers, Hoppers
- **Base Capacity**: 108 slots (4 chests)
- **Core Mechanic**: Auto-sorting and organization
- **Requirements**: Evolution Level 30, 15,000 blocks broken, Crystal Repository built
- **Rewards**: Automation upgrade unlocked
- **Available Upgrades**: Capacity (up to 144 slots), Sorting (Level 3), Filter (Level 2), Automation (Level 1)

### 5. Dimensional Cache (Tier 5)
- **Theme**: Void-touched storage
- **Size**: 7x7x5
- **Materials**: Ender Chests, Obsidian, Crying Obsidian, Ender Pearls
- **Base Capacity**: 135 slots (5 chests)
- **Core Mechanic**: Item compression (10:1 ratio)
- **Requirements**: Evolution Level 50, 50,000 blocks broken, Prestige 1, Mechanical Warehouse built
- **Rewards**: Compression upgrade unlocked
- **Available Upgrades**: Capacity (up to 180 slots), Sorting (Level 3), Filter (Level 3), Automation (Level 2), Compression (Level 1)

### 6. Nether Vault (Tier 6)
- **Theme**: Fire-proof nether storage
- **Size**: 9x9x5
- **Materials**: Netherite Blocks, Blackstone, Soul Fire, Ancient Debris
- **Base Capacity**: 162 slots (6 chests)
- **Core Mechanic**: Auto-smelting integration
- **Requirements**: Evolution Level 75, 150,000 blocks broken, Prestige 3, Dimensional Cache built
- **Rewards**: Speed upgrade unlocked, fire immunity for stored items
- **Available Upgrades**: All previous + Speed (Level 1-3), Compression (Level 2)

### 7. End Archive (Tier 7)
- **Theme**: Teleportation-enabled storage
- **Size**: 9x9x6
- **Materials**: End Stone, Purpur Blocks, End Rods, Shulker Boxes
- **Base Capacity**: 189 slots (7 chests)
- **Core Mechanic**: Remote access from anywhere on island
- **Requirements**: Evolution Level 100, 500,000 blocks broken, Prestige 5, Nether Vault built
- **Rewards**: Remote access unlocked
- **Available Upgrades**: All previous + Speed (Level 4-5), Compression (Level 3), Protection

### 8. Celestial Treasury (Tier 8)
- **Theme**: Ultimate divine storage
- **Size**: 11x11x7
- **Materials**: Beacons, Diamond Blocks, Netherite Blocks, Emerald Blocks, Nether Stars
- **Base Capacity**: 216 slots (8 chests)
- **Core Mechanic**: All features combined + item duplication prevention
- **Requirements**: Evolution Level 150, 1,000,000 blocks broken, Prestige 10, End Archive built
- **Rewards**: All upgrades at maximum level
- **Available Upgrades**: All upgrades maxed out

## OneBlock-Specific Requirements

### StorageTierRequirement
```java
package de.jexcellence.oneblock.storage.requirement;

/**
 * Requires player to have built a previous storage tier.
 */
public class StorageTierRequirement extends AbstractRequirement {
    private final EStorageDesignType requiredTier;
    
    public StorageTierRequirement(EStorageDesignType requiredTier) {
        super(Type.CUSTOM);
        this.requiredTier = requiredTier;
    }
    
    @Override
    public boolean isMet(@NotNull Player player) {
        return storageService.hasBuiltDesign(player, requiredTier);
    }
    
    @Override
    public double calculateProgress(@NotNull Player player) {
        return storageService.hasBuiltDesign(player, requiredTier) ? 1.0 : 0.0;
    }
    
    @Override
    public void consume(@NotNull Player player) {
        // Storage tier is not consumed
    }
    
    @Override
    public @NotNull String getDescriptionKey() {
        return "storage.requirement.tier";
    }
}
```

### StorageCountRequirement
```java
/**
 * Requires player to have built a certain number of storage structures.
 */
public class StorageCountRequirement extends AbstractRequirement {
    private final int requiredCount;
    
    @Override
    public boolean isMet(@NotNull Player player) {
        return storageService.getStorageCount(player) >= requiredCount;
    }
    
    @Override
    public double calculateProgress(@NotNull Player player) {
        int current = storageService.getStorageCount(player);
        return Math.min(1.0, (double) current / requiredCount);
    }
}
```

### OneBlockStorageRequirementProvider
```java
package de.jexcellence.oneblock.storage.requirement;

/**
 * Registers OneBlock storage requirements with RPlatform.
 */
public class OneBlockStorageRequirementProvider implements PluginRequirementProvider {
    
    @Override
    public @NotNull String getPluginId() {
        return "jexoneblock_storage";
    }
    
    @Override
    public @NotNull List<String> getCustomTypes() {
        return List.of(
            "STORAGE_TIER",
            "STORAGE_COUNT"
        );
    }
    
    @Override
    public @Nullable AbstractRequirement createRequirement(@NotNull String typeName, @NotNull JsonNode data) {
        return switch (typeName) {
            case "STORAGE_TIER" -> new StorageTierRequirement(
                EStorageDesignType.valueOf(data.get("requiredTier").asText())
            );
            case "STORAGE_COUNT" -> new StorageCountRequirement(
                data.get("requiredCount").asInt()
            );
            default -> null;
        };
    }
}
```

## Service Design

### StorageStructureManager
Central manager coordinating all storage operations:
```java
public class StorageStructureManager {
    private final StorageDesignService designService;
    private final StorageDetectionService detectionService;
    private final StorageBuildService buildService;
    private final StorageVisualizationService visualizationService;
    private final StorageInventoryService inventoryService;
    private final StorageUpgradeService upgradeService;
    private final StorageRequirementService requirementService;
    private final StorageDesignRegistry designRegistry;
    
    // Initialization
    public void initialize();
    public void shutdown();
    public void reload();
    
    // Design operations
    public List<StorageDesign> getAvailableDesigns(Player player);
    public Optional<StorageDesign> getDesign(String designKey);
    public boolean canUnlock(Player player, StorageDesign design);
    
    // Structure operations
    public CompletableFuture<BuildResult> buildStructure(Player player, StorageDesign design, Location location);
    public CompletableFuture<Boolean> destroyStructure(PlayerStorageStructure structure);
    public CompletableFuture<ValidationResult> validateStructure(Location location);
    
    // Inventory operations
    public void openStorage(Player player, PlayerStorageStructure structure);
    public CompletableFuture<Boolean> storeItem(PlayerStorageStructure structure, ItemStack item);
    public CompletableFuture<ItemStack> retrieveItem(PlayerStorageStructure structure, int slot);
    
    // Upgrade operations
    public List<EStorageUpgradeType> getAvailableUpgrades(PlayerStorageStructure structure);
    public CompletableFuture<Boolean> applyUpgrade(Player player, PlayerStorageStructure structure, EStorageUpgradeType type);
    
    // Visualization
    public void showPreview(Player player, StorageDesign design, Location location);
    public void hidePreview(Player player);
}
```

### StorageInventoryService
Handles item storage and retrieval:
```java
public class StorageInventoryService {
    // Item operations
    public CompletableFuture<Boolean> storeItem(PlayerStorageStructure structure, ItemStack item);
    public CompletableFuture<ItemStack> retrieveItem(PlayerStorageStructure structure, int slot);
    public CompletableFuture<Boolean> transferItems(PlayerStorageStructure from, PlayerStorageStructure to, int[] slots);
    
    // Sorting operations
    public CompletableFuture<Void> sortInventory(PlayerStorageStructure structure);
    public CompletableFuture<Void> autoSort(PlayerStorageStructure structure);
    
    // Compression operations
    public CompletableFuture<Integer> compressItems(PlayerStorageStructure structure);
    public CompletableFuture<Integer> decompressItems(PlayerStorageStructure structure, Material material, int amount);
    
    // Filter operations
    public void setWhitelist(PlayerStorageStructure structure, List<Material> materials);
    public void setBlacklist(PlayerStorageStructure structure, List<Material> materials);
    public boolean isItemAllowed(PlayerStorageStructure structure, ItemStack item);
    
    // Search operations
    public List<Integer> findItems(PlayerStorageStructure structure, Material material);
    public Map<Material, Integer> getItemCounts(PlayerStorageStructure structure);
}
```

### StorageUpgradeService
Handles upgrade purchases and applications:
```java
public class StorageUpgradeService {
    // Upgrade availability
    public List<EStorageUpgradeType> getAvailableUpgrades(PlayerStorageStructure structure);
    public boolean canApplyUpgrade(Player player, PlayerStorageStructure structure, EStorageUpgradeType type);
    public int getUpgradeLevel(PlayerStorageStructure structure, EStorageUpgradeType type);
    public int getMaxUpgradeLevel(EStorageUpgradeType type);
    
    // Upgrade costs
    public List<ItemRequirement> getUpgradeMaterialCost(EStorageUpgradeType type, int level);
    public double getUpgradeCurrencyCost(EStorageUpgradeType type, int level);
    
    // Upgrade application
    public CompletableFuture<Boolean> applyUpgrade(Player player, PlayerStorageStructure structure, EStorageUpgradeType type);
    public CompletableFuture<Boolean> removeUpgrade(PlayerStorageStructure structure, EStorageUpgradeType type);
    
    // Upgrade effects
    public int calculateCapacity(PlayerStorageStructure structure);
    public double calculateProcessingSpeed(PlayerStorageStructure structure);
    public int calculateAutomationRange(PlayerStorageStructure structure);
}
```

### StorageCompressionService
Handles item compression/decompression:
```java
public class StorageCompressionService {
    // Compression operations
    public CompletableFuture<CompressionResult> compressItems(StorageInventory inventory);
    public CompletableFuture<DecompressionResult> decompressItems(StorageInventory inventory, Material material, int amount);
    
    // Compression info
    public boolean isCompressible(Material material);
    public int getCompressionRatio(Material material, int compressionLevel);
    public Map<Material, Long> getCompressedItems(StorageInventory inventory);
    
    // Compression settings
    public void setAutoCompress(StorageInventory inventory, boolean enabled);
    public void setCompressionThreshold(StorageInventory inventory, int threshold);
}
```

### StorageAutomationService
Handles auto-collection and automation:
```java
public class StorageAutomationService {
    // Auto-collection
    public void startAutoCollection(PlayerStorageStructure structure);
    public void stopAutoCollection(PlayerStorageStructure structure);
    public boolean isAutoCollecting(PlayerStorageStructure structure);
    
    // Collection settings
    public void setCollectionRange(PlayerStorageStructure structure, int range);
    public void setCollectionFilter(PlayerStorageStructure structure, List<Material> filter);
    
    // Collection operations
    public CompletableFuture<Integer> collectNearbyItems(PlayerStorageStructure structure);
    public CompletableFuture<Integer> collectFromGenerators(PlayerStorageStructure structure);
}
```

### StorageVisualizationService
Handles all visual effects:
```java
public class StorageVisualizationService {
    // Preview methods
    public void showStructureOutline(Player player, StorageDesign design, Location location);
    public void showLayerPreview(Player player, StorageDesignLayer layer, Location location);
    public void show3DRotatingPreview(Player player, StorageDesign design);
    
    // Build effects
    public void playBuildParticles(Location location, Material material);
    public void playCompletionEffect(Location location, StorageDesign design);
    public void playValidationEffect(Location location, boolean valid);
    
    // Continuous effects
    public void startIdleParticles(PlayerStorageStructure structure);
    public void stopIdleParticles(PlayerStorageStructure structure);
    
    // Access effects
    public void playAccessEffect(Location location);
    public void playStorageFullEffect(Location location);
}
```

## View Design

### StorageBrowserView
Main view for browsing storage designs:
- Grid layout showing all 8 storage types
- Locked/unlocked status indicators
- Tier progression display
- Category filter (General, Ore, Crop, Mob, Currency)
- Click to view details

### StorageDesignDetailView
Detailed view of a specific design:
- 3D rotating preview
- Layer breakdown
- Material requirements with inventory check
- Unlock requirements with progress
- Build button (if requirements met)
- Capacity and upgrade information

### StorageInventoryView
Access stored items:
- Paginated item grid
- Search bar for filtering
- Sort options (name, type, quantity)
- Quick stack button
- Compress/decompress buttons
- Item count display

### StorageUpgradeView
View and purchase upgrades:
- List of available upgrades
- Current level and max level
- Upgrade costs (materials + currency)
- Upgrade effects description
- Purchase button (if requirements met)
- Applied upgrades display

### StorageManagementView
Manage multiple storage structures:
- List of all player's storage structures
- Location and tier display
- Capacity usage bars
- Quick access buttons
- Teleport to storage button
- Delete storage button

## i18n Keys Structure
```yaml
storage:
  browser:
    title: "<gradient:#4facfe:#00f2fe><bold>📦 Storage Designs</bold></gradient>"
    locked: "<red><bold>🔒 Locked</bold></red>"
    unlocked: "<green><bold>🔓 Unlocked</bold></green>"
    tier: "<yellow>Tier:</yellow> <white>{tier}</white>"
    capacity: "<yellow>Capacity:</yellow> <white>{capacity} slots</white>"
    category:
      general: "<gray>General Storage</gray>"
      ore: "<gold>Ore Vault</gold>"
      crop: "<green>Crop Silo</green>"
      mob: "<red>Mob Locker</red>"
      currency: "<yellow>Currency Vault</yellow>"
  
  design:
    basic_crate:
      name: "<gradient:#8B4513:#D2691E><bold>📦 Basic Crate</bold></gradient>"
      description:
        - "<gray>A simple wooden storage crate</gray>"
        - "<gray>for your basic needs.</gray>"
    iron_vault:
      name: "<gradient:#C0C0C0:#808080><bold>🔒 Iron Vault</bold></gradient>"
      description:
        - "<gray>Metal reinforced storage with</gray>"
        - "<gray>basic sorting capabilities.</gray>"
    crystal_repository:
      name: "<gradient:#9B59B6:#8E44AD><bold>💎 Crystal Repository</bold></gradient>"
      description:
        - "<gray>Magical crystalline storage with</gray>"
        - "<gray>advanced filtering system.</gray>"
    mechanical_warehouse:
      name: "<gradient:#E74C3C:#C0392B><bold>⚙ Mechanical Warehouse</bold></gradient>"
      description:
        - "<gray>Automated redstone-powered storage</gray>"
        - "<gray>with intelligent sorting.</gray>"
    dimensional_cache:
      name: "<gradient:#9B59B6:#2C3E50><bold>🌀 Dimensional Cache</bold></gradient>"
      description:
        - "<gray>Void-touched storage with</gray>"
        - "<gray>item compression technology.</gray>"
    nether_vault:
      name: "<gradient:#8B0000:#FF4500><bold>🔥 Nether Vault</bold></gradient>"
      description:
        - "<gray>Fire-proof storage with</gray>"
        - "<gray>auto-smelting integration.</gray>"
    end_archive:
      name: "<gradient:#E1BEE7:#9C27B0><bold>🌌 End Archive</bold></gradient>"
      description:
        - "<gray>Teleportation-enabled storage</gray>"
        - "<gray>accessible from anywhere.</gray>"
    celestial_treasury:
      name: "<gradient:#FFD700:#FFA500><bold>✨ Celestial Treasury</bold></gradient>"
      description:
        - "<gray>Ultimate divine storage with</gray>"
        - "<gray>all features combined.</gray>"
  
  inventory:
    title: "<gradient:#4facfe:#00f2fe><bold>📦 {storage_name}</bold></gradient>"
    capacity: "<yellow>Capacity:</yellow> <white>{used}/{max}</white>"
    search: "<gray>Search items...</gray>"
    sort:
      name: "Sort by Name"
      type: "Sort by Type"
      quantity: "Sort by Quantity"
    compress: "<green>Compress Items</green>"
    decompress: "<yellow>Decompress Items</yellow>"
    empty: "<gray>This storage is empty</gray>"
  
  upgrade:
    title: "<gradient:#4facfe:#00f2fe><bold>⬆ Storage Upgrades</bold></gradient>"
    capacity:
      name: "<yellow>Capacity Upgrade</yellow>"
      description: "<gray>Increases storage slots by {amount}</gray>"
      level: "<white>Level {current}/{max}</white>"
    sorting:
      name: "<yellow>Sorting Upgrade</yellow>"
      description: "<gray>Auto-sorts items by type</gray>"
    filter:
      name: "<yellow>Filter Upgrade</yellow>"
      description: "<gray>Whitelist/blacklist items</gray>"
    compression:
      name: "<yellow>Compression Upgrade</yellow>"
      description: "<gray>Compresses stackable items {ratio}:1</gray>"
    automation:
      name: "<yellow>Automation Upgrade</yellow>"
      description: "<gray>Auto-collect items within {range} blocks</gray>"
    speed:
      name: "<yellow>Speed Upgrade</yellow>"
      description: "<gray>Faster item processing</gray>"
    protection:
      name: "<yellow>Protection Upgrade</yellow>"
      description: "<gray>Prevents item loss on death</gray>"
    cost:
      materials: "<yellow>Materials:</yellow>"
      currency: "<yellow>Cost:</yellow> <white>{amount}</white>"
    purchase: "<green>Purchase Upgrade</green>"
    maxed: "<gold>✓ Maxed Out</gold>"
  
  management:
    title: "<gradient:#4facfe:#00f2fe><bold>📦 Storage Management</bold></gradient>"
    list: "<yellow>Your Storage Structures:</yellow>"
    location: "<gray>Location: {x}, {y}, {z}</gray>"
    tier: "<yellow>Tier {tier}</yellow>"
    access: "<green>Open Storage</green>"
    teleport: "<blue>Teleport</blue>"
    delete: "<red>Delete</red>"
    confirm_delete: "<red>Are you sure? This cannot be undone!</red>"
  
  requirement:
    storage_tier: "<yellow>Required Storage:</yellow> <white>{tier}</white>"
    storage_count: "<yellow>Storage Built:</yellow> <white>{current}/{required}</white>"
    evolution_level: "<yellow>Evolution Level:</yellow> <white>{current}/{required}</white>"
    blocks_broken: "<yellow>Blocks Broken:</yellow> <white>{current}/{required}</white>"
    prestige_level: "<yellow>Prestige Level:</yellow> <white>{current}/{required}</white>"
    met: "<green>✓ Requirement Met</green>"
    not_met: "<red>✗ Not Met</red>"
  
  build:
    start: "<green>Starting construction...</green>"
    progress: "<yellow>Building layer {current}/{total}...</yellow>"
    complete: "<green><bold>✓ Construction Complete!</bold></green>"
    failed: "<red><bold>✗ Construction Failed</bold></red>"
    
  error:
    no_space: "<red>Not enough space to build here!</red>"
    already_exists: "<red>A storage structure already exists here!</red>"
    requirements_not_met: "<red>You don't meet the requirements!</red>"
    storage_full: "<red>Storage is full!</red>"
    invalid_item: "<red>This item cannot be stored here!</red>"
```

## Configuration Structure

```yaml
# storage-system.yml
storage-system:
  enabled: true
  
  build-settings:
    animation-speed: 5 # ticks between block placements
    particle-density: 1.0
    sound-enabled: true
    
  detection-settings:
    scan-radius: 50
    validation-interval: 60 # seconds
    
  inventory-settings:
    auto-save-interval: 300 # seconds
    compression-enabled: true
    default-compression-ratio: 10
    max-items-per-slot: 64
    
  automation-settings:
    collection-interval: 20 # ticks
    max-collection-range: 16 # blocks
    collection-enabled: true
    
  designs:
    basic_crate:
      enabled: true
      tier: 1
      category: GENERAL
      base-capacity: 27
      requirements:
        evolution-level: 1
        blocks-broken: 100
      rewards:
        unlock-message: "storage.unlock.basic_crate"
      particle-effect: "VILLAGER_HAPPY"
      upgrades:
        capacity:
          enabled: true
          max-level: 1
          cost-per-level:
            materials:
              - material: OAK_PLANKS
                amount: 64
            currency: 1000
          effect-per-level: 9 # adds 9 slots per level
      
    iron_vault:
      enabled: true
      tier: 2
      category: GENERAL
      base-capacity: 54
      requirements:
        evolution-level: 10
        blocks-broken: 1000
        storage-tier: BASIC_CRATE
      rewards:
        unlock-message: "storage.unlock.iron_vault"
      particle-effect: "CRIT"
      upgrades:
        capacity:
          enabled: true
          max-level: 2
          cost-per-level:
            materials:
              - material: IRON_BLOCK
                amount: 32
            currency: 5000
          effect-per-level: 9
        sorting:
          enabled: true
          max-level: 1
          cost-per-level:
            materials:
              - material: HOPPER
                amount: 8
            currency: 2500
    
    crystal_repository:
      enabled: true
      tier: 3
      category: GENERAL
      base-capacity: 81
      requirements:
        evolution-level: 20
        blocks-broken: 5000
        storage-tier: IRON_VAULT
      rewards:
        unlock-message: "storage.unlock.crystal_repository"
      particle-effect: "END_ROD"
      upgrades:
        capacity:
          enabled: true
          max-level: 3
          cost-per-level:
            materials:
              - material: AMETHYST_BLOCK
                amount: 16
            currency: 10000
          effect-per-level: 9
        sorting:
          enabled: true
          max-level: 2
        filter:
          enabled: true
          max-level: 1
          cost-per-level:
            materials:
              - material: GLASS
                amount: 32
            currency: 7500
    
    mechanical_warehouse:
      enabled: true
      tier: 4
      category: GENERAL
      base-capacity: 108
      requirements:
        evolution-level: 30
        blocks-broken: 15000
        storage-tier: CRYSTAL_REPOSITORY
      rewards:
        unlock-message: "storage.unlock.mechanical_warehouse"
      particle-effect: "REDSTONE"
      upgrades:
        capacity:
          enabled: true
          max-level: 4
        sorting:
          enabled: true
          max-level: 3
        filter:
          enabled: true
          max-level: 2
        automation:
          enabled: true
          max-level: 1
          cost-per-level:
            materials:
              - material: PISTON
                amount: 16
              - material: OBSERVER
                amount: 8
            currency: 25000
          effect-per-level: 8 # collection range in blocks
    
    dimensional_cache:
      enabled: true
      tier: 5
      category: GENERAL
      base-capacity: 135
      requirements:
        evolution-level: 50
        blocks-broken: 50000
        prestige-level: 1
        storage-tier: MECHANICAL_WAREHOUSE
      rewards:
        unlock-message: "storage.unlock.dimensional_cache"
      particle-effect: "PORTAL"
      upgrades:
        capacity:
          enabled: true
          max-level: 5
        sorting:
          enabled: true
          max-level: 3
        filter:
          enabled: true
          max-level: 3
        automation:
          enabled: true
          max-level: 2
        compression:
          enabled: true
          max-level: 1
          cost-per-level:
            materials:
              - material: ENDER_CHEST
                amount: 4
              - material: ENDER_PEARL
                amount: 64
            currency: 50000
          effect-per-level: 10 # compression ratio
    
    nether_vault:
      enabled: true
      tier: 6
      category: ORE
      base-capacity: 162
      requirements:
        evolution-level: 75
        blocks-broken: 150000
        prestige-level: 3
        storage-tier: DIMENSIONAL_CACHE
      rewards:
        unlock-message: "storage.unlock.nether_vault"
        special-features:
          - auto-smelting
          - fire-immunity
      particle-effect: "SOUL_FIRE_FLAME"
      upgrades:
        capacity:
          enabled: true
          max-level: 5
        sorting:
          enabled: true
          max-level: 3
        filter:
          enabled: true
          max-level: 3
        automation:
          enabled: true
          max-level: 3
        compression:
          enabled: true
          max-level: 2
        speed:
          enabled: true
          max-level: 3
          cost-per-level:
            materials:
              - material: NETHERITE_INGOT
                amount: 4
            currency: 100000
          effect-per-level: 0.5 # speed multiplier
    
    end_archive:
      enabled: true
      tier: 7
      category: GENERAL
      base-capacity: 189
      requirements:
        evolution-level: 100
        blocks-broken: 500000
        prestige-level: 5
        storage-tier: NETHER_VAULT
      rewards:
        unlock-message: "storage.unlock.end_archive"
        special-features:
          - remote-access
          - teleportation
      particle-effect: "DRAGON_BREATH"
      upgrades:
        capacity:
          enabled: true
          max-level: 5
        sorting:
          enabled: true
          max-level: 3
        filter:
          enabled: true
          max-level: 3
        automation:
          enabled: true
          max-level: 3
        compression:
          enabled: true
          max-level: 3
        speed:
          enabled: true
          max-level: 5
        protection:
          enabled: true
          max-level: 1
          cost-per-level:
            materials:
              - material: SHULKER_BOX
                amount: 8
              - material: END_CRYSTAL
                amount: 1
            currency: 250000
    
    celestial_treasury:
      enabled: true
      tier: 8
      category: GENERAL
      base-capacity: 216
      requirements:
        evolution-level: 150
        blocks-broken: 1000000
        prestige-level: 10
        storage-tier: END_ARCHIVE
      rewards:
        unlock-message: "storage.unlock.celestial_treasury"
        special-features:
          - all-features
          - item-duplication-prevention
          - infinite-durability
      particle-effect: "END_ROD"
      upgrades:
        # All upgrades maxed by default
        capacity:
          enabled: true
          max-level: 5
          default-level: 5
        sorting:
          enabled: true
          max-level: 3
          default-level: 3
        filter:
          enabled: true
          max-level: 3
          default-level: 3
        automation:
          enabled: true
          max-level: 3
          default-level: 3
        compression:
          enabled: true
          max-level: 3
          default-level: 3
        speed:
          enabled: true
          max-level: 5
          default-level: 5
        protection:
          enabled: true
          max-level: 1
          default-level: 1
```

## Migration Strategy

1. **Phase 1**: Create new entity structure alongside existing storage
2. **Phase 2**: Migrate existing storage data to new `PlayerStorageStructure`
3. **Phase 3**: Update services to use new entities
4. **Phase 4**: Update views to use new system
5. **Phase 5**: Deprecate old storage system
6. **Phase 6**: Remove deprecated code

## Testing Strategy

- Unit tests for requirement checking
- Unit tests for structure validation
- Unit tests for item compression/decompression
- Integration tests for build process
- Integration tests for inventory operations
- Integration tests for upgrade system
- GUI tests for view rendering
- Performance tests for particle effects
- Performance tests for auto-collection
- Load tests for large inventories

## Performance Considerations

### Item Compression
- Use efficient compression algorithms
- Compress in background threads
- Cache compression results
- Limit compression frequency

### Auto-Collection
- Use spatial indexing for nearby items
- Batch item collection operations
- Limit collection frequency
- Use async operations

### Database Operations
- Use connection pooling
- Batch insert/update operations
- Cache frequently accessed data
- Use async queries

### Particle Effects
- Limit particle count per tick
- Use particle culling for distant players
- Batch particle spawning
- Optimize particle update frequency
