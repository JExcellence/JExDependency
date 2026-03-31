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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent representation of a player participating in Raindrop Towns.
 *
 * <p>Stores the player's UUID, optional town membership, and assigned town role ID. The role ID is
 * used to resolve the member's {@link TownRole} definition for permission checks.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.3
 */
@Entity
@Table(name = "rdt_players")
@SuppressWarnings({
        "DefaultAnnotationParam",
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the RDTPlayer API type.
 */
public class RDTPlayer extends BaseEntity {

    /** Default role assigned to newly joined town members. */
    private static final String DEFAULT_TOWN_ROLE_ID = RTown.MEMBER_ROLE_ID;

    /** Player's unique UUID (public identifier and cache key). */
    @Column(name = "player_uuid", unique = true, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID player_uuid;

    /** UUID of the town this player is currently a member of; {@code null} when not in a town. */
    @Column(name = "town_uuid", unique = false, nullable = true)
    @Convert(converter = UUIDConverter.class)
    private UUID town_uuid;

    /** Timestamp (epoch millis) when the player last joined their current town. */
    @Column(name = "join_date", unique = false, nullable = true)
    private long townJoinDate;

    /** Town role identifier used to resolve the member's permission model. */
    @Column(name = "town_role_id", unique = false, nullable = true)
    private String townRoleId;

    /** Explicit permission keys assigned to this player for town actions. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_player_town_permissions", joinColumns = @JoinColumn(name = "rdt_player_id"))
    @Column(name = "permission", nullable = false)
    private final Set<String> townPermissions = new HashSet<>();

    /**
     * Constructs a player record with an initial town membership.
     *
     * @param player_uuid player identifier
     * @param town_uuid joined town identifier
     */
    public RDTPlayer(final UUID player_uuid, final UUID town_uuid) {
        this(player_uuid, town_uuid, DEFAULT_TOWN_ROLE_ID);
    }

    /**
     * Constructs a player record with an initial town membership and explicit role ID.
     *
     * @param player_uuid player identifier
     * @param town_uuid joined town identifier
     * @param townRoleId assigned role ID
     */
    public RDTPlayer(
            final UUID player_uuid,
            final UUID town_uuid,
            final String townRoleId
    ) {
        this.player_uuid = player_uuid;
        this.town_uuid = town_uuid;
        this.townRoleId = townRoleId == null ? null : RTown.normalizeRoleId(townRoleId);
        if (this.townRoleId != null) {
            this.townPermissions.addAll(TownPermissions.defaultPermissionKeysForRole(this.townRoleId));
        }
        this.townJoinDate = System.currentTimeMillis();
    }

    /**
     * Constructs a player record with no town membership.
     *
     * @param player_uuid player identifier
     */
    public RDTPlayer(final UUID player_uuid) {
        this.player_uuid = player_uuid;
        this.town_uuid = null;
        this.townRoleId = null;
    }

    /** Required by Hibernate. */
    protected RDTPlayer() {
    }

    /**
     * Returns the player's unique identifier used by repositories and caches.
     *
     * @return player UUID identifier
     */
    public UUID getIdentifier() {
        return this.player_uuid;
    }

    /**
     * Returns the current town UUID or {@code null} if the player is unaffiliated.
     *
     * @return town UUID
     */
    public UUID getTownUUID() {
        return this.town_uuid;
    }

    /**
     * Returns the player's town role identifier.
     *
     * @return role ID, or {@code null} when unaffiliated
     */
    public @Nullable String getTownRoleId() {
        return this.townRoleId;
    }

    /**
     * Returns explicit permission keys currently assigned to this player.
     *
     * @return mutable permission key set
     */
    public Set<String> getTownPermissions() {
        return this.townPermissions;
    }

    /**
     * Returns whether this player currently has a specific town permission.
     *
     * @param permission permission enum value
     * @return {@code true} when enabled
     */
    public boolean hasTownPermission(final @NotNull TownPermissions permission) {
        return this.hasTownPermission(permission.getPermissionKey());
    }

    /**
     * Returns whether this player currently has a specific permission key.
     *
     * @param permissionKey permission key
     * @return {@code true} when enabled
     */
    public boolean hasTownPermission(final @NotNull String permissionKey) {
        return this.townPermissions.contains(normalizePermissionKey(permissionKey));
    }

    /**
     * Sets the player's town membership and records the current time as the join date.
     *
     * @param town_uuid new town UUID, or {@code null} to clear membership
     */
    public void setTownUUID(final @Nullable UUID town_uuid) {
        this.town_uuid = town_uuid;
        this.townJoinDate = System.currentTimeMillis();
        if (town_uuid == null) {
            this.townRoleId = null;
            this.townPermissions.clear();
            return;
        }
        if (this.townRoleId == null || this.townRoleId.isBlank()) {
            this.townRoleId = DEFAULT_TOWN_ROLE_ID;
        }
        if (this.townPermissions.isEmpty()) {
            this.townPermissions.addAll(TownPermissions.defaultPermissionKeysForRole(this.townRoleId));
        }
    }

    /**
     * Sets the player's explicit town role identifier.
     *
     * @param townRoleId role ID, or {@code null} to clear
     */
    public void setTownRoleId(final @Nullable String townRoleId) {
        if (townRoleId == null) {
            this.townRoleId = null;
            return;
        }
        this.townRoleId = RTown.normalizeRoleId(townRoleId);
        if (this.town_uuid != null && this.townPermissions.isEmpty()) {
            this.townPermissions.addAll(TownPermissions.defaultPermissionKeysForRole(this.townRoleId));
        }
    }

    /**
     * Replaces this player's explicit town permissions with the provided set.
     *
     * @param permissionKeys replacement permission keys
     */
    public void replaceTownPermissions(final @NotNull Set<String> permissionKeys) {
        this.townPermissions.clear();
        for (final String permissionKey : permissionKeys) {
            this.townPermissions.add(normalizePermissionKey(permissionKey));
        }
    }

    /**
     * Synchronizes this player's explicit permissions from a role definition.
     *
     * @param role role definition, or {@code null} to clear permissions
     */
    public void syncTownPermissionsFromRole(final @Nullable TownRole role) {
        if (role == null) {
            this.townPermissions.clear();
            return;
        }
        this.replaceTownPermissions(role.getPermissions());
    }

    /**
     * Grants a specific town permission.
     *
     * @param permission permission to grant
     */
    public void grantTownPermission(final @NotNull TownPermissions permission) {
        this.townPermissions.add(permission.getPermissionKey());
    }

    /**
     * Revokes a specific town permission.
     *
     * @param permission permission to revoke
     */
    public void revokeTownPermission(final @NotNull TownPermissions permission) {
        this.townPermissions.remove(permission.getPermissionKey());
    }

    /**
     * Toggles a specific town permission.
     *
     * @param permission permission to toggle
     * @return {@code true} when the permission is now enabled
     */
    public boolean toggleTownPermission(final @NotNull TownPermissions permission) {
        final String permissionKey = permission.getPermissionKey();
        if (this.townPermissions.contains(permissionKey)) {
            this.townPermissions.remove(permissionKey);
            return false;
        }
        this.townPermissions.add(permissionKey);
        return true;
    }

    /**
     * Legacy alias for role ID retrieval.
     *
     * @return role ID, or {@code null} when unaffiliated
     * @deprecated use {@link #getTownRoleId()} instead
     */
    @Deprecated
    public @Nullable String getRole() {
        return this.getTownRoleId();
    }

    /**
     * Legacy alias for setting role IDs.
     *
     * @param role role ID value
     * @deprecated use {@link #setTownRoleId(String)} instead
     */
    @Deprecated
    public void setRole(final @Nullable String role) {
        this.setTownRoleId(role);
    }

    private static @NotNull String normalizePermissionKey(final @NotNull String permissionKey) {
        return permissionKey.trim().toUpperCase(Locale.ROOT);
    }
}
