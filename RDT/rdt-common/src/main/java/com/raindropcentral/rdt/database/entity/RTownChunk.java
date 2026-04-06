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

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent claimed chunk owned by a town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_town_chunks")
public class RTownChunk extends BaseEntity {

    @Column(name = "chunk_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID chunkUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "world_name", nullable = false, length = 64)
    private String worldName;

    @Column(name = "x_loc", nullable = false)
    private int xLoc;

    @Column(name = "z_loc", nullable = false)
    private int zLoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false, length = 32)
    private ChunkType chunkType;

    @Column(name = "chunk_level", nullable = false)
    private int chunkLevel;

    @Convert(converter = LocationConverter.class)
    @Column(name = "chunk_block_location")
    private Location chunkBlockLocation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_chunk_protections", joinColumns = @JoinColumn(name = "chunk_id_fk"))
    @Column(name = "required_role_id", nullable = false, length = 64)
    private Map<String, String> protectionOverrides = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "seed_box_contents", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> seedBoxContents = new LinkedHashMap<>();

    /**
     * Creates a persisted town chunk.
     *
     * @param town owning town
     * @param worldName world name
     * @param xLoc chunk X
     * @param zLoc chunk Z
     * @param chunkType initial chunk type
     */
    public RTownChunk(
        final @NotNull RTown town,
        final @NotNull String worldName,
        final int xLoc,
        final int zLoc,
        final @NotNull ChunkType chunkType
    ) {
        this.chunkUuid = UUID.randomUUID();
        this.town = Objects.requireNonNull(town, "town");
        this.worldName = normalizeWorldName(worldName);
        this.xLoc = xLoc;
        this.zLoc = zLoc;
        this.chunkType = Objects.requireNonNull(chunkType, "chunkType");
        this.chunkLevel = 1;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RTownChunk() {
    }

    /**
     * Returns the stable chunk identifier.
     *
     * @return chunk UUID
     */
    public @NotNull UUID getIdentifier() {
        return this.chunkUuid;
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
     * Returns the claimed world name.
     *
     * @return claimed world name
     */
    public @NotNull String getWorldName() {
        return this.worldName;
    }

    /**
     * Returns the claimed chunk X coordinate.
     *
     * @return claimed chunk X coordinate
     */
    public int getX() {
        return this.xLoc;
    }

    /**
     * Returns the claimed chunk X coordinate.
     *
     * @return claimed chunk X coordinate
     */
    public int getXLoc() {
        return this.xLoc;
    }

    /**
     * Returns the claimed chunk X coordinate using the legacy name.
     *
     * @return claimed chunk X coordinate
     */
    public int getX_loc() {
        return this.xLoc;
    }

    /**
     * Returns the claimed chunk Z coordinate.
     *
     * @return claimed chunk Z coordinate
     */
    public int getZ() {
        return this.zLoc;
    }

    /**
     * Returns the claimed chunk Z coordinate.
     *
     * @return claimed chunk Z coordinate
     */
    public int getZLoc() {
        return this.zLoc;
    }

    /**
     * Returns the claimed chunk Z coordinate using the legacy name.
     *
     * @return claimed chunk Z coordinate
     */
    public int getZ_loc() {
        return this.zLoc;
    }

    /**
     * Returns the assigned chunk type.
     *
     * @return assigned chunk type
     */
    public @NotNull ChunkType getChunkType() {
        return this.chunkType;
    }

    /**
     * Returns the assigned chunk type using the legacy name.
     *
     * @return assigned chunk type
     */
    public @NotNull ChunkType getType() {
        return this.chunkType;
    }

    /**
     * Replaces the assigned chunk type.
     *
     * @param chunkType replacement chunk type
     */
    public void setChunkType(final @NotNull ChunkType chunkType) {
        final ChunkType validatedType = Objects.requireNonNull(chunkType, "chunkType");
        if (this.chunkType != validatedType) {
            this.chunkType = validatedType;
            this.chunkLevel = 1;
            this.protectionOverrides.clear();
            this.seedBoxContents.clear();
        }
    }

    /**
     * Returns the chunk upgrade level.
     *
     * @return chunk upgrade level
     */
    public int getChunkLevel() {
        return this.chunkLevel;
    }

    /**
     * Replaces the chunk upgrade level.
     *
     * @param chunkLevel replacement chunk level
     */
    public void setChunkLevel(final int chunkLevel) {
        this.chunkLevel = Math.max(1, chunkLevel);
    }

    /**
     * Returns the stored marker block location for this chunk.
     *
     * @return marker block location, or {@code null} when not placed
     */
    public @Nullable Location getChunkBlockLocation() {
        return this.chunkBlockLocation == null ? null : this.chunkBlockLocation.clone();
    }

    /**
     * Replaces the marker block location for this chunk.
     *
     * @param chunkBlockLocation replacement marker location
     */
    public void setChunkBlockLocation(final @Nullable Location chunkBlockLocation) {
        this.chunkBlockLocation = chunkBlockLocation == null ? null : chunkBlockLocation.clone();
    }

    /**
     * Returns a defensive copy of the chunk protection overrides.
     *
     * @return configured protection overrides
     */
    public @NotNull Map<String, String> getProtectionRoleIds() {
        return new LinkedHashMap<>(this.protectionOverrides);
    }

    /**
     * Returns the configured role override for a protection.
     *
     * @param protection protection to resolve
     * @return configured role override, or {@code null} when the chunk inherits the town setting
     */
    public @Nullable String getProtectionRoleId(final @Nullable TownProtections protection) {
        if (protection == null) {
            return null;
        }
        return this.protectionOverrides.get(protection.getProtectionKey());
    }

    /**
     * Stores or clears the chunk-level override for a protection.
     *
     * @param protection protection to update
     * @param roleId replacement required role, or {@code null} to clear the override
     */
    public void setProtectionRoleId(
        final @NotNull TownProtections protection,
        final @Nullable String roleId
    ) {
        Objects.requireNonNull(protection, "protection");
        if (roleId == null || roleId.isBlank()) {
            this.protectionOverrides.remove(protection.getProtectionKey());
            return;
        }
        this.protectionOverrides.put(
            protection.getProtectionKey(),
            TownProtections.normalizeRoleId(roleId)
        );
    }

    /**
     * Returns whether the chunk overrides the supplied protection.
     *
     * @param protection protection to resolve
     * @return {@code true} when the chunk has an explicit override
     */
    public boolean overridesProtection(final @Nullable TownProtections protection) {
        return protection != null && this.protectionOverrides.containsKey(protection.getProtectionKey());
    }

    /**
     * Returns a defensive copy of the farm seed-box contents.
     *
     * @return copied seed-box contents
     */
    public @NotNull Map<String, ItemStack> getSeedBoxContents() {
        return new LinkedHashMap<>(this.seedBoxContents);
    }

    /**
     * Replaces the farm seed-box contents.
     *
     * @param seedBoxContents replacement seed-box contents
     */
    public void setSeedBoxContents(final @NotNull Map<String, ItemStack> seedBoxContents) {
        this.seedBoxContents = new LinkedHashMap<>(Objects.requireNonNull(seedBoxContents, "seedBoxContents"));
    }

    private static @NotNull String normalizeWorldName(final @NotNull String worldName) {
        final String normalized = Objects.requireNonNull(worldName, "worldName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("worldName cannot be blank");
        }
        return normalized;
    }
}
