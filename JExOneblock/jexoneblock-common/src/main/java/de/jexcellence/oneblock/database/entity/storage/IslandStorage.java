package de.jexcellence.oneblock.database.entity.storage;

import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Island Storage System - Scalable storage with passive benefits
 * Automatically stores items from OneBlock breaking and provides passive XP/bonuses
 */
public class IslandStorage {
    
    private Long islandId;
    private UUID ownerId;
    private StorageTier currentTier;
    private Map<Material, Long> storedItems;
    private Map<EEvolutionRarityType, Long> rarityCapacities;
    private long lastPassiveReward;
    private double passiveXpMultiplier;
    private double passiveDropMultiplier;
    private boolean autoSellEnabled;
    private Map<Material, Boolean> autoSellFilters;
    
    public IslandStorage(Long islandId, UUID ownerId) {
        this.islandId = islandId;
        this.ownerId = ownerId;
        this.currentTier = StorageTier.BASIC;
        this.storedItems = new HashMap<>();
        this.rarityCapacities = new HashMap<>();
        this.lastPassiveReward = System.currentTimeMillis();
        this.passiveXpMultiplier = 1.0;
        this.passiveDropMultiplier = 1.0;
        this.autoSellEnabled = false;
        this.autoSellFilters = new HashMap<>();
        
        initializeCapacities();
    }
    
