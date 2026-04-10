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

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Enumerates management and view permissions granted through town roles.
 *
 * <p>These permissions are distinct from {@link TownProtections}, which control world
 * interaction rules such as block break or container access.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownPermissions {
    TOWN_INFO("PUBLIC"),
    VIEW_TOWN("PUBLIC"),
    VIEW_DIRECTORY("PUBLIC"),
    VIEW_INVITES("PUBLIC"),
    VIEW_ROLES("MEMBER"),
    VIEW_CHUNKS("MEMBER"),
    VIEW_BANK("MEMBER"),
    USE_FOB("MEMBER"),
    CONTRIBUTE("MEMBER"),
    TOWN_INVITE("MEMBER"),
    PLACE_CHUNK("MEMBER"),
    PICKUP_CHUNK("MEMBER"),
    TOWN_DEPOSIT("MEMBER"),
    SUPPLY_TOWN_SHOPS("MEMBER"),
    CHANGE_CHUNK_TYPE("MAYOR"),
    MANAGE_TOWN_SHOPS("MAYOR"),
    UPGRADE_CHUNK("MAYOR"),
    CLAIM_CHUNK("MAYOR"),
    UNCLAIM_CHUNK("MAYOR"),
    ASSIGN_ROLES("MAYOR"),
    CREATE_ROLES("MAYOR"),
    DELETE_ROLES("MAYOR"),
    EDIT_ROLES("MAYOR"),
    TOWN_PROTECTIONS("MAYOR"),
    TOWN_WITHDRAW("MAYOR"),
    TOWN_BANK_REMOTE("MAYOR"),
    PLACE_NEXUS("MAYOR"),
    PICKUP_NEXUS("MAYOR"),
    RENAME_TOWN("MAYOR"),
    CHANGE_TOWN_COLOR("MAYOR"),
    SET_ARCHETYPE("MAYOR"),
    MANAGE_RELATIONSHIPS("MAYOR"),
    MANAGE_NATIONS("MAYOR"),
    UPGRADE_TOWN("MAYOR");

    private final String minimumDefaultRoleId;

    TownPermissions(final @NotNull String minimumDefaultRoleId) {
        this.minimumDefaultRoleId = normalizeRoleId(minimumDefaultRoleId);
    }

    /**
     * Returns the persisted permission key used in entities and configs.
     *
     * @return stable uppercase permission key
     */
    public @NotNull String getPermissionKey() {
        return this.name();
    }

    /**
     * Returns whether this permission is included by default for the supplied role.
     *
     * @param roleId role identifier to evaluate
     * @return {@code true} when the role receives this permission by default
     */
    public boolean isDefaultForRole(final @Nullable String roleId) {
        return compareRolePriority(normalizeRoleId(roleId), this.minimumDefaultRoleId) >= 0;
    }

    /**
     * Normalizes a role or permission key into the stored uppercase identifier form.
     *
     * @param value raw role or permission key
     * @return normalized uppercase identifier, or an empty string when blank
     */
    public static @NotNull String normalize(final @Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns whether a role may be assigned directly to a player.
     *
     * @param roleId role identifier to evaluate
     * @return {@code true} when the role is assignable to players
     */
    public static boolean canAssignToRole(final @Nullable String roleId) {
        return !Objects.equals(normalizeRoleId(roleId), RTown.RESTRICTED_ROLE_ID)
            && !Objects.equals(normalizeRoleId(roleId), RTown.PUBLIC_ROLE_ID);
    }

    /**
     * Returns the default permission keys granted to a role.
     *
     * @param roleId role identifier to evaluate
     * @return immutable default permission set for the role
     */
    public static @NotNull Set<String> defaultPermissionKeysForRole(final @Nullable String roleId) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        if (!canAssignToRole(normalizedRoleId) && !Objects.equals(normalizedRoleId, RTown.PUBLIC_ROLE_ID)) {
            return Set.of();
        }

        final Set<String> permissionKeys = new LinkedHashSet<>();
        for (final TownPermissions permission : EnumSet.allOf(TownPermissions.class)) {
            if (permission.isDefaultForRole(normalizedRoleId)) {
                permissionKeys.add(permission.getPermissionKey());
            }
        }
        return Set.copyOf(permissionKeys);
    }

    /**
     * Normalizes a role identifier into the stored uppercase form.
     *
     * @param roleId raw role identifier
     * @return normalized role identifier, defaulting to {@link RTown#MEMBER_ROLE_ID} when blank
     */
    public static @NotNull String normalizeRoleId(final @Nullable String roleId) {
        final String normalizedRoleId = normalize(roleId);
        return normalizedRoleId.isEmpty() ? RTown.MEMBER_ROLE_ID : normalizedRoleId;
    }

    /**
     * Resolves a permission enum from a raw stored key.
     *
     * @param permissionKey raw permission key
     * @return matching enum, or {@code null} when no match exists
     */
    public static @Nullable TownPermissions fromKey(final @Nullable String permissionKey) {
        final String normalizedPermissionKey = normalize(permissionKey);
        if (normalizedPermissionKey.isEmpty()) {
            return null;
        }

        try {
            return TownPermissions.valueOf(normalizedPermissionKey);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int compareRolePriority(
        final @NotNull String left,
        final @NotNull String right
    ) {
        return Integer.compare(resolveRolePriority(left), resolveRolePriority(right));
    }

    private static int resolveRolePriority(final @NotNull String roleId) {
        return switch (roleId) {
            case RTown.RESTRICTED_ROLE_ID -> 0;
            case RTown.PUBLIC_ROLE_ID -> 1;
            case RTown.MEMBER_ROLE_ID -> 2;
            case RTown.MAYOR_ROLE_ID -> 4;
            default -> 3;
        };
    }
}
