# Generator Structure System Overhaul - Technical Design

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Generator Structure System                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   Config Layer  │  │  Service Layer  │  │    View Layer   │              │
│  │                 │  │                 │  │                 │              │
│  │ GeneratorConfig │  │ StructureManager│  │ GeneratorBrowser│              │
│  │ DesignConfig    │  │ DesignService   │  │ DesignDetailView│              │
│  │ EffectsConfig   │  │ BuildService    │  │ LayerDetailView │              │
│  │                 │  │ DetectionService│  │ MaterialsView   │              │
│  │                 │  │ VisualizationSvc│  │ Visualization3D │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                    │                        │
│  ┌────────▼────────────────────▼────────────────────▼────────┐              │
│  │                      Repository Layer                      │              │
│  │                                                            │              │
│  │  GeneratorDesignRepository  │  PlayerStructureRepository  │              │
│  │  DesignLayerRepository      │  DesignRequirementRepository│              │
│  └────────────────────────────────────────────────────────────┘              │
│                                │                                             │
│  ┌────────────────────────────▼────────────────────────────────┐            │
│  │                       Entity Layer                           │            │
│  │                                                              │            │
│  │  GeneratorDesign ──┬── GeneratorDesignLayer                 │            │
│  │                    ├── GeneratorDesignMaterial              │            │
│  │                    ├── GeneratorDesignRequirement           │            │
│  │                    └── GeneratorDesignReward                │            │
│  │                                                              │            │
│  │  PlayerGeneratorStructure ── BuildProgress                  │            │
│  └──────────────────────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Package Structure

### RPlatform Requirement System (New)
```
com.raindropcentral.rplatform.requirement/
├── Requirement.java (sealed interface)
├── AbstractRequirement.java (base class)
├── ERequirementType.java (enum)
├── RequirementRegistry.java (registration & lookup)
├── RequirementService.java (check/consume/progress)
├── RequirementEntity.java (JPA wrapper)
├── RequirementConverter.java (JPA JSON converter)
├── RequirementSection.java (config section)
├── PluginRequirementProvider.java (extension interface)
├── impl/
│   ├── ItemRequirement.java
│   ├── CurrencyRequirement.java
│   ├── ExperienceLevelRequirement.java
│   ├── PermissionRequirement.java
│   ├── PlaytimeRequirement.java
│   ├── StatisticRequirement.java
│   ├── LocationRequirement.java
│   ├── CompositeRequirement.java
│   ├── ChoiceRequirement.java
│   └── CustomRequirement.java
└── config/
    └── IconSection.java (display icon config)
```

