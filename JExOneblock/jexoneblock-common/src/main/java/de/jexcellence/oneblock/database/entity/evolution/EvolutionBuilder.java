package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

/**
 * Fluent builder for creating OneblockEvolution configurations with ease
 * Provides method chaining for all content types and bulk methods for common patterns
 */
public class EvolutionBuilder {
    
    private final OneblockEvolution evolution;
    private final Map<EEvolutionRarityType, List<Material>> blocksByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<ItemEntry>> itemsByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<Material>> entitiesByRarity = new HashMap<>();
    
    /**
     * Creates a new EvolutionBuilder for a custom evolution
     * @param name the evolution name
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     */
    public EvolutionBuilder(@NotNull String name, int level, int experienceToPass) {
        this.evolution = new CustomEvolution(name, level, experienceToPass);
        initializeRarityMaps();
    }
    
    /**
     * Creates a new EvolutionBuilder for a custom evolution with description
     * @param name the evolution name
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param description the evolution description
     */
    public EvolutionBuilder(@NotNull String name, int level, int experienceToPass, String description) {
        this.evolution = new CustomEvolution(name, level, experienceToPass, description);
        initializeRarityMaps();
    }
    
    /**
     * Creates a new EvolutionBuilder with an existing evolution instance
     * @param evolution the evolution to build upon
     */
    public EvolutionBuilder(@NotNull OneblockEvolution evolution) {
        this.evolution = evolution;
        initializeRarityMaps();
    }
    
    /**
     * Initialize rarity maps for all rarity types
     */
    private void initializeRarityMaps() {
        Arrays.stream(EEvolutionRarityType.values())
            .forEach(rarity -> {
                blocksByRarity.put(rarity, new ArrayList<>());
                itemsByRarity.put(rarity, new ArrayList<>());
                entitiesByRarity.put(rarity, new ArrayList<>());
            });
    }
    
    // ========== Evolution Configuration Methods ==========
    
    /**
     * Sets the showcase material for this evolution
     * @param showcase the showcase material
     * @return this builder for method chaining
     */
    public EvolutionBuilder showcase(@NotNull Material showcase) {
        evolution.setShowcase(showcase);
        return this;
    }
    
    /**
     * Sets the description for this evolution
     * @param description the evolution description
     * @return this builder for method chaining
     */
    public EvolutionBuilder description(String description) {
        evolution.setDescription(description);
        return this;
    }
    
    /**
     * Sets whether this evolution is disabled
     * @param disabled true to disable the evolution
     * @return this builder for method chaining
     */
    public EvolutionBuilder disabled(boolean disabled) {
        evolution.setDisabled(disabled);
        return this;
    }
    
    // ========== Block Configuration Methods ==========
    
    /**
     * Adds blocks to a specific rarity level
     * @param rarity the rarity level
     * @param materials the materials to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBlocks(@NotNull EEvolutionRarityType rarity, @NotNull Material... materials) {
        blocksByRarity.get(rarity).addAll(List.of(materials));
        return this;
    }
    
    /**
     * Adds blocks to a specific rarity level from a collection
     * @param rarity the rarity level
     * @param materials the materials to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBlocks(@NotNull EEvolutionRarityType rarity, @NotNull Collection<Material> materials) {
        blocksByRarity.get(rarity).addAll(materials);
        return this;
    }
    
    /**
     * Adds a single block to a specific rarity level
     * @param rarity the rarity level
     * @param material the material to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBlock(@NotNull EEvolutionRarityType rarity, @NotNull Material material) {
        blocksByRarity.get(rarity).add(material);
        return this;
    }
    
    // ========== Item Configuration Methods ==========
    
    /**
     * Adds an item with default amount (1) to a specific rarity level
     * @param rarity the rarity level
     * @param material the item material
     * @return this builder for method chaining
     */
    public EvolutionBuilder addItem(@NotNull EEvolutionRarityType rarity, @NotNull Material material) {
        return addItem(rarity, material, 1);
    }
    
    /**
     * Adds an item with specific amount to a specific rarity level
     * @param rarity the rarity level
     * @param material the item material
     * @param amount the item amount
     * @return this builder for method chaining
     */
    public EvolutionBuilder addItem(@NotNull EEvolutionRarityType rarity, @NotNull Material material, int amount) {
        itemsByRarity.get(rarity).add(new ItemEntry(material, amount));
        return this;
    }
    
    /**
     * Adds a custom item using a supplier for lazy initialization
     * @param rarity the rarity level
     * @param itemSupplier the supplier that creates the ItemStack
     * @return this builder for method chaining
     */
    public EvolutionBuilder addCustomItem(@NotNull EEvolutionRarityType rarity, @NotNull Supplier<ItemStack> itemSupplier) {
        itemsByRarity.get(rarity).add(new ItemEntry(itemSupplier));
        return this;
    }
    
