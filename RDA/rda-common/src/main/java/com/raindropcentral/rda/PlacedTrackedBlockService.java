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

import de.jexcellence.hibernate.entity.BaseEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Tracks placed blocks so only natural tracked blocks award XP.
 *
 * <p>The service keeps an in-memory cache backed by persisted rows, allowing suppression data to
 * survive plugin restarts while still providing constant-time lookups during block breaks.</p>
 *
 * @param <M> persisted placed-block marker type
 *
 * @author Codex
 * @since 1.0.0
 * @version 1.0.0
 */
public final class PlacedTrackedBlockService<M extends BaseEntity> {

    private final PlacedTrackedBlockRepository<M> repository;
    private final Set<Material> trackedMaterials;
    private final Logger logger;
    private final Function<Block, M> markerFactory;
    private final Function<M, String> locationKeyExtractor;
    private final ConcurrentMap<String, M> placedBlocks = new ConcurrentHashMap<>();

    /**
     * Creates a placed tracked-block tracking service.
     *
     * @param repository backing repository
     * @param trackedMaterials tracked materials for the owning skill
     * @param logger logger used for persistence failures
     * @param markerFactory factory creating new pending markers from blocks
     * @param locationKeyExtractor extractor resolving the stable marker location key
     * @throws NullPointerException if any argument is {@code null}
     */
    public PlacedTrackedBlockService(
        final @NotNull PlacedTrackedBlockRepository<M> repository,
        final @NotNull Set<Material> trackedMaterials,
        final @NotNull Logger logger,
        final @NotNull Function<Block, M> markerFactory,
        final @NotNull Function<M, String> locationKeyExtractor
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.trackedMaterials = Set.copyOf(Objects.requireNonNull(trackedMaterials, "trackedMaterials"));
        this.logger = Objects.requireNonNull(logger, "logger");
        this.markerFactory = Objects.requireNonNull(markerFactory, "markerFactory");
        this.locationKeyExtractor = Objects.requireNonNull(locationKeyExtractor, "locationKeyExtractor");
    }

    /**
     * Hydrates the in-memory cache from persisted placed-block rows.
     */
    public void initialize() {
        final List<M> persistedBlocks = this.repository.findAll();
        for (final M persistedBlock : persistedBlocks) {
            this.placedBlocks.put(this.locationKeyExtractor.apply(persistedBlock), persistedBlock);
        }
    }

    /**
     * Records a placed tracked block.
     *
     * @param block placed block
     */
    public void trackPlacedBlock(final @NotNull Block block) {
        Objects.requireNonNull(block, "block");
        if (!this.trackedMaterials.contains(block.getType())) {
            return;
        }

        final M pendingMarker = this.markerFactory.apply(block);
        final String locationKey = this.locationKeyExtractor.apply(pendingMarker);
        final M existingMarker = this.placedBlocks.putIfAbsent(locationKey, pendingMarker);
        if (existingMarker != null) {
            return;
        }

        this.repository.createAsync(pendingMarker).thenAccept(persistedMarker -> {
            final M currentMarker = this.placedBlocks.get(locationKey);
            if (currentMarker == null) {
                if (persistedMarker.getId() != null) {
                    this.repository.deleteAsync(persistedMarker.getId());
                }
                return;
            }

            if (currentMarker != pendingMarker) {
                if (persistedMarker.getId() != null) {
                    this.repository.deleteAsync(persistedMarker.getId());
                }
                return;
            }

            this.placedBlocks.replace(locationKey, pendingMarker, persistedMarker);
        }).exceptionally(throwable -> {
            this.placedBlocks.remove(locationKey, pendingMarker);
            this.logger.warning(
                "Failed to persist placed tracked block marker for " + locationKey + ": " + throwable.getMessage()
            );
            return null;
        });
    }

    /**
     * Removes and consumes a placed-block marker for the supplied block.
     *
     * @param block broken block
     * @return {@code true} when the block was player-placed and should not grant XP
     */
    public boolean consumePlacedBlock(final @NotNull Block block) {
        Objects.requireNonNull(block, "block");
        final String locationKey = this.locationKeyFromBlock(block);
        final M marker = this.placedBlocks.remove(locationKey);
        if (marker == null) {
            return false;
        }

        if (marker.getId() != null) {
            this.repository.deleteAsync(marker.getId()).exceptionally(throwable -> {
                this.logger.warning(
                    "Failed to delete placed tracked block marker for " + locationKey + ": " + throwable.getMessage()
                );
                return null;
            });
        }

        return true;
    }

    private @NotNull String locationKeyFromBlock(final @NotNull Block block) {
        final M marker = this.markerFactory.apply(block);
        return this.locationKeyExtractor.apply(marker);
    }
}