### JExOneblock Generator Package
```
de.jexcellence.oneblock.generator/
├── config/
│   ├── GeneratorStructureConfig.java
│   ├── GeneratorDesignSection.java
│   └── GeneratorEffectsSection.java
├── database/
│   ├── entity/
│   │   ├── GeneratorDesign.java
│   │   ├── GeneratorDesignLayer.java
│   │   ├── GeneratorDesignMaterial.java
│   │   ├── GeneratorDesignRequirement.java (uses RPlatform RequirementEntity)
│   │   ├── GeneratorDesignReward.java
│   │   └── PlayerGeneratorStructure.java
│   ├── repository/
│   │   ├── GeneratorDesignRepository.java
│   │   ├── GeneratorDesignLayerRepository.java
│   │   ├── GeneratorDesignRequirementRepository.java
│   │   └── PlayerGeneratorStructureRepository.java
│   └── converter/
│       └── MaterialPatternConverter.java
├── design/
│   ├── EGeneratorDesignType.java (enum for 10 types)
│   ├── GeneratorDesignRegistry.java
│   └── designs/
│       ├── FoundryDesign.java
│       ├── AquaticDesign.java
│       ├── VolcanicDesign.java
│       ├── CrystalDesign.java
│       ├── MechanicalDesign.java
│       ├── NatureDesign.java
│       ├── NetherDesign.java
│       ├── EndDesign.java
│       ├── AncientDesign.java
│       └── CelestialDesign.java
├── requirement/ (OneBlock-specific, extends RPlatform)
│   ├── EvolutionLevelRequirement.java (extends AbstractRequirement)
│   ├── BlocksBrokenRequirement.java (extends AbstractRequirement)
│   ├── PrestigeLevelRequirement.java (extends AbstractRequirement)
│   ├── IslandLevelRequirement.java (extends AbstractRequirement)
│   ├── GeneratorTierRequirement.java (extends AbstractRequirement)
│   └── OneBlockRequirementProvider.java (implements PluginRequirementProvider)
├── service/
│   ├── GeneratorStructureManager.java
│   ├── GeneratorDesignService.java
│   ├── StructureDetectionService.java
│   ├── StructureBuildService.java
│   ├── StructureVisualizationService.java
│   └── GeneratorRequirementService.java (uses RPlatform RequirementService)
├── visualization/
│   ├── ParticleEffectManager.java
│   ├── StructureVisualization3D.java
│   ├── StructureBuilder.java
│   ├── BuildProgressTracker.java
│   └── effects/
│       ├── GeneratorParticleEffect.java
│       ├── BuildParticleEffect.java
│       └── ValidationParticleEffect.java
└── view/
    ├── GeneratorBrowserView.java
    ├── GeneratorDesignDetailView.java
    ├── GeneratorLayerDetailView.java
    ├── GeneratorMaterialsView.java
    ├── GeneratorVisualization3DView.java
    ├── GeneratorBuildProgressView.java
    └── AnimatedGeneratorStructureView.java
```

## Database Entity Design

### RPlatform Requirement System (Shared across all plugins)

#### Requirement Interface (RPlatform)
```java
package com.raindropcentral.rplatform.requirement;

/**
 * Sealed interface for all requirement types in the RPlatform system.
 * Plugins can extend via PluginRequirementProvider.
 */
public sealed interface Requirement permits AbstractRequirement {

    enum Type {
        ITEM, CURRENCY, EXPERIENCE_LEVEL, PERMISSION, PLAYTIME, 
        STATISTIC, LOCATION, COMPOSITE, CHOICE, CUSTOM
    }

    @NotNull Type getType();
    boolean isMet(@NotNull Player player);
    double calculateProgress(@NotNull Player player);
    void consume(@NotNull Player player);
    
    @JsonIgnore
    @NotNull String getDescriptionKey();
}
```

#### AbstractRequirement (RPlatform)
```java
package com.raindropcentral.rplatform.requirement;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ItemRequirement.class, name = "ITEM"),
    @JsonSubTypes.Type(value = CurrencyRequirement.class, name = "CURRENCY"),
    @JsonSubTypes.Type(value = ExperienceLevelRequirement.class, name = "EXPERIENCE_LEVEL"),
    @JsonSubTypes.Type(value = PermissionRequirement.class, name = "PERMISSION"),
    @JsonSubTypes.Type(value = PlaytimeRequirement.class, name = "PLAYTIME"),
    @JsonSubTypes.Type(value = StatisticRequirement.class, name = "STATISTIC"),
    @JsonSubTypes.Type(value = LocationRequirement.class, name = "LOCATION"),
    @JsonSubTypes.Type(value = CompositeRequirement.class, name = "COMPOSITE"),
    @JsonSubTypes.Type(value = ChoiceRequirement.class, name = "CHOICE"),
    @JsonSubTypes.Type(value = CustomRequirement.class, name = "CUSTOM")
})
public abstract non-sealed class AbstractRequirement implements Requirement {
    protected final Type type;

    protected AbstractRequirement(@NotNull Type type) {
        this.type = type;
    }

    @Override
    public @NotNull Type getType() {
        return type;
    }
}
```