    /**
     * Adds multiple items to a specific rarity level
     * @param rarity the rarity level
     * @param materials the materials to add as items
     * @return this builder for method chaining
     */
    public EvolutionBuilder addItems(@NotNull EEvolutionRarityType rarity, @NotNull Material... materials) {
        Arrays.stream(materials)
            .forEach(material -> addItem(rarity, material, 1));
        return this;
    }
    
    /**
     * Adds an ItemStack directly to a specific rarity level
     * @param rarity the rarity level
     * @param itemStack the ItemStack to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addItemStack(@NotNull EEvolutionRarityType rarity, @NotNull ItemStack itemStack) {
        itemsByRarity.get(rarity).add(new ItemEntry(() -> itemStack.clone()));
        return this;
    }
    
    // ========== Entity Configuration Methods ==========
    
    /**
     * Adds entities (spawn eggs) to a specific rarity level
     * @param rarity the rarity level
     * @param spawnEggs the spawn egg materials to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addEntities(@NotNull EEvolutionRarityType rarity, @NotNull Material... spawnEggs) {
        entitiesByRarity.get(rarity).addAll(List.of(spawnEggs));
        return this;
    }
    
    /**
     * Adds entities (spawn eggs) to a specific rarity level from a collection
     * @param rarity the rarity level
     * @param spawnEggs the spawn egg materials to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addEntities(@NotNull EEvolutionRarityType rarity, @NotNull Collection<Material> spawnEggs) {
        entitiesByRarity.get(rarity).addAll(spawnEggs);
        return this;
    }
    
    /**
     * Adds a single entity (spawn egg) to a specific rarity level
     * @param rarity the rarity level
     * @param spawnEgg the spawn egg material to add
     * @return this builder for method chaining
     */
    public EvolutionBuilder addEntity(@NotNull EEvolutionRarityType rarity, @NotNull Material spawnEgg) {
        entitiesByRarity.get(rarity).add(spawnEgg);
        return this;
    }
    
    // ========== Bulk Methods for Common Patterns ==========
    
    /**
     * Adds basic ores (coal, iron, copper) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBasicOres(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity, 
            Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_COPPER_ORE
        );
    }
    
    /**
     * Adds precious ores (gold, diamond, emerald) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addPreciousOres(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE
        );
    }
    
    /**
     * Adds rare ores (lapis, redstone, nether ores) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addRareOres(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.NETHER_QUARTZ_ORE,
            Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
        );
    }
    
    /**
     * Adds all basic building blocks (stone variants) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBasicBlocks(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.POLISHED_GRANITE, Material.POLISHED_DIORITE, Material.POLISHED_ANDESITE
        );
    }
    
    /**
     * Adds passive mobs (farm animals) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addPassiveMobs(@NotNull EEvolutionRarityType rarity) {
        return addEntities(rarity,
            Material.COW_SPAWN_EGG, Material.PIG_SPAWN_EGG, Material.SHEEP_SPAWN_EGG,
            Material.CHICKEN_SPAWN_EGG, Material.RABBIT_SPAWN_EGG, Material.HORSE_SPAWN_EGG
        );
    }
    
    /**
     * Adds hostile mobs (common enemies) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addHostileMobs(@NotNull EEvolutionRarityType rarity) {
        return addEntities(rarity,
            Material.ZOMBIE_SPAWN_EGG, Material.SKELETON_SPAWN_EGG, Material.CREEPER_SPAWN_EGG,
            Material.SPIDER_SPAWN_EGG, Material.ENDERMAN_SPAWN_EGG, Material.WITCH_SPAWN_EGG
        );
    }
    
    /**
     * Adds rare/boss mobs to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addRareMobs(@NotNull EEvolutionRarityType rarity) {
        return addEntities(rarity,
            Material.WITHER_SKELETON_SPAWN_EGG, Material.BLAZE_SPAWN_EGG, Material.GHAST_SPAWN_EGG,
            Material.ENDER_DRAGON_SPAWN_EGG, Material.WARDEN_SPAWN_EGG
        );
    }
    
    /**
     * Adds all sapling types to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addSaplings(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
            Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
        );
    }
    
    /**
     * Adds basic food items to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBasicFood(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.BREAD, Material.APPLE, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.SWEET_BERRIES, Material.GLOW_BERRIES
        );
    }
    
    /**
     * Adds cooked food items to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addCookedFood(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD,
            Material.COOKED_SALMON, Material.BAKED_POTATO
        );
    }
    
    /**
     * Adds basic tools to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addBasicTools(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_SHOVEL,
            Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL
        );
    }
    
    /**
     * Adds iron tools to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addIronTools(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL,
            Material.IRON_SWORD, Material.IRON_HOE
        );
    }
    
    /**
     * Adds diamond tools to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public EvolutionBuilder addDiamondTools(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL,
            Material.DIAMOND_SWORD, Material.DIAMOND_HOE
        );
    }
    
    // ========== Lambda-based Configuration Methods ==========
    
    /**
     * Configures blocks using a lambda function for advanced customization
     * @param configurator the lambda function to configure blocks
     * @return this builder for method chaining
     */
    public EvolutionBuilder configureBlocks(@NotNull BlockConfigurator configurator) {
        configurator.configure(this);
        return this;
    }
    
