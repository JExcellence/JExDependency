package de.jexcellence.oneblock.database.entity.storage;

/**
 * Storage Source Enum
 * 
 * Defines the various sources from which items can be added to storage.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public enum StorageSource {
    
    /**
     * Items manually deposited by players
     */
    MANUAL_DEPOSIT("Manual Deposit"),
    
    /**
     * Items automatically collected from OneBlock breaking
     */
    ONEBLOCK_AUTO_COLLECT("OneBlock Auto-Collect"),
    
    /**
     * Items from chest spawns
     */
    CHEST_SPAWN("Chest Spawn"),
    
    /**
     * Items from mob drops
     */
    MOB_DROP("Mob Drop"),
    
    /**
     * Items from automated systems (hoppers, etc.)
     */
    AUTOMATION("Automation"),
    
    /**
     * Items from trading or exchange
     */
    TRADE("Trade"),
    
    /**
     * Items from rewards or bonuses
     */
    REWARD("Reward"),
    
    /**
     * Items from crafting or processing
     */
    CRAFTING("Crafting"),
    
    /**
     * Items transferred from other storage
     */
    TRANSFER("Transfer"),
    
    /**
     * Items from administrative actions
     */
    ADMIN("Admin"),
    
    /**
     * Unknown or unspecified source
     */
    UNKNOWN("Unknown");
    
    private final String displayName;
    
    StorageSource(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}