#### CurrencyRequirement (RPlatform - Supports Vault & JExEconomy)
```java
package com.raindropcentral.rplatform.requirement.impl;

/**
 * Currency requirement supporting both Vault (single economy) and 
 * JExEconomy (multiple named currencies).
 */
public class CurrencyRequirement extends AbstractRequirement {
    
    private final double requiredAmount;
    private final boolean consumeOnComplete;
    
    /**
     * Currency identifier for JExEconomy support.
     * - null or empty = use Vault/default economy
     * - "coins", "gems", etc. = use JExEconomy with specific currency
     */
    @Nullable
    private final String currencyIdentifier;
    
    /**
     * Economy provider to use.
     */
    public enum EconomyProvider {
        VAULT,      // Use Vault API (single economy)
        JEXECONOMY  // Use JExEconomy API (multiple currencies)
    }
    
    @NotNull
    private final EconomyProvider provider;
    
    /**
     * Creates a Vault-based currency requirement (default economy).
     */
    public CurrencyRequirement(double requiredAmount, boolean consumeOnComplete) {
        super(Type.CURRENCY);
        this.requiredAmount = requiredAmount;
        this.consumeOnComplete = consumeOnComplete;
        this.currencyIdentifier = null;
        this.provider = EconomyProvider.VAULT;
    }
    
    /**
     * Creates a JExEconomy-based currency requirement with specific currency.
     */
    public CurrencyRequirement(
            double requiredAmount, 
            boolean consumeOnComplete,
            @NotNull String currencyIdentifier
    ) {
        super(Type.CURRENCY);
        this.requiredAmount = requiredAmount;
        this.consumeOnComplete = consumeOnComplete;
        this.currencyIdentifier = currencyIdentifier;
        this.provider = EconomyProvider.JEXECONOMY;
    }
    
    @Override
    public boolean isMet(@NotNull Player player) {
        double balance = getPlayerBalance(player);
        return balance >= requiredAmount;
    }
    
    @Override
    public double calculateProgress(@NotNull Player player) {
        double balance = getPlayerBalance(player);
        return Math.min(1.0, balance / requiredAmount);
    }
    
    @Override
    public void consume(@NotNull Player player) {
        if (!consumeOnComplete) return;
        
        if (provider == EconomyProvider.JEXECONOMY && currencyIdentifier != null) {
            // Use JExEconomy CurrencyAdapter
            JExEconomy economy = getJExEconomyInstance();
            if (economy != null) {
                economy.getCurrencyAdapter()
                    .withdraw(player, currencyIdentifier, requiredAmount)
                    .join();
            }
        } else {
            // Use Vault
            Economy vaultEconomy = getVaultEconomy();
            if (vaultEconomy != null) {
                vaultEconomy.withdrawPlayer(player, requiredAmount);
            }
        }
    }
    
    private double getPlayerBalance(@NotNull Player player) {
        if (provider == EconomyProvider.JEXECONOMY && currencyIdentifier != null) {
            JExEconomy economy = getJExEconomyInstance();
            if (economy != null) {
                return economy.getCurrencyAdapter()
                    .getBalance(player, currencyIdentifier)
                    .join();
            }
            return 0.0;
        } else {
            Economy vaultEconomy = getVaultEconomy();
            if (vaultEconomy != null) {
                return vaultEconomy.getBalance(player);
            }
            return 0.0;
        }
    }
    
    @Override
    public @NotNull String getDescriptionKey() {
        if (currencyIdentifier != null) {
            return "requirement.currency.jexeconomy";
        }
        return "requirement.currency.vault";
    }
    
    // Getters
    public double getRequiredAmount() { return requiredAmount; }
    public boolean isConsumeOnComplete() { return consumeOnComplete; }
    @Nullable public String getCurrencyIdentifier() { return currencyIdentifier; }
    @NotNull public EconomyProvider getProvider() { return provider; }
}
```

#### RequirementEntity (RPlatform - JPA Wrapper)
```java
package com.raindropcentral.rplatform.requirement;

@MappedSuperclass
public class RequirementEntity {
    
    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;
    
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "requirement_icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    // Convenience methods
    public boolean isMet(@NotNull Player player) {
        return requirement.isMet(player);
    }
    
    public double calculateProgress(@NotNull Player player) {
        return requirement.calculateProgress(player);
    }
    
    public void consume(@NotNull Player player) {
        requirement.consume(player);
    }
}
```

