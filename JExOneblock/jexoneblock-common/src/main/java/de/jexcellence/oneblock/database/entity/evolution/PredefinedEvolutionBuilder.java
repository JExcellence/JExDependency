package de.jexcellence.oneblock.database.entity.evolution;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.requirement.EvolutionCurrencyRequirement;
import de.jexcellence.oneblock.requirement.EvolutionCustomRequirement;
import de.jexcellence.oneblock.requirement.EvolutionExperienceRequirement;
import de.jexcellence.oneblock.requirement.EvolutionItemRequirement;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for creating PredefinedEvolution configurations
 * Creates PredefinedEvolution objects directly instead of converting from CustomEvolution
 */
public class PredefinedEvolutionBuilder {
    
    private String evolutionName;
    private int level;
    private int experienceToPass;
    private String description;
    private Material showcase;
    
    private final Map<EEvolutionRarityType, List<Material>> blocksByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<ItemEntry>> itemsByRarity = new HashMap<>();
    private final Map<EEvolutionRarityType, List<Material>> entitiesByRarity = new HashMap<>();
    private final List<AbstractRequirement> requirements = new ArrayList<>();
    
    /**
     * Creates a new PredefinedEvolutionBuilder
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     */
    public PredefinedEvolutionBuilder(@NotNull String evolutionName, int level, int experienceToPass) {
        this.evolutionName = evolutionName;
        this.level = level;
        this.experienceToPass = experienceToPass;
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
    
    /**
     * Inner class to handle item entries with large amounts
     */
    private static class ItemEntry {
        private final Material material;
        private final int amount;
        private final Supplier<ItemStack> customSupplier;
        
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

    
    // ========== Evolution Configuration Methods ==========
    
    /**
     * Sets the evolution name
     * @param evolutionName the name of the evolution
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder evolutionName(@NotNull String evolutionName) {
        this.evolutionName = evolutionName;
        return this;
    }
    
    /**
     * Sets the showcase material for this evolution
     * @param showcase the showcase material
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder showcase(@NotNull Material showcase) {
        this.showcase = showcase;
        return this;
    }
    
    /**
     * Sets the description for this evolution
     * @param description the evolution description
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    // ========== Block Configuration Methods ==========
    
    /**
     * Adds blocks to a specific rarity level
     * @param rarity the rarity level
     * @param materials the materials to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addBlocks(@NotNull EEvolutionRarityType rarity, @NotNull Material... materials) {
        blocksByRarity.get(rarity).addAll(List.of(materials));
        return this;
    }
    
    /**
     * Adds blocks to a specific rarity level from a collection
     * @param rarity the rarity level
     * @param materials the materials to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addBlocks(@NotNull EEvolutionRarityType rarity, @NotNull Collection<Material> materials) {
        blocksByRarity.get(rarity).addAll(materials);
        return this;
    }
    
    /**
     * Adds a single block to a specific rarity level
     * @param rarity the rarity level
     * @param material the material to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addBlock(@NotNull EEvolutionRarityType rarity, @NotNull Material material) {
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
    public PredefinedEvolutionBuilder addItem(@NotNull EEvolutionRarityType rarity, @NotNull Material material) {
        return addItem(rarity, material, 1);
    }
    
    /**
     * Adds an item with specific amount to a specific rarity level
     * @param rarity the rarity level
     * @param material the item material
     * @param amount the item amount
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addItem(@NotNull EEvolutionRarityType rarity, @NotNull Material material, int amount) {
        itemsByRarity.get(rarity).add(new ItemEntry(material, amount));
        return this;
    }
    
    /**
     * Adds a custom item using a supplier for lazy initialization
     * @param rarity the rarity level
     * @param itemSupplier the supplier that creates the ItemStack
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addCustomItem(@NotNull EEvolutionRarityType rarity, @NotNull Supplier<ItemStack> itemSupplier) {
        itemsByRarity.get(rarity).add(new ItemEntry(itemSupplier));
        return this;
    }
    
    /**
     * Adds a custom item using a function that takes a player parameter
     * @param rarity the rarity level
     * @param itemFunction the function that creates the ItemStack from a player
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addCustomItem(@NotNull EEvolutionRarityType rarity, @NotNull Function<Player, ItemStack> itemFunction) {
        itemsByRarity.get(rarity).add(new ItemEntry(() -> itemFunction.apply(null)));
        return this;
    }
    
    /**
     * Adds multiple items to a specific rarity level
     * @param rarity the rarity level
     * @param materials the materials to add as items
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addItems(@NotNull EEvolutionRarityType rarity, @NotNull Material... materials) {
        Arrays.stream(materials).forEach(material -> addItem(rarity, material, 1));
        return this;
    }
    
    /**
     * Adds an ItemStack directly to a specific rarity level
     * @param rarity the rarity level
     * @param itemStack the ItemStack to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addItemStack(@NotNull EEvolutionRarityType rarity, @NotNull ItemStack itemStack) {
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
    public PredefinedEvolutionBuilder addEntities(@NotNull EEvolutionRarityType rarity, @NotNull Material... spawnEggs) {
        entitiesByRarity.get(rarity).addAll(List.of(spawnEggs));
        return this;
    }
    
    /**
     * Adds entities (spawn eggs) to a specific rarity level from a collection
     * @param rarity the rarity level
     * @param spawnEggs the spawn egg materials to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addEntities(@NotNull EEvolutionRarityType rarity, @NotNull Collection<Material> spawnEggs) {
        entitiesByRarity.get(rarity).addAll(spawnEggs);
        return this;
    }
    
    /**
     * Adds a single entity (spawn egg) to a specific rarity level
     * @param rarity the rarity level
     * @param spawnEgg the spawn egg material to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addEntity(@NotNull EEvolutionRarityType rarity, @NotNull Material spawnEgg) {
        entitiesByRarity.get(rarity).add(spawnEgg);
        return this;
    }
    
    // ========== Bulk Methods for Common Patterns ==========
    
    /**
     * Adds basic ores (coal, iron, copper) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addBasicOres(@NotNull EEvolutionRarityType rarity) {
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
    public PredefinedEvolutionBuilder addPreciousOres(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.GOLD_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE
        );
    }
    
    /**
     * Adds stone variants to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addStoneVariants(@NotNull EEvolutionRarityType rarity) {
        return addBlocks(rarity,
            Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS,
            Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
            Material.CHISELED_STONE_BRICKS, Material.SMOOTH_STONE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE,
            Material.POLISHED_ANDESITE, Material.POLISHED_DIORITE, Material.POLISHED_GRANITE
        );
    }
    
    /**
     * Adds passive mobs (farm animals) to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addPassiveMobs(@NotNull EEvolutionRarityType rarity) {
        return addEntities(rarity,
            Material.COW_SPAWN_EGG, Material.PIG_SPAWN_EGG, Material.SHEEP_SPAWN_EGG,
            Material.CHICKEN_SPAWN_EGG, Material.RABBIT_SPAWN_EGG, Material.HORSE_SPAWN_EGG
        );
    }
    
    /**
     * Adds hostile mobs to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addHostileMobs(@NotNull EEvolutionRarityType rarity) {
        return addEntities(rarity,
            Material.ZOMBIE_SPAWN_EGG, Material.SKELETON_SPAWN_EGG, Material.CREEPER_SPAWN_EGG,
            Material.SPIDER_SPAWN_EGG, Material.ENDERMAN_SPAWN_EGG, Material.WITCH_SPAWN_EGG
        );
    }
    
    /**
     * Adds saplings to a specific rarity level
     * @param rarity the rarity level
     * @param amount the amount of each sapling
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addSaplings(@NotNull EEvolutionRarityType rarity, int amount) {
        addItem(rarity, Material.OAK_SAPLING, amount);
        addItem(rarity, Material.BIRCH_SAPLING, amount);
        addItem(rarity, Material.SPRUCE_SAPLING, amount);
        addItem(rarity, Material.JUNGLE_SAPLING, amount);
        addItem(rarity, Material.ACACIA_SAPLING, amount);
        addItem(rarity, Material.DARK_OAK_SAPLING, amount);
        return this;
    }
    
    /**
     * Adds basic food items to a specific rarity level
     * @param rarity the rarity level
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addBasicFood(@NotNull EEvolutionRarityType rarity) {
        return addItems(rarity,
            Material.BREAD, Material.APPLE, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.SWEET_BERRIES, Material.GLOW_BERRIES
        );
    }
    
    // ========== Requirement Configuration Methods ==========
    
    /**
     * Adds a requirement to this evolution
     * @param requirement the requirement to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addRequirement(@NotNull AbstractRequirement requirement) {
        requirements.add(requirement);
        return this;
    }
    
    /**
     * Adds multiple requirements to this evolution
     * @param requirements the requirements to add
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addRequirements(@NotNull AbstractRequirement... requirements) {
        this.requirements.addAll(List.of(requirements));
        return this;
    }
    
    /**
     * Adds a currency requirement (island coins)
     * @param amount the required amount
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireCurrency(long amount) {
        return addRequirement(new EvolutionCurrencyRequirement(amount, 
            EvolutionCurrencyRequirement.CurrencyType.ISLAND_COINS, evolutionName, true));
    }
    
    /**
     * Adds a currency requirement with specific type
     * @param amount the required amount
     * @param currencyType the currency type
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireCurrency(long amount, @NotNull EvolutionCurrencyRequirement.CurrencyType currencyType) {
        return addRequirement(new EvolutionCurrencyRequirement(amount, currencyType, evolutionName, true));
    }
    
    /**
     * Adds an experience level requirement
     * @param levels the required experience levels
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireExperience(int levels) {
        return addRequirement(new EvolutionExperienceRequirement(levels, 
            EvolutionExperienceRequirement.ExperienceType.MINECRAFT_LEVELS, evolutionName, true));
    }
    
    /**
     * Adds an experience requirement with specific type
     * @param amount the required amount
     * @param experienceType the experience type
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireExperience(double amount, 
            @NotNull EvolutionExperienceRequirement.ExperienceType experienceType) {
        return addRequirement(new EvolutionExperienceRequirement(amount, experienceType, evolutionName, true));
    }
    
    /**
     * Adds an item requirement
     * @param material the required material
     * @param amount the required amount
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireItem(@NotNull Material material, int amount) {
        return addRequirement(new EvolutionItemRequirement(material, amount));
    }
    
    /**
     * Adds multiple item requirements
     * @param items the required items
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireItems(@NotNull List<ItemStack> items) {
        return addRequirement(new EvolutionItemRequirement(items, false, false, evolutionName, true));
    }
    
    /**
     * Adds a custom requirement with a predicate
     * @param requirementId unique identifier for the requirement
     * @param description description of the requirement
     * @param predicate the predicate to check
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder requireCustom(@NotNull String requirementId, @NotNull String description,
                                                    @NotNull java.util.function.Predicate<org.bukkit.entity.Player> predicate) {
        // Convert the predicate-based requirement to the new constructor format
        // For now, we'll use a basic custom requirement with BLOCKS_BROKEN type
        return addRequirement(new EvolutionCustomRequirement(
            EvolutionCustomRequirement.CustomType.BLOCKS_BROKEN, 
            1.0, 
            Map.of("description", description, "requirementId", requirementId), 
            evolutionName, 
            false
        ));
    }
    
    /**
     * Adds scaled requirements based on evolution level
     * Automatically calculates currency and experience requirements
     * @return this builder for method chaining
     */
    public PredefinedEvolutionBuilder addScaledRequirements() {
        // Scale currency requirement based on level
        long baseCurrency = 100L;
        long currencyMultiplier = (long) Math.pow(1.5, level - 1);
        long currencyRequired = baseCurrency * currencyMultiplier;
        
        // Scale experience requirement based on level
        int baseExperience = 5;
        int experienceRequired = baseExperience + (level * 2);
        
        if (level > 1) {
            requireCurrency(currencyRequired);
        }
        if (level > 5) {
            requireExperience(experienceRequired);
        }
        
        return this;
    }
    
    // ========== Build Method ==========
    
    /**
     * Builds the PredefinedEvolution with all configured content
     * @return the configured OneblockEvolution
     */
    public @NotNull OneblockEvolution build() {
        // Create the predefined evolution directly
        PredefinedEvolution evolution = new PredefinedEvolution();
        evolution.setEvolutionName(evolutionName);
        evolution.setLevel(level);
        evolution.setExperienceToPass(experienceToPass);
        evolution.setShowcase(showcase);
        evolution.setDescription(description);
        evolution.setDisabled(false);
        
        // Set required PredefinedEvolution fields
        evolution.setSystemVersion("2.0.0");
        evolution.setCategory(determineCategoryFromLevel(level));
        evolution.setPriority(100 - level); // Higher level = lower priority number
        evolution.setCoreEvolution(true);
        evolution.setDefault(level == 1); // First level is default
        evolution.setTemplate(false);
        
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
        
        // Apply item configurations with stack splitting
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
        
        // Apply requirements
        if (!requirements.isEmpty()) {
            evolution.setRequirements(new ArrayList<>(requirements));
        }
        
        return evolution;
    }
    
    /**
     * Determines the category based on evolution level
     * @param level the evolution level
     * @return the appropriate category
     */
    private String determineCategoryFromLevel(int level) {
        if (level <= 10) return "Basic";
        if (level <= 20) return "Advanced";
        if (level <= 30) return "Expert";
        if (level <= 40) return "Master";
        return "Legendary";
    }
    
    // ========== Static Factory Methods ==========
    
    /**
     * Creates a new PredefinedEvolutionBuilder
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     * @return a new PredefinedEvolutionBuilder instance
     */
    public static PredefinedEvolutionBuilder create(@NotNull String evolutionName, int level, int experienceToPass) {
        return new PredefinedEvolutionBuilder(evolutionName, level, experienceToPass);
    }
}