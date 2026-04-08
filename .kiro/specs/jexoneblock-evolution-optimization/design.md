# Design Document

## Overview

The JExOneblock evolution optimization transforms the entity system into modern, clean Java that Adam would actually respect. We're eliminating boilerplate, embracing Lombok, using evolution-based naming, and creating a system that's both powerful and concise. No more 200-line classes for simple data structures.

## Architecture

### Modern Entity Hierarchy

```
BaseEntity (platform)
├── OneblockPlayer (record-style with Lombok)
├── Island (optimized with embedded components)
└── OneblockEvolution (abstract, builder-based)
    ├── CustomEvolution (user-created)
    └── PredefinedEvolution (system evolutions)

Embeddable Components:
├── IslandRegion (clean coordinate management)
└── OneblockCore (evolution progression tracking)

Evolution Content (optimized relationships):
├── EvolutionBlock
├── EvolutionEntity  
└── EvolutionItem
```

### Package Structure (Clean & Logical)

```
com.jexcellence.oneblock.database.entity/
├── OneblockPlayer.java
├── Island.java
├── IslandRegion.java (embeddable)
├── OneblockCore.java (embeddable)
├── evolution/
│   ├── OneblockEvolution.java (abstract)
│   ├── CustomEvolution.java
│   ├── PredefinedEvolution.java
│   ├── EvolutionBlock.java
│   ├── EvolutionEntity.java
│   ├── EvolutionItem.java
│   └── EvolutionBuilder.java
├── factory/
│   ├── EvolutionFactory.java
│   └── EnhancedItemFactory.java
└── converter/
    ├── LocationConverter.java
    ├── ItemStackListConverter.java
    └── MaterialListConverter.java
```

## Components and Interfaces

### Core Entities (Modern Java Style)

#### OneblockPlayer
```java
@Entity
@Table(name = "oneblock_players")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "uniqueId")
public class OneblockPlayer extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private UUID uniqueId;
    
    @Column(nullable = false)
    private String playerName;
    
    // Constructor for Bukkit Player
    public OneblockPlayer(Player player) {
        this.uniqueId = player.getUniqueId();
        this.playerName = player.getName();
    }
}
```

#### Island (Optimized with Embedded Components)
```java
@Entity
@Table(name = "islands")
@Getter @Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Island extends BaseEntity {
    
    @OneToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private OneblockPlayer owner;
    
    @Column(nullable = false, unique = true)
    private String identifier;
    
    @Column(nullable = false)
    private String islandName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private int currentSize;
    private int maximalSize;
    private int level;
    private double experience;
    private boolean privacy;
    private long islandCoins;
    
    @Convert(converter = LocationConverter.class)
    private Location islandCenter;
    
    @Embedded
    private IslandRegion region;
    
    @Embedded
    private OneblockCore oneblock;
    
    @ManyToMany
    @JoinTable(name = "island_members")
    @Builder.Default
    private Set<OneblockPlayer> members = new HashSet<>();
    
    @ManyToMany
    @JoinTable(name = "island_banned")
    @Builder.Default
    private Set<OneblockPlayer> bannedPlayers = new HashSet<>();
    
    // Business logic methods
    public boolean isMember(OneblockPlayer player) {
        return members.contains(player);
    }
    
    public boolean isBanned(OneblockPlayer player) {
        return bannedPlayers.contains(player);
    }
    
    public boolean canAccess(OneblockPlayer player) {
        return owner.equals(player) || (isMember(player) && !isBanned(player));
    }
}
```

#### OneblockCore (Embeddable Evolution Tracker)
```java
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneblockCore {
    
    private int currentEvolution;
    private int prestige;
    private double experience;
    private long blocksDestroyed;
    
    @Convert(converter = LocationConverter.class)
    private Location oneblockLocation;
    
    private boolean isActive;
    
    // Evolution progression methods
    public void addExperience(double exp) {
        this.experience += exp;
        this.blocksDestroyed++;
    }
    
    public void nextEvolution() {
        this.currentEvolution++;
        this.experience = 0.0;
    }
    
    public void prestige() {
        this.prestige++;
        this.currentEvolution = 0;
        this.experience = 0.0;
    }
}
```

### Evolution System (Builder-Based)