#### RequirementConverter (RPlatform - JPA Converter)
```java
package com.raindropcentral.rplatform.requirement;

@Converter
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new ParameterNamesModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    @Override
    public String convertToDatabaseColumn(AbstractRequirement requirement) {
        try {
            return MAPPER.writeValueAsString(requirement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize requirement", e);
        }
    }
    
    @Override
    public AbstractRequirement convertToEntityAttribute(String json) {
        try {
            return MAPPER.readValue(json, AbstractRequirement.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize requirement", e);
        }
    }
}
```

#### PluginRequirementProvider (RPlatform - Extension Point)
```java
package com.raindropcentral.rplatform.requirement;

/**
 * Interface for plugins to register custom requirement types.
 */
public interface PluginRequirementProvider {
    
    /**
     * @return unique plugin identifier
     */
    @NotNull String getPluginId();
    
    /**
     * @return list of custom requirement type names this plugin provides
     */
    @NotNull List<String> getCustomTypes();
    
    /**
     * Creates a requirement instance from JSON data
     */
    @Nullable AbstractRequirement createRequirement(@NotNull String typeName, @NotNull JsonNode data);
}
```

### JExOneblock Generator Entities

### JExOneblock Generator Entities

### GeneratorDesign Entity
```java
@Entity
@Table(name = "oneblock_generator_designs")
public class GeneratorDesign extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "design_key", unique = true, nullable = false)
    private String designKey; // e.g., "foundry", "aquatic"
    
    @Column(name = "name_key", nullable = false)
    private String nameKey; // i18n key: "generator.design.foundry.name"
    
    @Column(name = "description_key", nullable = false)
    private String descriptionKey; // i18n key
    
    @Enumerated(EnumType.STRING)
    @Column(name = "design_type", nullable = false)
    private GeneratorDesignType designType;
    
    @Column(name = "tier", nullable = false)
    private Integer tier; // 1-10 progression tier
    
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty; // calculated difficulty score
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "icon_material", nullable = false)
    private String iconMaterial; // Material for GUI display
    
    @Column(name = "particle_effect")
    private String particleEffect; // Custom particle effect key
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("layerIndex ASC")
    private List<GeneratorDesignLayer> layers = new ArrayList<>();
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratorDesignRequirement> requirements = new ArrayList<>();
    
    @OneToMany(mappedBy = "design", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratorDesignReward> rewards = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;
}
```

### GeneratorDesignLayer Entity
```java
@Entity
@Table(name = "oneblock_generator_design_layers")
public class GeneratorDesignLayer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;
    
    @Column(name = "layer_index", nullable = false)
    private Integer layerIndex;
    
    @Column(name = "name_key", nullable = false)
    private String nameKey; // i18n key
    
    @Column(name = "width", nullable = false)
    private Integer width;
    
    @Column(name = "depth", nullable = false)
    private Integer depth;
    
    @Column(name = "pattern", columnDefinition = "LONGTEXT", nullable = false)
    @Convert(converter = MaterialPatternConverter.class)
    private Material[][] pattern;
    
    @Column(name = "core_offset_x")
    private Integer coreOffsetX;
    
    @Column(name = "core_offset_z")
    private Integer coreOffsetZ;
    
    @OneToMany(mappedBy = "layer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratorDesignMaterial> materials = new ArrayList<>();
}
```

### GeneratorDesignRequirement Entity
```java
@Entity
@Table(name = "oneblock_generator_design_requirements")
public class GeneratorDesignRequirement extends RequirementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;
    
    // Inherits requirement, icon, displayOrder from RequirementEntity
}
```

