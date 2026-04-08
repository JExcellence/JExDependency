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
import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent player membership row for Raindrop Towns.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_players")
public class RDTPlayer extends BaseEntity {

    private static final String DEFAULT_TOWN_ROLE_ID = RTown.MEMBER_ROLE_ID;

    @Column(name = "player_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID player_uuid;

    @Column(name = "town_uuid")
    @Convert(converter = UUIDConverter.class)
    private UUID town_uuid;

    @Column(name = "town_join_date", nullable = false)
    private long townJoinDate;

    @Column(name = "town_role_id", length = 64)
    private String townRoleId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_player_permissions", joinColumns = @JoinColumn(name = "player_id_fk"))
    @Column(name = "permission_key", nullable = false, length = 64)
    private Set<String> townPermissions = new LinkedHashSet<>();

    @Column(name = "boss_bar_enabled", nullable = false)
    private boolean bossBarEnabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_player_creation_currency_progress", joinColumns = @JoinColumn(name = "player_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> townCreationCurrencyProgress = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "town_creation_item_progress", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> townCreationItemProgress = new LinkedHashMap<>();

    /**
     * Creates a player profile that already belongs to a town as a default member.
     *
     * @param playerUuid player UUID
     * @param townUuid town UUID
     */
    public RDTPlayer(final @NotNull UUID playerUuid, final @Nullable UUID townUuid) {
        this(playerUuid, townUuid, DEFAULT_TOWN_ROLE_ID);
    }

    /**
     * Creates a player profile with an explicit town role.
     *
     * @param playerUuid player UUID
     * @param townUuid town UUID
     * @param townRoleId role identifier
     */
    public RDTPlayer(
        final @NotNull UUID playerUuid,
        final @Nullable UUID townUuid,
        final @Nullable String townRoleId
    ) {
        this.player_uuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.town_uuid = townUuid;
        this.townJoinDate = System.currentTimeMillis();
        this.bossBarEnabled = true;
        this.setTownRoleId(townRoleId);
        if (townUuid == null) {
            this.townRoleId = null;
            this.townPermissions.clear();
        }
    }

    /**
     * Creates a player profile with no town membership.
     *
     * @param playerUuid player UUID
     */
    public RDTPlayer(final @NotNull UUID playerUuid) {
        this(playerUuid, null, null);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDTPlayer() {
    }

    /**
     * Returns the stable player identifier.
     *
     * @return player UUID
     */
    public @NotNull UUID getIdentifier() {
        return this.player_uuid;
    }

    /**
     * Returns the player's current town UUID.
     *
     * @return town UUID, or {@code null} when the player is not in a town
     */
    public @Nullable UUID getTownUUID() {
        return this.town_uuid;
    }

    /**
     * Returns the player's current town UUID.
     *
     * @return town UUID, or {@code null} when the player is not in a town
     */
    public @Nullable UUID getTownUuid() {
        return this.getTownUUID();
    }

    /**
     * Returns the current town role identifier.
     *
     * @return normalized role identifier, or {@code null} when not in a town
     */
    public @Nullable String getTownRoleId() {
        return this.townRoleId;
    }

    /**
     * Returns the mutable cached town permission set.
     *
     * @return mutable cached permission set
     */
    public @NotNull Set<String> getTownPermissions() {
        return this.townPermissions;
    }

    /**
     * Returns whether the player currently has a specific town permission.
     *
     * @param permission permission to resolve
     * @return {@code true} when the player has the permission
     */
    public boolean hasTownPermission(final @Nullable TownPermissions permission) {
        return permission != null && this.hasTownPermission(permission.getPermissionKey());
    }

    /**
     * Returns whether the player currently has a specific town permission.
     *
     * @param permissionKey permission key to resolve
     * @return {@code true} when the player has the permission
     */
    public boolean hasTownPermission(final @Nullable String permissionKey) {
        final TownPermissions permission = TownPermissions.fromKey(permissionKey);
        return permission != null && this.townPermissions.contains(permission.getPermissionKey());
    }

    /**
     * Replaces the player's town membership.
     *
     * @param townUuid replacement town UUID, or {@code null} to clear membership
     */
    public void setTownUUID(final @Nullable UUID townUuid) {
        this.town_uuid = townUuid;
        if (townUuid == null) {
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
     * Replaces the cached town role identifier.
     *
     * @param townRoleId replacement role identifier
     */
    public void setTownRoleId(final @Nullable String townRoleId) {
        if (this.town_uuid == null && (townRoleId == null || townRoleId.isBlank())) {
            this.townRoleId = null;
            return;
        }
        this.townRoleId = RTown.normalizeRoleId(
            townRoleId == null || townRoleId.isBlank() ? DEFAULT_TOWN_ROLE_ID : townRoleId
        );
        if (this.townPermissions.isEmpty()) {
            this.townPermissions.addAll(TownPermissions.defaultPermissionKeysForRole(this.townRoleId));
        }
    }

    /**
     * Replaces the cached permission set after normalizing each permission key.
     *
     * @param permissionKeys replacement permission keys
     */
    public void replaceTownPermissions(final @NotNull Set<String> permissionKeys) {
        this.townPermissions.clear();
        for (final String permissionKey : permissionKeys) {
            final TownPermissions permission = TownPermissions.fromKey(permissionKey);
            if (permission != null) {
                this.townPermissions.add(permission.getPermissionKey());
            }
        }
    }

    /**
     * Syncs cached permissions from a town role definition.
     *
     * @param townRole role definition to mirror
     */
    public void syncTownPermissionsFromRole(final @Nullable TownRole townRole) {
        if (townRole == null) {
            this.replaceTownPermissions(Set.of());
            return;
        }
        this.townRoleId = townRole.getRoleId();
        if (Objects.equals(this.townRoleId, RTown.MAYOR_ROLE_ID)) {
            this.replaceTownPermissions(TownPermissions.defaultPermissionKeysForRole(RTown.MAYOR_ROLE_ID));
            return;
        }
        this.replaceTownPermissions(new LinkedHashSet<>(townRole.getPermissions()));
    }

    /**
     * Grants a town permission to this player.
     *
     * @param permission permission to grant
     */
    public void grantTownPermission(final @Nullable TownPermissions permission) {
        if (permission != null) {
            this.townPermissions.add(permission.getPermissionKey());
        }
    }

    /**
     * Revokes a town permission from this player.
     *
     * @param permission permission to revoke
     */
    public void revokeTownPermission(final @Nullable TownPermissions permission) {
        if (permission != null) {
            this.townPermissions.remove(permission.getPermissionKey());
        }
    }

    /**
     * Toggles a town permission on this player.
     *
     * @param permission permission to toggle
     * @return {@code true} when the permission ended enabled
     */
    public boolean toggleTownPermission(final @Nullable TownPermissions permission) {
        if (permission == null) {
            return false;
        }
        if (this.townPermissions.contains(permission.getPermissionKey())) {
            this.townPermissions.remove(permission.getPermissionKey());
            return false;
        }
        this.townPermissions.add(permission.getPermissionKey());
        return true;
    }

    /**
     * Returns the legacy role alias used by earlier code.
     *
     * @return town role identifier
     */
    public @Nullable String getRole() {
        return this.getTownRoleId();
    }

    /**
     * Replaces the legacy role alias used by earlier code.
     *
     * @param role replacement role identifier
     */
    public void setRole(final @Nullable String role) {
        this.setTownRoleId(role);
    }

    /**
     * Returns whether the player is currently the mayor role.
     *
     * @return {@code true} when the player's cached role is mayor
     */
    public boolean isMayor() {
        return Objects.equals(this.townRoleId, RTown.MAYOR_ROLE_ID);
    }

    /**
     * Returns whether the player wants the town boss bar enabled.
     *
     * @return {@code true} when the boss bar should be shown
     */
    public boolean isBossBarEnabled() {
        return this.bossBarEnabled;
    }

    /**
     * Replaces the persisted boss-bar preference.
     *
     * @param bossBarEnabled replacement boss-bar enabled state
     */
    public void setBossBarEnabled(final boolean bossBarEnabled) {
        this.bossBarEnabled = bossBarEnabled;
    }

    /**
     * Returns stored town-creation item progress.
     *
     * @return stored town-creation item progress
     */
    public @NotNull Map<String, ItemStack> getTownCreationItemProgress() {
        return new LinkedHashMap<>(this.townCreationItemProgress);
    }

    /**
     * Returns stored town-creation item progress for a key.
     *
     * @param progressKey progress entry key
     * @return stored progress item, or {@code null} when absent
     */
    public @Nullable ItemStack getTownCreationItemProgress(final @NotNull String progressKey) {
        final ItemStack storedItem = this.townCreationItemProgress.get(normalizeProgressKey(progressKey));
        return storedItem == null ? null : storedItem.clone();
    }

    /**
     * Replaces stored town-creation item progress for a key.
     *
     * @param progressKey progress entry key
     * @param itemStack stored progress item, or {@code null} to clear the key
     */
    public void setTownCreationItemProgress(
        final @NotNull String progressKey,
        final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (itemStack == null || itemStack.isEmpty()) {
            this.townCreationItemProgress.remove(normalizedProgressKey);
            return;
        }
        this.townCreationItemProgress.put(normalizedProgressKey, itemStack.clone());
    }

    /**
     * Returns stored town-creation currency progress for a key.
     *
     * @param progressKey progress entry key
     * @return stored progress amount
     */
    public double getTownCreationCurrencyProgress(final @NotNull String progressKey) {
        return this.townCreationCurrencyProgress.getOrDefault(normalizeProgressKey(progressKey), 0.0D);
    }

    /**
     * Returns stored town-creation currency progress.
     *
     * @return stored town-creation currency progress
     */
    public @NotNull Map<String, Double> getTownCreationCurrencyProgress() {
        return new LinkedHashMap<>(this.townCreationCurrencyProgress);
    }

    /**
     * Replaces stored town-creation currency progress for a key.
     *
     * @param progressKey progress entry key
     * @param amount stored progress amount
     */
    public void setTownCreationCurrencyProgress(final @NotNull String progressKey, final double amount) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (amount <= 0.0D) {
            this.townCreationCurrencyProgress.remove(normalizedProgressKey);
            return;
        }
        this.townCreationCurrencyProgress.put(normalizedProgressKey, amount);
    }

    /**
     * Clears stored town-creation requirement progress that matches a stable prefix.
     *
     * @param progressKeyPrefix stable progress-key prefix
     */
    public void clearTownCreationRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String normalizedPrefix = normalizeProgressKey(progressKeyPrefix);
        this.townCreationItemProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
        this.townCreationCurrencyProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
    }

    /**
     * Clears all stored town-creation progress.
     */
    public void clearTownCreationProgress() {
        this.townCreationItemProgress.clear();
        this.townCreationCurrencyProgress.clear();
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalized = Objects.requireNonNull(progressKey, "progressKey").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalized;
    }
}
