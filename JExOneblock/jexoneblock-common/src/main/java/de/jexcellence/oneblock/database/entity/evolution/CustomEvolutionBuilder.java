package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.Value;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * Fluent builder for creating CustomEvolution configurations with factory integration
 * Provides method chaining for all content types and returns Supplier<OneblockEvolution> for factory compatibility
 */
public class CustomEvolutionBuilder {
    
    private String evolutionName;
    private int level;
    private int experienceToPass;
    private String description;
    private String creatorName;
    private Material showcase;
    private boolean isTemplate = false;
    private boolean isDisabled = false;
    
    private final Map<EEvolutionRarityType, List<Material>> blocksByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<ItemEntry>> itemsByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<Material>> entitiesByRarity = new HashMap<>();
    
    /**
     * Creates a new CustomEvolutionBuilder
     */
    public CustomEvolutionBuilder() {
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
     * Sets the evolution name
     * @param evolutionName the name of the evolution
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder evolutionName(@NotNull String evolutionName) {
        this.evolutionName = evolutionName;
        return this;
    }
    
    /**
     * Sets the evolution level
     * @param level the evolution level
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder level(int level) {
        this.level = level;
        return this;
    }
    
    /**
     * Sets the experience required to pass this evolution
     * @param experienceToPass experience required to pass this evolution
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder experienceRequired(int experienceToPass) {
        this.experienceToPass = experienceToPass;
        return this;
    }
    
    /**
     * Sets the description for this evolution
     * @param description the evolution description
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Sets the creator name for this evolution
     * @param creatorName the name of the creator
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder creatorName(@NotNull String creatorName) {
        this.creatorName = creatorName;
        return this;
    }
    
    /**
     * Sets the showcase material for this evolution
     * @param showcase the showcase material
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder showcase(@NotNull Material showcase) {
        this.showcase = showcase;
        return this;
    }
    
    /**
     * Sets whether this evolution is a template
     * @param isTemplate true to mark as template
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder template(boolean isTemplate) {
        this.isTemplate = isTemplate;
        return this;
    }
    
    /**
     * Sets whether this evolution is disabled
     * @param disabled true to disable the evolution
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder disabled(boolean disabled) {
        this.isDisabled = disabled;
        return this;
    }
    
    // ========== Block Configuration Methods ==========
    
    /**
     * Adds blocks to a specific rarity level
     * @param rarity the rarity level
     * @param materials the materials to add
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addBlocks(@NotNull EEvolutionRarityType rarity, @NotNull Material... materials) {
        blocksByRarity.get(rarity).addAll(List.of(materials));
        return this;
    }
    
    /**
     * Adds blocks to a specific rarity level from a collection
     * @param rarity the rarity level
     * @param materials the materials to add
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addBlocks(@NotNull EEvolutionRarityType rarity, @NotNull Collection<Material> materials) {
        blocksByRarity.get(rarity).addAll(materials);
        return this;
    }
    
    /**
     * Adds a single block to a specific rarity level
     * @param rarity the rarity level
     * @param material the material to add
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addBlock(@NotNull EEvolutionRarityType rarity, @NotNull Material material) {
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
    public CustomEvolutionBuilder addItem(@NotNull EEvolutionRarityType rarity, @NotNull Material material) {
        return addItem(rarity, material, 1);
    }
    
    /**
     * Adds an item with specific amount to a specific rarity level
     * @param rarity the rarity level
     * @param material the item material
     * @param amount the item amount
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addItem(@NotNull EEvolutionRarityType rarity, @NotNull Material material, int amount) {
        itemsByRarity.get(rarity).add(new ItemEntry(material, amount));
        return this;
    }
    
    /**
     * Adds a custom item using a supplier for lazy initialization
     * @param rarity the rarity level
     * @param itemSupplier the supplier that creates the ItemStack
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addCustomItem(@NotNull EEvolutionRarityType rarity, @NotNull Supplier<ItemStack> itemSupplier) {
        itemsByRarity.get(rarity).add(new ItemEntry(itemSupplier));
        return this;
    }
    
    /**
     * Adds multiple items to a specific rarity level
     * @param rarity the rarity level
     * @param materials the materials to add as items
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addItems(@NotNull EEvolutionRarityType rarity, @NotNull Material... materials) {
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
    public CustomEvolutionBuilder addItemStack(@NotNull EEvolutionRarityType rarity, @NotNull ItemStack itemStack) {
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
    public CustomEvolutionBuilder addEntities(@NotNull EEvolutionRarityType rarity, @NotNull Material... spawnEggs) {
        entitiesByRarity.get(rarity).addAll(List.of(spawnEggs));
        return this;
    }
    
    /**
     * Adds entities (spawn eggs) to a specific rarity level from a collection
     * @param rarity the rarity level
     * @param spawnEggs the spawn egg materials to add
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addEntities(@NotNull EEvolutionRarityType rarity, @NotNull Collection<Material> spawnEggs) {
        entitiesByRarity.get(rarity).addAll(spawnEggs);
        return this;
    }
    
    /**
     * Adds a single entity (spawn egg) to a specific rarity level
     * @param rarity the rarity level
     * @param spawnEgg the spawn egg material to add
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addEntity(@NotNull EEvolutionRarityType rarity, @NotNull Material spawnEgg) {
        entitiesByRarity.get(rarity).add(spawnEgg);
        return this;
    }
    
    // ========== Bulk Methods for Common Patterns ==========
    
    /**
     * Adds basic ores (coal, iron, copper) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addBasicOres(@NotNull EEvolutionRarityType rarity) {
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
    public CustomEvolutionBuilder addPreciousOres(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE
        );
    }
    
    /**
     * Adds passive mobs (farm animals) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addPassiveMobs(@NotNull EEvolutionRarityType rarity) {
        return addEntities(rarity,
            Material.COW_SPAWN_EGG, Material.PIG_SPAWN_EGG, Material.SHEEP_SPAWN_EGG,
            Material.CHICKEN_SPAWN_EGG, Material.RABBIT_SPAWN_EGG, Material.HORSE_SPAWN_EGG
        );
    }
    
    /**
     * Adds basic food items to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public CustomEvolutionBuilder addBasicFood(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.BREAD, Material.APPLE, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.SWEET_BERRIES, Material.GLOW_BERRIES
        );
    }
    
    // ========== Build Method ==========
    
    /**
     * Builds the CustomEvolution with all configured content and returns a supplier for factory compatibility
     * @return a supplier that creates the configured OneblockEvolution
     */
    public @NotNull Supplier<OneblockEvolution> build() {
        return () -> {
            // Validate required fields
            if (evolutionName == null || evolutionName.trim().isEmpty()) {
                throw new IllegalArgumentException("Evolution name is required");
            }
            if (experienceToPass <= 0) {
                throw new IllegalArgumentException("Experience to pass must be greater than 0");
            }
            
            // Create the custom evolution using setters for parent fields
            CustomEvolution evolution = new CustomEvolution();
            evolution.setEvolutionName(evolutionName);
            evolution.setLevel(level);
            evolution.setExperienceToPass(experienceToPass);
            evolution.setShowcase(showcase);
            evolution.setDescription(description);
            evolution.setDisabled(isDisabled);
            evolution.setCreatorName(creatorName);
            evolution.setCreatedAt(LocalDateTime.now());
            evolution.setTemplate(isTemplate);
            
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
                        .flatMap(entry -> entry.toItemStacks().stream())
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
        };
    }
    
    // ========== Static Factory Methods ==========
    
    /**
     * Creates a new CustomEvolutionBuilder
     * @return a new CustomEvolutionBuilder instance
     */
    public static CustomEvolutionBuilder create() {
        return new CustomEvolutionBuilder();
    }
    
    /**
     * Creates a new CustomEvolutionBuilder with basic parameters
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @return a new CustomEvolutionBuilder instance
     */
    public static CustomEvolutionBuilder create(@NotNull String evolutionName, int level, int experienceToPass) {
        return new CustomEvolutionBuilder()
            .evolutionName(evolutionName)
            .level(level)
            .experienceRequired(experienceToPass);
    }
    
    /**
     * Creates a new CustomEvolutionBuilder with creator information
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @param creatorName the name of the creator
     * @return a new CustomEvolutionBuilder instance
     */
    public static CustomEvolutionBuilder create(@NotNull String evolutionName, int level, int experienceToPass, @NotNull String creatorName) {
        return new CustomEvolutionBuilder()
            .evolutionName(evolutionName)
            .level(level)
            .experienceRequired(experienceToPass)
            .creatorName(creatorName);
    }
    
    // ========== Inner Classes ==========
    
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
                ItemStack stack = customSupplier.get();
                // Ensure custom ItemStacks don't exceed stack limits
                if (stack != null && stack.getAmount() > 64) {
                    stack.setAmount(Math.min(stack.getAmount(), 64));
                }
                return stack;
            } else if (material != null) {
                // Limit amount to 64 for serialization compatibility
                int safeAmount = Math.min(amount, 64);
                return new ItemStack(material, safeAmount);
            }
            return null;
        }
        
        /**
         * Converts this item entry to multiple ItemStacks if the amount exceeds 64
         * @return list of ItemStacks with amounts ≤ 64
         */
        public List<ItemStack> toItemStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            
            if (customSupplier != null) {
                ItemStack stack = customSupplier.get();
                if (stack != null) {
                    stacks.addAll(splitItemStack(stack));
                }
            } else if (material != null && amount > 0) {
                int remainingAmount = amount;
                while (remainingAmount > 0) {
                    int stackAmount = Math.min(remainingAmount, 64);
                    stacks.add(new ItemStack(material, stackAmount));
                    remainingAmount -= stackAmount;
                }
            }
            
            return stacks;
        }
        
        /**
         * Splits a large ItemStack into multiple stacks with amounts ≤ 64
         * @param itemStack the ItemStack to split
         * @return list of ItemStacks with amounts ≤ 64
         */
        private List<ItemStack> splitItemStack(ItemStack itemStack) {
            List<ItemStack> stacks = new ArrayList<>();
            int totalAmount = itemStack.getAmount();
            
            while (totalAmount > 0) {
                int stackAmount = Math.min(totalAmount, 64);
                ItemStack newStack = itemStack.clone();
                newStack.setAmount(stackAmount);
                stacks.add(newStack);
                totalAmount -= stackAmount;
            }
            
            return stacks;
        }
    }
}