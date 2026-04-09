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

package com.raindropcentral.rds.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Persists one RDT outpost's town-shop entitlement state.
 *
 * <p>The stored level controls the level-based base capacity, while bonus capacity tracks extra
 * admin-granted town shops that should survive later level syncs for the same outpost.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rds_town_shop_outposts")
@SuppressWarnings({
    "FieldCanBeLocal",
    "unused",
    "JpaDataSourceORMInspection"
})
public class TownShopOutpost extends BaseEntity {

    @Column(name = "protection_plugin", nullable = false, length = 32)
    private String protectionPlugin;

    @Column(name = "town_identifier", nullable = false, length = 128)
    private String townIdentifier;

    @Column(name = "town_display_name", nullable = false, length = 128)
    private String townDisplayName;

    @Column(name = "chunk_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID chunkUuid;

    @Column(name = "world_name", nullable = false, length = 64)
    private String worldName;

    @Column(name = "chunk_x", nullable = false)
    private int chunkX;

    @Column(name = "chunk_z", nullable = false)
    private int chunkZ;

    @Column(name = "chunk_level", nullable = false)
    private int chunkLevel;

    @Column(name = "bonus_shop_capacity", nullable = false)
    private int bonusShopCapacity;

    /**
     * Creates an outpost entitlement record.
     *
     * @param protectionPlugin source protection plugin id
     * @param townIdentifier stable town identifier
     * @param townDisplayName human-readable town name
     * @param chunkUuid stable outpost chunk UUID
     * @param worldName outpost world name
     * @param chunkX outpost chunk x
     * @param chunkZ outpost chunk z
     * @param chunkLevel persisted outpost level
     */
    public TownShopOutpost(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String townDisplayName,
        final @NotNull UUID chunkUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final int chunkLevel
    ) {
        this.protectionPlugin = normalizeProtectionPlugin(protectionPlugin);
        this.townIdentifier = normalizeTownIdentifier(townIdentifier);
        this.townDisplayName = normalizeTownDisplayName(townDisplayName, this.townIdentifier);
        this.chunkUuid = Objects.requireNonNull(chunkUuid, "chunkUuid");
        this.worldName = normalizeWorldName(worldName);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkLevel = Math.max(1, chunkLevel);
        this.bonusShopCapacity = 0;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected TownShopOutpost() {
    }

    /**
     * Returns the source protection plugin id.
     *
     * @return source protection plugin id
     */
    public @NotNull String getProtectionPlugin() {
        return this.protectionPlugin;
    }

    /**
     * Returns the stable town identifier.
     *
     * @return stable town identifier
     */
    public @NotNull String getTownIdentifier() {
        return this.townIdentifier;
    }

    /**
     * Returns the town display name.
     *
     * @return human-readable town display name
     */
    public @NotNull String getTownDisplayName() {
        return this.townDisplayName;
    }

    /**
     * Returns the stable outpost chunk UUID.
     *
     * @return outpost chunk UUID
     */
    public @NotNull UUID getChunkUuid() {
        return this.chunkUuid;
    }

    /**
     * Returns the outpost world name.
     *
     * @return world name
     */
    public @NotNull String getWorldName() {
        return this.worldName;
    }

    /**
     * Returns the outpost chunk x coordinate.
     *
     * @return chunk x coordinate
     */
    public int getChunkX() {
        return this.chunkX;
    }

    /**
     * Returns the outpost chunk z coordinate.
     *
     * @return chunk z coordinate
     */
    public int getChunkZ() {
        return this.chunkZ;
    }

    /**
     * Returns the stored outpost level.
     *
     * @return outpost level
     */
    public int getChunkLevel() {
        return this.chunkLevel;
    }

    /**
     * Returns the stored admin-granted bonus capacity.
     *
     * @return extra town-shop capacity beyond level unlocks
     */
    public int getBonusShopCapacity() {
        return Math.max(0, this.bonusShopCapacity);
    }

    /**
     * Returns the level-based base town-shop capacity.
     *
     * @return base capacity unlocked by the current outpost level
     */
    public int getBaseShopCapacity() {
        return resolveBaseShopCapacity(this.chunkLevel);
    }

    /**
     * Returns the total active town-shop capacity for this outpost.
     *
     * @return total town-shop capacity including bonus grants
     */
    public int getTotalShopCapacity() {
        return Math.max(0, this.getBaseShopCapacity() + this.getBonusShopCapacity());
    }

    /**
     * Replaces the stored outpost identity and level data.
     *
     * @param townDisplayName updated town display name
     * @param worldName updated world name
     * @param chunkX updated chunk x
     * @param chunkZ updated chunk z
     * @param chunkLevel updated outpost level
     */
    public void sync(
        final @NotNull String townDisplayName,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final int chunkLevel
    ) {
        this.townDisplayName = normalizeTownDisplayName(townDisplayName, this.townIdentifier);
        this.worldName = normalizeWorldName(worldName);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkLevel = Math.max(1, chunkLevel);
    }

    /**
     * Adds additional admin-granted shop capacity.
     *
     * @param amount amount of capacity to add
     * @return updated bonus capacity
     */
    public int addBonusShopCapacity(final int amount) {
        if (amount > 0) {
            this.bonusShopCapacity += amount;
        }
        return this.getBonusShopCapacity();
    }

    /**
     * Returns whether a location belongs to this outpost chunk.
     *
     * @param location location to inspect
     * @return {@code true} when the location is inside this outpost chunk
     */
    public boolean matchesLocation(final Location location) {
        return location != null
            && location.getWorld() != null
            && location.getWorld().getName().equalsIgnoreCase(this.worldName)
            && location.getChunk().getX() == this.chunkX
            && location.getChunk().getZ() == this.chunkZ;
    }

    /**
     * Resolves the base unlocked town-shop capacity for an outpost level.
     *
     * @param chunkLevel outpost level
     * @return base unlocked capacity for the supplied level
     */
    public static int resolveBaseShopCapacity(final int chunkLevel) {
        return switch (Math.max(1, chunkLevel)) {
            case 1, 2 -> 0;
            case 3 -> 1;
            case 4 -> 3;
            default -> 5;
        };
    }

    private static @NotNull String normalizeProtectionPlugin(final @NotNull String protectionPlugin) {
        final String normalized = Objects.requireNonNull(protectionPlugin, "protectionPlugin").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("protectionPlugin cannot be blank");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static @NotNull String normalizeTownIdentifier(final @NotNull String townIdentifier) {
        final String normalized = Objects.requireNonNull(townIdentifier, "townIdentifier").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("townIdentifier cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeTownDisplayName(
        final @NotNull String townDisplayName,
        final @NotNull String fallbackIdentifier
    ) {
        final String normalized = Objects.requireNonNull(townDisplayName, "townDisplayName").trim();
        return normalized.isEmpty() ? fallbackIdentifier : normalized;
    }

    private static @NotNull String normalizeWorldName(final @NotNull String worldName) {
        final String normalized = Objects.requireNonNull(worldName, "worldName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("worldName cannot be blank");
        }
        return normalized;
    }
}