### OneBlock-Specific Requirements (extends RPlatform)
```java
package de.jexcellence.oneblock.generator.requirement;

/**
 * Requires player to have reached a specific evolution level.
 */
public class EvolutionLevelRequirement extends AbstractRequirement {
    private final int requiredLevel;
    private final String evolutionName; // optional, null = any evolution
    
    public EvolutionLevelRequirement(int requiredLevel, @Nullable String evolutionName) {
        super(Type.CUSTOM); // Uses CUSTOM type with plugin-specific handling
        this.requiredLevel = requiredLevel;
        this.evolutionName = evolutionName;
    }
    
    @Override
    public boolean isMet(@NotNull Player player) {
        // Check player's evolution level via OneBlock services
        OneblockIsland island = getIslandForPlayer(player);
        if (island == null || island.getOneblock() == null) return false;
        return island.getOneblock().getEvolutionLevel() >= requiredLevel;
    }
    
    @Override
    public double calculateProgress(@NotNull Player player) {
        OneblockIsland island = getIslandForPlayer(player);
        if (island == null || island.getOneblock() == null) return 0.0;
        return Math.min(1.0, (double) island.getOneblock().getEvolutionLevel() / requiredLevel);
    }
    
    @Override
    public void consume(@NotNull Player player) {
        // Evolution level is not consumed
    }
    
    @Override
    public @NotNull String getDescriptionKey() {
        return "generator.requirement.evolution_level";
    }
}

/**
 * Requires player to have broken a certain number of blocks.
 */
public class BlocksBrokenRequirement extends AbstractRequirement {
    private final long requiredBlocks;
    
    @Override
    public boolean isMet(@NotNull Player player) {
        OneblockIsland island = getIslandForPlayer(player);
        if (island == null || island.getOneblock() == null) return false;
        return island.getOneblock().getTotalBlocksBroken() >= requiredBlocks;
    }
    
    @Override
    public double calculateProgress(@NotNull Player player) {
        OneblockIsland island = getIslandForPlayer(player);
        if (island == null || island.getOneblock() == null) return 0.0;
        return Math.min(1.0, (double) island.getOneblock().getTotalBlocksBroken() / requiredBlocks);
    }
}

/**
 * Requires player to have reached a specific prestige level.
 */
public class PrestigeLevelRequirement extends AbstractRequirement {
    private final int requiredPrestige;
    
    @Override
    public boolean isMet(@NotNull Player player) {
        OneblockIsland island = getIslandForPlayer(player);
        if (island == null || island.getOneblock() == null) return false;
        return island.getOneblock().getPrestigeLevel() >= requiredPrestige;
    }
}

/**
 * Requires player to have unlocked a previous generator tier.
 */
public class GeneratorTierRequirement extends AbstractRequirement {
    private final EGeneratorDesignType requiredTier;
    
    @Override
    public boolean isMet(@NotNull Player player) {
        // Check if player has built/unlocked the required tier generator
        return generatorService.hasUnlockedDesign(player, requiredTier);
    }
}
```

### OneBlockRequirementProvider
```java
package de.jexcellence.oneblock.generator.requirement;

/**
 * Registers OneBlock-specific requirements with RPlatform.
 */
public class OneBlockRequirementProvider implements PluginRequirementProvider {
    
    @Override
    public @NotNull String getPluginId() {
        return "jexoneblock";
    }
    
    @Override
    public @NotNull List<String> getCustomTypes() {
        return List.of(
            "EVOLUTION_LEVEL",
            "BLOCKS_BROKEN", 
            "PRESTIGE_LEVEL",
            "ISLAND_LEVEL",
            "GENERATOR_TIER"
        );
    }
    
    @Override
    public @Nullable AbstractRequirement createRequirement(@NotNull String typeName, @NotNull JsonNode data) {
        return switch (typeName) {
            case "EVOLUTION_LEVEL" -> new EvolutionLevelRequirement(
                data.get("requiredLevel").asInt(),
                data.has("evolutionName") ? data.get("evolutionName").asText() : null
            );
            case "BLOCKS_BROKEN" -> new BlocksBrokenRequirement(
                data.get("requiredBlocks").asLong()
            );
            case "PRESTIGE_LEVEL" -> new PrestigeLevelRequirement(
                data.get("requiredPrestige").asInt()
            );
            case "ISLAND_LEVEL" -> new IslandLevelRequirement(
                data.get("requiredLevel").asInt()
            );
            case "GENERATOR_TIER" -> new GeneratorTierRequirement(
                EGeneratorDesignType.valueOf(data.get("requiredTier").asText())
            );
            default -> null;
        };
    }
}
```

