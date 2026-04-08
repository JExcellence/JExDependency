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

package com.raindropcentral.rdt.database.repository;

import com.raindropcentral.rdt.database.entity.RTown;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted {@link RTown} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRTown extends CachedRepository<RTown, Long, UUID> {

    /**
     * Creates the town repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRTown(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RTown> entityClass,
        final @NotNull Function<RTown, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a town by mayor UUID.
     *
     * @param mayorUuid mayor UUID
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown findByMayor(final @NotNull UUID mayorUuid) {
        return findByAttributes(Map.of("mayorUuid", mayorUuid)).orElse(null);
    }

    /**
     * Finds a town by town name.
     *
     * @param townName town name
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown findByTName(final @NotNull String townName) {
        return findByAttributes(Map.of("townName", townName.trim())).orElse(null);
    }

    /**
     * Finds a town by town UUID.
     *
     * @param townUuid town UUID
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown findByTownUUID(final @NotNull UUID townUuid) {
        return findByAttributes(Map.of("townUuid", townUuid)).orElse(null);
    }

    /**
     * Finds a town by town UUID.
     *
     * @param townUuid town UUID
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown findByTownUuid(final @NotNull UUID townUuid) {
        return this.findByTownUUID(townUuid);
    }

    /**
     * Finds a town by identifier.
     *
     * @param identifier town UUID
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown findByIdentifier(final @NotNull UUID identifier) {
        return this.findByTownUUID(identifier);
    }

    /**
     * Finds a town by exact nexus location.
     *
     * @param location nexus location
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown findByNexusLocation(final @NotNull Location location) {
        return this.findAll().stream()
            .filter(town -> sameLocation(town.getNexusLocation(), location))
            .findFirst()
            .orElse(null);
    }

    private static boolean sameLocation(final @Nullable Location left, final @NotNull Location right) {
        return left != null
            && left.getWorld() != null
            && right.getWorld() != null
            && Objects.equals(left.getWorld().getUID(), right.getWorld().getUID())
            && left.getBlockX() == right.getBlockX()
            && left.getBlockY() == right.getBlockY()
            && left.getBlockZ() == right.getBlockZ();
    }
}
