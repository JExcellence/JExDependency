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

package com.raindropcentral.rdt.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Enumerates all configurable town and chunk protection checks.
 *
 * <p>Each protection stores a default role ID that is used when no explicit override exists at
 * town or chunk scope.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
public enum TownProtections {

    /** Hostile mob presence inside town territory. */
    TOWN_HOSTILE_ENTITIES("restricted"),
    /** Neutral/passive mob presence inside town territory. */
    TOWN_NEUTRAL_ENTITIES("public"),
    /** Fire spread behavior inside town territory. */
    TOWN_FIRE("restricted"),
    /** Water flow behavior inside town territory. */
    TOWN_WATER("public"),
    /** Lava flow behavior inside town territory. */
    TOWN_LAVA("public"),
    /** PvP between members of the same town inside town territory. */
    TOWN_FRIENDLY_PVP("restricted"),
    /** PvP involving non-town members inside town territory. */
    TOWN_PUBLIC_PVP("restricted"),

    /** Block-breaking permission inside protected chunks. */
    BREAK_BLOCK("member"),
    /** Block-placement permission inside protected chunks. */
    PLACE_BLOCK("member"),

    /** Chest right-click interaction. */
    CHEST("member"),
    /** Shulker-box right-click interaction. */
    SHULKER_BOXES("member"),
    /** Trapped-chest right-click interaction. */
    TRAPPED_CHEST("member"),
    /** Furnace right-click interaction. */
    FURNACE("member"),
    /** Blast-furnace right-click interaction. */
    BLAST_FURNACE("member"),
    /** Dispenser right-click interaction. */
    DISPENSER("member"),
    /** Hopper right-click interaction. */
    HOPPER("member"),
    /** Dropper right-click interaction. */
    DROPPER("member"),
    /** Jukebox right-click interaction. */
    JUKEBOX("member"),
    /** Stonecutter right-click interaction. */
    STONECUTTER("member"),
    /** Smithing-table right-click interaction. */
    SMITHING_TABLE("member"),
    /** Fletching-table right-click interaction. */
    FLETCHING_TABLE("member"),
    /** Smoker right-click interaction. */
    SMOKER("member"),
    /** Loom right-click interaction. */
    LOOM("member"),
    /** Grindstone right-click interaction. */
    GRINDSTONE("member"),
    /** Composter right-click interaction. */
    COMPOSTER("member"),
    /** Cartography-table right-click interaction. */
    CARTOGRAPHY_TABLE("member"),
    /** Bell right-click interaction. */
    BELL("member"),
    /** Barrel right-click interaction. */
    BARREL("member"),
    /** Brewing-stand right-click interaction. */
    BREWING_STAND("member"),
    /** Lever toggling interaction. */
    LEVER("member"),
    /** Pressure-plate triggered interaction. */
    PRESSURE_PLATES("member"),
    /** Button press interaction. */
    BUTTONS("member"),
    /** Wooden-door right-click interaction. */
    WOOD_DOORS("member"),
    /** Fence-gate right-click interaction. */
    FENCE_GATES("member"),
    /** Trapdoor right-click interaction. */
    TRAPDOORS("member"),
    /** Minecart interaction. */
    MINECARTS("member"),
    /** Lodestone right-click interaction. */
    LODESTONE("member"),
    /** Respawn-anchor right-click interaction. */
    RESPAWN_ANCHOR("member"),
    /** Boat interaction. */
    BOATS("member"),
    /** Ender-pearl usage interaction. */
    ENDER_PEARL("member"),
    /** Fireball usage interaction. */
    FIREBALL("member"),
    /** Chorus-fruit usage interaction. */
    CHORUS_FRUIT("member"),
    /** Lead usage interaction. */
    LEAD("member");

    private final String defaultRoleId;

    TownProtections(final @NotNull String defaultRoleId) {
        this.defaultRoleId = normalizeRoleId(defaultRoleId);
    }

    /**
    * Returns the storage key for this protection.
    *
    * @return normalized protection key
    */
    public @NotNull String getProtectionKey() {
        return this.name();
    }

    /**
     * Returns the fallback role ID used when no explicit override exists.
     *
     * @return normalized default role ID
     */
    public @NotNull String getDefaultRoleId() {
        return this.defaultRoleId;
    }

    /**
     * Normalizes role IDs to persisted uppercase format.
     *
     * @param roleId raw role ID
     * @return normalized role ID
     */
    public static @NotNull String normalizeRoleId(final @NotNull String roleId) {
        return roleId.trim().toUpperCase(Locale.ROOT);
    }
}
