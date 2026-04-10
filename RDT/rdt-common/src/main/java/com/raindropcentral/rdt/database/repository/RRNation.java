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

import com.raindropcentral.rdt.database.entity.NationStatus;
import com.raindropcentral.rdt.database.entity.RNation;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Cached repository for persisted {@link RNation} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRNation extends CachedRepository<RNation, Long, UUID> {

    /**
     * Creates the nation repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     */
    public RRNation(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RNation.class, RNation::getNationUuid);
    }

    /**
     * Finds a nation by UUID.
     *
     * @param nationUuid nation UUID
     * @return matching nation, or {@code null} when none exists
     */
    public @Nullable RNation findByNationUuid(final @NotNull UUID nationUuid) {
        return this.findByAttributes(java.util.Map.of("nationUuid", nationUuid)).orElse(null);
    }

    /**
     * Finds a nation by name using case-insensitive matching.
     *
     * @param nationName nation name
     * @return matching nation, or {@code null} when none exists
     */
    public @Nullable RNation findByNationName(final @NotNull String nationName) {
        final String normalizedLookup = RNation.normalizeNationLookupName(nationName);
        return this.findAll().stream()
            .filter(nation -> nation.getNationName().trim().toLowerCase(Locale.ROOT).equals(normalizedLookup))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all pending nation proposals initiated by one town.
     *
     * @param initiatingTownUuid initiating town UUID
     * @return matching pending nations
     */
    public @NotNull List<RNation> findPendingByInitiatingTownUuid(final @NotNull UUID initiatingTownUuid) {
        return this.findAll().stream()
            .filter(nation -> nation.getStatus() == NationStatus.PENDING)
            .filter(nation -> java.util.Objects.equals(nation.getInitiatingTownUuid(), initiatingTownUuid))
            .toList();
    }

    /**
     * Finds every pending nation.
     *
     * @return pending nations
     */
    public @NotNull List<RNation> findPending() {
        return this.findAll().stream()
            .filter(nation -> nation.getStatus() == NationStatus.PENDING)
            .toList();
    }
}
