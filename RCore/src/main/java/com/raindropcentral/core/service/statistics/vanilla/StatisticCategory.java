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

package com.raindropcentral.core.service.statistics.vanilla;

/**
 * Categorizes vanilla Minecraft statistics into logical groups for collection and configuration.
 * Each category represents a distinct type of player activity tracked by the Minecraft client.
 *
 * <p>Categories enable:
 * <ul>
 *   <li>Selective collection - enable/disable specific statistic types</li>
 *   <li>Different collection frequencies per category</li>
 *   <li>Organized filtering and aggregation</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum StatisticCategory {
    
    /**
     * Block-related statistics including mining and placing blocks.
     * Examples: MINE_BLOCK, USE_ITEM (for placeable blocks)
     */
    BLOCKS,
    
    /**
     * Item-related statistics including crafting, using, and breaking items.
     * Examples: CRAFT_ITEM, USE_ITEM, BREAK_ITEM, PICKUP, DROP
     */
    ITEMS,
    
    /**
     * Mob-related statistics including kills and deaths.
     * Examples: KILL_ENTITY, ENTITY_KILLED_BY
     */
    MOBS,
    
    /**
     * Travel and movement statistics.
     * Examples: WALK_ONE_CM, SPRINT_ONE_CM, SWIM_ONE_CM, FLY_ONE_CM, AVIATE_ONE_CM
     */
    TRAVEL,
    
    /**
     * General gameplay statistics.
     * Examples: DEATHS, PLAYER_KILLS, PLAY_ONE_MINUTE, JUMP, SNEAK_TIME
     */
    GENERAL,
    
    /**
     * Block and entity interaction statistics.
     * Examples: INTERACT_WITH_ANVIL, INTERACT_WITH_BEACON, TRADED_WITH_VILLAGER
     */
    INTERACTIONS
}