#### OneblockEvolution (Abstract Base)
```java
@Entity
@Table(name = "oneblock_evolutions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "evolution_type")
@Getter @Setter
@NoArgsConstructor
public abstract class OneblockEvolution extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String evolutionName;
    
    private int level;
    private int experienceToPass;
    
    @Enumerated(EnumType.STRING)
    private Material showcase;
    
    private boolean isDisabled;
    
    @OneToMany(mappedBy = "evolution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvolutionBlock> blocks = new ArrayList<>();
    
    @OneToMany(mappedBy = "evolution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvolutionEntity> entities = new ArrayList<>();
    
    @OneToMany(mappedBy = "evolution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvolutionItem> items = new ArrayList<>();
    
    // Content retrieval by rarity
    public List<Material> getBlocksByRarity(EEvolutionRarityType rarity) {
        return blocks.stream()
            .filter(block -> block.getRarity() == rarity)
            .flatMap(block -> block.getMaterials().stream())
            .toList();
    }
    
    public List<ItemStack> getItemsByRarity(EEvolutionRarityType rarity) {
        return items.stream()
            .filter(item -> item.getRarity() == rarity)
            .flatMap(item -> item.getItemStacks().stream())
            .toList();
    }
}
```

#### EvolutionBuilder (Fluent API)
```java
public class EvolutionBuilder {
    
    private final OneblockEvolution evolution;
    private final Map<EEvolutionRarityType, List<Material>> blocksByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<ItemEntry>> itemsByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<Material>> entitiesByRarity = new HashMap<>();
    
    public EvolutionBuilder(String name, int level, int experienceToPass) {
        this.evolution = new CustomEvolution();
        evolution.setEvolutionName(name);
        evolution.setLevel(level);
        evolution.setExperienceToPass(experienceToPass);
        
        // Initialize rarity maps
        Arrays.stream(EEvolutionRarityType.values())
            .forEach(rarity -> {
                blocksByRarity.put(rarity, new ArrayList<>());
                itemsByRarity.put(rarity, new ArrayList<>());
                entitiesByRarity.put(rarity, new ArrayList<>());
            });
    }
    
    // Fluent block methods
    public EvolutionBuilder addBlocks(EEvolutionRarityType rarity, Material... materials) {
        blocksByRarity.get(rarity).addAll(List.of(materials));
        return this;
    }
    
    // Fluent item methods
    public EvolutionBuilder addItem(EEvolutionRarityType rarity, Material material, int amount) {
        itemsByRarity.get(rarity).add(new ItemEntry(material, amount));
        return this;
    }
    
    public EvolutionBuilder addCustomItem(EEvolutionRarityType rarity, Supplier<ItemStack> itemSupplier) {
        itemsByRarity.get(rarity).add(new ItemEntry(itemSupplier));
        return this;
    }
    
    // Bulk methods for common patterns
    public EvolutionBuilder addBasicOres(EEvolutionRarityType rarity) {
        return addBlocks(rarity, 
            Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_COPPER_ORE
        );
    }
    
    public EvolutionBuilder addPassiveMobs(EEvolutionRarityType rarity) {
        return addEntity(rarity, Material.COW_SPAWN_EGG)
               .addEntity(rarity, Material.PIG_SPAWN_EGG)
               .addEntity(rarity, Material.SHEEP_SPAWN_EGG)
               .addEntity(rarity, Material.CHICKEN_SPAWN_EGG);
    }
    
    // Build method
    public OneblockEvolution build() {
        // Apply all configurations to evolution
        blocksByRarity.forEach((rarity, materials) -> {
            if (!materials.isEmpty()) {
                var evolutionBlock = EvolutionBlock.builder()
                    .evolution(evolution)
                    .rarity(rarity)
                    .materials(materials)
                    .build();
                evolution.getBlocks().add(evolutionBlock);
            }
        });
        
        // Similar for items and entities...
        
        return evolution;
    }
    
    @Value
    private static class ItemEntry {
        Material material;
        int amount;
        Supplier<ItemStack> customSupplier;
        
        public ItemEntry(Material material, int amount) {
            this.material = material;
            this.amount = amount;
            this.customSupplier = null;
        }
        
        public ItemEntry(Supplier<ItemStack> customSupplier) {
            this.material = null;
            this.amount = 0;
            this.customSupplier = customSupplier;
        }
    }
}
```

## Data Models

### Evolution Content Entities (Optimized)

#### EvolutionBlock
```java
@Entity
@Table(name = "evolution_blocks")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolutionBlock extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evolution_id", nullable = false)
    private OneblockEvolution evolution;
    
    @Enumerated(EnumType.STRING)
    private EEvolutionRarityType rarity;
    
    @Convert(converter = MaterialListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Material> materials;
}
```

