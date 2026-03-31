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
import jakarta.persistence.UniqueConstraint;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Persistent role definition owned by a town.
 *
 * <p>Each role stores a stable role ID, display name, and a set of string permissions used by
 * town action checks.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
@Entity
@Table(
        name = "town_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"town_id", "role_id"})
)
@SuppressWarnings({
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the TownRole API type.
 */
public class TownRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "role_id", nullable = false)
    private String roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "town_role_permissions", joinColumns = @JoinColumn(name = "town_role_id"))
    @Column(name = "permission", nullable = false)
    private final Set<String> permissions = new HashSet<>();

    /** Required by Hibernate. */
    protected TownRole() {
    }

    /**
     * Creates a persisted role definition.
     *
     * @param town owning town
     * @param roleId unique role ID in the town
     * @param roleName display name
     * @param permissions role permission set
     */
    public TownRole(
            final @NonNull RTown town,
            final @NonNull String roleId,
            final @NonNull String roleName,
            final @NonNull Set<String> permissions
    ) {
        this.town = town;
        this.roleId = RTown.normalizeRoleId(roleId);
        this.roleName = roleName.trim();
        this.permissions.addAll(this.normalizePermissions(permissions));
    }

    /**
     * Returns the role ID.
     *
     * @return normalized role ID
     */
    public String getRoleId() {
        return this.roleId;
    }

    /**
     * Returns the display name.
     *
     * @return role display name
     */
    public String getRoleName() {
        return this.roleName;
    }

    /**
     * Updates the display name.
     *
     * @param roleName new role display name
     */
    public void setRoleName(final @NonNull String roleName) {
        this.roleName = roleName.trim();
    }

    /**
     * Returns the mutable permission collection for this role.
     *
     * @return role permissions
     */
    public Set<String> getPermissions() {
        return this.permissions;
    }

    /**
     * Returns whether this role currently contains a specific permission key.
     *
     * @param permissionKey permission key
     * @return {@code true} when this role has the permission
     */
    public boolean hasPermission(final @NonNull String permissionKey) {
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return false;
        }
        return this.permissions.contains(normalizePermission(permissionKey));
    }

    /**
     * Returns whether this role currently contains a specific town permission.
     *
     * @param permission town permission enum value
     * @return {@code true} when this role has the permission
     */
    public boolean hasPermission(final @NonNull TownPermissions permission) {
        return this.hasPermission(permission.getPermissionKey());
    }

    /**
     * Replaces all permissions with the provided set.
     *
     * @param permissions replacement permission set
     */
    public void replacePermissions(final @NonNull Set<String> permissions) {
        this.permissions.clear();
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return;
        }
        this.permissions.addAll(this.normalizePermissions(permissions));
    }

    /**
     * Adds a single permission value.
     *
     * @param permission permission to add
     */
    public void addPermission(final @NonNull String permission) {
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return;
        }
        this.permissions.add(normalizePermission(permission));
    }

    /**
     * Adds a single enum-based permission value.
     *
     * @param permission permission to add
     */
    public void addPermission(final @NonNull TownPermissions permission) {
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return;
        }
        this.permissions.add(permission.getPermissionKey());
    }

    /**
     * Removes a single permission value.
     *
     * @param permission permission to remove
     */
    public void removePermission(final @NonNull String permission) {
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return;
        }
        this.permissions.remove(normalizePermission(permission));
    }

    /**
     * Removes a single enum-based permission value.
     *
     * @param permission permission to remove
     */
    public void removePermission(final @NonNull TownPermissions permission) {
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return;
        }
        this.permissions.remove(permission.getPermissionKey());
    }

    /**
     * Toggles a permission key on this role.
     *
     * @param permissionKey permission key to toggle
     * @return {@code true} when the permission is now enabled
     */
    public boolean togglePermission(final @NonNull String permissionKey) {
        if (!TownPermissions.canAssignToRole(this.roleId)) {
            return false;
        }
        final String normalizedPermission = normalizePermission(permissionKey);
        if (this.permissions.contains(normalizedPermission)) {
            this.permissions.remove(normalizedPermission);
            return false;
        }
        this.permissions.add(normalizedPermission);
        return true;
    }

    /**
     * Toggles an enum-based permission on this role.
     *
     * @param permission permission to toggle
     * @return {@code true} when the permission is now enabled
     */
    public boolean togglePermission(final @NonNull TownPermissions permission) {
        return this.togglePermission(permission.getPermissionKey());
    }

    /**
     * Returns a debug summary of this role entity.
     *
     * @return formatted role summary string
     */
    @Override
    public String toString() {
        return "TownRole{" +
                "roleId='" + this.roleId + '\'' +
                ", roleName='" + this.roleName + '\'' +
                ", permissions=" + this.permissions +
                '}';
    }

    private @NonNull Set<String> normalizePermissions(final @NonNull Set<String> permissions) {
        final Set<String> normalized = new HashSet<>();
        for (final String permission : permissions) {
            normalized.add(normalizePermission(permission));
        }
        return normalized;
    }

    private static @NonNull String normalizePermission(final @NonNull String permission) {
        return permission.trim().toUpperCase(Locale.ROOT);
    }
}
