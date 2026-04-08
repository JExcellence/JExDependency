package de.jexcellence.oneblock.database.entity.infrastructure;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Custom materials for the infrastructure system.
 * These represent fictional materials that don't exist in vanilla Minecraft
 * but are implemented as custom items with special NBT data.
 */
@Getter
public enum CustomMaterial {

    URANIUM_ORE(Material.DEEPSLATE_EMERALD_ORE, "§a§lUranium Ore", List.of(
        "§7A radioactive ore found deep underground",
        "§8Used in nuclear reactor construction"
    ), 1001),
    
    URANIUM_BLOCK(Material.EMERALD_BLOCK, "§a§lUranium Block", List.of(
        "§7Compressed uranium for reactor fuel",
        "§8Handle with extreme caution"
    ), 1002),
    
    PLUTONIUM_INGOT(Material.NETHERITE_INGOT, "§5§lPlutonium Ingot", List.of(
        "§7Highly enriched nuclear material",
        "§8The most dangerous substance known"
    ), 1003),
    
    LEAD_ORE(Material.DEEPSLATE_IRON_ORE, "§7§lLead Ore", List.of(
        "§7Dense metalite ore for radiation shielding",
        "§8Essential for nuclear safety"
    ), 1004),
    
    LEAD_BLOCK(Material.IRON_BLOCK, "§7§lLead Block", List.of(
        "§7Radiation shielding material",
        "§8Blocks harmful radiation"
    ), 1005),
    
    // Reactor Components
    CONTROL_ROD(Material.BLAZE_ROD, "§6§lControl Rod", List.of(
        "§7Regulates nuclear fission rate",
        "§8Critical for reactor safety"
    ), 1006),
    
    REACTOR_CORE(Material.RESPAWN_ANCHOR, "§c§lReactor Core", List.of(
        "§7The heart of a nuclear reactor",
        "§8Contains controlled nuclear reactions"
    ), 1007),
    
    HEAVY_WATER_BUCKET(Material.WATER_BUCKET, "§b§lHeavy Water", List.of(
        "§7Deuterium oxide coolant",
        "§8Used as neutron moderator"
    ), 1008),
    
    REINFORCED_CONCRETE(Material.WHITE_CONCRETE, "§f§lReinforced Concrete", List.of(
        "§7Ultra-dense construction material",
        "§8Withstands extreme conditions"
    ), 1009),
    
    // Advanced Energy Materials
    PLASMA_CELL(Material.END_CRYSTAL, "§d§lPlasma Cell", List.of(
        "§7Contained superheated plasma",
        "§8Powers fusion reactors"
    ), 1010),
    
    ANTIMATTER_CAPSULE(Material.DRAGON_EGG, "§5§lAntimatter Capsule", List.of(
        "§7Contained antimatter particles",
        "§8The ultimate energy source"
    ), 1011),
    
    DARK_MATTER_SHARD(Material.ECHO_SHARD, "§8§lDark Matter Shard", List.of(
        "§7Fragment of dark matter",
        "§8Bends spacetime around it"
    ), 1012),
    
    QUANTUM_CRYSTAL(Material.AMETHYST_SHARD, "§d§lQuantum Crystal", List.of(
        "§7Crystallized quantum energy",
        "§8Exists in multiple states simultaneously"
    ), 1013),
    
    // Cosmic Materials
    STELLAR_CORE_FRAGMENT(Material.NETHER_STAR, "§e§lStellar Core Fragment", List.of(
        "§7Fragment from a dying star",
        "§8Contains immense gravitational energy"
    ), 1014),
    
    VOID_ESSENCE(Material.SCULK_CATALYST, "§0§lVoid Essence", List.of(
        "§7Concentrated nothingness",
        "§8Harvested from the void between dimensions"
    ), 1015),
    
    COSMIC_DUST(Material.GLOWSTONE_DUST, "§e§lCosmic Dust", List.of(
        "§7Particles from distant galaxies",
        "§8Carries the energy of creation"
    ), 1016),
    
    // Automation Components
    CIRCUIT_BOARD(Material.COMPARATOR, "§a§lCircuit Board", List.of(
        "§7Advanced electronic component",
        "§8Controls automation systems"
    ), 1017),
    
    PROCESSOR_CHIP(Material.REDSTONE_BLOCK, "§c§lProcessor Chip", List.of(
        "§7High-speed computing unit",
        "§8Processes millions of operations per second"
    ), 1018),
    
    QUANTUM_PROCESSOR(Material.SCULK_SENSOR, "§d§lQuantum Processor", List.of(
        "§7Quantum computing unit",
        "§8Processes in parallel universes"
    ), 1019),
    
    NEURAL_MATRIX(Material.BRAIN_CORAL_BLOCK, "§f§lNeural Matrix", List.of(
        "§7Artificial neural network",
        "§8Learns and adapts autonomously"
    ), 1020);
    
    private final Material baseMaterial;
    private final String displayName;
    private final List<String> lore;
    private final int customModelData;
    
    private static NamespacedKey customMaterialKey;
    
    CustomMaterial(Material baseMaterial, String displayName, List<String> lore, int customModelData) {
        this.baseMaterial = baseMaterial;
        this.displayName = displayName;
        this.lore = lore;
        this.customModelData = customModelData;
    }
    
    /**
     * Initializes the custom material system with the plugin instance.
     * Must be called during plugin startup.
     */
    public static void initialize(@NotNull Plugin plugin) {
        customMaterialKey = new NamespacedKey(plugin, "custom_material");
    }
    
    /**
     * Creates an ItemStack of this custom material.
     * 
     * @param amount the amount of items
     * @return the custom ItemStack
     */
    @NotNull
    public ItemStack createItem(int amount) {
        ItemStack item = new ItemStack(baseMaterial, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.setCustomModelData(customModelData);
            
            if (customMaterialKey != null) {
                meta.getPersistentDataContainer().set(
                    customMaterialKey, 
                    PersistentDataType.STRING, 
                    this.name()
                );
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a single ItemStack of this custom material.
     * 
     * @return the custom ItemStack
     */
    @NotNull
    public ItemStack createItem() {
        return createItem(1);
    }
    
    /**
     * Checks if an ItemStack is this custom material.
     * 
     * @param item the item to check
     * @return true if the item is this custom material
     */
    public boolean matches(@Nullable ItemStack item) {
        if (item == null || item.getType() != baseMaterial) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || customMaterialKey == null) {
            return false;
        }
        
        String storedMaterial = meta.getPersistentDataContainer().get(
            customMaterialKey, 
            PersistentDataType.STRING
        );
        
        return this.name().equals(storedMaterial);
    }
    
    /**
     * Gets the CustomMaterial from an ItemStack.
     * 
     * @param item the item to check
     * @return the CustomMaterial, or null if not a custom material
     */
    @Nullable
    public static CustomMaterial fromItem(@Nullable ItemStack item) {
        if (item == null) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || customMaterialKey == null) {
            return null;
        }
        
        String storedMaterial = meta.getPersistentDataContainer().get(
            customMaterialKey, 
            PersistentDataType.STRING
        );
        
        if (storedMaterial == null) {
            return null;
        }
        
        try {
            return CustomMaterial.valueOf(storedMaterial);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Counts how many of this custom material are in an ItemStack.
     * 
     * @param item the item to count
     * @return the amount, or 0 if not this material
     */
    public int countInItem(@Nullable ItemStack item) {
        return matches(item) ? item.getAmount() : 0;
    }
}
