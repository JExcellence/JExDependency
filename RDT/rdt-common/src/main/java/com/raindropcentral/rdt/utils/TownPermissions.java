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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Enumerates all town permission flags and their default owning role.
 *
 * <p>Permission values are stored as upper-case string keys (the enum constant name).</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.10
 */
public enum TownPermissions {

    /** Allows opening general town information from server town listings. */
    TOWN_INFO("public"),
    /** Allows requesting to join a town from public-facing town lists. */
    JOIN_TOWN("public"),
    /** Allows placing a town nexus block. */
    PLACE_NEXUS("mayor"),
    /** Allows picking up a previously placed town nexus block. */
    PICKUP_NEXUS("mayor"),
    /** Allows opening role-related management views. */
    VIEW_ROLES("mayor"),
    /** Allows configuring town and chunk protection role requirements. */
    TOWN_PROTECTIONS("mayor"),
    /** Allows placing a town Chunk Block in its target chunk. */
    PLACE_CHUNK("mayor"),
    /** Allows picking up a placed town Chunk Block through chunk management UI. */
    PICKUP_CHUNK("mayor"),
    /** Allows creating new town roles. */
    CREATE_ROLES("mayor"),
    /** Allows deleting custom town roles. */
    DELETE_ROLES("mayor"),
    /** Allows inviting players to join the town. */
    TOWN_INVITE("member"),
    /** Allows opening a town overview view. */
    VIEW_TOWN("member"),
    /** Allows opening and using the chunk-claim map view. */
    CLAIM_CHUNK("mayor"),
    /** Allows depositing currencies from players into the town bank. */
    TOWN_DEPOSIT("member"),
    /** Allows withdrawing currencies from the town bank to a player wallet. */
    TOWN_WITHDRAW("mayor"),
    /** Allows opening town level-up controls and triggering level progression. */
    TOWN_LEVEL_UP("mayor"),
    /** Allows opening chunk upgrade controls on a placed Chunk Block. */
    UPGRADE_CHUNK("mayor");

    private static final String PUBLIC_ROLE_ID = normalize("public");
    private static final String MEMBER_ROLE_ID = normalize("member");
    private static final String MAYOR_ROLE_ID = normalize("mayor");
    private static final String RESTRICTED_ROLE_ID = normalize("restricted");

    private final String defaultRoleId;

    /**
     * Creates a permission enum value with a default role assignment.
     *
     * @param defaultRoleId default role that should initially own this permission
     */
    TownPermissions(final @NotNull String defaultRoleId) {
        this.defaultRoleId = normalize(defaultRoleId);
    }

    /**
     * Returns the default role ID for this permission.
     *
     * @return normalized role ID
     */
    public @NotNull String getDefaultRoleId() {
        return this.defaultRoleId;
    }

    /**
     * Returns the persisted key used for permission checks.
     *
     * @return permission key
     */
    public @NotNull String getPermissionKey() {
        return this.name();
    }

    /**
     * Returns whether this permission belongs to the provided default role.
     *
     * @param roleId role ID to test
     * @return {@code true} when this permission is default for that role
     */
    public boolean isDefaultForRole(final @NotNull String roleId) {
        return this.defaultRoleId.equals(normalize(roleId));
    }

    /**
     * Resolves all permission keys that should be assigned to a role by default.
     *
     * @param roleId role ID
     * @return default permission keys for the role
     */
    public static @NotNull Set<String> defaultPermissionKeysForRole(final @NotNull String roleId) {
        final String normalizedRoleId = normalize(roleId);
        final Set<String> result = new LinkedHashSet<>();
        if (!canAssignToRole(normalizedRoleId)) {
            return result;
        }

        for (final TownPermissions permission : values()) {
            if (permission.defaultRoleId.equals(normalizedRoleId)
                    || inheritsPermission(normalizedRoleId, permission.defaultRoleId)) {
                result.add(permission.getPermissionKey());
            }
        }

        return result;
    }

    /**
     * Returns whether town permissions can be assigned to the provided role ID.
     *
     * @param roleId target role ID
     * @return {@code true} when permissions are assignable to the role
     */
    public static boolean canAssignToRole(final @NotNull String roleId) {
        return !RESTRICTED_ROLE_ID.equals(normalize(roleId));
    }

    private static boolean inheritsPermission(
            final @NotNull String roleId,
            final @NotNull String permissionDefaultRoleId
    ) {
        if (MAYOR_ROLE_ID.equals(roleId)) {
            return MEMBER_ROLE_ID.equals(permissionDefaultRoleId) || PUBLIC_ROLE_ID.equals(permissionDefaultRoleId);
        }
        if (MEMBER_ROLE_ID.equals(roleId)) {
            return PUBLIC_ROLE_ID.equals(permissionDefaultRoleId);
        }
        return false;
    }

    /**
     * Normalizes role and permission key fragments to upper-case storage format.
     *
     * @param value raw value
     * @return normalized value
     */
    public static @NotNull String normalize(final @NotNull String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
