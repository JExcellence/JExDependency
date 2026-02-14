package com.raindropcentral.rdq.database.entity.perk;

/**
 * Enumeration of perk thematic categories.
 * <p>
 * Used for organizing and filtering perks in the UI.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public enum PerkCategory {
    /**
     * Combat-related perks (e.g., damage boost, critical strikes).
     */
    COMBAT,
    
    /**
     * Movement-related perks (e.g., speed, fly, jump boost).
     */
    MOVEMENT,
    
    /**
     * Utility perks (e.g., night vision, haste).
     */
    UTILITY,
    
    /**
     * Survival perks (e.g., no fall damage, keep inventory).
     */
    SURVIVAL,
    
    /**
     * Economy-related perks (e.g., double money, discount).
     */
    ECONOMY,
    
    /**
     * Social perks (e.g., chat colors, titles).
     */
    SOCIAL,
    
    /**
     * Cosmetic perks (e.g., glow effect, particles).
     */
    COSMETIC,
    
    /**
     * Special or unique perks that don't fit other categories.
     */
    SPECIAL
}
