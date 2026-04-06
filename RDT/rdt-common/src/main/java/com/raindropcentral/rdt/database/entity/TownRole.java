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
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Persistent role definition for a town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_town_roles")
public class TownRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "role_id", nullable = false, length = 64)
    private String roleId;

    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    @Column(name = "role_priority", nullable = false)
    private int rolePriority;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_town_role_permissions", joinColumns = @JoinColumn(name = "role_id_fk"))
    @Column(name = "permission_key", nullable = false, length = 64)
    private Set<String> permissions = new LinkedHashSet<>();

    /**
     * Creates a custom town role with default system-role detection.
     *
     * @param town owning town
     * @param roleId stable role identifier
     * @param roleName display name
     * @param permissions initial permission set
     */
    public TownRole(
        final @NotNull RTown town,
        final @NotNull String roleId,
        final @NotNull String roleName,
        final @NotNull Set<String> permissions
    ) {
        this(
            town,
            roleId,
            roleName,
            permissions,
            RTown.resolveDefaultRolePriority(roleId),
            RTown.isDefaultRoleId(roleId)
        );
    }

    /**
     * Creates a town role.
     *
     * @param town owning town
     * @param roleId stable role identifier
     * @param roleName display name
     * @param permissions initial permission set
     * @param rolePriority priority used for ordered comparisons
     * @param systemRole whether the role is a built-in non-removable role
     */
    public TownRole(
        final @NotNull RTown town,
        final @NotNull String roleId,
        final @NotNull String roleName,
        final @NotNull Set<String> permissions,
        final int rolePriority,
        final boolean systemRole
    ) {
        this.town = Objects.requireNonNull(town, "town");
        this.roleId = RTown.normalizeRoleId(roleId);
        this.roleName = normalizeRoleName(roleName);
        this.rolePriority = rolePriority;
        this.systemRole = systemRole || RTown.isDefaultRoleId(this.roleId);
        this.permissions = new LinkedHashSet<>();
        this.replacePermissions(permissions);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected TownRole() {
    }

    /**
     * Returns the owning town.
     *
     * @return owning town
     */
    public @NotNull RTown getTown() {
        return this.town;
    }

    /**
     * Returns the stable role identifier.
     *
     * @return stable uppercase role identifier
     */
    public @NotNull String getRoleId() {
        return this.roleId;
    }

    /**
     * Returns the display name for this role.
     *
     * @return display name
     */
    public @NotNull String getRoleName() {
        return this.roleName;
    }

    /**
     * Replaces the display name for this role.
     *
     * @param roleName replacement role name
     */
    public void setRoleName(final @NotNull String roleName) {
        if (Objects.equals(this.roleId, RTown.RESTRICTED_ROLE_ID)) {
            return;
        }
        this.roleName = normalizeRoleName(roleName);
    }

    /**
     * Returns the relative role priority.
     *
     * @return role priority
     */
    public int getRolePriority() {
        return this.rolePriority;
    }

    /**
     * Returns whether this is a built-in system role.
     *
     * @return {@code true} when the role is a system role
     */
    public boolean isSystemRole() {
        return this.systemRole;
    }

    /**
     * Returns the mutable stored permission set.
     *
     * @return mutable permission set
     */
    public @NotNull Set<String> getPermissions() {
        return this.permissions;
    }

    /**
     * Returns whether the role grants the supplied permission key.
     *
     * @param permissionKey permission key to resolve
     * @return {@code true} when the role grants the permission
     */
    public boolean hasPermission(final @Nullable String permissionKey) {
        final TownPermissions permission = TownPermissions.fromKey(permissionKey);
        return permission != null && this.hasPermission(permission);
    }

    /**
     * Returns whether the role grants the supplied permission.
     *
     * @param permission permission to resolve
     * @return {@code true} when the role grants the permission
     */
    public boolean hasPermission(final @Nullable TownPermissions permission) {
        if (permission == null) {
            return false;
        }
        if (Objects.equals(this.roleId, RTown.RESTRICTED_ROLE_ID)) {
            return false;
        }
        if (Objects.equals(this.roleId, RTown.MAYOR_ROLE_ID)) {
            return true;
        }
        if (Objects.equals(this.roleId, RTown.PUBLIC_ROLE_ID)) {
            return permission.isDefaultForRole(this.roleId);
        }
        return this.permissions.contains(permission.getPermissionKey());
    }

    /**
     * Replaces the stored permission set.
     *
     * @param permissions replacement permission set
     */
    public void replacePermissions(final @NotNull Set<String> permissions) {
        if (!this.canMutatePermissions()) {
            this.permissions.clear();
            return;
        }
        this.permissions.clear();
        this.permissions.addAll(this.normalizePermissions(permissions));
    }

    /**
     * Adds a permission key to this role.
     *
     * @param permissionKey permission key to add
     */
    public void addPermission(final @NotNull String permissionKey) {
        if (!this.canMutatePermissions()) {
            return;
        }
        final TownPermissions permission = TownPermissions.fromKey(permissionKey);
        if (permission != null) {
            this.permissions.add(permission.getPermissionKey());
        }
    }

    /**
     * Adds a permission to this role.
     *
     * @param permission permission to add
     */
    public void addPermission(final @Nullable TownPermissions permission) {
        if (permission != null) {
            this.addPermission(permission.getPermissionKey());
        }
    }

    /**
     * Removes a permission key from this role.
     *
     * @param permissionKey permission key to remove
     */
    public void removePermission(final @NotNull String permissionKey) {
        if (!this.canMutatePermissions()) {
            return;
        }
        final TownPermissions permission = TownPermissions.fromKey(permissionKey);
        if (permission != null) {
            this.permissions.remove(permission.getPermissionKey());
        }
    }

    /**
     * Removes a permission from this role.
     *
     * @param permission permission to remove
     */
    public void removePermission(final @Nullable TownPermissions permission) {
        if (permission != null) {
            this.removePermission(permission.getPermissionKey());
        }
    }

    /**
     * Toggles a permission key on this role.
     *
     * @param permissionKey permission key to toggle
     * @return {@code true} when the permission ended enabled
     */
    public boolean togglePermission(final @NotNull String permissionKey) {
        final TownPermissions permission = TownPermissions.fromKey(permissionKey);
        return permission != null && this.togglePermission(permission);
    }

    /**
     * Toggles a permission on this role.
     *
     * @param permission permission to toggle
     * @return {@code true} when the permission ended enabled
     */
    public boolean togglePermission(final @Nullable TownPermissions permission) {
        if (permission == null || !this.canMutatePermissions()) {
            return false;
        }
        if (this.permissions.contains(permission.getPermissionKey())) {
            this.permissions.remove(permission.getPermissionKey());
            return false;
        }
        this.permissions.add(permission.getPermissionKey());
        return true;
    }

    @Override
    public @NotNull String toString() {
        return this.roleId + "(" + this.roleName + ")";
    }

    private boolean canMutatePermissions() {
        return !Objects.equals(this.roleId, RTown.RESTRICTED_ROLE_ID)
            && !Objects.equals(this.roleId, RTown.PUBLIC_ROLE_ID)
            && !Objects.equals(this.roleId, RTown.MAYOR_ROLE_ID);
    }

    private @NotNull Set<String> normalizePermissions(final @NotNull Set<String> permissionKeys) {
        final Set<String> normalized = new LinkedHashSet<>();
        for (final String permissionKey : permissionKeys) {
            final TownPermissions permission = TownPermissions.fromKey(permissionKey);
            if (permission != null) {
                normalized.add(permission.getPermissionKey());
            }
        }
        return normalized;
    }

    private static @NotNull String normalizeRoleName(final @NotNull String rawRoleName) {
        final String trimmed = Objects.requireNonNull(rawRoleName, "roleName").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("roleName cannot be blank");
        }
        return Character.toUpperCase(trimmed.charAt(0))
            + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
}