### Enhanced Item Factory (Modern Approach)

```java
public class EnhancedItemFactory {
    
    // God weapons with builder pattern
    public static ItemStack createBowOfArtemis() {
        return ItemBuilder.of(Material.BOW)
            .displayName("<gradient:#00ff00:#008000>Bow of Artemis</gradient>")
            .lore("<green></green>", "<bold><dark_green>[RARE]</dark_green></bold> <gold>Evolution Artemis</gold>")
            .enchant(Enchantment.POWER, 5)
            .enchant(Enchantment.PUNCH, 2)
            .enchant(Enchantment.UNBREAKING, 3)
            .build();
    }
    
    // Armor sets with method chaining
    public static List<ItemStack> createFarmerArmorSet() {
        return Stream.of(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
        )
        .map(material -> ItemBuilder.of(material)
            .displayName("<bold><gradient:gold:yellow>FARMER " + material.name() + "</gradient></bold>")
            .lore("<red></red>", "<bold><red>[SPECIAL]</red></bold> <gold>Evolution Farmer</gold>")
            .customModelData(70000 + material.ordinal())
            .leatherColor(Color.fromRGB(248, 255, 56))
            .build())
        .toList();
    }
    
    // Builder pattern for custom items
    public static class ItemBuilder {
        private final ItemStack item;
        private final ItemMeta meta;
        
        private ItemBuilder(Material material) {
            this.item = new ItemStack(material);
            this.meta = item.getItemMeta();
        }
        
        public static ItemBuilder of(Material material) {
            return new ItemBuilder(material);
        }
        
        public ItemBuilder displayName(String name) {
            meta.setDisplayName(name);
            return this;
        }
        
        public ItemBuilder lore(String... lore) {
            meta.setLore(List.of(lore));
            return this;
        }
        
        public ItemBuilder enchant(Enchantment enchantment, int level) {
            meta.addEnchant(enchantment, level, true);
            return this;
        }
        
        public ItemBuilder customModelData(int data) {
            meta.setCustomModelData(data);
            return this;
        }
        
        public ItemBuilder leatherColor(Color color) {
            if (meta instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(color);
            }
            return this;
        }
        
        public ItemStack build() {
            item.setItemMeta(meta);
            return item;
        }
    }
}
```

## Error Handling

### Modern Exception Handling
- Use Optional for nullable returns instead of null checks
- Leverage pattern matching for type-safe error handling
- Implement proper validation with Bean Validation annotations
- Use Result/Either patterns for complex error scenarios

### Validation Strategy
```java
@Entity
@Validated
public class Island extends BaseEntity {
    
    @NotNull
    @Size(min = 3, max = 50)
    private String islandName;
    
    @Min(1) @Max(1000)
    private int currentSize;
    
    // Custom validation methods
    @AssertTrue(message = "Current size cannot exceed maximal size")
    private boolean isValidSize() {
        return currentSize <= maximalSize;
    }
}
```

## Testing Strategy

### Modern Testing Approach
- Use JUnit 5 with parameterized tests
- Leverage Testcontainers for integration testing
- Mock only external dependencies, not internal logic
- Use record-based test data builders

```java
@ExtendWith(MockitoExtension.class)
class IslandTest {
    
    @Test
    void shouldCreateIslandWithDefaults() {
        var player = new OneblockPlayer(UUID.randomUUID(), "TestPlayer");
        var location = new Location(mock(World.class), 0, 64, 0);
        
        var island = Island.builder()
            .owner(player)
            .identifier("test-island")
            .islandName("Test Island")
            .islandCenter(location)
            .build();
        
        assertThat(island.getOwner()).isEqualTo(player);
        assertThat(island.getMembers()).isEmpty();
        assertThat(island.canAccess(player)).isTrue();
    }
}
```

## Performance Considerations

### Optimized Queries
- Use @EntityGraph for controlled eager loading
- Implement pagination for large collections
- Use projection interfaces for read-only queries
- Leverage database indexes on frequently queried fields

### Caching Strategy
- Second-level cache for evolution definitions
- Query result cache for expensive operations
- Proper cache invalidation on entity updates

## Migration Strategy

### Smooth Transition
1. Create new optimized entities alongside existing ones
2. Implement data migration scripts for table renames
3. Update repositories to use new entities
4. Remove old entities after validation
5. Update all references to use evolution terminology

This design eliminates the verbosity Adam criticizes while maintaining type safety and clear contracts. Modern Java at its finest.