    /**
     * Configures items using a lambda function for advanced customization
     * @param configurator the lambda function to configure items
     * @return this builder for method chaining
     */
    public EvolutionBuilder configureItems(@NotNull ItemConfigurator configurator) {
        configurator.configure(this);
        return this;
    }
    
    /**
     * Configures entities using a lambda function for advanced customization
     * @param configurator the lambda function to configure entities
     * @return this builder for method chaining
     */
    public EvolutionBuilder configureEntities(@NotNull EntityConfigurator configurator) {
        configurator.configure(this);
        return this;
    }
    
    // ========== Build Method ==========
    
    /**
     * Builds the OneblockEvolution with all configured content
     * @return the configured OneblockEvolution
     */
    public @NotNull OneblockEvolution build() {
        // Apply block configurations
        blocksByRarity.forEach((rarity, materials) -> {
            if (!materials.isEmpty()) {
                var evolutionBlock = EvolutionBlock.builder()
                    .evolution(evolution)
                    .rarity(rarity)
                    .materials(new ArrayList<>(materials))
                    .build();
                evolution.addBlock(evolutionBlock);
            }
        });
        
        // Apply item configurations
        itemsByRarity.forEach((rarity, itemEntries) -> {
            if (!itemEntries.isEmpty()) {
                List<ItemStack> itemStacks = itemEntries.stream()
                    .map(ItemEntry::toItemStack)
                    .filter(Objects::nonNull)
                    .toList();
                
                if (!itemStacks.isEmpty()) {
                    var evolutionItem = EvolutionItem.builder()
                        .evolution(evolution)
                        .rarity(rarity)
                        .itemStacks(new ArrayList<>(itemStacks))
                        .build();
                    evolution.addItem(evolutionItem);
                }
            }
        });
        
        // Apply entity configurations
        entitiesByRarity.forEach((rarity, spawnEggs) -> {
            if (!spawnEggs.isEmpty()) {
                var evolutionEntity = EvolutionEntity.builder()
                    .evolution(evolution)
                    .rarity(rarity)
                    .spawnEggs(new ArrayList<>(spawnEggs))
                    .build();
                evolution.addEntity(evolutionEntity);
            }
        });
        
        return evolution;
    }
    
    // ========== Static Factory Methods ==========
    
    /**
     * Creates a new EvolutionBuilder for a custom evolution
     * @param name the evolution name
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @return a new EvolutionBuilder instance
     */
    public static EvolutionBuilder create(@NotNull String name, int level, int experienceToPass) {
        return new EvolutionBuilder(name, level, experienceToPass);
    }
    
    /**
     * Creates a new EvolutionBuilder for a custom evolution with description
     * @param name the evolution name
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param description the evolution description
     * @return a new EvolutionBuilder instance
     */
    public static EvolutionBuilder create(@NotNull String name, int level, int experienceToPass, String description) {
        return new EvolutionBuilder(name, level, experienceToPass, description);
    }
    
    /**
     * Creates a new EvolutionBuilder from an existing evolution
     * @param evolution the evolution to build upon
     * @return a new EvolutionBuilder instance
     */
    public static EvolutionBuilder from(@NotNull OneblockEvolution evolution) {
        return new EvolutionBuilder(evolution);
    }
    
    // ========== Inner Classes and Interfaces ==========
    
    /**
     * Represents an item entry that can be either a simple material/amount or a custom supplier
     */
    @Value
    private static class ItemEntry {
        Material material;
        int amount;
        Supplier<ItemStack> customSupplier;
        
        public ItemEntry(@NotNull Material material, int amount) {
            this.material = material;
            this.amount = amount;
            this.customSupplier = null;
        }
        
        public ItemEntry(@NotNull Supplier<ItemStack> customSupplier) {
            this.material = null;
            this.amount = 0;
            this.customSupplier = customSupplier;
        }
        
        public ItemStack toItemStack() {
            if (customSupplier != null) {
                return customSupplier.get();
            } else if (material != null) {
                return new ItemStack(material, amount);
            }
            return null;
        }
    }
    
    /**
     * Functional interface for configuring blocks with lambda expressions
     */
    @FunctionalInterface
    public interface BlockConfigurator {
        void configure(EvolutionBuilder builder);
    }
    
    /**
     * Functional interface for configuring items with lambda expressions
     */
    @FunctionalInterface
    public interface ItemConfigurator {
        void configure(EvolutionBuilder builder);
    }
    
    /**
     * Functional interface for configuring entities with lambda expressions
     */
    @FunctionalInterface
    public interface EntityConfigurator {
        void configure(EvolutionBuilder builder);
    }
}