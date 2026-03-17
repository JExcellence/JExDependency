/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.database.entity.perk;

/**
 * Enumeration of perk thematic categories.
 *
 * <p>Used for organizing and filtering perks in the UI.
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