### PlayerGeneratorStructure Entity
```java
@Entity
@Table(name = "oneblock_player_generator_structures")
public class PlayerGeneratorStructure extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "island_id", nullable = false)
    private Long islandId;
    
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;
    
    // Location fields
    @Column(name = "world_name")
    private String worldName;
    
    @Column(name = "location_x")
    private Integer locationX;
    
    @Column(name = "location_y")
    private Integer locationY;
    
    @Column(name = "location_z")
    private Integer locationZ;
    
    // Core location (where blocks generate)
    @Column(name = "core_x")
    private Integer coreX;
    
    @Column(name = "core_y")
    private Integer coreY;
    
    @Column(name = "core_z")
    private Integer coreZ;
    
    // Status
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;
    
    // Statistics
    @Column(name = "blocks_generated", nullable = false)
    private Long blocksGenerated = 0L;
    
    @Column(name = "total_experience", nullable = false)
    private Long totalExperience = 0L;
    
    // Upgrades
    @Column(name = "speed_level", nullable = false)
    private Integer speedLevel = 0;
    
    @Column(name = "efficiency_level", nullable = false)
    private Integer efficiencyLevel = 0;
    
    @Column(name = "fortune_level", nullable = false)
    private Integer fortuneLevel = 0;
    
    // Timestamps
    @Column(name = "built_at")
    private Long builtAt;
    
    @Column(name = "last_used")
    private Long lastUsed;
}
```

## Generator Design Types

### GeneratorDesignType Enum
```java
public enum GeneratorDesignType {
    FOUNDRY("foundry", 1, Material.FURNACE),
    AQUATIC("aquatic", 2, Material.PRISMARINE),
    VOLCANIC("volcanic", 3, Material.MAGMA_BLOCK),
    CRYSTAL("crystal", 4, Material.AMETHYST_BLOCK),
    MECHANICAL("mechanical", 5, Material.PISTON),
    NATURE("nature", 6, Material.MOSS_BLOCK),
    NETHER("nether", 7, Material.BLACKSTONE),
    END("end", 8, Material.END_STONE),
    ANCIENT("ancient", 9, Material.DEEPSLATE),
    CELESTIAL("celestial", 10, Material.BEACON);
    
    private final String key;
    private final int tier;
    private final Material icon;
}
```

## 10 Generator Design Specifications

### 1. Foundry Generator (Tier 1)
- **Theme**: Industrial smelting facility
- **Size**: 5x5x3
- **Materials**: Furnaces, Hoppers, Iron Blocks, Cobblestone
- **Core Mechanic**: Basic generation with auto-smelting potential
- **Requirements**: Evolution Level 5, 500 blocks broken
- **Rewards**: 1.2x generation speed

### 2. Aquatic Generator (Tier 2)
- **Theme**: Underwater temple aesthetic
- **Size**: 5x5x4
- **Materials**: Prismarine, Sea Lanterns, Water, Conduit
- **Core Mechanic**: Water-based generation with aquatic drops
- **Requirements**: Evolution Level 10, 2,000 blocks broken
- **Rewards**: 1.4x speed, chance for prismarine shards

### 3. Volcanic Generator (Tier 3)
- **Theme**: Volcanic forge
- **Size**: 7x7x4
- **Materials**: Magma Blocks, Basalt, Blackstone, Lava
- **Core Mechanic**: Heat-based generation with fire resistance
- **Requirements**: Evolution Level 15, 5,000 blocks broken
- **Rewards**: 1.6x speed, auto-smelting

