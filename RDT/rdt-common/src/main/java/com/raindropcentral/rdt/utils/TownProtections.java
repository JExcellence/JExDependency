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

import com.raindropcentral.rdt.database.entity.RTown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Enumerates grouped town protection rules for world interaction.
 *
 * <p>Each protection stores a minimum allowed role rather than a per-role matrix. Chunk-level
 * settings may inherit the town value or override it for a specific claim.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownProtections {
    TOWN_HOSTILE_ENTITIES(RTown.RESTRICTED_ROLE_ID),
    TOWN_PASSIVE_ENTITIES(RTown.PUBLIC_ROLE_ID),
    TOWN_FIRE(RTown.PUBLIC_ROLE_ID),
    TOWN_WATER(RTown.PUBLIC_ROLE_ID),
    TOWN_LAVA(RTown.PUBLIC_ROLE_ID),
    BREAK_BLOCK(RTown.MEMBER_ROLE_ID),
    PLACE_BLOCK(RTown.MEMBER_ROLE_ID),
    CHEST(RTown.MEMBER_ROLE_ID),
    CONTAINER_ACCESS(RTown.MEMBER_ROLE_ID),
    SWITCH_ACCESS(RTown.MEMBER_ROLE_ID),
    LEVER(RTown.MEMBER_ROLE_ID),
    BUTTONS(RTown.MEMBER_ROLE_ID),
    WOOD_DOORS(RTown.MEMBER_ROLE_ID),
    FENCE_GATES(RTown.MEMBER_ROLE_ID),
    TRAPDOORS(RTown.MEMBER_ROLE_ID);

    private final String defaultRoleId;

    TownProtections(final @NotNull String defaultRoleId) {
        this.defaultRoleId = normalizeRoleId(defaultRoleId);
    }

    /**
     * Returns the stable persisted protection key.
     *
     * @return stable uppercase protection key
     */
    public @NotNull String getProtectionKey() {
        return this.name();
    }

    /**
     * Returns the default minimum role allowed for this protection.
     *
     * @return normalized default role identifier
     */
    public @NotNull String getDefaultRoleId() {
        return this.defaultRoleId;
    }

    /**
     * Normalizes a stored role identifier into the uppercase persisted form.
     *
     * @param roleId raw role identifier
     * @return normalized role identifier, defaulting to {@link RTown#MEMBER_ROLE_ID} when blank
     */
    public static @NotNull String normalizeRoleId(final @Nullable String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return RTown.MEMBER_ROLE_ID;
        }
        return roleId.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Resolves a protection enum from a raw key.
     *
     * @param protectionKey raw protection key
     * @return matching protection enum, or {@code null} when no protection matches
     */
    public static @Nullable TownProtections fromKey(final @Nullable String protectionKey) {
        if (protectionKey == null || protectionKey.isBlank()) {
            return null;
        }

        try {
            return TownProtections.valueOf(protectionKey.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }
}
