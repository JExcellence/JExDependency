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

import com.raindropcentral.rdt.database.entity.RTownRelationship;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Cached repository for persisted {@link RTownRelationship} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRTownRelationship extends CachedRepository<RTownRelationship, Long, String> {

    /**
     * Creates the town-relationship repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     */
    public RRTownRelationship(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RTownRelationship.class, RTownRelationship::getPairKey);
    }

    /**
     * Finds one persisted relationship by unordered town pair.
     *
     * @param leftTownUuid first town UUID
     * @param rightTownUuid second town UUID
     * @return matching relationship, or {@code null} when none exists
     */
    public @Nullable RTownRelationship findByTownPair(
        final @NotNull UUID leftTownUuid,
        final @NotNull UUID rightTownUuid
    ) {
        return this.findAll().stream()
            .filter(relationship -> relationship.matchesTownPair(leftTownUuid, rightTownUuid))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds every persisted relationship involving one town.
     *
     * @param townUuid town UUID to inspect
     * @return immutable list of matching town relationships
     */
    public @NotNull List<RTownRelationship> findByTownUuid(final @NotNull UUID townUuid) {
        return this.findAll().stream()
            .filter(relationship -> relationship.containsTown(townUuid))
            .toList();
    }
}
