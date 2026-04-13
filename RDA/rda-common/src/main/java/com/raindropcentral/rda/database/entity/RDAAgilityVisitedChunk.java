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

package com.raindropcentral.rda.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Persisted per-player agility chunk visit used for one-time exploration bonuses.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@Entity
@Table(
    name = "rda_agility_visited_chunks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"chunk_key"})
)
public class RDAAgilityVisitedChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id_fk", nullable = false)
    private RDAPlayer playerProfile;

    @Column(name = "chunk_key", nullable = false, unique = true, length = 255)
    private String chunkKey;

    @Column(name = "world_name", nullable = false, length = 128)
    private String worldName;

    @Column(name = "chunk_x", nullable = false)
    private int chunkX;

    @Column(name = "chunk_z", nullable = false)
    private int chunkZ;

    /**
     * Creates a visited chunk row.
     *
     * @param playerProfile owning player profile
     * @param chunk visited chunk
     */
    public RDAAgilityVisitedChunk(final @NotNull RDAPlayer playerProfile, final @NotNull Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        this.playerProfile = Objects.requireNonNull(playerProfile, "playerProfile");
        this.worldName = chunk.getWorld().getName();
        this.chunkX = chunk.getX();
        this.chunkZ = chunk.getZ();
        this.chunkKey = toChunkKey(playerProfile.getPlayerUuid(), this.worldName, this.chunkX, this.chunkZ);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDAAgilityVisitedChunk() {
    }

    /**
     * Returns the stable chunk cache key.
     *
     * @return chunk cache key
     */
    public @NotNull String getChunkKey() {
        return this.chunkKey;
    }

    /**
     * Returns the stable persisted chunk key for the supplied player and chunk coordinates.
     *
     * @param playerUuid owning player UUID
     * @param worldName world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return stable chunk key
     */
    public static @NotNull String toChunkKey(
        final @NotNull UUID playerUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        return Objects.requireNonNull(playerUuid, "playerUuid")
            + ":" + Objects.requireNonNull(worldName, "worldName")
            + ":" + chunkX
            + ":" + chunkZ;
    }
}
