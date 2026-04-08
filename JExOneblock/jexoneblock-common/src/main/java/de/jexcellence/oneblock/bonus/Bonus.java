package de.jexcellence.oneblock.bonus;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for evolution bonuses.
 * Defines all possible bonus types that can be applied during evolutions.
 * 
 * @author JExcellence
 * @since 2.0.0
 */
public interface Bonus {

    /**
     * Gets the type of this bonus.
     * @return the bonus type
     */
    @NotNull
    Type getType();

    /**
     * Gets the value of this bonus.
     * @return the bonus value (usually a multiplier or percentage)
     */
    double getValue();

    /**
     * Gets the formatted description of this bonus for display.
     * @return formatted bonus description
     */
    @NotNull
    String getFormattedDescription();

    /**
     * Checks if this bonus is active.
     * @return true if the bonus is currently active
     */
    boolean isActive();

    /**
     * Gets the duration of this bonus in ticks (-1 for permanent).
     * @return duration in ticks, or -1 for permanent bonuses
     */
    long getDuration();

    /**
     * Enumeration of all bonus types.
     */
    enum Type {
        // Core Bonuses
        DROP_RATE("Drop Rate", "Increases item drop rates"),
        EXPERIENCE("Experience", "Increases experience gain"),
        TOOL_DURABILITY("Tool Durability", "Increases tool durability"),
        ARMOR_PROTECTION("Armor Protection", "Increases armor protection"),
        
        // Resource Bonuses
        FUEL_EFFICIENCY("Fuel Efficiency", "Increases fuel burning time"),
        TRADING("Trading", "Improves villager trade rates"),
        FARMING("Farming", "Increases crop yield and growth speed"),
        FISHING("Fishing", "Improves fishing luck and speed"),
        SMELTING("Smelting", "Increases smelting speed and efficiency"),
        
        // Combat Bonuses
        DAMAGE("Damage", "Increases damage dealt"),
        DEFENSE("Defense", "Increases damage resistance"),
        CRITICAL_HIT("Critical Hit", "Increases critical hit chance"),
        HEALTH("Health", "Increases maximum health"),
        REGENERATION("Regeneration", "Increases health regeneration"),
        
        // Movement Bonuses
        SPEED("Speed", "Increases movement speed"),
        FLIGHT("Flight", "Grants flight ability"),
        TELEPORTATION("Teleportation", "Improves teleportation abilities"),
        
        // Resistance Bonuses
        FIRE_RESISTANCE("Fire Resistance", "Provides fire damage resistance"),
        WATER_BREATHING("Water Breathing", "Allows underwater breathing"),
        
        // Utility Bonuses
        LUCK("Luck", "Increases general luck"),
        PROBABILITY("Probability", "Increases chance-based events"),
        RARE_DROPS("Rare Drops", "Increases rare item drop chances"),
        ENCHANTING("Enchanting", "Improves enchanting results"),
        BLOCK_BREAK_SPEED("Block Break Speed", "Increases block breaking speed"),
        
        // Mob Related Bonuses
        MOB_SPAWN("Mob Spawn", "Affects mob spawn rates"),
        
        // Energy System Bonuses
        ENERGY("Energy", "Increases energy generation/capacity"),
        MANA("Mana", "Increases mana capacity and regeneration"),
        
        // Vision Bonuses
        NIGHT_VISION("Night Vision", "Provides night vision"),
        INVISIBILITY("Invisibility", "Grants invisibility"),
        
        // Meta Bonuses
        ALL_STATS("All Stats", "Increases all bonus effects"),
        ULTIMATE("Ultimate", "Ultimate tier bonus with multiple effects"),
        
        // OneBlock Specific Bonuses
        EXPERIENCE_MULTIPLIER("Experience Multiplier", "Multiplies experience gain"),
        AUTOMATION_EFFICIENCY("Automation Efficiency", "Increases automation system efficiency"),
        STORAGE_CAPACITY("Storage Capacity", "Increases storage capacity"),
        GENERATOR_SPEED("Generator Speed", "Increases generator processing speed"),
        EVOLUTION_PROGRESS("Evolution Progress", "Accelerates evolution progression"),
        EFFICIENCY("Efficiency", "General efficiency improvement");

        private final String displayName;
        private final String description;

        Type(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }

        @NotNull
        public String getDescription() {
            return description;
        }
    }
}