    private void initializeCapacities() {
        // Set initial capacities based on current tier
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            rarityCapacities.put(rarity, currentTier.getCapacityForRarity(rarity));
        }
    }
    
    /**
     * Attempts to store an item in the island storage
     * @param material The material to store
     * @param amount The amount to store
     * @return The amount that couldn't be stored (overflow)
     */
    public long storeItem(Material material, long amount) {
        EEvolutionRarityType rarity = getRarityForMaterial(material);
        long currentAmount = storedItems.getOrDefault(material, 0L);
        long maxCapacity = rarityCapacities.get(rarity);
        
        long canStore = Math.min(amount, maxCapacity - currentAmount);
        if (canStore > 0) {
            storedItems.put(material, currentAmount + canStore);
        }
        
        return amount - canStore; // Return overflow
    }
    
    /**
     * Retrieves items from storage
     * @param material The material to retrieve
     * @param amount The amount to retrieve
     * @return The actual amount retrieved
     */
    public long retrieveItem(Material material, long amount) {
        long currentAmount = storedItems.getOrDefault(material, 0L);
        long canRetrieve = Math.min(amount, currentAmount);
        
        if (canRetrieve > 0) {
            storedItems.put(material, currentAmount - canRetrieve);
            if (storedItems.get(material) == 0) {
                storedItems.remove(material);
            }
        }
        
        return canRetrieve;
    }
    
    /**
     * Removes items from storage (convenience method)
     * @param material The material to remove
     * @param amount The amount to remove
     * @return true if the full amount was removed
     */
    public boolean removeItem(Material material, long amount) {
        long retrieved = retrieveItem(material, amount);
        return retrieved == amount;
    }
    
    /**
     * Upgrades the storage tier
     * @param newTier The new storage tier
     * @return true if upgrade was successful
     */
    public boolean upgradeTier(StorageTier newTier) {
        if (newTier.ordinal() > currentTier.ordinal()) {
            this.currentTier = newTier;
            updateCapacities();
            updatePassiveBonuses();
            return true;
        }
        return false;
    }
    
    private void updateCapacities() {
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            rarityCapacities.put(rarity, currentTier.getCapacityForRarity(rarity));
        }
    }
    
    private void updatePassiveBonuses() {
        this.passiveXpMultiplier = currentTier.getPassiveXpMultiplier();
        this.passiveDropMultiplier = currentTier.getPassiveDropMultiplier();
    }
    
    /**
     * Processes passive rewards based on stored items
     * @param player The island owner
     */
    public void processPassiveRewards(Player player) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastPassiveReward;
        
        if (timeDiff >= 60000) { // Every minute
            double baseXp = calculatePassiveXp();
            double bonusXp = baseXp * passiveXpMultiplier * currentTier.getPassiveEfficiency();
            
            if (bonusXp > 0) {
                // Give XP to player (implement your XP system here)
                givePassiveXp(player, (int) bonusXp);
            }
            
            lastPassiveReward = currentTime;
        }
    }
    
    private double calculatePassiveXp() {
        double totalValue = 0;
        for (Map.Entry<Material, Long> entry : storedItems.entrySet()) {
            EEvolutionRarityType rarity = getRarityForMaterial(entry.getKey());
            totalValue += entry.getValue() * rarity.getPassiveXpValue();
        }
        return Math.min(totalValue * 0.001, currentTier.getMaxPassiveXpPerMinute());
    }
    
    private void givePassiveXp(final Player player, final int xp) {
        // Implement your XP giving logic here
        player.giveExp(xp);
        new de.jexcellence.jextranslate.i18n.I18n.Builder("storage.passive.xp_generated", player)
            .withPlaceholder("xp", String.valueOf(xp))
            .includePrefix().build().sendMessage();
    }
    
    /**
     * Gets the rarity type for a material (simplified - you'd implement proper mapping)
     */
    private EEvolutionRarityType getRarityForMaterial(Material material) {
        // This is a simplified version - you'd implement proper rarity mapping
        // based on your evolution system
        switch (material) {
            case DIRT, COBBLESTONE, STONE -> {
                return EEvolutionRarityType.COMMON;
            }
            case IRON_INGOT, COPPER_INGOT -> {
                return EEvolutionRarityType.UNCOMMON;
            }
            case GOLD_INGOT, DIAMOND -> {
                return EEvolutionRarityType.RARE;
            }
            case NETHERITE_INGOT, ANCIENT_DEBRIS -> {
                return EEvolutionRarityType.EPIC;
            }
            case NETHER_STAR, DRAGON_EGG -> {
                return EEvolutionRarityType.LEGENDARY;
            }
            case TOTEM_OF_UNDYING, ELYTRA -> {
                return EEvolutionRarityType.SPECIAL;
            }
            case BEACON, CONDUIT -> {
                return EEvolutionRarityType.UNIQUE;
            }
            case RECOVERY_COMPASS, ECHO_SHARD -> {
                return EEvolutionRarityType.MYTHICAL;
            }
            case SCULK_CATALYST, SCULK_SENSOR -> {
                return EEvolutionRarityType.DIVINE;
            }
            case TRIAL_KEY, OMINOUS_TRIAL_KEY -> {
                return EEvolutionRarityType.CELESTIAL;
            }
            case HEAVY_CORE, MACE -> {
                return EEvolutionRarityType.TRANSCENDENT;
            }
            case WIND_CHARGE, BREEZE_ROD -> {
                return EEvolutionRarityType.ETHEREAL;
            }
            case CRAFTER, COPPER_BULB -> {
                return EEvolutionRarityType.COSMIC;
            }
            case ENCHANTED_BOOK, EXPERIENCE_BOTTLE -> {
                return EEvolutionRarityType.INFINITE;
            }
            case KNOWLEDGE_BOOK, BUNDLE -> {
                return EEvolutionRarityType.OMNIPOTENT;
            }
            default -> {
                return EEvolutionRarityType.COMMON;
            }
        }
    }
    
    // Getters and setters
    public Long getIslandId() { return islandId; }
    public UUID getOwnerId() { return ownerId; }
    public StorageTier getCurrentTier() { return currentTier; }
    public Map<Material, Long> getStoredItems() { return storedItems; }
    public Map<EEvolutionRarityType, Long> getRarityCapacities() { return rarityCapacities; }
    public double getPassiveXpMultiplier() { return passiveXpMultiplier; }
    public double getPassiveDropMultiplier() { return passiveDropMultiplier; }
    public boolean isAutoSellEnabled() { return autoSellEnabled; }
    public void setAutoSellEnabled(boolean autoSellEnabled) { this.autoSellEnabled = autoSellEnabled; }
    public Map<Material, Boolean> getAutoSellFilters() { return autoSellFilters; }
}