### 4. Crystal Generator (Tier 4)
- **Theme**: Ethereal crystal formation
- **Size**: 7x7x5
- **Materials**: Amethyst, Glass, Tinted Glass, End Rods
- **Core Mechanic**: Crystal resonance with bonus XP
- **Requirements**: Evolution Level 20, 10,000 blocks broken
- **Rewards**: 1.8x speed, 1.5x XP bonus

### 5. Mechanical Generator (Tier 5)
- **Theme**: Redstone-powered machine
- **Size**: 9x9x5
- **Materials**: Pistons, Observers, Redstone Blocks, Iron
- **Core Mechanic**: Automated collection system
- **Requirements**: Evolution Level 30, 25,000 blocks broken
- **Rewards**: 2.0x speed, auto-collection

### 6. Nature Generator (Tier 6)
- **Theme**: Living organic structure
- **Size**: 9x9x6
- **Materials**: Moss, Leaves, Bee Nests, Flowering Azalea
- **Core Mechanic**: Regenerative with nature drops
- **Requirements**: Evolution Level 40, 50,000 blocks broken
- **Rewards**: 2.2x speed, nature material drops

### 7. Nether Generator (Tier 7)
- **Theme**: Hellish forge
- **Size**: 11x11x6
- **Materials**: Blackstone, Soul Fire, Crying Obsidian, Gilded Blackstone
- **Core Mechanic**: Nether material generation
- **Requirements**: Evolution Level 50, 100,000 blocks broken, Prestige 1
- **Rewards**: 2.5x speed, nether material drops

### 8. End Generator (Tier 8)
- **Theme**: Void-touched structure
- **Size**: 11x11x7
- **Materials**: End Stone, Purpur, End Rods, Chorus Plants
- **Core Mechanic**: End material generation with teleportation
- **Requirements**: Evolution Level 75, 200,000 blocks broken, Prestige 3
- **Rewards**: 3.0x speed, end material drops

### 9. Ancient Generator (Tier 9)
- **Theme**: Deep dark excavation
- **Size**: 13x13x7
- **Materials**: Deepslate, Sculk, Ancient Debris, Reinforced Deepslate
- **Core Mechanic**: Ancient material generation
- **Requirements**: Evolution Level 100, 500,000 blocks broken, Prestige 5
- **Rewards**: 4.0x speed, ancient debris chance

### 10. Celestial Generator (Tier 10)
- **Theme**: Divine beacon structure
- **Size**: 15x15x8
- **Materials**: Beacons, Diamond Blocks, Netherite Blocks, Emerald Blocks
- **Core Mechanic**: Ultimate generation with all bonuses
- **Requirements**: Evolution Level 200, 1,000,000 blocks broken, Prestige 10
- **Rewards**: 5.0x speed, all automation, rare drops

## Service Design

### GeneratorStructureManager
Central manager coordinating all generator structure operations:
```java
public class GeneratorStructureManager {
    private final GeneratorDesignService designService;
    private final StructureDetectionService detectionService;
    private final StructureBuildService buildService;
    private final StructureVisualizationService visualizationService;
    private final GeneratorRequirementService requirementService;
    private final GeneratorDesignRegistry designRegistry;
    
    // Initialization
    public void initialize();
    public void shutdown();
    public void reload();
    
    // Design operations
    public List<GeneratorDesign> getAvailableDesigns(Player player);
    public Optional<GeneratorDesign> getDesign(String designKey);
    public boolean canUnlock(Player player, GeneratorDesign design);
    
    // Structure operations
    public CompletableFuture<BuildResult> buildStructure(Player player, GeneratorDesign design, Location location);
    public CompletableFuture<Boolean> destroyStructure(PlayerGeneratorStructure structure);
    public CompletableFuture<ValidationResult> validateStructure(Location location);
    
    // Visualization
    public void showPreview(Player player, GeneratorDesign design, Location location);
    public void hidePreview(Player player);
    public void showBuildProgress(Player player, BuildProgress progress);
}
```

