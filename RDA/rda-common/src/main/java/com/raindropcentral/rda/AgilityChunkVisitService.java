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

package com.raindropcentral.rda;

import com.raindropcentral.rda.database.entity.RDAAgilityVisitedChunk;
import com.raindropcentral.rda.database.entity.RDAPlayer;
import com.raindropcentral.rda.database.repository.RRDAAgilityVisitedChunk;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks persisted one-time agility chunk bonuses per player.
 *
 * <p>The service keeps an in-memory cache backed by a child table so exploration bonuses survive
 * restarts while remaining cheap to query during movement events.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class AgilityChunkVisitService {

    private final RRDAPlayer playerRepository;
    private final RRDAAgilityVisitedChunk agilityVisitedChunkRepository;
    private final Logger logger;
    private final Set<String> visitedChunkKeys = ConcurrentHashMap.newKeySet();

    /**
     * Creates an agility chunk visit service.
     *
     * @param playerRepository root player repository
     * @param agilityVisitedChunkRepository agility chunk repository
     * @param logger logger used for persistence failures
     */
    public AgilityChunkVisitService(
        final @NotNull RRDAPlayer playerRepository,
        final @NotNull RRDAAgilityVisitedChunk agilityVisitedChunkRepository,
        final @NotNull Logger logger
    ) {
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.agilityVisitedChunkRepository = Objects.requireNonNull(
            agilityVisitedChunkRepository,
            "agilityVisitedChunkRepository"
        );
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Hydrates the in-memory cache from persisted chunk rows.
     */
    public void initialize() {
        for (final RDAAgilityVisitedChunk visitedChunk : this.agilityVisitedChunkRepository.findAll()) {
            this.visitedChunkKeys.add(visitedChunk.getChunkKey());
        }
    }

    /**
     * Marks the supplied chunk as visited for the player when it has not been seen before.
     *
     * @param player player exploring the chunk
     * @param chunk chunk being entered
     * @return {@code true} when the chunk was newly visited
     */
    public boolean markVisited(final @NotNull Player player, final @NotNull Chunk chunk) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(chunk, "chunk");

        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player);
        final String chunkKey = RDAAgilityVisitedChunk.toChunkKey(
            playerProfile.getPlayerUuid(),
            chunk.getWorld().getName(),
            chunk.getX(),
            chunk.getZ()
        );
        if (!this.visitedChunkKeys.add(chunkKey)) {
            return false;
        }

        final RDAAgilityVisitedChunk pendingChunk = new RDAAgilityVisitedChunk(playerProfile, chunk);
        this.agilityVisitedChunkRepository.createAsync(pendingChunk).exceptionally(throwable -> {
            this.visitedChunkKeys.remove(chunkKey);
            this.logger.warning(
                "Failed to persist agility chunk visit for " + chunkKey + ": " + throwable.getMessage()
            );
            return null;
        });
        return true;
    }

    private @NotNull RDAPlayer getOrCreatePlayerProfile(final @NotNull Player player) {
        return this.playerRepository.findOrCreateByPlayer(player.getUniqueId());
    }
}