### StructureVisualizationService
Handles all visual effects:
```java
public class StructureVisualizationService {
    // Preview methods
    public void showStructureOutline(Player player, GeneratorDesign design, Location location);
    public void showLayerPreview(Player player, GeneratorDesignLayer layer, Location location);
    public void show3DRotatingPreview(Player player, GeneratorDesign design);
    
    // Build effects
    public void playBuildParticles(Location location, Material material);
    public void playCompletionEffect(Location location, GeneratorDesign design);
    public void playValidationEffect(Location location, boolean valid);
    
    // Continuous effects
    public void startIdleParticles(PlayerGeneratorStructure structure);
    public void stopIdleParticles(PlayerGeneratorStructure structure);
}
```

## View Design

### GeneratorBrowserView
Main view for browsing generator designs:
- Grid layout showing all 10 generator types
- Locked/unlocked status indicators
- Tier progression display
- Click to view details

### GeneratorDesignDetailView
Detailed view of a specific design:
- 3D rotating preview
- Layer breakdown
- Material requirements with inventory check
- Unlock requirements with progress
- Build button (if requirements met)

### i18n Keys Structure
```yaml
generator:
  browser:
    title: "<gradient:#4facfe:#00f2fe><bold>⚙ Generator Designs</bold></gradient>"
    locked: "<red><bold>🔒 Locked</bold></red>"
    unlocked: "<green><bold>🔓 Unlocked</bold></green>"
    tier: "<yellow>Tier:</yellow> <white>{tier}</white>"
  
  design:
    foundry:
      name: "<gradient:#ff6b35:#f7931e><bold>🔥 Foundry Generator</bold></gradient>"
      description:
        - "<gray>An industrial smelting facility</gray>"
        - "<gray>that harnesses the power of fire.</gray>"
    aquatic:
      name: "<gradient:#00d2d3:#0abde3><bold>🌊 Aquatic Generator</bold></gradient>"
      description:
        - "<gray>A prismarine structure blessed</gray>"
        - "<gray>by the ocean's ancient power.</gray>"
    # ... etc for all 10 types
  
  layer:
    title: "<gradient:#a29bfe:#6c5ce7><bold>📐 Layer {number}</bold></gradient>"
    materials: "<yellow>Materials Required:</yellow>"
    pattern: "<yellow>Pattern Preview:</yellow>"
  
  requirement:
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
```

## Configuration Structure

```yaml
# generator-structures.yml
generator-structures:
  enabled: true
  
  build-settings:
    animation-speed: 5 # ticks between block placements
    particle-density: 1.0
    sound-enabled: true
    
  detection-settings:
    scan-radius: 50
    validation-interval: 60 # seconds
    
  designs:
    foundry:
      enabled: true
      tier: 1
      requirements:
        evolution-level: 5
        blocks-broken: 500
      rewards:
        speed-multiplier: 1.2
        efficiency-multiplier: 1.0
      particle-effect: "FLAME"
      
    aquatic:
      enabled: true
      tier: 2
      requirements:
        evolution-level: 10
        blocks-broken: 2000
      rewards:
        speed-multiplier: 1.4
        efficiency-multiplier: 1.1
        special-drops:
          - material: PRISMARINE_SHARD
            chance: 0.05
      particle-effect: "WATER_SPLASH"
    
    # ... etc for all 10 types
```

## Migration Strategy

1. **Phase 1**: Create new entity structure alongside existing
2. **Phase 2**: Migrate existing `CobblestoneGenerator` data to new `PlayerGeneratorStructure`
3. **Phase 3**: Update services to use new entities
4. **Phase 4**: Update views to use new system
5. **Phase 5**: Deprecate old `CobblestoneGeneratorType` enum
6. **Phase 6**: Remove deprecated code

## Testing Strategy

- Unit tests for requirement checking
- Unit tests for structure validation
- Integration tests for build process
- GUI tests for view rendering
- Performance tests for particle effects
